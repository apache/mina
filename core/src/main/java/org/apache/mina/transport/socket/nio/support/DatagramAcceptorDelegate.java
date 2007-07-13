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
package org.apache.mina.transport.socket.nio.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.ExpiringSessionRecycler;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.IoServiceListenerSupport;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.DefaultDatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptorDelegate extends BaseIoAcceptor implements
        IoAcceptor, DatagramService {
    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();

    private static volatile int nextId = 0;

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    private final IoAcceptor wrapper;

    private final Executor executor;

    private final int id = nextId++;

    private final Selector selector;

    private DatagramChannel channel;

    private final Queue<RegistrationRequest> registerQueue = new ConcurrentLinkedQueue<RegistrationRequest>();

    private final Queue<CancellationRequest> cancelQueue = new ConcurrentLinkedQueue<CancellationRequest>();

    private final Queue<DatagramSessionImpl> flushingSessions = new ConcurrentLinkedQueue<DatagramSessionImpl>();

    private Worker worker;

    /**
     * Creates a new instance.
     */
    public DatagramAcceptorDelegate(IoAcceptor wrapper, Executor executor) {
        super(new DefaultDatagramSessionConfig());

        // The default reuseAddress should be 'true' for an accepted socket.
        getSessionConfig().setReuseAddress(true);

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to open a selector.", e);
        }

        this.wrapper = wrapper;
        this.executor = executor;
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

    public TransportType getTransportType() {
        return TransportType.DATAGRAM;
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    @Override
    protected void doBind() throws IOException {
        RegistrationRequest request = new RegistrationRequest();

        registerQueue.offer(request);
        startupWorker();
        selector.wakeup();

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        if (request.exception != null) {
            throw (IOException) new IOException("Failed to bind")
                    .initCause(request.exception);
        } else {
            setLocalAddress(channel.socket().getLocalSocketAddress());
        }
    }

    @Override
    protected void doUnbind() {
        CancellationRequest request = new CancellationRequest();

        cancelQueue.offer(request);
        startupWorker();
        selector.wakeup();

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        if (request.exception != null) {
            throw new RuntimeException("Failed to unbind", request.exception);
        }
    }

    public IoSession newSession(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        synchronized (bindLock) {
            if (!isBound()) {
                throw new IllegalStateException(
                        "Can't create a session from a unbound service.");
            }

            return newSessionWithoutLock(remoteAddress);
        }
    }

    private IoSession newSessionWithoutLock(SocketAddress remoteAddress) {
        Selector selector = this.selector;
        DatagramChannel ch = this.channel;
        SelectionKey key = ch.keyFor(selector);

        IoSession session;
        IoSessionRecycler sessionRecycler = getSessionRecycler();
        synchronized (sessionRecycler) {
            session = sessionRecycler.recycle(getLocalAddress(), remoteAddress);
            if (session != null) {
                return session;
            }

            // If a new session needs to be created.
            DatagramSessionImpl datagramSession = new DatagramSessionImpl(
                    wrapper, this, ch, getHandler(),
                    (InetSocketAddress) remoteAddress);
            datagramSession.setSelectionKey(key);

            getSessionRecycler().put(datagramSession);
            session = datagramSession;
        }

        try {
            buildFilterChain(session);
            getListeners().fireSessionCreated(session);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
        }

        return session;
    }

    public IoSessionRecycler getSessionRecycler() {
        return sessionRecycler;
    }

    public void setSessionRecycler(IoSessionRecycler sessionRecycler) {
        synchronized (bindLock) {
            if (isBound()) {
                throw new IllegalStateException(
                        "sessionRecycler can't be set while the acceptor is bound.");
            }

            if (sessionRecycler == null) {
                sessionRecycler = DEFAULT_RECYCLER;
            }
            this.sessionRecycler = sessionRecycler;
        }
    }

    @Override
    public IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }

    private void buildFilterChain(IoSession session) throws Exception {
        this.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
    }

    private synchronized void startupWorker() {
        if (worker == null) {
            worker = new Worker();
            executor.execute(new NamePreservingRunnable(worker));
        }
    }

    public void flushSession(DatagramSessionImpl session) {
        scheduleFlush(session);
        Selector selector = this.selector;
        if (selector != null) {
            selector.wakeup();
        }
    }

    public void closeSession(DatagramSessionImpl session) {
    }

    private void scheduleFlush(DatagramSessionImpl session) {
        flushingSessions.offer(session);
    }

    private class Worker implements Runnable {
        public void run() {
            Thread.currentThread().setName("DatagramAcceptor-" + id);

            for (;;) {
                try {
                    int nKeys = selector.select();

                    registerNew();

                    if (nKeys > 0) {
                        processReadySessions(selector.selectedKeys());
                    }

                    flushSessions();
                    cancelKeys();

                    if (selector.keys().isEmpty()) {
                        synchronized (DatagramAcceptorDelegate.this) {
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
                    }
                }
            }
        }
    }

    private void processReadySessions(Set<SelectionKey> keys) {
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            DatagramChannel ch = (DatagramChannel) key.channel();

            try {
                if (key.isReadable()) {
                    readSession(ch);
                }

                if (key.isWritable()) {
                    for (IoSession session : getManagedSessions()) {
                        scheduleFlush((DatagramSessionImpl) session);
                    }
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            }
        }
    }

    private void readSession(DatagramChannel channel) throws Exception {
        ByteBuffer readBuf = ByteBuffer.allocate(getSessionConfig()
                .getReceiveBufferSize());

        SocketAddress remoteAddress = channel.receive(readBuf.buf());
        if (remoteAddress != null) {
            DatagramSessionImpl session = (DatagramSessionImpl) newSessionWithoutLock(remoteAddress);

            readBuf.flip();

            ByteBuffer newBuf = ByteBuffer.allocate(readBuf.limit());
            newBuf.put(readBuf);
            newBuf.flip();

            session.increaseReadBytes(newBuf.remaining());
            session.getFilterChain().fireMessageReceived(session, newBuf);
        }
    }

    private void flushSessions() {
        for (;;) {
            DatagramSessionImpl session = flushingSessions.poll();
            if (session == null) {
                break;
            }

            try {
                flush(session);
            } catch (IOException e) {
                session.getFilterChain().fireExceptionCaught(session, e);
            }
        }
    }

    private void flush(DatagramSessionImpl session) throws IOException {
        DatagramChannel ch = session.getChannel();

        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();

        WriteRequest req;
        for (;;) {
            synchronized (writeRequestQueue) {
                req = writeRequestQueue.peek();
            }

            if (req == null) {
                break;
            }

            ByteBuffer buf = (ByteBuffer) req.getMessage();
            if (buf.remaining() == 0) {
                // pop and fire event
                synchronized (writeRequestQueue) {
                    writeRequestQueue.poll();
                }

                session.increaseWrittenMessages();
                buf.reset();
                ((DatagramFilterChain) session.getFilterChain())
                        .fireMessageSent(session, req);
                continue;
            }

            SelectionKey key = session.getSelectionKey();
            if (key == null) {
                scheduleFlush(session);
                break;
            }
            if (!key.isValid()) {
                continue;
            }

            SocketAddress destination = req.getDestination();
            if (destination == null) {
                destination = session.getRemoteAddress();
            }

            int writtenBytes = ch.send(buf.buf(), destination);

            if (writtenBytes == 0) {
                // Kernel buffer is full
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            } else if (writtenBytes > 0) {
                key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));

                // pop and fire event
                synchronized (writeRequestQueue) {
                    writeRequestQueue.poll();
                }

                session.increaseWrittenBytes(writtenBytes);
                session.increaseWrittenMessages();
                buf.reset();
                session.getFilterChain().fireMessageSent(session, req);
            }
        }
    }

    private void registerNew() {
        if (registerQueue.isEmpty()) {
            return;
        }

        for (;;) {
            RegistrationRequest req = registerQueue.poll();
            if (req == null) {
                break;
            }

            DatagramChannel ch = null;
            try {
                ch = DatagramChannel.open();
                DatagramSessionConfig cfg = getSessionConfig();
                ch.socket().setReuseAddress(cfg.isReuseAddress());
                ch.socket().setBroadcast(cfg.isBroadcast());
                ch.socket().setReceiveBufferSize(cfg.getReceiveBufferSize());
                ch.socket().setSendBufferSize(cfg.getSendBufferSize());

                if (ch.socket().getTrafficClass() != cfg.getTrafficClass()) {
                    ch.socket().setTrafficClass(cfg.getTrafficClass());
                }

                ch.configureBlocking(false);
                ch.socket().bind(getLocalAddress());
                ch.register(selector, SelectionKey.OP_READ, req);
                this.channel = ch;

                getListeners().fireServiceActivated();
            } catch (Throwable t) {
                req.exception = t;
            } finally {
                synchronized (req) {
                    req.done = true;
                    req.notify();
                }

                if (ch != null && req.exception != null) {
                    try {
                        ch.disconnect();
                        ch.close();
                    } catch (Throwable e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            }
        }
    }

    private void cancelKeys() {
        for (;;) {
            CancellationRequest request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            DatagramChannel ch = this.channel;
            this.channel = null;

            // close the channel
            try {
                SelectionKey key = ch.keyFor(selector);
                key.cancel();
                selector.wakeup(); // wake up again to trigger thread death
                ch.disconnect();
                ch.close();
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            } finally {
                synchronized (request) {
                    request.done = true;
                    request.notify();
                }

                if (request.exception == null) {
                    getListeners().fireServiceDeactivated();
                }
            }
        }
    }

    public void updateTrafficMask(DatagramSessionImpl session) {
        // There's no point in changing the traffic mask for sessions originating
        // from this acceptor since new sessions are created every time data is
        // received.
    }

    private static class RegistrationRequest {
        private Throwable exception;

        private boolean done;
    }

    private static class CancellationRequest {
        private boolean done;

        private RuntimeException exception;
    }
}
