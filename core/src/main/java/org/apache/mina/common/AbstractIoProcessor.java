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
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.util.NamePreservingRunnable;

/**
 * An abstract implementation of {@link IoProcessor} which helps
 * transport developers to write an {@link IoProcessor} easily.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * TODO Provide abstraction for bind, unbind and connect (+ cancellation)
 */
public abstract class AbstractIoProcessor implements IoProcessor {

    private final Object lock = new Object();
    private final String threadName;
    private final Executor executor;

    private final Queue<AbstractIoSession> newSessions =
        new ConcurrentLinkedQueue<AbstractIoSession>();
    private final Queue<AbstractIoSession> removingSessions = 
        new ConcurrentLinkedQueue<AbstractIoSession>();
    private final Queue<AbstractIoSession> flushingSessions = 
        new ConcurrentLinkedQueue<AbstractIoSession>();
    private final Queue<AbstractIoSession> trafficControllingSessions = 
        new ConcurrentLinkedQueue<AbstractIoSession>();

    private Worker worker;
    private long lastIdleCheckTime;

    protected AbstractIoProcessor(String threadName, Executor executor) {
        this.threadName = threadName;
        this.executor = executor;
    }
    
    protected abstract int select(int timeout) throws Exception;

    protected abstract void wakeup();
    
    protected abstract Iterator<AbstractIoSession> allSessions() throws Exception;

    protected abstract Iterator<AbstractIoSession> selectedSessions() throws Exception;

    protected abstract SessionState state(IoSession session);

    protected abstract int readyOps(IoSession session) throws Exception;

    protected abstract int interestOps(IoSession session) throws Exception;

    protected abstract void interestOps(IoSession session, int interestOps) throws Exception;

    protected abstract void doAdd(IoSession session) throws Exception;

    protected abstract void doRemove(IoSession session) throws Exception;

    protected abstract int read(IoSession session, ByteBuffer buf) throws Exception;

    protected abstract int write(IoSession session, ByteBuffer buf) throws Exception;

    protected abstract long transferFile(IoSession session, FileRegion region) throws Exception;

    public void add(IoSession session) {
        newSessions.add((AbstractIoSession) session);
        startupWorker();
    }

    public void remove(IoSession session) {
        scheduleRemove((AbstractIoSession) session);
        startupWorker();
    }

    public void flush(IoSession session, WriteRequest writeRequest) {
        boolean needsWakeup = flushingSessions.isEmpty();
        if (scheduleFlush((AbstractIoSession) session) && needsWakeup) {
            wakeup();
        }
    }

    public void updateTrafficMask(IoSession session) {
        scheduleTrafficControl((AbstractIoSession) session);
        wakeup();
    }

    private void startupWorker() {
        synchronized (lock) {
            if (worker == null) {
                worker = new Worker();
                executor.execute(new NamePreservingRunnable(worker));
            }
        }
        wakeup();
    }

    private void scheduleRemove(AbstractIoSession session) {
        removingSessions.add(session);
    }

