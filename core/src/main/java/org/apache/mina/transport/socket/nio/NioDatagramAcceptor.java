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
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoAcceptor;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.ExpiringSessionRecycler;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class NioDatagramAcceptor extends AbstractIoAcceptor implements DatagramAcceptor {
    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();

    private static volatile int nextId = 0;

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    private final Executor executor;

    private final int id = nextId++;

    private final Selector selector;

    private final IoProcessor processor = new DatagramAcceptorProcessor();

    private DatagramChannel channel;

    private final Queue<ServiceOperationFuture> registerQueue = new ConcurrentLinkedQueue<ServiceOperationFuture>();

    private final Queue<ServiceOperationFuture> cancelQueue = new ConcurrentLinkedQueue<ServiceOperationFuture>();

    private final Queue<NioDatagramSession> flushingSessions = new ConcurrentLinkedQueue<NioDatagramSession>();

    private Worker worker;

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptor() {
        this(new NewThreadExecutor());
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptor(Executor executor) {
        super(new DefaultDatagramSessionConfig());

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }

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

    public TransportMetadata getTransportMetadata() {
        return NioDatagramSession.METADATA;
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }
    
    public void setLocalAddress(InetSocketAddress localAddress) {
        setLocalAddress((SocketAddress) localAddress);
    }

    @Override
    protected void doBind() throws Exception {
        ServiceOperationFuture request = new ServiceOperationFuture();

        registerQueue.add(request);
        startupWorker();
        selector.wakeup();

        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }
        
        setLocalAddress(channel.socket().getLocalSocketAddress());
    }

    @Override
    protected void doUnbind() throws Exception {
        ServiceOperationFuture request = new ServiceOperationFuture();

        cancelQueue.add(request);
        startupWorker();
        selector.wakeup();

        request.awaitUninterruptibly();
        
        if (request.getException() != null) {
            throw request.getException();
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
            NioDatagramSession datagramSession = new NioDatagramSession(
                    this, ch, processor, remoteAddress);
            datagramSession.setSelectionKey(key);

            getSessionRecycler().put(datagramSession);
            session = datagramSession;
        }

        try {
            this.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
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
    protected IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }

    IoProcessor getProcessor() {
        return processor;
    }

    private class DatagramAcceptorProcessor implements IoProcessor {

        public void add(IoSession session) {
        }

        public void flush(IoSession session) {
            if (scheduleFlush((NioDatagramSession) session)) {
                Selector selector = NioDatagramAcceptor.this.selector;
                if (selector != null) {
                    selector.wakeup();
                }
            }
        }

        public void remove(IoSession session) {
            getListeners().fireSessionDestroyed(session);
        }

        public void updateTrafficMask(IoSession session) {
        }
    }

    private synchronized void startupWorker() {
        if (worker == null) {
            worker = new Worker();
            executor.execute(new NamePreservingRunnable(worker));
        }
    }

    private boolean scheduleFlush(NioDatagramSession session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        } else {
            return false;
        }
    }

    private class Worker implements Runnable {
        public void run() {
            Thread.currentThread().setName("DatagramAcceptor-" + id);

            for (; ;) {
                try {
                    int nKeys = selector.select();

                    registerNew();

                    if (nKeys > 0) {
                        processReadySessions(selector.selectedKeys());
                    }

                    flushSessions();
                    cancelKeys();

                    if (selector.keys().isEmpty()) {
                        synchronized (NioDatagramAcceptor.this) {
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
                        scheduleFlush((NioDatagramSession) session);
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
            NioDatagramSession session = (NioDatagramSession) newSessionWithoutLock(remoteAddress);

            readBuf.flip();

            ByteBuffer newBuf = ByteBuffer.allocate(readBuf.limit());
            newBuf.put(readBuf);
            newBuf.flip();

            session.increaseReadBytes(newBuf.remaining());
            session.getFilterChain().fireMessageReceived(newBuf);
        }
    }

    private void flushSessions() {
        for (; ;) {
            NioDatagramSession session = flushingSessions.poll();
            if (session == null) {
                break;
            }

            session.setScheduledForFlush(false);

            try {
                boolean flushedAll = flush(session);
                if (flushedAll && !session.getWriteRequestQueue().isEmpty() && !session.isScheduledForFlush()) {
                    scheduleFlush(session);
                }
            } catch (IOException e) {
                session.getFilterChain().fireExceptionCaught(e);
            }
        }
    }

    private boolean flush(NioDatagramSession session) throws IOException {
        // Clear OP_WRITE
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            scheduleFlush(session);
            return false;
        }
        if (!key.isValid()) {
            return false;
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

        DatagramChannel ch = session.getChannel();
        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();

        int writtenBytes = 0;
        int maxWrittenBytes = session.getConfig().getSendBufferSize() << 1;
        try {
            for (; ;) {
                WriteRequest req = writeRequestQueue.peek();
                if (req == null) {
                    break;
                }

                ByteBuffer buf = (ByteBuffer) req.getMessage();
                if (buf.remaining() == 0) {
                    // pop and fire event
                    writeRequestQueue.poll();
                    session.increaseWrittenMessages();
                    buf.reset();
                    session.getFilterChain().fireMessageSent(req);
                    continue;
                }

                SocketAddress destination = req.getDestination();
                if (destination == null) {
                    destination = session.getRemoteAddress();
                }

                int localWrittenBytes = ch.send(buf.buf(), destination);
                if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
                    // Kernel buffer is full or wrote too much
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    return false;
                } else {
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

                    // pop and fire event
                    writeRequestQueue.poll();
                    writtenBytes += localWrittenBytes;
                    session.increaseWrittenMessages();
                    buf.reset();
                    session.getFilterChain().fireMessageSent(req);
                }
            }
        } finally {
            session.increaseWrittenBytes(writtenBytes);
        }

        return true;
    }

    private void registerNew() {
        if (registerQueue.isEmpty()) {
            return;
        }

        for (; ;) {
            ServiceOperationFuture req = registerQueue.poll();
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
                req.setDone();
            } catch (Exception e) {
                req.setException(e);
            } finally {
                if (ch != null && req.getException() != null) {
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
        for (; ;) {
            ServiceOperationFuture request = cancelQueue.poll();
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
                getListeners().fireServiceDeactivated();
                request.setDone();
            }
        }
    }
}
