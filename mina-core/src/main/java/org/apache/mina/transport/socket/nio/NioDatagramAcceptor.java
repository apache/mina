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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
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
import java.util.concurrent.Semaphore;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;
import org.apache.mina.util.ExceptionMonitor;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public final class NioDatagramAcceptor extends AbstractIoAcceptor implements DatagramAcceptor, IoProcessor<NioSession> {
    /**
     * A session recycler that is used to retrieve an existing session, unless it's too old.
     **/
    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();

    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     */
    private static final long SELECT_TIMEOUT = 1000L;

    /** A lock used to protect the selector to be waked up before it's created */
    private final Semaphore lock = new Semaphore(1);

    /** A queue used to store the list of pending Binds */
    private final Queue<AcceptorOperationFuture> registerQueue = new ConcurrentLinkedQueue<AcceptorOperationFuture>();

    private final Queue<AcceptorOperationFuture> cancelQueue = new ConcurrentLinkedQueue<AcceptorOperationFuture>();

    private final Queue<NioSession> flushingSessions = new ConcurrentLinkedQueue<NioSession>();

    private final Map<SocketAddress, DatagramChannel> boundHandles = Collections
            .synchronizedMap(new HashMap<SocketAddress, DatagramChannel>());

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    private final ServiceOperationFuture disposalFuture = new ServiceOperationFuture();

    private volatile boolean selectable;

    /** The thread responsible of accepting incoming requests */
    private Acceptor acceptor;

    private long lastIdleCheckTime;

    /** The Selector used by this acceptor */
    private volatile Selector selector;

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptor() {
        this(new DefaultDatagramSessionConfig(), null);
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptor(Executor executor) {
        this(new DefaultDatagramSessionConfig(), executor);
    }

    /**
     * Creates a new instance.
     */
    private NioDatagramAcceptor(IoSessionConfig sessionConfig, Executor executor) {
        super(sessionConfig, executor);

        try {
            init();
            selectable = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to initialize.", e);
        } finally {
            if (!selectable) {
                try {
                    destroy();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    /**
     * This private class is used to accept incoming connection from
     * clients. It's an infinite loop, which can be stopped when all
     * the registered handles have been removed (unbound).
     */
    private class Acceptor implements Runnable {
        public void run() {
            int nHandles = 0;
            lastIdleCheckTime = System.currentTimeMillis();

            // Release the lock
            lock.release();

            while (selectable) {
                try {
                    int selected = select(SELECT_TIMEOUT);

                    nHandles += registerHandles();

                    if (nHandles == 0) {
                        try {
                            lock.acquire();

                            if (registerQueue.isEmpty() && cancelQueue.isEmpty()) {
                                acceptor = null;
                                break;
                            }
                        } finally {
                            lock.release();
                        }
                    }

                    if (selected > 0) {
                        processReadySessions(selectedHandles());
                    }

                    long currentTime = System.currentTimeMillis();
                    flushSessions(currentTime);
                    nHandles -= unregisterHandles();

                    notifyIdleSessions(currentTime);
                } catch (ClosedSelectorException cse) {
                    // If the selector has been closed, we can exit the loop
                    ExceptionMonitor.getInstance().exceptionCaught(cse);
                    break;
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                }
            }

            if (selectable && isDisposing()) {
                selectable = false;
                try {
                    destroy();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    disposalFuture.setValue(true);
                }
            }
        }
    }

    private int registerHandles() {
        for (;;) {
            AcceptorOperationFuture req = registerQueue.poll();

            if (req == null) {
                break;
            }

            Map<SocketAddress, DatagramChannel> newHandles = new HashMap<SocketAddress, DatagramChannel>();
            List<SocketAddress> localAddresses = req.getLocalAddresses();

            try {
                for (SocketAddress socketAddress : localAddresses) {
                    DatagramChannel handle = open(socketAddress);
                    newHandles.put(localAddress(handle), handle);
                }

                boundHandles.putAll(newHandles);

                getListeners().fireServiceActivated();
                req.setDone();

                return newHandles.size();
            } catch (Exception e) {
                req.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (req.getException() != null) {
                    for (DatagramChannel handle : newHandles.values()) {
                        try {
                            close(handle);
                        } catch (Exception e) {
                            ExceptionMonitor.getInstance().exceptionCaught(e);
                        }
                    }

                    wakeup();
                }
            }
        }

        return 0;
    }

    private void processReadySessions(Set<SelectionKey> handles) {
        Iterator<SelectionKey> iterator = handles.iterator();

        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            DatagramChannel handle = (DatagramChannel) key.channel();
            iterator.remove();

            try {
                if ((key != null) && key.isValid() && key.isReadable()) {
                    readHandle(handle);
                }

                if ((key != null) && key.isValid() && key.isWritable()) {
                    for (IoSession session : getManagedSessions().values()) {
                        scheduleFlush((NioSession) session);
                    }
                }
            } catch (Exception e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    private boolean scheduleFlush(NioSession session) {
        // Set the schedule for flush flag if the session
        // has not already be added to the flushingSessions
        // queue
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        } else {
            return false;
        }
    }

    private void readHandle(DatagramChannel handle) throws Exception {
        IoBuffer readBuf = IoBuffer.allocate(getSessionConfig().getReadBufferSize());

        SocketAddress remoteAddress = receive(handle, readBuf);

        if (remoteAddress != null) {
            IoSession session = newSessionWithoutLock(remoteAddress, localAddress(handle));

            readBuf.flip();

            session.getFilterChain().fireMessageReceived(readBuf);
        }
    }

    private IoSession newSessionWithoutLock(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        DatagramChannel handle = boundHandles.get(localAddress);

        if (handle == null) {
            throw new IllegalArgumentException("Unknown local address: " + localAddress);
        }

        IoSession session;

        synchronized (sessionRecycler) {
            session = sessionRecycler.recycle(remoteAddress);

            if (session != null) {
                return session;
            }

            // If a new session needs to be created.
            NioSession newSession = newSession(this, handle, remoteAddress);
            getSessionRecycler().put(newSession);
            session = newSession;
        }

        initSession(session, null, null);

        try {
            this.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
            getListeners().fireSessionCreated(session);
        } catch (Exception e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }

        return session;
    }

    private void flushSessions(long currentTime) {
        for (;;) {
            NioSession session = flushingSessions.poll();

            if (session == null) {
                break;
            }

            // Reset the Schedule for flush flag for this session,
            // as we are flushing it now
            session.unscheduledForFlush();

            try {
                boolean flushedAll = flush(session, currentTime);

                if (flushedAll && !session.getWriteRequestQueue().isEmpty(session) && !session.isScheduledForFlush()) {
                    scheduleFlush(session);
                }
            } catch (Exception e) {
                session.getFilterChain().fireExceptionCaught(e);
            }
        }
    }

    private boolean flush(NioSession session, long currentTime) throws Exception {
        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        final int maxWrittenBytes = session.getConfig().getMaxReadBufferSize()
                + (session.getConfig().getMaxReadBufferSize() >>> 1);

        int writtenBytes = 0;

        try {
            for (;;) {
                WriteRequest req = session.getCurrentWriteRequest();

                if (req == null) {
                    req = writeRequestQueue.poll(session);

                    if (req == null) {
                        setInterestedInWrite(session, false);
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

                int localWrittenBytes = send(session, buf, destination);

                if ((localWrittenBytes == 0) || (writtenBytes >= maxWrittenBytes)) {
                    // Kernel buffer is full or wrote too much
                    setInterestedInWrite(session, true);

                    return false;
                } else {
                    setInterestedInWrite(session, false);

                    // Clear and fire event
                    session.setCurrentWriteRequest(null);
                    writtenBytes += localWrittenBytes;
                    buf.reset();
                    session.getFilterChain().fireMessageSent(req);
                }
            }
        } finally {
            session.increaseWrittenBytes(writtenBytes, currentTime);
        }

        return true;
    }

    private int unregisterHandles() {
        int nHandles = 0;

        for (;;) {
            AcceptorOperationFuture request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            // close the channels
            for (SocketAddress socketAddress : request.getLocalAddresses()) {
                DatagramChannel handle = boundHandles.remove(socketAddress);

                if (handle == null) {
                    continue;
                }

                try {
                    close(handle);
                    wakeup(); // wake up again to trigger thread death
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    nHandles++;
                }
            }

            request.setDone();
        }

        return nHandles;
    }

    private void notifyIdleSessions(long currentTime) {
        // process idle sessions
        if (currentTime - lastIdleCheckTime >= 1000) {
            lastIdleCheckTime = currentTime;
            AbstractIoSession.notifyIdleness(getListeners().getManagedSessions().values().iterator(), currentTime);
        }
    }

    /**
     * Starts the inner Acceptor thread.
     */
    private void startupAcceptor() throws InterruptedException {
        if (!selectable) {
            registerQueue.clear();
            cancelQueue.clear();
            flushingSessions.clear();
        }

        lock.acquire();

        if (acceptor == null) {
            acceptor = new Acceptor();
            executeWorker(acceptor);
        } else {
            lock.release();
        }
    }

    protected void init() throws Exception {
        this.selector = Selector.open();
    }

    /**
     * {@inheritDoc}
     */
    public void add(NioSession session) {
        // Nothing to do for UDP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Set<SocketAddress> bindInternal(List<? extends SocketAddress> localAddresses) throws Exception {
        // Create a bind request as a Future operation. When the selector
        // have handled the registration, it will signal this future.
        AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);

        // adds the Registration request to the queue for the Workers
        // to handle
        registerQueue.add(request);

        // creates the Acceptor instance and has the local
        // executor kick it off.
        startupAcceptor();

        // As we just started the acceptor, we have to unblock the select()
        // in order to process the bind request we just have added to the
        // registerQueue.
        try {
            lock.acquire();

            // Wait a bit to give a chance to the Acceptor thread to do the select()
            Thread.sleep(10);
            wakeup();
        } finally {
            lock.release();
        }

        // Now, we wait until this request is completed.
        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }

        // Update the local addresses.
        // setLocalAddresses() shouldn't be called from the worker thread
        // because of deadlock.
        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();

        for (DatagramChannel handle : boundHandles.values()) {
            newLocalAddresses.add(localAddress(handle));
        }

        return newLocalAddresses;
    }

    protected void close(DatagramChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);

        if (key != null) {
            key.cancel();
        }

        handle.disconnect();
        handle.close();
    }

    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispose0() throws Exception {
        unbind();
        startupAcceptor();
        wakeup();
    }

    /**
     * {@inheritDoc}
     */
    public void flush(NioSession session) {
        if (scheduleFlush(session)) {
            wakeup();
        }
    }

    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    /**
     * {@inheritDoc}
     */
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) sessionConfig;
    }

    public final IoSessionRecycler getSessionRecycler() {
        return sessionRecycler;
    }

    public TransportMetadata getTransportMetadata() {
        return NioDatagramSession.METADATA;
    }

    protected boolean isReadable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return false;
        }

        return key.isReadable();
    }

    protected boolean isWritable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return false;
        }

        return key.isWritable();
    }

    protected SocketAddress localAddress(DatagramChannel handle) throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) handle.socket().getLocalSocketAddress();
        InetAddress inetAddress = inetSocketAddress.getAddress();

        if ((inetAddress instanceof Inet6Address) && (((Inet6Address) inetAddress).isIPv4CompatibleAddress())) {
            // Ugly hack to workaround a problem on linux : the ANY address is always converted to IPV6
            // even if the original address was an IPV4 address. We do store the two IPV4 and IPV6
            // ANY address in the map.
            byte[] ipV6Address = ((Inet6Address) inetAddress).getAddress();
            byte[] ipV4Address = new byte[4];

            for (int i = 0; i < 4; i++) {
                ipV4Address[i] = ipV6Address[12 + i];
            }

            InetAddress inet4Adress = Inet4Address.getByAddress(ipV4Address);
            return new InetSocketAddress(inet4Adress, inetSocketAddress.getPort());
        } else {
            return inetSocketAddress;
        }
    }

    protected NioSession newSession(IoProcessor<NioSession> processor, DatagramChannel handle,
            SocketAddress remoteAddress) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return null;
        }

        NioDatagramSession newSession = new NioDatagramSession(this, handle, processor, remoteAddress);
        newSession.setSelectionKey(key);

        return newSession;
    }

    /**
     * {@inheritDoc}
     */
    public final IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        if (remoteAddress == null) {
            throw new IllegalArgumentException("remoteAddress");
        }

        synchronized (bindLock) {
            if (!isActive()) {
                throw new IllegalStateException("Can't create a session from a unbound service.");
            }

            try {
                return newSessionWithoutLock(remoteAddress, localAddress);
            } catch (RuntimeException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeIoException("Failed to create a session.", e);
            }
        }
    }

    protected DatagramChannel open(SocketAddress localAddress) throws Exception {
        final DatagramChannel ch = DatagramChannel.open();
        boolean success = false;
        try {
            new NioDatagramSessionConfig(ch).setAll(getSessionConfig());
            ch.configureBlocking(false);

            try {
                ch.socket().bind(localAddress);
            } catch (IOException ioe) {
                // Add some info regarding the address we try to bind to the
                // message
                String newMessage = "Error while binding on " + localAddress + "\n" + "original message : "
                        + ioe.getMessage();
                Exception e = new IOException(newMessage);
                e.initCause(ioe.getCause());

                // And close the channel
                ch.close();

                throw e;
            }

            ch.register(selector, SelectionKey.OP_READ);
            success = true;
        } finally {
            if (!success) {
                close(ch);
            }
        }

        return ch;
    }

    protected SocketAddress receive(DatagramChannel handle, IoBuffer buffer) throws Exception {
        return handle.receive(buffer.buf());
    }

    /**
     * {@inheritDoc}
     */
    public void remove(NioSession session) {
        getSessionRecycler().remove(session);
        getListeners().fireSessionDestroyed(session);
    }

    protected int select() throws Exception {
        return selector.select();
    }

    protected int select(long timeout) throws Exception {
        return selector.select(timeout);
    }

    protected Set<SelectionKey> selectedHandles() {
        return selector.selectedKeys();
    }

    protected int send(NioSession session, IoBuffer buffer, SocketAddress remoteAddress) throws Exception {
        return ((DatagramChannel) session.getChannel()).send(buffer.buf(), remoteAddress);
    }

    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }

    protected void setInterestedInWrite(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();

        if (key == null) {
            return;
        }

        int newInterestOps = key.interestOps();

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }

        key.interestOps(newInterestOps);
    }

    public final void setSessionRecycler(IoSessionRecycler sessionRecycler) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException("sessionRecycler can't be set while the acceptor is bound.");
            }

            if (sessionRecycler == null) {
                sessionRecycler = DEFAULT_RECYCLER;
            }

            this.sessionRecycler = sessionRecycler;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void unbind0(List<? extends SocketAddress> localAddresses) throws Exception {
        AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);

        cancelQueue.add(request);
        startupAcceptor();
        wakeup();

        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateTrafficControl(NioSession session) {
        throw new UnsupportedOperationException();
    }

    protected void wakeup() {
        selector.wakeup();
    }

    /**
     * {@inheritDoc}
     */
    public void write(NioSession session, WriteRequest writeRequest) {
        // We will try to write the message directly
        long currentTime = System.currentTimeMillis();
        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        final int maxWrittenBytes = session.getConfig().getMaxReadBufferSize()
                + (session.getConfig().getMaxReadBufferSize() >>> 1);

        int writtenBytes = 0;

        // Deal with the special case of a Message marker (no bytes in the request)
        // We just have to return after having calle dthe messageSent event
        IoBuffer buf = (IoBuffer) writeRequest.getMessage();

        if (buf.remaining() == 0) {
            // Clear and fire event
            session.setCurrentWriteRequest(null);
            buf.reset();
            session.getFilterChain().fireMessageSent(writeRequest);
            return;
        }

        // Now, write the data
        try {
            for (;;) {
                if (writeRequest == null) {
                    writeRequest = writeRequestQueue.poll(session);

                    if (writeRequest == null) {
                        setInterestedInWrite(session, false);
                        break;
                    }

                    session.setCurrentWriteRequest(writeRequest);
                }

                buf = (IoBuffer) writeRequest.getMessage();

                if (buf.remaining() == 0) {
                    // Clear and fire event
                    session.setCurrentWriteRequest(null);
                    buf.reset();
                    session.getFilterChain().fireMessageSent(writeRequest);
                    continue;
                }

                SocketAddress destination = writeRequest.getDestination();

                if (destination == null) {
                    destination = session.getRemoteAddress();
                }

                int localWrittenBytes = send(session, buf, destination);

                if ((localWrittenBytes == 0) || (writtenBytes >= maxWrittenBytes)) {
                    // Kernel buffer is full or wrote too much
                    setInterestedInWrite(session, true);

                    session.getWriteRequestQueue().offer(session, writeRequest);
                    scheduleFlush(session);
                } else {
                    setInterestedInWrite(session, false);

                    // Clear and fire event
                    session.setCurrentWriteRequest(null);
                    writtenBytes += localWrittenBytes;
                    buf.reset();
                    session.getFilterChain().fireMessageSent(writeRequest);

                    break;
                }
            }
        } catch (Exception e) {
            session.getFilterChain().fireExceptionCaught(e);
        } finally {
            session.increaseWrittenBytes(writtenBytes, currentTime);
        }
    }
}