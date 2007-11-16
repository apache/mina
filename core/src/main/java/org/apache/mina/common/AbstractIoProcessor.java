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

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.util.CopyOnWriteMap;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * An abstract implementation of {@link IoProcessor} which helps
 * transport developers to write an {@link IoProcessor} easily.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoProcessor<T extends AbstractIoSession> implements IoProcessor<T> {
    /**
     * The maximum loop count for a write operation until
     * {@link #write(IoSession, IoBuffer)} returns non-zero value.
     * It is similar to what a spin lock is for in concurrency programming.
     * It improves memory utilization and write throughput significantly.
     */
    private static final int WRITE_SPIN_COUNT = 256;
    
    private static final Map<Class<?>, AtomicInteger> threadIds =
        new CopyOnWriteMap<Class<?>, AtomicInteger>();
    
    private final Object lock = new Object();
    private final String threadName;
    private final Executor executor;

    private final Queue<T> newSessions = new ConcurrentLinkedQueue<T>();
    private final Queue<T> removingSessions = new ConcurrentLinkedQueue<T>();
    private final Queue<T> flushingSessions = new ConcurrentLinkedQueue<T>();
    private final Queue<T> trafficControllingSessions = new ConcurrentLinkedQueue<T>();

    private Worker worker;
    private long lastIdleCheckTime;
    private volatile boolean toBeDisposed;
    
    protected AbstractIoProcessor(Executor executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        
        this.threadName = nextThreadName();
        this.executor = executor;
    }
    
    private String nextThreadName() {
        Class<?> cls = getClass();
        AtomicInteger threadId = threadIds.get(cls);
        int newThreadId;
        if (threadId == null) {
            newThreadId = 1;
            threadIds.put(cls, new AtomicInteger(newThreadId));
        } else {
            newThreadId = threadId.incrementAndGet();
        }
        
        return cls.getSimpleName() + '-' + newThreadId;
    }
    
    protected Executor getExecutor() {
        return executor;
    }
    

    public void dispose() {
        toBeDisposed = true;
        startupWorker();
    }
    
    protected abstract void doDispose() throws Exception;

    /**
     * poll those sessions for the given timeout
     * @param timeout milliseconds before the call timeout if no event appear
     * @return true if at least a session is ready for read or for write
     * @throws Exception if some low level IO error occurs
     */
    protected abstract boolean select(int timeout) throws Exception;

    protected abstract void wakeup();

    protected abstract Iterator<T> allSessions() throws Exception;

    protected abstract Iterator<T> selectedSessions() throws Exception;

    protected abstract SessionState state(T session);


    /**
     * Is the session ready for writing
     * @param session the session queried
     * @return true is ready, false if not ready
     * @throws Exception if some low level IO error occurs
     */
    protected abstract boolean isWritable(T session) throws Exception;

    /**
     * Is the session ready for reading
     * @param session the session queried
     * @return true is ready, false if not ready
     * @throws Exception if some low level IO error occurs
     */
    protected abstract boolean isReadable(T session) throws Exception;
    /**
     * register a session for writing
     * @param session the session registered
     * @param value true for registering, false for removing
     * @throws Exception if some low level IO error occurs
     */
    protected abstract void setOpWrite(T session, boolean value) throws Exception;

    /**
     * register a session for reading
     * @param session the session registered
     * @param value true for registering, false for removing
     * @throws Exception if some low level IO error occurs
     */
    protected abstract void setOpRead(T session, boolean value) throws Exception;

    /**
     * is this session registered for reading
     * @param session the session queried
     * @return true is registered for reading
     * @throws Exception if some low level IO error occurs
     */
    protected abstract boolean isOpRead(T session) throws Exception;

    /**
     * is this session registered for writing
     * @param session the session queried
     * @return true is registered for writing
     * @throws Exception if some low level IO error occurs
     */
    protected abstract boolean isOpWrite(T session) throws Exception;

    protected abstract void doAdd(T session) throws Exception;

    protected abstract void doRemove(T session) throws Exception;

    protected abstract int read(T session, IoBuffer buf) throws Exception;

    protected abstract int write(T session, IoBuffer buf) throws Exception;

    protected abstract long transferFile(T session, FileRegion region) throws Exception;

    public void add(T session) {
        newSessions.add(session);
        startupWorker();
    }

    public void remove(T session) {
        scheduleRemove(session);
        startupWorker();
    }

    public void flush(T session) {
        boolean needsWakeup = flushingSessions.isEmpty();
        if (scheduleFlush(session) && needsWakeup) {
            wakeup();
        }
    }

    public void updateTrafficMask(T session) {
        scheduleTrafficControl(session);
        wakeup();
    }

    private void startupWorker() {
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker, threadName));
            }
        }
        wakeup();
    }

    private void scheduleRemove(T session) {
        removingSessions.add(session);
    }

    private boolean scheduleFlush(T session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        }
        return false;
    }

    private void scheduleTrafficControl(T session) {
        trafficControllingSessions.add(session);
    }

    private int add() {
        int addedSessions = 0;
        for (; ;) {
            T session = newSessions.poll();

            if (session == null) {
                break;
            }

            boolean notified = false;
            try {
                doAdd(session);
                addedSessions ++;

                // Build the filter chain of this session.
                session.getService().getFilterChainBuilder().buildFilterChain(
                        session.getFilterChain());

                // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
                // in AbstractIoFilterChain.fireSessionOpened().
                ((AbstractIoService) session.getService()).getListeners().fireSessionCreated(session);
                notified = true;
            } catch (Exception e) {
                if (notified) {
                    // Clear the DefaultIoFilterChain.CONNECT_FUTURE attribute
                    // and call ConnectFuture.setException().
                    session.getFilterChain().fireExceptionCaught(e);
                    scheduleRemove(session);
                    wakeup();
                } else {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                    try {
                        doRemove(session);
                    } catch (Exception e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
        }

        return addedSessions;
    }

    private int remove() {
        int removedSessions = 0;
        for (; ;) {
            T session = removingSessions.poll();

            if (session == null) {
                break;
            }

            SessionState state = state(session);
            switch (state) {
            case OPEN:
                try {
                    doRemove(session);
                    removedSessions ++;
                } catch (Exception e) {
                    session.getFilterChain().fireExceptionCaught(e);
                } finally {
                    clearWriteRequestQueue(session);
                    ((AbstractIoService) session.getService()).getListeners().fireSessionDestroyed(session);
                }
                break;
            case CLOSED:
                // Skip if channel is already closed
                break;
            case PREPARING:
                // Retry later if session is not yet fully initialized.
                // (In case that Session.close() is called before addSession() is processed)
                scheduleRemove(session);
                return removedSessions;
            default:
                throw new IllegalStateException(String.valueOf(state));
            }
        }

        return removedSessions;
    }

    private void process() throws Exception {
        for (Iterator<T> i = selectedSessions(); i.hasNext();) {
            process(i.next());
            i.remove();
        }
    }

    private void process(T session) throws Exception {

        if (isReadable(session) && session.getTrafficMask().isReadable()) {
            read(session);
        }

        if (isWritable(session) && session.getTrafficMask().isWritable()) {
            scheduleFlush(session);
        }
    }

    private void read(T session) {
        IoSessionConfig config = session.getConfig();
        IoBuffer buf = IoBuffer.allocate(config.getReadBufferSize());

        try {
            int readBytes = 0;
            int ret;

            try {
                if (session.getTransportMetadata().hasFragmentation()) {
                    while ((ret = read(session, buf)) > 0) {
                        readBytes += ret;
                    }
                } else {
                    ret = read(session, buf);
                    if (ret > 0) {
                        readBytes = ret;
                    }
                }
            } finally {
                buf.flip();
            }

            if (readBytes > 0) {
                session.getFilterChain().fireMessageReceived(buf);
                buf = null;

                if (session.getTransportMetadata().hasFragmentation()) {
                    if (readBytes << 1 < config.getReadBufferSize()) {
                        session.decreaseReadBufferSize();
                    } else if (readBytes == config.getReadBufferSize()) {
                        session.increaseReadBufferSize();
                    }
                }
            }
            if (ret < 0) {
                scheduleRemove(session);
            }
        } catch (IOException e) {
            scheduleRemove(session);
            session.getFilterChain().fireExceptionCaught(e);
        } catch (Throwable e) {
            session.getFilterChain().fireExceptionCaught(e);
        }
    }

    private void notifyIdleSessions() throws Exception {
        // process idle sessions
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastIdleCheckTime >= 1000) {
            lastIdleCheckTime = currentTime;
            IdleStatusChecker.notifyIdleness(allSessions(), currentTime);
        }
    }

    private void write() {
        for (; ;) {
            T session = flushingSessions.poll();

            if (session == null) {
                break;
            }

            session.setScheduledForFlush(false);

            if (!session.isConnected()) {
                clearWriteRequestQueue(session);
                continue;
            }

            SessionState state = state(session);
            switch (state) {
            case OPEN:
                try {
                    boolean flushedAll = write(session);
                    if (flushedAll && !session.getWriteRequestQueue().isEmpty(session) &&
                        !session.isScheduledForFlush()) {
                        scheduleFlush(session);
                    }
                } catch (Exception e) {
                    scheduleRemove(session);
                    session.getFilterChain().fireExceptionCaught(e);
                }
                break;
            case CLOSED:
                // Skip if the channel is already closed.
                break;
            case PREPARING:
                // Retry later if session is not yet fully initialized.
                // (In case that Session.write() is called before addSession() is processed)
                scheduleFlush(session);
                return;
            default:
                throw new IllegalStateException(String.valueOf(state));
            }
        }
    }

    private void clearWriteRequestQueue(T session) {
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        WriteRequest req;
        
        List<WriteRequest> failedRequests = new ArrayList<WriteRequest>();

        if ((req = writeRequestQueue.poll(session)) != null) {
            Object m = req.getMessage();
            if (m instanceof IoBuffer) {
                IoBuffer buf = (IoBuffer) req.getMessage();

                // The first unwritten empty buffer must be
                // forwarded to the filter chain.
                if (buf.hasRemaining()) {
                    buf.reset();
                    failedRequests.add(req);
                } else {
                    session.getFilterChain().fireMessageSent(req);
                }
            } else {
                failedRequests.add(req);
            }

            // Discard others.
            while ((req = writeRequestQueue.poll(session)) != null) {
                failedRequests.add(req);
            }
        }
        
        // Create an exception and notify.
        if (!failedRequests.isEmpty()) {
            WriteToClosedSessionException cause = new WriteToClosedSessionException(failedRequests);
            for (WriteRequest r: failedRequests) {
                session.decreaseScheduledBytesAndMessages(r);
                r.getFuture().setException(cause);
            }
            session.getFilterChain().fireExceptionCaught(cause);
        }
    }

    private boolean write(T session) throws Exception {
        if (!session.isConnected()) {
            scheduleRemove(session);
            return false;
        }

        // Clear OP_WRITE
        setOpWrite(session, false);

        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();

        // Set limitation for the number of written bytes for read-write
        // fairness.  I used maxReadBufferSize * 3 / 2, which yields best
        // performance in my experience while not breaking fairness much.
        int maxWrittenBytes = session.getConfig().getMaxReadBufferSize() +
                              (session.getConfig().getMaxReadBufferSize() >>> 1);
        int writtenBytes = 0;

        do {
            // Check for pending writes.
            WriteRequest req = session.getCurrentWriteRequest();
            if (req == null) {
                req = writeRequestQueue.poll(session);
                if (req == null) {
                    break;
                }
                session.setCurrentWriteRequest(req);
            }

            long localWrittenBytes = 0;
            Object message = req.getMessage();
            if (message instanceof FileRegion) {
                FileRegion region = (FileRegion) message;

                if (region.getCount() <= 0) {
                    // File has been sent, clear the current request.
                    session.setCurrentWriteRequest(null);
                    session.getFilterChain().fireMessageSent(req);
                    continue;
                }

                localWrittenBytes = transferFile(session, region);
                region.setPosition(region.getPosition() + localWrittenBytes);
            } else {
                IoBuffer buf = (IoBuffer) message;
                if (buf.remaining() == 0) {
                    // Buffer has been sent, clear the current request.
                    session.setCurrentWriteRequest(null);
                    buf.reset();
                    session.getFilterChain().fireMessageSent(req);
                    continue;
                }

                for (int i = WRITE_SPIN_COUNT; i > 0; i --) {
                    localWrittenBytes = write(session, buf);
                    if (localWrittenBytes != 0 || !buf.hasRemaining()) {
                        break;
                    }
                }
            }
            
            writtenBytes += localWrittenBytes;

            if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
                // Kernel buffer is full or wrote too much.
                setOpWrite(session, true);
                return false;
            }
        } while (writtenBytes < maxWrittenBytes);

        return true;
    }

    private void updateTrafficMask() {
        for (; ;) {
            T session = trafficControllingSessions.poll();

            if (session == null) {
                break;
            }

            SessionState state = state(session);
            switch (state) {
            case OPEN:
                // The normal is OP_READ and, if there are write requests in the
                // session's write queue, set OP_WRITE to trigger flushing.
                int mask = session.getTrafficMask().getInterestOps();
                try {
                    setOpRead(session, (mask & SelectionKey.OP_READ) != 0);
                } catch (Exception e) {
                    session.getFilterChain().fireExceptionCaught(e);
                }
                try {
                    setOpWrite(
                            session,
                            !session.getWriteRequestQueue().isEmpty(session) &&
                                    (mask & SelectionKey.OP_WRITE) != 0);
                } catch (Exception e) {
                    session.getFilterChain().fireExceptionCaught(e);
                }
                break;
            case CLOSED:
                break;
            case PREPARING:
                // Retry later if session is not yet fully initialized.
                // (In case that Session.suspend??() or session.resume??() is
                // called before addSession() is processed)
                scheduleTrafficControl(session);
                return;
            default:
                throw new IllegalStateException(String.valueOf(state));
            }
        }
    }

    private class Worker implements Runnable {
        public void run() {
            int nSessions = 0;
            lastIdleCheckTime = System.currentTimeMillis();

            for (;;) {
                try {
                    boolean selected = select(1000);

                    nSessions += add();
                    updateTrafficMask();

                    if (selected) {
                        process();
                    }

                    write();
                    nSessions -= remove();
                    notifyIdleSessions();

                    if (nSessions == 0) {
                        synchronized (lock) {
                            if (newSessions.isEmpty()) {
                                worker = null;
                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
                }
            }
            
            if (toBeDisposed) {
                try {
                    doDispose();
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);
                }
            }
        }
    }

    protected static enum SessionState {
        OPEN,
        CLOSED,
        PREPARING,
    }
}