    private boolean scheduleFlush(AbstractIoSession session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        }
        return false;
    }

    private void scheduleTrafficControl(AbstractIoSession session) {
        trafficControllingSessions.add(session);
    }

    private int add() {
        int addedSessions = 0;
        for (; ;) {
            AbstractIoSession session = newSessions.poll();

            if (session == null) {
                break;
            }

            try {
                doAdd(session);
                addedSessions ++;
                
                // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
                // in AbstractIoFilterChain.fireSessionOpened().
                ((AbstractIoService) session.getService()).getListeners().fireSessionCreated(session);
            } catch (Exception e) {
                // Clear the DefaultIoFilterChain.CONNECT_FUTURE attribute
                // and call ConnectFuture.setException().
                session.getFilterChain().fireExceptionCaught(session, e);
            }
        }
        
        return addedSessions;
    }
    
    private int remove() {
        int removedSessions = 0;
        for (; ;) {
            AbstractIoSession session = removingSessions.poll();

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
                    session.getFilterChain().fireExceptionCaught(session, e);
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
        for (Iterator<AbstractIoSession> i = selectedSessions(); i.hasNext();) {
            process(i.next());
            i.remove();
        }
    }
    
    private void process(AbstractIoSession session) throws Exception {
        int readyOps = readyOps(session);
        if (((readyOps & SelectionKey.OP_READ) != 0) && session.getTrafficMask().isReadable()) {
            read(session);
        }

        if (((readyOps & SelectionKey.OP_WRITE) != 0) && session.getTrafficMask().isWritable()) {
            scheduleFlush(session);
        }
    }
    
    private void read(AbstractIoSession session) {
        IoSessionConfig config = session.getConfig();
        ByteBuffer buf = ByteBuffer.allocate(config.getReadBufferSize());

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

            session.increaseReadBytes(readBytes);

            if (readBytes > 0) {
                session.getFilterChain().fireMessageReceived(session, buf);
                buf = null;
                
                if (!session.getTransportMetadata().hasFragmentation()) {
                    if (readBytes * 2 < config.getReadBufferSize()) {
                        if (config.getReadBufferSize() > config.getMinReadBufferSize()) {
                            config.setReadBufferSize(config.getReadBufferSize() >>> 1);
                        }
                    } else if (readBytes == config.getReadBufferSize()) {
                        int newReadBufferSize = config.getReadBufferSize() << 1;
                        if (newReadBufferSize <= (config.getMaxReadBufferSize())) {
                            config.setReadBufferSize(newReadBufferSize);
                        } else {
                            config.setReadBufferSize(config.getMaxReadBufferSize());
                        }
                    }
                }
            }
            if (ret < 0) {
                scheduleRemove(session);
            }
        } catch (IOException e) {
            scheduleRemove(session);
            session.getFilterChain().fireExceptionCaught(session, e);
        } catch (Throwable e) {
            session.getFilterChain().fireExceptionCaught(session, e);
        }
    }
    
    private void notifyIdleness() throws Exception {
        // process idle sessions
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastIdleCheckTime) >= 1000) {
            lastIdleCheckTime = currentTime;
            for (Iterator<AbstractIoSession> i = allSessions(); i.hasNext();) {
                AbstractIoSession session = i.next();
                try {
                    notifyIdleness(session, currentTime);
                } catch (Exception e) {
                    session.getFilterChain().fireExceptionCaught(session, e);
                }
            }
        }
    }

    private void notifyIdleness(AbstractIoSession session, long currentTime) throws Exception {
        notifyIdleness0(session, currentTime, session
                .getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(session.getLastIoTime(), session
                .getLastIdleTime(IdleStatus.BOTH_IDLE)));
        notifyIdleness0(session, currentTime, session
                .getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE, Math.max(session.getLastReadTime(),
                session.getLastIdleTime(IdleStatus.READER_IDLE)));
        notifyIdleness0(session, currentTime, session
                .getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE, Math.max(session.getLastWriteTime(),
                session.getLastIdleTime(IdleStatus.WRITER_IDLE)));

        notifyWriteTimeout(session, currentTime, session
                .getConfig().getWriteTimeoutInMillis(), session.getLastWriteTime());
    }

    private void notifyIdleness0(AbstractIoSession session, long currentTime,
                                 long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && (currentTime - lastIoTime) >= idleTime) {
            session.increaseIdleCount(status);
            session.getFilterChain().fireSessionIdle(session, status);
        }
    }

    private void notifyWriteTimeout(AbstractIoSession session,
                                    long currentTime, long writeTimeout, long lastIoTime) throws Exception {
        if (writeTimeout > 0 && (currentTime - lastIoTime) >= writeTimeout
                && (interestOps(session) & SelectionKey.OP_WRITE) != 0) {
            session.getFilterChain().fireExceptionCaught(session,
                    new WriteTimeoutException());
        }
    }

    private void flush() {
        if (flushingSessions.size() == 0) {
            return;
        }

        for (; ;) {
            AbstractIoSession session = flushingSessions.poll();

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
                    boolean flushedAll = flush(session);
                    if (flushedAll && !session.getWriteRequestQueue().isEmpty() && !session.isScheduledForFlush()) {
                        scheduleFlush(session);
                    }
                } catch (Exception e) {
                    scheduleRemove(session);
                    session.getFilterChain().fireExceptionCaught(session, e);
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
    
    private void clearWriteRequestQueue(AbstractIoSession session) {
        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();
        WriteRequest req;

        if ((req = writeRequestQueue.poll()) != null) {
            Object m = req.getMessage();
            if (m instanceof ByteBuffer) {
                ByteBuffer buf = (ByteBuffer) req.getMessage();

                // The first unwritten empty buffer must be
                // forwarded to the filter chain.
                if (buf.hasRemaining()) {
                    req.getFuture().setWritten(false);
                } else {
                    session.getFilterChain().fireMessageSent(session, req);
                }
            } else {
                req.getFuture().setWritten(false);
            }

            // Discard others.
            while ((req = writeRequestQueue.poll()) != null) {
                req.getFuture().setWritten(false);
            }
        }
    }
    
    private boolean flush(AbstractIoSession session) throws Exception {
        // Clear OP_WRITE
        interestOps(session, interestOps(session) & (~SelectionKey.OP_WRITE));

        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();

        // Set limitation for the number of written bytes for read-write
        // fairness.
        int maxWrittenBytes = session.getConfig().getMaxReadBufferSize();
        int writtenBytes = 0;
        
        try {
            do {
                // Check for pending writes.
                WriteRequest req = writeRequestQueue.peek();

                if (req == null) {
                    break;
                }

                Object message = req.getMessage();
                if (message instanceof FileRegion) {
                    FileRegion region = (FileRegion) message;

                    if (region.getCount() <= 0) {
                        // File has been sent, remove from queue
                        writeRequestQueue.poll();
                        session.increaseWrittenMessages();
                        session.getFilterChain().fireMessageSent(session, req);
                        continue;
                    }

                    if ((readyOps(session) & SelectionKey.OP_WRITE) != 0) {
                        long localWrittenBytes = transferFile(session, region);
                        region.setPosition(region.getPosition() + localWrittenBytes);
                        writtenBytes += localWrittenBytes;
                    }
                    
                    if (region.getCount() > 0 || writtenBytes >= maxWrittenBytes) {
                        // Kernel buffer is full or wrote too much.
                        interestOps(session, interestOps(session) | SelectionKey.OP_WRITE);
                        return false;
                    }

                } else {
                    ByteBuffer buf = (ByteBuffer) message;
                    if (buf.remaining() == 0) {
                        // Buffer has been completely sent, remove request form queue
                        writeRequestQueue.poll();

                        session.increaseWrittenMessages();

                        buf.reset();
                        session.getFilterChain().fireMessageSent(session, req);
                        continue;
                    }

                    if ((readyOps(session) & SelectionKey.OP_WRITE) != 0) {
                        writtenBytes += write(session, buf);
                    }

                    if (buf.hasRemaining() || writtenBytes >= maxWrittenBytes) {
                        // Kernel buffer is full or wrote too much.
                        interestOps(session, interestOps(session) | SelectionKey.OP_WRITE);
                        return false;
                    }
                }
            } while (writtenBytes < maxWrittenBytes);
        } finally {
            session.increaseWrittenBytes(writtenBytes);
        }

        return true;
    }
    
    private void updateTrafficMask() {
        for (; ;) {
            AbstractIoSession session = trafficControllingSessions.poll();

            if (session == null) {
                break;
            }
            
            SessionState state = state(session);
            switch (state) {
            case OPEN:
                // The normal is OP_READ and, if there are write requests in the
                // session's write queue, set OP_WRITE to trigger flushing.
                int ops = SelectionKey.OP_READ;
                if (!session.getWriteRequestQueue().isEmpty()) {
                    ops |= SelectionKey.OP_WRITE;
                }

                // Now mask the preferred ops with the mask of the current session
                int mask = session.getTrafficMask().getInterestOps();
                try {
                    interestOps(session, ops & mask);
                } catch (Exception e) {
                    session.getFilterChain().fireExceptionCaught(session, e);
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
            
            Thread.currentThread().setName(AbstractIoProcessor.this.threadName);
            lastIdleCheckTime = System.currentTimeMillis();

            for (;;) {
                try {
                    int nKeys = select(1000);
                    
                    nSessions += add();
                    updateTrafficMask();

                    if (nKeys > 0) {
                        process();
                    }

                    flush();
                    nSessions -= remove();
                    notifyIdleness();

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
        }
    }
    
    protected static enum SessionState {
        OPEN,
        CLOSED,
        PREPARING,
    }
}
