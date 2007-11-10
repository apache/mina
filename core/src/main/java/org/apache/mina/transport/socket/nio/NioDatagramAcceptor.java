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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
import org.apache.mina.common.ExpiringSessionRecycler;
import org.apache.mina.common.IdleStatusChecker;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.WriteRequestQueue;
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

    private static final AtomicInteger id = new AtomicInteger();

    private final Executor executor;
    private final String threadName;
    private final Selector selector;
    private final IoProcessor<NioSession> processor = new DatagramAcceptorProcessor();
    private final Queue<ServiceOperationFuture> registerQueue = new ConcurrentLinkedQueue<ServiceOperationFuture>();
    private final Queue<ServiceOperationFuture> cancelQueue = new ConcurrentLinkedQueue<ServiceOperationFuture>();
    private final Queue<NioDatagramSession> flushingSessions = new ConcurrentLinkedQueue<NioDatagramSession>();
    private final Map<SocketAddress, DatagramChannel> serverChannels =
        Collections.synchronizedMap(new HashMap<SocketAddress, DatagramChannel>());

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    private Worker worker;
    private long lastIdleCheckTime;

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

        threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeIoException("Failed to open a selector.", e);
        }

        this.executor = executor;
    }

    private void disposeNow() {
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
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

        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
        for (DatagramChannel c: serverChannels.values()) {
            newLocalAddresses.add(c.socket().getLocalSocketAddress());
        }
        setLocalAddresses(newLocalAddresses);
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

    public IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        synchronized (bindLock) {
            if (!isActive()) {
                throw new IllegalStateException(
                        "Can't create a session from a unbound service.");
            }

            return newSessionWithoutLock(remoteAddress, localAddress);
        }
    }

    private IoSession newSessionWithoutLock(
            SocketAddress remoteAddress, SocketAddress localAddress) {
        Selector selector = this.selector;
        DatagramChannel ch = serverChannels.get(localAddress);
        if (ch == null) {
            throw new IllegalArgumentException("Unknown local address: " + localAddress);
        }
        SelectionKey key = ch.keyFor(selector);

        IoSession session;
        IoSessionRecycler sessionRecycler = getSessionRecycler();
        synchronized (sessionRecycler) {
            session = sessionRecycler.recycle(localAddress, remoteAddress);
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

        finishSessionInitialization(session, null);

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
            if (isActive()) {
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

    IoProcessor<NioSession> getProcessor() {
        return processor;
    }

    private class DatagramAcceptorProcessor implements IoProcessor<NioSession> {

        public void add(NioSession session) {
        }

        public void flush(NioSession session) {
            if (scheduleFlush((NioDatagramSession) session)) {
                Selector selector = NioDatagramAcceptor.this.selector;
                if (selector != null) {
                    selector.wakeup();
                }
            }
        }

        public void remove(NioSession session) {
            getSessionRecycler().remove(session);
            getListeners().fireSessionDestroyed(session);
        }

        public void updateTrafficMask(NioSession session) {
            throw new UnsupportedOperationException();
        }

        public void dispose() {
            // TODO Implement me.
        }
    }

    private  void startupWorker() {
        if (!selector.isOpen()) {
            registerQueue.clear();
            cancelQueue.clear();
            flushingSessions.clear();
            throw new ClosedSelectorException();
        }
        synchronized (this) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(
                        new NamePreservingRunnable(worker, threadName));
            }
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
            lastIdleCheckTime = System.currentTimeMillis();

            for (; ;) {
                try {
                    int nKeys = selector.select(1000);

                    registerNew();

                    if (nKeys > 0) {
                        processReadySessions(selector.selectedKeys());
                    }

                    flushSessions();
                    cancelKeys();

                    notifyIdleSessions();

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
            
            if (isDisposed()) {
                disposeNow();
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
        IoBuffer readBuf = IoBuffer.allocate(getSessionConfig()
                .getReceiveBufferSize());

        SocketAddress remoteAddress = channel.receive(readBuf.buf());
        if (remoteAddress != null) {
            IoSession session = newSessionWithoutLock(
                    remoteAddress, channel.socket().getLocalSocketAddress());

            readBuf.flip();

            IoBuffer newBuf = IoBuffer.allocate(readBuf.limit());
            newBuf.put(readBuf);
            newBuf.flip();

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
                if (flushedAll && !session.getWriteRequestQueue().isEmpty(session) &&
                    !session.isScheduledForFlush()) {
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
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();

        int writtenBytes = 0;
        int maxWrittenBytes = session.getConfig().getSendBufferSize() << 1;
        for (; ;) {
            WriteRequest req = session.getCurrentWriteRequest();
            if (req == null) {
                req = writeRequestQueue.poll(session);
                if (req == null) {
                    break;
                }
                session.setCurrentWriteRequest(req);
            }

            IoBuffer buf = (IoBuffer) req.getMessage();
            if (buf.remaining() == 0) {
                // Clear and fire event
                session.setCurrentWriteRequest(null);
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

                // Clear and fire event
                session.setCurrentWriteRequest(null);
                writtenBytes += localWrittenBytes;
                buf.reset();
                session.getFilterChain().fireMessageSent(req);
            }
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

            Map<SocketAddress, DatagramChannel> newServerChannels =
                new HashMap<SocketAddress, DatagramChannel>();
            List<SocketAddress> localAddresses = getLocalAddresses();

            try {
                for (SocketAddress a: localAddresses) {
                    DatagramChannel c = null;
                    boolean success = false;
                    try {
                        c = DatagramChannel.open();
                        DatagramSessionConfig cfg = getSessionConfig();
                        c.socket().setReuseAddress(cfg.isReuseAddress());
                        c.socket().setBroadcast(cfg.isBroadcast());
                        c.socket().setReceiveBufferSize(cfg.getReceiveBufferSize());
                        c.socket().setSendBufferSize(cfg.getSendBufferSize());
        
                        if (c.socket().getTrafficClass() != cfg.getTrafficClass()) {
                            c.socket().setTrafficClass(cfg.getTrafficClass());
                        }
        
                        c.configureBlocking(false);
                        c.socket().bind(a);
                        c.register(selector, SelectionKey.OP_READ, req);
                        success = true;
                    } finally {
                        if (c != null && !success) {
                            try {
                                c.disconnect();
                                c.close();
                            } catch (Throwable e) {
                                ExceptionMonitor.getInstance().exceptionCaught(e);
                            }
                        }
                    }
                    
                    newServerChannels.put(c.socket().getLocalSocketAddress(), c);
                }
                
                serverChannels.putAll(newServerChannels);
                
                getListeners().fireServiceActivated();
                req.setDone();
            } catch (Exception e) {
                req.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (req.getException() != null) {
                    for (DatagramChannel c: newServerChannels.values()) {
                        c.keyFor(selector).cancel();
                        try {
                            c.disconnect();
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

    private void cancelKeys() {
        for (; ;) {
            ServiceOperationFuture request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            // close the channels
            for (DatagramChannel c: serverChannels.values()) {
                try {
                    SelectionKey key = c.keyFor(selector);
                    key.cancel();

                    selector.wakeup(); // wake up again to trigger thread death
                    c.disconnect();
                    c.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
            
            serverChannels.clear();
            request.setDone();
        }
    }

    private void notifyIdleSessions() {
        // process idle sessions
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastIdleCheckTime >= 1000) {
            lastIdleCheckTime = currentTime;
            IdleStatusChecker.notifyIdleSessions(
                    getListeners().getManagedSessions().iterator(),
                    currentTime);
        }
    }
}
