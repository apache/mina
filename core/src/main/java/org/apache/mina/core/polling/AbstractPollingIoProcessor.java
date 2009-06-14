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
package org.apache.mina.core.polling;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfig;
import org.apache.mina.util.ExceptionMonitor;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * An abstract implementation of {@link IoProcessor} which helps
 * transport developers to write an {@link IoProcessor} easily.
 * This class is in charge of active polling a set of {@link IoSession}
 * and trigger events when some I/O operation is possible.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractPollingIoProcessor<T extends AbstractIoSession>
        implements IoProcessor<T> {
    /**
     * The maximum loop count for a write operation until
     * {@link #write(AbstractIoSession, IoBuffer, int)} returns non-zero value.
     * It is similar to what a spin lock is for in concurrency programming.
     * It improves memory utilization and write throughput significantly.
     */
    private static final int WRITE_SPIN_COUNT = 256;

    /** A timeout used for the select, as we need to get out to deal with idle sessions */
    private static final long SELECT_TIMEOUT = 1000L;

    /** A map containing the last Thread ID for each class */
    private static final Map<Class<?>, AtomicInteger> threadIds = new HashMap<Class<?>, AtomicInteger>();

    private final Object lock = new Object();

    private final String threadName;

    private final Executor executor;

    /** A Session queue containing the newly created sessions */
    private final Queue<T> newSessions = new ConcurrentLinkedQueue<T>();

    private final Queue<T> removingSessions = new ConcurrentLinkedQueue<T>();

    private final Queue<T> flushingSessions = new ConcurrentLinkedQueue<T>();

    private final Queue<T> trafficControllingSessions = new ConcurrentLinkedQueue<T>();

    /** The processor thread : it handles the incoming messages */
    private Processor processor;

    private long lastIdleCheckTime;

    private final Object disposalLock = new Object();

    private volatile boolean disposing;

    private volatile boolean disposed;

    private final DefaultIoFuture disposalFuture = new DefaultIoFuture(null);

    /**
     * Create an {@link AbstractPollingIoProcessor} with the given {@link Executor}
     * for handling I/Os events.
     * 
     * @param executor the {@link Executor} for handling I/O events
     */
    protected AbstractPollingIoProcessor(Executor executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }

        this.threadName = nextThreadName();
        this.executor = executor;
    }

    /**
     * Compute the thread ID for this class instance. As we may have different
     * classes, we store the last ID number into a Map associating the class
     * name to the last assigned ID.
     *   
     * @return a name for the current thread, based on the class name and
     * an incremental value, starting at 1. 
     */
    private String nextThreadName() {
        Class<?> cls = getClass();
        int newThreadId;

        // We synchronize this block to avoid a concurrent access to 
        // the actomicInteger (it can be modified by another thread, while
        // being seen as null by another thread)
        synchronized (threadIds) {
            // Get the current ID associated to this class' name
            AtomicInteger threadId = threadIds.get(cls);

            if (threadId == null) {
                // We never have seen this class before, just create a
                // new ID starting at 1 for it, and associate this ID
                // with the class name in the map.
                newThreadId = 1;
                threadIds.put(cls, new AtomicInteger(newThreadId));
            } else {
                // Just increment the lat ID, and get it.
                newThreadId = threadId.incrementAndGet();
            }
        }

        // Now we can compute the name for this thread
        return cls.getSimpleName() + '-' + newThreadId;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposed() {
        return disposed;
    }

    /**
     * {@inheritDoc}
     */
    public final void dispose() {
        if (disposed) {
            return;
        }

        synchronized (disposalLock) {
            if (!disposing) {
                disposing = true;
                startupProcessor();
            }
        }

        disposalFuture.awaitUninterruptibly();
        disposed = true;
    }

    /**
     * Dispose the resources used by this {@link IoProcessor} for polling 
     * the client connections
     * @throws Exception if some low level IO error occurs
     */
    protected abstract void dispose0() throws Exception;

    /**
     * poll those sessions for the given timeout
     * @param timeout milliseconds before the call timeout if no event appear
     * @return The number of session ready for read or for write
     * @throws Exception if some low level IO error occurs
     */
    protected abstract int select(long timeout) throws Exception;

    /**
     * poll those sessions forever
     * @return The number of session ready for read or for write
     * @throws Exception if some low level IO error occurs
     */
    protected abstract int select() throws Exception;

    /**
     * Say if the list of {@link IoSession} polled by this {@link IoProcessor} 
     * is empty
     * @return true if at least a session is managed by this {@link IoProcessor}
     */
    protected abstract boolean isSelectorEmpty();

    /**
     * Interrupt the {@link AbstractPollingIoProcessor#select(int) call.
     */
    protected abstract void wakeup();

    /**
     * Get an {@link Iterator} for the list of {@link IoSession} polled by this
     * {@link IoProcessor}   
     * @return {@link Iterator} of {@link IoSession}
     */
    protected abstract Iterator<T> allSessions();

    /**
     * Get an {@link Iterator} for the list of {@link IoSession} found selected 
     * by the last call of {@link AbstractPollingIoProcessor#select(int)
     * @return {@link Iterator} of {@link IoSession} read for I/Os operation
     */
    protected abstract Iterator<T> selectedSessions();

    /**
     * Get the state of a session (preparing, open, closed)
     * @param session the {@link IoSession} to inspect
     * @return the state of the session
     */
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

    /**
     * Initialize the polling of a session. Add it to the polling process. 
     * @param session the {@link IoSession} to add to the polling
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void init(T session) throws Exception;

    /**
     * Destroy the underlying client socket handle
     * @param session the {@link IoSession}
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void destroy(T session) throws Exception;

    /**
     * Reads a sequence of bytes from a {@link IoSession} into the given {@link IoBuffer}. 
     * Is called when the session was found ready for reading.
     * @param session the session to read
     * @param buf the buffer to fill
     * @return the number of bytes read
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int read(T session, IoBuffer buf) throws Exception;

    /**
     * Write a sequence of bytes to a {@link IoSession}, means to be called when a session
     * was found ready for writing.
     * @param session the session to write
     * @param buf the buffer to write
     * @param length the number of bytes to write can be superior to the number of bytes remaining
     * in the buffer
     * @return the number of byte written
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int write(T session, IoBuffer buf, int length)
            throws Exception;

    /**
     * Write a part of a file to a {@link IoSession}, if the underlying API isn't supporting
     * system calls like sendfile(), you can throw a {@link UnsupportedOperationException} so 
     * the file will be send using usual {@link #write(AbstractIoSession, IoBuffer, int)} call. 
     * @param session the session to write
     * @param region the file region to write
     * @param length the length of the portion to send
     * @return the number of written bytes
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract int transferFile(T session, FileRegion region, int length)
            throws Exception;

    /**
     * {@inheritDoc}
     */
    public final void add(T session) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        // Adds the session to the newSession queue and starts the worker
        newSessions.add(session);
        startupProcessor();
    }

    /**
     * {@inheritDoc}
     */
    public final void remove(T session) {
        scheduleRemove(session);
        startupProcessor();
    }

    private void scheduleRemove(T session) {
        removingSessions.add(session);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public final void updateTrafficMask(T session) {
        scheduleTrafficControl(session);
        wakeup();
    }

    private void scheduleTrafficControl(T session) {
        trafficControllingSessions.add(session);
    }

    /**
     * Starts the inner Processor, asking the executor to pick a thread in its
     * pool. The Runnable will be renamed 
     */
    private void startupProcessor() {
        synchronized (lock) {
            if (processor == null) {
                processor = new Processor();
                executor.execute(new NamePreservingRunnable(processor,
                        threadName));
            }
        }

        // Just stop the select() and start it again, so that the processor
        // can be activated immediately. 
        wakeup();
    }

    /**
     * Loops over the new sessions blocking queue and returns
     * the number of sessions which are effectively created
     *
     * @return The number of new sessions
     */
    private int handleNewSessions() {
        int addedSessions = 0;

        for (;;) {
            T session = newSessions.poll();

            if (session == null) {
                // All new sessions have been handled
                break;
            }

            if (addNow(session)) {
                // A new session has been created 
                addedSessions++;
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
            ((AbstractIoService) session.getService()).getListeners()
                    .fireSessionCreated(session);
            notified = true;
        } catch (Throwable e) {
            if (notified) {
                // Clear the DefaultIoFilterChain.CONNECT_FUTURE attribute
                // and call ConnectFuture.setException().
                scheduleRemove(session);
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.fireExceptionCaught(e);
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
        for (;;) {
            T session = removingSessions.poll();

            if (session == null) {
                break;
            }

            SessionState state = state(session);
            switch (state) {
            case OPEN:
                if (removeNow(session)) {
                    removedSessions++;
                }
                break;
            case CLOSED:
                // Skip if channel is already closed
                break;
            case PREPARING:
                // Remove session from the newSessions queue and
                // remove it
                newSessions.remove(session);
                if (removeNow(session)) {
                    removedSessions++;
                }
                break;
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
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
        } finally {
            clearWriteRequestQueue(session);
            ((AbstractIoService) session.getService()).getListeners()
                    .fireSessionDestroyed(session);
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
                    IoFilterChain filterChain = session.getFilterChain();
                    filterChain.fireMessageSent(req);
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
            WriteToClosedSessionException cause = new WriteToClosedSessionException(
                    failedRequests);
            for (WriteRequest r : failedRequests) {
                session.decreaseScheduledBytesAndMessages(r);
                r.getFuture().setException(cause);
            }
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(cause);
        }
    }

    private void process() throws Exception {
        for (Iterator<T> i = selectedSessions(); i.hasNext();) {
            T session = i.next();
            process(session);
            i.remove();
        }
    }

    /**
     * Deal with session ready for the read or write operations, or both.
     */
    private void process(T session) {
        // Process Reads
        if (isReadable(session) && !session.isReadSuspended()) {
            read(session);
        }

        // Process writes
        if (isWritable(session) && !session.isWriteSuspended()) {
            scheduleFlush(session);
        }
    }

    private void read(T session) {
        IoSessionConfig config = session.getConfig();
        IoBuffer buf = IoBuffer.allocate(config.getReadBufferSize());

        final boolean hasFragmentation = session.getTransportMetadata()
                .hasFragmentation();

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
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.fireMessageReceived(buf);
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
                if (!(e instanceof PortUnreachableException)
                        || !AbstractDatagramSessionConfig.class
                                .isAssignableFrom(config.getClass())
                        || ((AbstractDatagramSessionConfig) config)
                                .isCloseOnPortUnreachable())

                    scheduleRemove(session);
            }
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
        }
    }

    private void notifyIdleSessions(long currentTime) throws Exception {
        // process idle sessions
        if (currentTime - lastIdleCheckTime >= SELECT_TIMEOUT) {
            lastIdleCheckTime = currentTime;
            AbstractIoSession.notifyIdleness(allSessions(), currentTime);
        }
    }

    private void flush(long currentTime) {
        final T firstSession = flushingSessions.peek();
        if (firstSession == null) {
            return;
        }

        T session = flushingSessions.poll(); // the same one with firstSession
        for (;;) {
            session.setScheduledForFlush(false);
            SessionState state = state(session);

            switch (state) {
            case OPEN:
                try {
                    boolean flushedAll = flushNow(session, currentTime);
                    if (flushedAll
                            && !session.getWriteRequestQueue().isEmpty(session)
                            && !session.isScheduledForFlush()) {
                        scheduleFlush(session);
                    }
                } catch (Exception e) {
                    scheduleRemove(session);
                    IoFilterChain filterChain = session.getFilterChain();
                    filterChain.fireExceptionCaught(e);
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

            session = flushingSessions.peek();
            if (session == null || session == firstSession) {
                break;
            }
            session = flushingSessions.poll();
        }
    }

    private boolean flushNow(T session, long currentTime) {
        if (!session.isConnected()) {
            scheduleRemove(session);
            return false;
        }

        final boolean hasFragmentation = session.getTransportMetadata()
                .hasFragmentation();

        final WriteRequestQueue writeRequestQueue = session
                .getWriteRequestQueue();

        // Set limitation for the number of written bytes for read-write
        // fairness.  I used maxReadBufferSize * 3 / 2, which yields best
        // performance in my experience while not breaking fairness much.
        final int maxWrittenBytes = session.getConfig().getMaxReadBufferSize()
                + (session.getConfig().getMaxReadBufferSize() >>> 1);
        int writtenBytes = 0;
        try {
            // Clear OP_WRITE
            setInterestedInWrite(session, false);
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
                    localWrittenBytes = writeBuffer(session, req,
                            hasFragmentation, maxWrittenBytes - writtenBytes,
                            currentTime);
                    if (localWrittenBytes > 0
                            && ((IoBuffer) message).hasRemaining()) {
                        // the buffer isn't empty, we re-interest it in writing 
                        writtenBytes += localWrittenBytes;
                        setInterestedInWrite(session, true);
                        return false;
                    }
                } else if (message instanceof FileRegion) {
                    localWrittenBytes = writeFile(session, req,
                            hasFragmentation, maxWrittenBytes - writtenBytes,
                            currentTime);

                    // Fix for Java bug on Linux http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
                    // If there's still data to be written in the FileRegion, return 0 indicating that we need
                    // to pause until writing may resume.
                    if (localWrittenBytes > 0
                            && ((FileRegion) message).getRemainingBytes() > 0) {
                        writtenBytes += localWrittenBytes;
                        setInterestedInWrite(session, true);
                        return false;
                    }
                } else {
                    throw new IllegalStateException(
                            "Don't know how to handle message of type '"
                                    + message.getClass().getName()
                                    + "'.  Are you missing a protocol encoder?");
                }

                if (localWrittenBytes == 0) {
                    // Kernel buffer is full.
                    setInterestedInWrite(session, true);
                    return false;
                }

                writtenBytes += localWrittenBytes;

                if (writtenBytes >= maxWrittenBytes) {
                    // Wrote too much
                    scheduleFlush(session);
                    return false;
                }
            } while (writtenBytes < maxWrittenBytes);
        } catch (Exception e) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
            return false;
        }

        return true;
    }

    private int writeBuffer(T session, WriteRequest req,
            boolean hasFragmentation, int maxLength, long currentTime)
            throws Exception {
        IoBuffer buf = (IoBuffer) req.getMessage();
        int localWrittenBytes = 0;
        if (buf.hasRemaining()) {
            int length;
            if (hasFragmentation) {
                length = Math.min(buf.remaining(), maxLength);
            } else {
                length = buf.remaining();
            }
            for (int i = WRITE_SPIN_COUNT; i > 0; i--) {
                localWrittenBytes = write(session, buf, length);
                if (localWrittenBytes != 0) {
                    break;
                }
            }
        }

        session.increaseWrittenBytes(localWrittenBytes, currentTime);

        if (!buf.hasRemaining() || !hasFragmentation && localWrittenBytes != 0) {
            // Buffer has been sent, clear the current request.
            buf.reset();
            fireMessageSent(session, req);
        }
        return localWrittenBytes;
    }

    private int writeFile(T session, WriteRequest req,
            boolean hasFragmentation, int maxLength, long currentTime)
            throws Exception {
        int localWrittenBytes;
        FileRegion region = (FileRegion) req.getMessage();
        if (region.getRemainingBytes() > 0) {
            int length;
            if (hasFragmentation) {
                length = (int) Math.min(region.getRemainingBytes(), maxLength);
            } else {
                length = (int) Math.min(Integer.MAX_VALUE, region
                        .getRemainingBytes());
            }
            localWrittenBytes = transferFile(session, region, length);
            region.update(localWrittenBytes);
        } else {
            localWrittenBytes = 0;
        }

        session.increaseWrittenBytes(localWrittenBytes, currentTime);

        if (region.getRemainingBytes() <= 0 || !hasFragmentation
                && localWrittenBytes != 0) {
            fireMessageSent(session, req);
        }

        return localWrittenBytes;
    }

    private void fireMessageSent(T session, WriteRequest req) {
        session.setCurrentWriteRequest(null);
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageSent(req);
    }

    private void updateTrafficMask() {
        for (;;) {
            T session = trafficControllingSessions.poll();

            if (session == null) {
                break;
            }

            SessionState state = state(session);
            switch (state) {
            case OPEN:
                updateTrafficControl(session);
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

    public void updateTrafficControl(T session) {
        try {
            setInterestedInRead(session, !session.isReadSuspended());
        } catch (Exception e) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
        }
        try {
            setInterestedInWrite(session, !session.getWriteRequestQueue()
                    .isEmpty(session)
                    && !session.isWriteSuspended());
        } catch (Exception e) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
        }
    }

    private class Processor implements Runnable {
        public void run() {
            int nSessions = 0;
            lastIdleCheckTime = System.currentTimeMillis();

            for (;;) {
                try {
                    // This select has a timeout so that we can manage
                    // idle session when we get out of the select every
                    // second. (note : this is a hack to avoid creating
                    // a dedicated thread).
                    int selected = select(SELECT_TIMEOUT);

                    nSessions += handleNewSessions();
                    updateTrafficMask();

                    // Now, if we have had some incoming or outgoing events,
                    // deal with them
                    if (selected > 0) {
                        process();
                    }

                    long currentTime = System.currentTimeMillis();
                    flush(currentTime);
                    nSessions -= remove();
                    notifyIdleSessions(currentTime);

                    if (nSessions == 0) {
                        synchronized (lock) {
                            if (newSessions.isEmpty() && isSelectorEmpty()) {
                                processor = null;
                                break;
                            }
                        }
                    }

                    // Disconnect all sessions immediately if disposal has been
                    // requested so that we exit this loop eventually.
                    if (isDisposing()) {
                        for (Iterator<T> i = allSessions(); i.hasNext();) {
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

            try {
                synchronized (disposalLock) {
                    if (isDisposing()) {
                        dispose0();
                    }
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            } finally {
                disposalFuture.setValue(true);
            }
        }
    }

    protected static enum SessionState {
        OPEN, CLOSED, PREPARING,
    }
}
