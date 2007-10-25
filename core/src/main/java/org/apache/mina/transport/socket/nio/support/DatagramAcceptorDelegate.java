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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.IoServiceListenerSupport;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.DatagramServiceConfig;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptorDelegate extends BaseIoAcceptor implements
        IoAcceptor, DatagramService {
    private static final AtomicInteger nextId = new AtomicInteger();

    private final Object lock = new Object();

    private final IoAcceptor wrapper;

    private final Executor executor;

    private final int id = nextId.getAndIncrement();

    private volatile Selector selector;

    private DatagramAcceptorConfig defaultConfig = new DatagramAcceptorConfig();

    private final Map<SocketAddress, DatagramChannel> channels = new ConcurrentHashMap<SocketAddress, DatagramChannel>();

    private final Queue<RegistrationRequest> registerQueue = new ConcurrentLinkedQueue<RegistrationRequest>();

    private final Queue<CancellationRequest> cancelQueue = new ConcurrentLinkedQueue<CancellationRequest>();

    private final Queue<DatagramSessionImpl> flushingSessions = new ConcurrentLinkedQueue<DatagramSessionImpl>();

    private Worker worker;

    /**
     * Creates a new instance.
     */
    public DatagramAcceptorDelegate(IoAcceptor wrapper, Executor executor) {
        this.wrapper = wrapper;
        this.executor = executor;

        // The default reuseAddress of an accepted socket should be 'true'.
        defaultConfig.getSessionConfig().setReuseAddress(true);
    }

    public void bind(SocketAddress address, IoHandler handler,
            IoServiceConfig config) throws IOException {
        if (handler == null)
            throw new NullPointerException("handler");
        if (config == null) {
            config = getDefaultConfig();
        }

        if (address != null && !(address instanceof InetSocketAddress))
            throw new IllegalArgumentException("Unexpected address type: "
                    + address.getClass());

        RegistrationRequest request = new RegistrationRequest(address, handler,
                config);
        
        synchronized (lock) {
            startupWorker();
            registerQueue.add(request);
            selector.wakeup();
        }

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }

        if (request.exception != null) {
            throw (IOException) new IOException("Failed to bind")
                    .initCause(request.exception);
        }
    }

    public void unbind(SocketAddress address) {
        if (address == null)
            throw new NullPointerException("address");

        CancellationRequest request = new CancellationRequest(address);
        synchronized (lock) {
            try {
                startupWorker();
            } catch (IOException e) {
                // IOException is thrown only when Worker thread is not
                // running and failed to open a selector.  We simply throw
                // IllegalArgumentException here because we can simply
                // conclude that nothing is bound to the selector.
                throw new IllegalArgumentException("Address not bound: " + address);
            }
    
            cancelQueue.add(request);
            selector.wakeup();
        }

        synchronized (request) {
            while (!request.done) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }

        if (request.exception != null) {
            throw new RuntimeException("Failed to unbind", request.exception);
        }
    }

    public void unbindAll() {
        List<SocketAddress> addresses = new ArrayList<SocketAddress>(channels
                .keySet());

        for (SocketAddress address : addresses) {
            unbind(address);
        }
    }

    @Override
    public IoSession newSession(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }

        Selector selector = this.selector;
        DatagramChannel ch = channels.get(localAddress);
        if (selector == null || ch == null) {
            throw new IllegalArgumentException("Unknown localAddress: "
                    + localAddress);
        }

        SelectionKey key = ch.keyFor(selector);
        if (key == null) {
            throw new IllegalArgumentException("Unknown localAddress: "
                    + localAddress);
        }

        RegistrationRequest req = (RegistrationRequest) key.attachment();
        IoSession session;
        IoSessionRecycler sessionRecycler = getSessionRecycler(req);
        synchronized (sessionRecycler) {
            session = sessionRecycler.recycle(localAddress, remoteAddress);
            if (session != null) {
                return session;
            }

            // If a new session needs to be created.
            // Note that the local address is the service address in the
            // acceptor side, and I didn't call getLocalSocketAddress().
            // This avoids strange cases where getLocalSocketAddress() on the
            // underlying socket would return an IPv6 address while the
            // specified service address is an IPv4 address.
            DatagramSessionImpl datagramSession = new DatagramSessionImpl(
                    wrapper, this, req.config, ch, req.handler, req.address,
                    req.address);
            datagramSession.setRemoteAddress(remoteAddress);
            datagramSession.setSelectionKey(key);

            getSessionRecycler(req).put(datagramSession);
            session = datagramSession;
        }

        try {
            buildFilterChain(req, session);
            getListeners().fireSessionCreated(session);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
        }

        return session;
    }

    private IoSessionRecycler getSessionRecycler(RegistrationRequest req) {
        IoSessionRecycler sessionRecycler;
        if (req.config instanceof DatagramServiceConfig) {
            sessionRecycler = ((DatagramServiceConfig) req.config)
                    .getSessionRecycler();
        } else {
            sessionRecycler = defaultConfig.getSessionRecycler();
        }
        return sessionRecycler;
    }

    @Override
    public IoServiceListenerSupport getListeners() {
        return super.getListeners();
    }

    private void buildFilterChain(RegistrationRequest req, IoSession session)
            throws Exception {
        this.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
        req.config.getFilterChainBuilder().buildFilterChain(
                session.getFilterChain());
        req.config.getThreadModel().buildFilterChain(session.getFilterChain());
    }

    public DatagramAcceptorConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Sets the config this acceptor will use by default.
     *
     * @param defaultConfig the default config.
     * @throws NullPointerException if the specified value is <code>null</code>.
     */
    public void setDefaultConfig(DatagramAcceptorConfig defaultConfig) {
        if (defaultConfig == null) {
            throw new NullPointerException("defaultConfig");
        }
        this.defaultConfig = defaultConfig;
    }

    private void startupWorker() throws IOException {
        synchronized (lock) {
            if (worker == null) {
                selector = Selector.open();
                worker = new Worker();
                executor.execute(
                        new NamePreservingRunnable(worker, "DatagramAcceptor-" + id));
            }
        }
    }

    public void flushSession(DatagramSessionImpl session) {
        if (scheduleFlush(session)) {
            Selector selector = this.selector;
            if (selector != null) {
                selector.wakeup();
            }
        }
    }

    public void closeSession(DatagramSessionImpl session) {
    }

    private boolean scheduleFlush(DatagramSessionImpl session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        } else {
            return false;
        }
    }

    private class Worker implements Runnable {
        public void run() {
            Selector selector = DatagramAcceptorDelegate.this.selector;
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
                        synchronized (lock) {
                            if (selector.keys().isEmpty()
                                    && registerQueue.isEmpty()
                                    && cancelQueue.isEmpty()) {
                                worker = null;
                                try {
                                    selector.close();
                                } catch (IOException e) {
                                    ExceptionMonitor.getInstance()
                                            .exceptionCaught(e);
                                } finally {
                                    DatagramAcceptorDelegate.this.selector = null;
                                }
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
    }

    private void processReadySessions(Set<SelectionKey> keys) {
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            DatagramChannel ch = (DatagramChannel) key.channel();

            RegistrationRequest req = (RegistrationRequest) key.attachment();
            try {
                if (key.isReadable()) {
                    readSession(ch, req);
                }

                if (key.isWritable()) {
                    for (Object o : getManagedSessions(req.address)) {
                        scheduleFlush((DatagramSessionImpl) o);
                    }
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            }
        }
    }

    private void readSession(DatagramChannel channel, RegistrationRequest req)
            throws Exception {
        ByteBuffer readBuf = ByteBuffer
                .allocate(((DatagramSessionConfig) req.config
                        .getSessionConfig()).getReceiveBufferSize());
        try {
            SocketAddress remoteAddress = channel.receive(readBuf.buf());
            if (remoteAddress != null) {
                DatagramSessionImpl session = (DatagramSessionImpl) newSession(
                        remoteAddress, req.address);

                readBuf.flip();

                ByteBuffer newBuf = ByteBuffer.allocate(readBuf.limit());
                newBuf.put(readBuf);
                newBuf.flip();

                session.increaseReadBytes(newBuf.remaining());
                session.getFilterChain().fireMessageReceived(session, newBuf);
            }
        } finally {
            readBuf.release();
        }
    }

    private void flushSessions() {
        if (flushingSessions.size() == 0)
            return;

        for (;;) {
            DatagramSessionImpl session = flushingSessions.poll();

            if (session == null)
                break;

            session.setScheduledForFlush(false);

            try {
                boolean flushedAll = flush(session);
                if (flushedAll && !session.getWriteRequestQueue().isEmpty() && !session.isScheduledForFlush()) {
                    scheduleFlush(session);
                }
            } catch (IOException e) {
                session.getFilterChain().fireExceptionCaught(session, e);
            }
        }
    }

    private boolean flush(DatagramSessionImpl session) throws IOException {
        // Clear OP_WRITE
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            scheduleFlush(session);
            return false;
        }
        if (!key.isValid()) {
            return false;
        }
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));

        DatagramChannel ch = session.getChannel();
        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();

        int writtenBytes = 0;
        int maxWrittenBytes = ((DatagramSessionConfig) session.getConfig()).getSendBufferSize() << 1;
        try {
            for (;;) {
                WriteRequest req = writeRequestQueue.peek();
    
                if (req == null)
                    break;
    
                ByteBuffer buf = (ByteBuffer) req.getMessage();
                if (buf.remaining() == 0) {
                    // pop and fire event
                    writeRequestQueue.poll();
    
                    buf.reset();
                    
                    if (!buf.hasRemaining()) {
                        session.increaseWrittenMessages();
                    }
                    session.getFilterChain().fireMessageSent(session, req);
                    continue;
                }
    
                SocketAddress destination = req.getDestination();
                if (destination == null) {
                    destination = session.getRemoteAddress();
                }
    
                int localWrittenBytes = ch.send(buf.buf(), destination);
                writtenBytes += localWrittenBytes;
    
                if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
                    // Kernel buffer is full or wrote too much
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    return false;
                } else {
                    // pop and fire event
                    writeRequestQueue.poll();
    
                    buf.reset();
                    
                    if (!buf.hasRemaining()) {
                        session.increaseWrittenMessages();
                    }

                    session.getFilterChain().fireMessageSent(session, req);
                }
            }
        } finally {
            session.increaseWrittenBytes(writtenBytes);
        }
        
        return true;
    }

    private void registerNew() {
        if (registerQueue.isEmpty())
            return;

        Selector selector = this.selector;
        for (;;) {
            RegistrationRequest req = registerQueue.poll();

            if (req == null)
                break;

            DatagramChannel ch = null;
            try {
                ch = DatagramChannel.open();
                DatagramSessionConfig cfg;
                if (req.config.getSessionConfig() instanceof DatagramSessionConfig) {
                    cfg = (DatagramSessionConfig) req.config.getSessionConfig();
                } else {
                    cfg = getDefaultConfig().getSessionConfig();
                }

                ch.socket().setReuseAddress(cfg.isReuseAddress());
                ch.socket().setBroadcast(cfg.isBroadcast());
                ch.socket().setReceiveBufferSize(cfg.getReceiveBufferSize());
                ch.socket().setSendBufferSize(cfg.getSendBufferSize());

                if (ch.socket().getTrafficClass() != cfg.getTrafficClass()) {
                    ch.socket().setTrafficClass(cfg.getTrafficClass());
                }

                ch.configureBlocking(false);
                ch.socket().bind(req.address);
                if (req.address == null || req.address.getPort() == 0) {
                    req.address = (InetSocketAddress) ch.socket()
                            .getLocalSocketAddress();
                }
                ch.register(selector, SelectionKey.OP_READ, req);
                channels.put(req.address, ch);

                getListeners().fireServiceActivated(this, req.address,
                        req.handler, req.config);
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
        if (cancelQueue.isEmpty())
            return;

        Selector selector = this.selector;
        for (;;) {
            CancellationRequest request = cancelQueue.poll();

            if (request == null) {
                break;
            }

            DatagramChannel ch = channels.remove(request.address);

            // close the channel
            try {
                if (ch == null) {
                    request.exception = new IllegalArgumentException(
                            "Address not bound: " + request.address);
                } else {
                    SelectionKey key = ch.keyFor(selector);
                    request.registrationRequest = (RegistrationRequest) key
                            .attachment();
                    key.cancel();
                    selector.wakeup(); // wake up again to trigger thread death
                    ch.disconnect();
                    ch.close();
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            } finally {
                synchronized (request) {
                    request.done = true;
                    request.notify();
                }

                if (request.exception == null) {
                    getListeners().fireServiceDeactivated(this,
                            request.address,
                            request.registrationRequest.handler,
                            request.registrationRequest.config);
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
        private InetSocketAddress address;

        private final IoHandler handler;

        private final IoServiceConfig config;

        private Throwable exception;

        private boolean done;

        private RegistrationRequest(SocketAddress address, IoHandler handler,
                IoServiceConfig config) {
            this.address = (InetSocketAddress) address;
            this.handler = handler;
            this.config = config;
        }
    }

    private static class CancellationRequest {
        private final SocketAddress address;

        private boolean done;

        private RegistrationRequest registrationRequest;

        private RuntimeException exception;

        private CancellationRequest(SocketAddress address) {
            this.address = address;
        }
    }
}
