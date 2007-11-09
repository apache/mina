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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AbstractIoAcceptor;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.SimpleIoProcessorPool;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class NioSocketAcceptor extends AbstractIoAcceptor implements SocketAcceptor {

    private static final AtomicInteger id = new AtomicInteger();

    private int backlog = 50;
    private boolean reuseAddress = true;

    private final Executor executor;
    private final String threadName;
    private final IoProcessor<NioSession> processor;
    private final boolean createdProcessor;

    private final Object lock = new Object();

    private final Queue<ServiceOperationFuture> registerQueue =
        new ConcurrentLinkedQueue<ServiceOperationFuture>();
    private final Queue<ServiceOperationFuture> cancelQueue =
        new ConcurrentLinkedQueue<ServiceOperationFuture>();

    private final Map<SocketAddress, ServerSocketChannel> serverChannels =
        Collections.synchronizedMap(new HashMap<SocketAddress, ServerSocketChannel>());
    private final Selector selector;
    private Worker worker;

    /**
     * Create an acceptor with a single processing thread using a NewThreadExecutor
     */
    public NioSocketAcceptor() {
        this(null, new SimpleIoProcessorPool<NioSession>(NioProcessor.class), true);
    }

    public NioSocketAcceptor(int processorCount) {
        this(null, new SimpleIoProcessorPool<NioSession>(NioProcessor.class, processorCount), true);
    }
    
    public NioSocketAcceptor(IoProcessor<NioSession> processor) {
        this(null, processor, false);
    }

    public NioSocketAcceptor(Executor executor, IoProcessor<NioSession> processor) {
        this(executor, processor, false);
    }

    private NioSocketAcceptor(Executor executor, IoProcessor<NioSession> processor, boolean createdProcessor) {
        super(new DefaultSocketSessionConfig());
        
        if (executor == null) {
            executor = new NewThreadExecutor();
        }
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        
        this.executor = executor;
        this.threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();
        this.processor = processor;
        this.createdProcessor = createdProcessor;
        
        // The default reuseAddress of an accepted socket should be 'true'.
        getSessionConfig().setReuseAddress(true);

        // Get the default configuration
        ServerSocket s = null;
        try {
            s = new ServerSocket();
            reuseAddress = s.getReuseAddress();
        } catch (IOException e) {
            throw new RuntimeIoException(
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
            disposeNow();
            throw new RuntimeIoException("Failed to open a selector.", e);
        }
    }
    
    private void disposeNow() {
        try {
            if (createdProcessor) {
                processor.dispose();
            }
        } finally {
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        setLocalAddress((SocketAddress) localAddress);
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.reuseAddress = reuseAddress;
        }
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound.");
            }

            this.backlog = backlog;
        }
    }

    @Override
    protected void doBind() throws Exception {
        ServiceOperationFuture request = new ServiceOperationFuture();

        // adds the Registration request to the queue for the Workers
        // to handle
        registerQueue.add(request);

        // creates an instance of a Worker and has the local
        // executor kick it off.
        startupWorker();

        selector.wakeup();

        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }

        // Update the local addresses.
        // setLocalAddresses() shouldn't be called from the worker thread
        // because of deadlock.
        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
        for (ServerSocketChannel c: serverChannels.values()) {
            newLocalAddresses.add(c.socket().getLocalSocketAddress());
        }
        setLocalAddresses(newLocalAddresses);
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
        if (!selector.isOpen()) {
            registerQueue.clear();
            cancelQueue.clear();
            throw new ClosedSelectorException();
        }
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();

                executor.execute(new NamePreservingRunnable(worker, threadName));
            }
        }
    }

    @Override
    protected void doUnbind() throws Exception {
        ServiceOperationFuture future = new ServiceOperationFuture();

        cancelQueue.add(future);
        startupWorker();
        selector.wakeup();

        future.awaitUninterruptibly();
        if (future.getException() != null) {
            throw future.getException();
        }
    }

    /**
     * This class is called by the startupWorker() method and is
     * placed into a NamePreservingRunnable class.
     */
    private class Worker implements Runnable {
        public void run() {
            for (;;) {
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
            
            if (isDisposed()) {
                disposeNow();
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
                    NioSocketSession session = new NioSocketSession(
                            NioSocketAcceptor.this, processor, ch);
                    
                    finishSessionInitialization(session, null);

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
        for (;;) {
            ServiceOperationFuture future = registerQueue.poll();
            if (future == null) {
                break;
            }

            Map<SocketAddress, ServerSocketChannel> newServerChannels =
                new HashMap<SocketAddress, ServerSocketChannel>();
            List<SocketAddress> localAddresses = getLocalAddresses();
            
            try {
                for (SocketAddress a: localAddresses) {
                    ServerSocketChannel c = null;
                    boolean success = false;
                    try {
                        c = ServerSocketChannel.open();
                        c.configureBlocking(false);
                        // Configure the server socket,
                        c.socket().setReuseAddress(isReuseAddress());
                        c.socket().setReceiveBufferSize(
                                getSessionConfig().getReceiveBufferSize());
                        // and bind.
                        c.socket().bind(a, getBacklog());
                        c.register(selector, SelectionKey.OP_ACCEPT, future);
                        success = true;
                    } finally {
                        if (!success && c != null) {
                            try {
                                c.close();
                            } catch (IOException e) {
                                ExceptionMonitor.getInstance().exceptionCaught(e);
                            }
                        }
                    }
                    
                    newServerChannels.put(c.socket().getLocalSocketAddress(), c);
                }
                
                serverChannels.putAll(newServerChannels);

                // and notify.
                future.setDone();
            } catch (Exception e) {
                future.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (future.getException() != null) {
                    for (ServerSocketChannel c: newServerChannels.values()) {
                        c.keyFor(selector).cancel();
                        try {
                            c.close();
                        } catch (IOException e) {
                            ExceptionMonitor.getInstance().exceptionCaught(e);
                        }
                    }
                    selector.wakeup();
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
            ServiceOperationFuture future = cancelQueue.poll();
            if (future == null) {
                break;
            }

            // close the channels
            for (ServerSocketChannel c: serverChannels.values()) {
                try {
                    SelectionKey key = c.keyFor(selector);
                    key.cancel();

                    selector.wakeup(); // wake up again to trigger thread death
                    c.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
            
            serverChannels.clear();
            future.setDone();
        }
    }

    public IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }
}
