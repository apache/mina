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
public abstract class AbstractPollingIoProcessor<T extends AbstractIoSession> implements IoProcessor<T> {
    /**
     * The maximum loop count for a write operation until
     * {@link #write(AbstractIoSession, IoBuffer, int)} returns non-zero value.
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

    private final Object disposalLock = new Object();
    private volatile boolean disposing;
    private volatile boolean disposed;
    private final DefaultIoFuture disposalFuture = new DefaultIoFuture(null);

    protected AbstractPollingIoProcessor(Executor executor) {
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
    
    public final boolean isDisposing() {
        return disposing;
    }
    
    public final boolean isDisposed() {
        return disposed;
    }
    
    public final void dispose() {
        if (disposed) {
            return;
        }

        synchronized (disposalLock) {
            if (!disposing) {
                disposing = true;
                startupWorker();
            }
        }
        
        disposalFuture.awaitUninterruptibly();
        disposed = true;
    }
    
    protected abstract void dispose0() throws Exception;

    /**
     * poll those sessions for the given timeout
     * @param timeout milliseconds before the call timeout if no event appear
     * @return true if at least a session is ready for read or for write
     * @throws Exception if some low level IO error occurs
     */
    protected abstract boolean select(int timeout) throws Exception;
    protected abstract void wakeup();
    protected abstract Iterator<T> allSessions();
    protected abstract Iterator<T> selectedSessions();
    protected abstract SessionState state(T session);

    /**
     * Is the session ready for writing
     * @param session the session queried
     * @return true is ready, false if not ready
     */
    protected abstract boolean isWritable(T session);

    /**
     * Is the session ready for reading
     * @param session the session queried
     * @return true is ready, false if not ready
     */
    protected abstract boolean isReadable(T session);

    /**
     * register a session for writing
     * @param session the session registered
     * @param interested true for registering, false for removing
     */
    protected abstract void setInterestedInWrite(T session, boolean interested)
            throws Exception;

    /**
     * register a session for reading
     * @param session the session registered
     * @param interested true for registering, false for removing
     */
    protected abstract void setInterestedInRead(T session, boolean interested)
            throws Exception;

    /**
     * is this session registered for reading
     * @param session the session queried
     * @return true is registered for reading
     */
    protected abstract boolean isInterestedInRead(T session);

    /**
     * is this session registered for writing
     * @param session the session queried
     * @return true is registered for writing
     */
    protected abstract boolean isInterestedInWrite(T session);

    protected abstract void init(T session) throws Exception;
    protected abstract void destroy(T session) throws Exception;
    protected abstract int read(T session, IoBuffer buf) throws Exception;
    protected abstract int write(T session, IoBuffer buf, int length) throws Exception;
    protected abstract int transferFile(T session, FileRegion region, int length) throws Exception;

