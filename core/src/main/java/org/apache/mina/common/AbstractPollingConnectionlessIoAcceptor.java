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
package org.apache.mina.common;

import java.net.SocketAddress;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.util.NamePreservingRunnable;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractPollingConnectionlessIoAcceptor<T extends AbstractIoSession, H>
        extends AbstractIoAcceptor {

    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();

    private static final AtomicInteger id = new AtomicInteger();

    private final Object lock = new Object();
    private final Executor executor;
    private final boolean createdExecutor;
    private final String threadName;
    private final IoProcessor<T> processor = new ConnectionlessAcceptorProcessor();
    private final Queue<AcceptorOperationFuture> registerQueue = 
        new ConcurrentLinkedQueue<AcceptorOperationFuture>();
    private final Queue<AcceptorOperationFuture> cancelQueue = 
        new ConcurrentLinkedQueue<AcceptorOperationFuture>();
    private final Queue<T> flushingSessions = new ConcurrentLinkedQueue<T>();
    private final Map<SocketAddress, H> boundHandles =
        Collections.synchronizedMap(new HashMap<SocketAddress, H>());

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    private final ServiceOperationFuture disposalFuture =
        new ServiceOperationFuture();
    private volatile boolean selectable;
    private Worker worker;
    private long lastIdleCheckTime;

    /**
     * Creates a new instance.
     */
    protected AbstractPollingConnectionlessIoAcceptor(IoSessionConfig sessionConfig) {
        this(sessionConfig, null);
    }

    /**
     * Creates a new instance.
     */
    protected AbstractPollingConnectionlessIoAcceptor(IoSessionConfig sessionConfig, Executor executor) {
        super(sessionConfig);

        threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();
        
        if (executor == null) {
            this.executor = new ThreadPoolExecutor(
                    1, 1, 1L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            this.createdExecutor = true;
        } else {
            this.executor = executor;
            this.createdExecutor = false;
        }
        
        try {
            init();
            selectable = true;
        } catch (RuntimeException e){
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

    protected abstract void init() throws Exception;
    protected abstract void destroy() throws Exception;
    protected abstract boolean select(int timeout) throws Exception;
    protected abstract void wakeup();
    protected abstract Iterator<H> selectedHandles();
    protected abstract H open(SocketAddress localAddress) throws Exception;
    protected abstract void close(H handle) throws Exception;
    protected abstract SocketAddress localAddress(H handle) throws Exception;
    protected abstract boolean isReadable(H handle);
    protected abstract boolean isWritable(H handle);
    protected abstract SocketAddress receive(H handle, IoBuffer buffer) throws Exception;
    protected abstract int send(T session, IoBuffer buffer, SocketAddress remoteAddress) throws Exception;
    protected abstract T newSession(IoProcessor<T> processor, H handle, SocketAddress remoteAddress) throws Exception;
    protected abstract void setInterestedInWrite(T session, boolean interested) throws Exception;

    @Override
    protected IoFuture dispose0() throws Exception {
        unbindAll();
        if (!disposalFuture.isDone()) {
            startupWorker();
            wakeup();
        }
        return disposalFuture;
    }

    @Override
    protected final Set<SocketAddress> bind0(
            List<? extends SocketAddress> localAddresses) throws Exception {
        AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);

        registerQueue.add(request);
        startupWorker();
        wakeup();

        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }

        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
        for (H handle: boundHandles.values()) {
            newLocalAddresses.add(localAddress(handle));
        }
        return newLocalAddresses;
    }

    @Override
    protected final void unbind0(
            List<? extends SocketAddress> localAddresses) throws Exception {
        AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);

        cancelQueue.add(request);
        startupWorker();
        wakeup();

        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }
    }

    public final IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        synchronized (bindLock) {
            if (!isActive()) {
                throw new IllegalStateException(
                        "Can't create a session from a unbound service.");
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

    private IoSession newSessionWithoutLock(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        H handle = boundHandles.get(localAddress);
        if (handle == null) {
            throw new IllegalArgumentException("Unknown local address: " + localAddress);
        }

        IoSession session;
        IoSessionRecycler sessionRecycler = getSessionRecycler();
        synchronized (sessionRecycler) {
            session = sessionRecycler.recycle(localAddress, remoteAddress);
            if (session != null) {
                return session;
            }

            // If a new session needs to be created.
            T newSession = newSession(processor, handle, remoteAddress);
            getSessionRecycler().put(newSession);
            session = newSession;
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

    public final IoSessionRecycler getSessionRecycler() {
        return sessionRecycler;
    }

    public final void setSessionRecycler(IoSessionRecycler sessionRecycler) {
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

    private class ConnectionlessAcceptorProcessor implements IoProcessor<T> {

        public void add(T session) {
        }

        public void flush(T session) {
            if (scheduleFlush(session)) {
                wakeup();
            }
        }

        public void remove(T session) {
            getSessionRecycler().remove(session);
            getListeners().fireSessionDestroyed(session);
        }

        public void updateTrafficMask(T session) {
            throw new UnsupportedOperationException();
        }

        public void dispose() {
        }

        public boolean isDisposed() {
            return false;
        }

        public boolean isDisposing() {
            return false;
        }
    }

    private void startupWorker() {
        if (!selectable) {
            registerQueue.clear();
            cancelQueue.clear();
            flushingSessions.clear();
        }
        
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(
                        new NamePreservingRunnable(worker, threadName));
            }
        }
    }

    private boolean scheduleFlush(T session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        } else {
            return false;
        }
    }

    private class Worker implements Runnable {
        public void run() {
            int nHandles = 0;
            lastIdleCheckTime = System.currentTimeMillis();

            while (selectable) {
                try {
                    boolean selected = select(1000);

                    nHandles += registerHandles();

                    if (selected) {
                        processReadySessions(selectedHandles());
                    }

                    flushSessions();
                    nHandles -= unregisterHandles();

                    notifyIdleSessions();

                    if (nHandles == 0) {
                        synchronized (lock) {
                            if (registerQueue.isEmpty() && cancelQueue.isEmpty()) {
                                worker = null;
                                break;
                            }
                        }
                    }
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
                if (createdExecutor) {
                    ((ExecutorService) executor).shutdown();
                }
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

    @SuppressWarnings("unchecked")
    private void processReadySessions(Iterator<H> handles) {
        while (handles.hasNext()) {
            H h = handles.next();
            handles.remove();
            try {
                if (isReadable(h)) {
                    readHandle(h);
                }

                if (isWritable(h)) {
                    for (IoSession session : getManagedSessions()) {
                        scheduleFlush((T) session);
                    }
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            }
        }
    }

    private void readHandle(H handle) throws Exception {
        IoBuffer readBuf = IoBuffer.allocate(
                getSessionConfig().getReadBufferSize());

        SocketAddress remoteAddress = receive(handle, readBuf);
        if (remoteAddress != null) {
            IoSession session = newSessionWithoutLock(
                    remoteAddress, localAddress(handle));

            readBuf.flip();

            IoBuffer newBuf = IoBuffer.allocate(readBuf.limit());
            newBuf.put(readBuf);
            newBuf.flip();

            session.getFilterChain().fireMessageReceived(newBuf);
        }
    }

    private void flushSessions() {
        for (; ;) {
            T session = flushingSessions.poll();
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
            } catch (Exception e) {
                session.getFilterChain().fireExceptionCaught(e);
            }
        }
    }

    private boolean flush(T session) throws Exception {
        // Clear OP_WRITE
        setInterestedInWrite(session, false);
        
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();

        int maxWrittenBytes =
            session.getConfig().getMaxReadBufferSize() +
            (session.getConfig().getMaxReadBufferSize() >>> 1);

        int writtenBytes = 0;
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

            int localWrittenBytes = send(session, buf, destination);
            if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
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

        return true;
    }

    private int registerHandles() {
        for (;;) {
            AcceptorOperationFuture req = registerQueue.poll();
            if (req == null) {
                break;
            }

            Map<SocketAddress, H> newHandles = new HashMap<SocketAddress, H>();
            List<SocketAddress> localAddresses = req.getLocalAddresses();
            try {
                for (SocketAddress a: localAddresses) {
                    H handle = open(a);
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
                    for (H handle: newHandles.values()) {
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

    private int unregisterHandles() {
        int nHandles = 0;
        for (;;) {
            AcceptorOperationFuture request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            // close the channels
            for (SocketAddress a: request.getLocalAddresses()) {
                H handle = boundHandles.remove(a);
                if (handle == null) {
                    continue;
                }

                try {
                    close(handle);
                    wakeup(); // wake up again to trigger thread death
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    nHandles ++;
                }
            }
            
            request.setDone();
        }
        
        return nHandles;
    }

    private void notifyIdleSessions() {
        // process idle sessions
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastIdleCheckTime >= 1000) {
            lastIdleCheckTime = currentTime;
            IdleStatusChecker.notifyIdleness(
                    getListeners().getManagedSessions().iterator(),
                    currentTime);
        }
    }
}
