/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoAcceptor;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class SocketAcceptor extends AbstractIoAcceptor {
    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private int backlog = 50;

    private boolean reuseAddress;

    private final Executor executor;

    private final Object lock = new Object();

    private final int id = nextId++;

    private final String threadName = "SocketAcceptor-" + id;

    private ServerSocketChannel serverSocketChannel;

    private final Queue<RegistrationRequest> registerQueue = new ConcurrentLinkedQueue<RegistrationRequest>();

    private final Queue<CancellationRequest> cancelQueue = new ConcurrentLinkedQueue<CancellationRequest>();

    private final SocketIoProcessor[] ioProcessors;

    private final int processorCount;

    private final Selector selector;

    private Worker worker;

    private int processorDistributor = 0;

    /**
     * Create an acceptor with a single processing thread using a NewThreadExecutor
     */
    public SocketAcceptor() {
        this(new NewThreadExecutor());
    }

    /**
     * Creates an acceptor with a processing thread count set to the
     * number of available processors + 1 and the submitted executor
     *
     * @param executor Executor to use for launching threads
     */
    public SocketAcceptor(Executor executor) {
        this(Runtime.getRuntime().availableProcessors() + 1, executor);
    }

    /**
     * Create an acceptor with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor       Executor to use for launching threads
     */
    public SocketAcceptor(int processorCount, Executor executor) {
        super(new DefaultSocketSessionConfig());

        // The default reuseAddress of an accepted socket should be 'true'.
        getSessionConfig().setReuseAddress(true);

        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        // Get the default configuration
        ServerSocket s = null;
        try {
            s = new ServerSocket();
            reuseAddress = s.getReuseAddress();
        } catch (IOException e) {
            throw new RuntimeIOException(
                    "Failed to get the default configuration.", e);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to open a selector.", e);
        }

        // Set other properties and initialize
        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new SocketIoProcessor[processorCount];

        // create an array of SocketIoProcessors that will be used for
        // handling sessions.
        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new SocketIoProcessor(
                    "SocketAcceptorIoProcessor-" + id + "." + i, executor);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            selector.close();
        } catch (IOException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
    }

    /**
     * @see org.apache.mina.common.IoService#getTransportMetadata()
     */
    public TransportMetadata getTransportMetadata() {
        return SocketSessionImpl.METADATA;
    }

    /**
     * @see org.apache.mina.common.AbstractIoService#getSessionConfig()
     */
    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }

    /**
     * @see org.apache.mina.common.AbstractIoAcceptor#getLocalAddress()
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    // This method is overriden to work around a problem with
    // bean property access mechanism.

    /**
     * @see org.apache.mina.common.AbstractIoAcceptor#setLocalAddress(java.net.SocketAddress)
     */
    @Override
    public void setLocalAddress(SocketAddress localAddress) {
        super.setLocalAddress(localAddress);
    }

    /**
     * @see ServerSocket#getReuseAddress()
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isBound()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.reuseAddress = reuseAddress;
        }
    }

    /**
     * Returns the size of the backlog.
     *
     * @return
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Sets the size of the backlog.  This can only be done when this
     * class is not bound
     *
     * @param backlog
     */
    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isBound()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.backlog = backlog;
        }
    }

    @Override
    protected void doBind() throws IOException {
        RegistrationRequest request = new RegistrationRequest();

        // adds the Registration request to the queue for the Workers
        // to handle
        registerQueue.add(request);

        // creates an instance of a Worker and has the local
        // executor kick it off.
        startupWorker();

        selector.wakeup();

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        if (request.exception != null) {
            // TODO better exception handling.
            if (request.exception instanceof RuntimeException) {
                throw (RuntimeException) request.exception;
            } else if (request.exception instanceof IOException) {
                throw (IOException) request.exception;
            } else {
                throw new RuntimeIOException(request.exception);
            }
        } else {
            // Update the local address.
            // setLocalAddress() shouldn't be called from the worker thread
            // because of deadlock.
            setLocalAddress(serverSocketChannel.socket()
                    .getLocalSocketAddress());
        }
    }

    /**
     * This method is called by the doBind() and doUnbind()
     * methods.  If the worker object is not null, presumably
     * the acceptor is starting up, then the worker object will
     * be created and kicked off by the executor.  If the worker
     * object is not null, probably already created and this class
     * is now working, then nothing will happen and the method
     * will just return.
     */
    private void startupWorker() {
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();

                executor.execute(new NamePreservingRunnable(worker));
            }
        }
    }

    @Override
    protected void doUnbind() {
        CancellationRequest request = new CancellationRequest();

        cancelQueue.add(request);
        startupWorker();
        selector.wakeup();

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        if (request.exception != null) {
            request.exception.fillInStackTrace();

            throw request.exception;
        }
    }

    /**
     * This class is called by the startupWorker() method and is
     * placed into a NamePreservingRunnable class.
     */
    private class Worker implements Runnable {
        public void run() {
            Thread.currentThread().setName(SocketAcceptor.this.threadName);

            for (; ;) {
                try {
                    // gets the number of keys that are ready to go
                    int nKeys = selector.select();

                    // this actually sets the selector to OP_ACCEPT,
                    // and binds to the port in which this class will
                    // listen on
                    registerNew();

                    if (nKeys > 0) {
                        processSessions(selector.selectedKeys());
                    }

                    // check to see if any cancellation request has been made.
                    cancelKeys();

                    if (selector.keys().isEmpty()) {
                        synchronized (lock) {
                            if (selector.keys().isEmpty()
                                    && registerQueue.isEmpty()
                                    && cancelQueue.isEmpty()) {
                                worker = null;
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
        }

        /**
         * This method will process new sessions for the Worker class.  All
         * keys that have had their status updates as per the Selector.selectedKeys()
         * method will be processed here.  Only keys that are ready to accept
         * connections are handled here.
         * <p/>
         * Session objects are created by making new instances of SocketSessionImpl
         * and passing the session object to the SocketIoProcessor class.
         */
        private void processSessions(Set<SelectionKey> keys) throws IOException {
            Iterator<SelectionKey> it = keys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();

                it.remove();

                if (!key.isAcceptable()) {
                    continue;
                }

                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();

                // accept the connection from the client
                SocketChannel ch = ssc.accept();

                if (ch == null) {
                    continue;
                }

                boolean success = false;
                try {
                    // Create a new session object.  This class extends
                    // BaseIoSession and is custom for socket-based sessions.
                    SocketSessionImpl session = new SocketSessionImpl(
                            SocketAcceptor.this, nextProcessor(), ch);

                    // build the list of filters for this session.
                    getFilterChainBuilder().buildFilterChain(
                            session.getFilterChain());

                    // add the session to the SocketIoProcessor
                    session.getProcessor().add(session);
                    success = true;
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);
                } finally {
                    if (!success) {
                        ch.close();
                    }
                }
            }
        }
    }

    private SocketIoProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    /**
     * Sets up the socket communications.  Sets items such as:
     * <p/>
     * Blocking
     * Reuse address
     * Receive buffer size
     * Bind to listen port
     * Registers OP_ACCEPT for selector
     */
    private void registerNew() {
        for (; ;) {
            RegistrationRequest req = registerQueue.poll();
            if (req == null) {
                break;
            }

            ServerSocketChannel ssc = null;

            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);

                // Configure the server socket,
                ssc.socket().setReuseAddress(isReuseAddress());
                ssc.socket().setReceiveBufferSize(
                        getSessionConfig().getReceiveBufferSize());

                // and bind.
                ssc.socket().bind(getLocalAddress(), getBacklog());
                ssc.register(selector, SelectionKey.OP_ACCEPT, req);

                serverSocketChannel = ssc;

                // and notify.
                getListeners().fireServiceActivated();
            } catch (Throwable e) // TODO better exception handling.
            {
                req.exception = e;
            } finally {
                synchronized (req) {
                    req.done = true;

                    req.notifyAll();
                }

                if (ssc != null && req.exception != null) {
                    try {
                        ssc.close();
                    } catch (IOException e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        }
    }

    /**
     * This method just checks to see if anything has been placed into the
     * cancellation queue.  The only thing that should be in the cancelQueue
     * is CancellationRequest objects and the only place this happens is in
     * the doUnbind() method.
     */
    private void cancelKeys() {
        for (; ;) {
            CancellationRequest request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            // close the channel
            try {
                SelectionKey key = serverSocketChannel.keyFor(selector);
                key.cancel();

                selector.wakeup(); // wake up again to trigger thread death

                serverSocketChannel.close();
                serverSocketChannel = null;
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            } finally {
                synchronized (request) {
                    request.done = true;
                    request.notifyAll();
                }

                if (request.exception == null) {
                    getListeners().fireServiceDeactivated();
                }
            }
        }
    }

    /**
     * Class that triggers registration, or startup, of this class
     */
    private static class RegistrationRequest {
        private Throwable exception;

        private boolean done;
    }

    /**
     * Class that triggers a signal to unbind.
     */
    private static class CancellationRequest {
        private boolean done;

        private RuntimeException exception;
    }

    /**
     * @see org.apache.mina.common.IoAcceptor#newSession(java.net.SocketAddress)
     */
    public IoSession newSession(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }
}