    public final void add(T session) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        newSessions.add(session);
        startupWorker();
    }

    public final void remove(T session) {
        scheduleRemove(session);
        startupWorker();
    }

    private void scheduleRemove(T session) {
        removingSessions.add(session);
    }

    public final void flush(T session) {
        boolean needsWakeup = flushingSessions.isEmpty();
        if (scheduleFlush(session) && needsWakeup) {
            wakeup();
        }
    }

    private boolean scheduleFlush(T session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        }
        return false;
    }

    public final void updateTrafficMask(T session) {
        scheduleTrafficControl(session);
        wakeup();
    }

    private void scheduleTrafficControl(T session) {
        trafficControllingSessions.add(session);
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

    private int add() {
        int addedSessions = 0;
        for (; ;) {
            T session = newSessions.poll();

            if (session == null) {
                break;
            }

            
            if (addNow(session)) {
                addedSessions ++;
            }
        }

        return addedSessions;
    }

    private boolean addNow(T session) {

        boolean registered = false;
        boolean notified = false;
        try {
            init(session);
            registered = true;

            // Build the filter chain of this session.
            session.getService().getFilterChainBuilder().buildFilterChain(
                    session.getFilterChain());

            // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
            // in AbstractIoFilterChain.fireSessionOpened().
            ((AbstractIoService) session.getService()).getListeners().fireSessionCreated(session);
            notified = true;
        } catch (Throwable e) {
            if (notified) {
                // Clear the DefaultIoFilterChain.CONNECT_FUTURE attribute
                // and call ConnectFuture.setException().
                scheduleRemove(session);
                session.getFilterChain().fireExceptionCaught(e);
                wakeup();
            } else {
                ExceptionMonitor.getInstance().exceptionCaught(e);
                try {
                    destroy(session);
                } catch (Exception e1) {
                    ExceptionMonitor.getInstance().exceptionCaught(e1);
                } finally {
                    registered = false;
                }
            }
        }
        return registered;
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
                if (removeNow(session)) {
                    removedSessions ++;
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

    private boolean removeNow(T session) {
        clearWriteRequestQueue(session);

        try {
            destroy(session);
            return true;
        } catch (Exception e) {
            session.getFilterChain().fireExceptionCaught(e);
        } finally {
            clearWriteRequestQueue(session);
            ((AbstractIoService) session.getService()).getListeners().fireSessionDestroyed(session);
        }
        return false;
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

    private void process() throws Exception {
        for (Iterator<T> i = selectedSessions(); i.hasNext();) {
            process(i.next());
            i.remove();
        }
    }

    private void process(T session) {

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

        final boolean hasFragmentation =
            session.getTransportMetadata().hasFragmentation();

        try {
            int readBytes = 0;
            int ret;

            try {
                if (hasFragmentation) {
                    while ((ret = read(session, buf)) > 0) {
                        readBytes += ret;
                        if (!buf.hasRemaining()) {
                            break;
                        }
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

                if (hasFragmentation) {
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
        } catch (Throwable e) {
            if (e instanceof IOException) {
                scheduleRemove(session);
            }
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

    private void flush() {
        for (; ;) {
            T session = flushingSessions.poll();

            if (session == null) {
                break;
            }

            session.setScheduledForFlush(false);
            SessionState state = state(session);
            switch (state) {
            case OPEN:
                try {
                    boolean flushedAll = flushNow(session);
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

    private boolean flushNow(T session) {
        if (!session.isConnected()) {
            scheduleRemove(session);
            return false;
        }
        
        final boolean hasFragmentation = 
            session.getTransportMetadata().hasFragmentation();

        try {
            // Clear OP_WRITE
            setInterestedInWrite(session, false);
    
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
    
                int localWrittenBytes = 0;
                Object message = req.getMessage();
                if (message instanceof IoBuffer) {
                    localWrittenBytes = writeBuffer(
                            session, req, hasFragmentation,
                            maxWrittenBytes - writtenBytes);
                } else if (message instanceof FileRegion) {
                    localWrittenBytes = writeFile(
                            session, req, hasFragmentation,
                            maxWrittenBytes - writtenBytes);
                } else {
                	throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
                }
                
                writtenBytes += localWrittenBytes;
    
                if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
                    // Kernel buffer is full or wrote too much.
                    setInterestedInWrite(session, true);
                    return false;
                }
            } while (writtenBytes < maxWrittenBytes);
        } catch (Exception e) {
            session.getFilterChain().fireExceptionCaught(e);
            return false;
        }

        return true;
    }

    private int writeBuffer(T session, WriteRequest req,
            boolean hasFragmentation, int maxLength) throws Exception {
        IoBuffer buf = (IoBuffer) req.getMessage();
        int localWrittenBytes = 0;
        if (buf.hasRemaining()) {
            int length;
            if (hasFragmentation) {
                length = Math.min(buf.remaining(), maxLength);
            } else {
                length = buf.remaining();
            }
            for (int i = WRITE_SPIN_COUNT; i > 0; i --) {
                localWrittenBytes = write(session, buf, length);
                if (localWrittenBytes != 0) {
                    break;
                }
            }
        }

        if (!buf.hasRemaining() ||
                (!hasFragmentation && localWrittenBytes != 0)) {
            // Buffer has been sent, clear the current request.
            buf.reset();
            fireMessageSent(session, req);
        }
        return localWrittenBytes;
    }

    private int writeFile(T session, WriteRequest req,
            boolean hasFragmentation, int maxLength) throws Exception {
        int localWrittenBytes;
        FileRegion region = (FileRegion) req.getMessage();
        if (region.getCount() > 0) {
            int length;
            if (hasFragmentation) {
                length = (int) Math.min(region.getCount(), maxLength);
            } else {
                length = (int) Math.min(Integer.MAX_VALUE, region.getCount());
            }
            localWrittenBytes = transferFile(session, region, length);
            region.setPosition(region.getPosition() + localWrittenBytes);
            
            // Fix for Java bug on Linux http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
            // If there's still data to be written in the FileRegion, return 0 indicating that we need
            // to pause until writing may resume.
            if (localWrittenBytes > 0 && region.getCount() > 0) {
                return 0;
            }
        } else {
            localWrittenBytes = 0;
        }

        if (region.getCount() <= 0 ||
                (!hasFragmentation && localWrittenBytes != 0)) {
            fireMessageSent(session, req);
        }
        return localWrittenBytes;
    }

    private void fireMessageSent(T session, WriteRequest req) {
        session.setCurrentWriteRequest(null);
        session.getFilterChain().fireMessageSent(req);
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
                updateTrafficMaskNow(session);
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

    private void updateTrafficMaskNow(T session) {
        // The normal is OP_READ and, if there are write requests in the
        // session's write queue, set OP_WRITE to trigger flushing.
        int mask = session.getTrafficMask().getInterestOps();
        try {
            setInterestedInRead(session, (mask & SelectionKey.OP_READ) != 0);
        } catch (Exception e) {
            session.getFilterChain().fireExceptionCaught(e);
        }
        try {
            setInterestedInWrite(
                    session,
                    !session.getWriteRequestQueue().isEmpty(session) &&
                            (mask & SelectionKey.OP_WRITE) != 0);
        } catch (Exception e) {
            session.getFilterChain().fireExceptionCaught(e);
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

                    flush();
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
                    
                    // Disconnect all sessions immediately if disposal has been
                    // requested so that we exit this loop eventually.
                    if (isDisposing()) {
                        for (Iterator<T> i = allSessions(); i.hasNext(); ) {
                            scheduleRemove(i.next());
                        }
                        wakeup();
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
            
            if (isDisposing()) {
                try {
                    dispose0();
                } catch (Throwable t) {
                    ExceptionMonitor.getInstance().exceptionCaught(t);
                } finally {
                    disposalFuture.setValue(true);
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
