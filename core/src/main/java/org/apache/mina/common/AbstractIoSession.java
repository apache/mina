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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.util.CircularQueue;


/**
 * Base implementation of {@link IoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoSession implements IoSession {

    private static final AttributeKey READY_READ_FUTURES =
        new AttributeKey(AbstractIoSession.class, "readyReadFutures");
    private static final AttributeKey WAITING_READ_FUTURES =
        new AttributeKey(AbstractIoSession.class, "waitingReadFutures");
    
    private static final IoFutureListener<CloseFuture> SCHEDULED_COUNTER_RESETTER =
        new IoFutureListener<CloseFuture>() {
            public void operationComplete(CloseFuture future) {
                AbstractIoSession s = (AbstractIoSession) future.getSession();
                s.scheduledWriteBytes.set(0);
                s.scheduledWriteMessages.set(0);
                s.readBytesThroughput = 0;
                s.readMessagesThroughput = 0;
                s.writtenBytesThroughput = 0;
                s.writtenMessagesThroughput = 0;
            }
    };

    /**
     * An internal write request object that triggers session close.
     * @see #writeRequestQueue
     */
    private static final WriteRequest CLOSE_REQUEST =
        new DefaultWriteRequest(new Object());

    private final Object lock = new Object();
    
    private IoSessionAttributeMap attributes;
    private WriteRequestQueue writeRequestQueue;
    private WriteRequest currentWriteRequest;
    private final long creationTime;
    
    /**
     * A future that will be set 'closed' when the connection is closed.
     */
    private final CloseFuture closeFuture = new DefaultCloseFuture(this);

    private volatile boolean closing;
    private volatile TrafficMask trafficMask = TrafficMask.ALL;

    // Status variables
    private final AtomicBoolean scheduledForFlush = new AtomicBoolean();
    private final AtomicLong scheduledWriteBytes = new AtomicLong();
    private final AtomicInteger scheduledWriteMessages = new AtomicInteger();

    private long readBytes;
    private long writtenBytes;
    private long readMessages;
    private long writtenMessages;
    private long lastReadTime;
    private long lastWriteTime;
    
    private long lastThroughputCalculationTime;
    private long lastReadBytes;
    private long lastWrittenBytes;
    private long lastReadMessages;
    private long lastWrittenMessages;
    private double readBytesThroughput;
    private double writtenBytesThroughput;
    private double readMessagesThroughput;
    private double writtenMessagesThroughput;

    private int idleCountForBoth;
    private int idleCountForRead;
    private int idleCountForWrite;

    private long lastIdleTimeForBoth;
    private long lastIdleTimeForRead;
    private long lastIdleTimeForWrite;

    private boolean deferDecreaseReadBuffer = true;

    protected AbstractIoSession() {
        creationTime = lastThroughputCalculationTime = 
            lastReadTime = lastWriteTime =
            lastIdleTimeForBoth = lastIdleTimeForRead =
            lastIdleTimeForWrite = System.currentTimeMillis();
        closeFuture.addListener(SCHEDULED_COUNTER_RESETTER);
    }
    
    public final long getId() {
        return hashCode() & 0xFFFFFFFFL;
    }

    @SuppressWarnings("unchecked")
    protected abstract IoProcessor getProcessor();

    public final boolean isConnected() {
        return !closeFuture.isClosed();
    }

    public final boolean isClosing() {
        return closing || closeFuture.isClosed();
    }

    public final CloseFuture getCloseFuture() {
        return closeFuture;
    }

    protected final boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }

    protected final boolean setScheduledForFlush(boolean flag) {
        if (flag) {
            return scheduledForFlush.compareAndSet(false, true);
        } else {
            scheduledForFlush.set(false);
            return true;
        }
    }

    public final CloseFuture close(boolean rightNow) {
        if (rightNow) {
            return close();
        } else {
            return closeOnFlush();
        }
    }

    public final CloseFuture close() {
        synchronized (lock) {
            if (isClosing()) {
                return closeFuture;
            } else {
                closing = true;
            }
        }

        getFilterChain().fireFilterClose();
        return closeFuture;
    }

    @SuppressWarnings("unchecked")
    public final CloseFuture closeOnFlush() {
        getWriteRequestQueue().offer(this, CLOSE_REQUEST);
        getProcessor().flush(this);
        return closeFuture;
    }

    public final ReadFuture read() {
        if (!getConfig().isUseReadOperation()) {
            throw new IllegalStateException("useReadOperation is not enabled.");
        }
        
        Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
        ReadFuture future;
        synchronized (readyReadFutures) {
            future = readyReadFutures.poll();
            if (future != null) {
                if (future.isClosed()) {
                    // Let other readers get notified.
                    readyReadFutures.offer(future);
                }
            } else {
                future = new DefaultReadFuture(this);
                getWaitingReadFutures().offer(future);
            }
        }
        
        return future;
    }
    
    protected final void offerReadFuture(Object message) {
        newReadFuture().setRead(message);
    }

    protected final void offerFailedReadFuture(Throwable exception) {
        newReadFuture().setException(exception);
    }

    protected final void offerClosedReadFuture() {
        Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
        synchronized (readyReadFutures) {
            newReadFuture().setClosed();
        }
    }

    private ReadFuture newReadFuture() {
        Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
        Queue<ReadFuture> waitingReadFutures = getWaitingReadFutures();
        ReadFuture future;
        synchronized (readyReadFutures) {
            future = waitingReadFutures.poll();
            if (future == null) {
                future = new DefaultReadFuture(this);
                readyReadFutures.offer(future);
            }
        }
        return future;
    }

    @SuppressWarnings("unchecked")
    private Queue<ReadFuture> getReadyReadFutures() {
        Queue<ReadFuture> readyReadFutures = 
            (Queue<ReadFuture>) getAttribute(READY_READ_FUTURES);
        if (readyReadFutures == null) {
            readyReadFutures = new CircularQueue<ReadFuture>();
            
            Queue<ReadFuture> oldReadyReadFutures =
                (Queue<ReadFuture>) setAttributeIfAbsent(
                        READY_READ_FUTURES, readyReadFutures);
            if (oldReadyReadFutures != null) {
                readyReadFutures = oldReadyReadFutures;
            }
            
            // Initialize waitingReadFutures together.
            Queue<ReadFuture> waitingReadFutures = 
                new CircularQueue<ReadFuture>();
            setAttributeIfAbsent(WAITING_READ_FUTURES, waitingReadFutures);
        }
        return readyReadFutures;
    }

    @SuppressWarnings("unchecked")
    private Queue<ReadFuture> getWaitingReadFutures() {
        return (Queue<ReadFuture>) getAttribute(WAITING_READ_FUTURES);
    }

    public final WriteFuture write(Object message) {
        return write(message, null);
    }

    public final WriteFuture write(Object message, SocketAddress remoteAddress) {
        if (message == null) {
            throw new NullPointerException("message");
        }

        if (!getTransportMetadata().isConnectionless() &&
                remoteAddress != null) {
            throw new UnsupportedOperationException();
        }

        if (isClosing() || !isConnected()) {
            WriteFuture future = new DefaultWriteFuture(this);
            WriteRequest request = new DefaultWriteRequest(message, future, remoteAddress);
            future.setException(new WriteToClosedSessionException(request));
            return future;
        }

        FileChannel channel = null;
        if (message instanceof IoBuffer
                && !((IoBuffer) message).hasRemaining()) {
            throw new IllegalArgumentException(
                    "message is empty. Forgot to call flip()?");
        } else if (message instanceof FileChannel) {
            channel = (FileChannel) message;
            try {
                message = new DefaultFileRegion(channel, 0, channel.size());
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
                return DefaultWriteFuture.newNotWrittenFuture(this, e);
            }
        } else if (message instanceof File) {
            File file = (File) message;
            try {
                channel = new FileInputStream(file).getChannel();
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
                return DefaultWriteFuture.newNotWrittenFuture(this, e);
            }
        }

        WriteFuture future = new DefaultWriteFuture(this);
        getFilterChain().fireFilterWrite(
                new DefaultWriteRequest(message, future, remoteAddress));

        if (message instanceof File) {
            final FileChannel finalChannel = channel;
            future.addListener(new IoFutureListener<WriteFuture>() {
                public void operationComplete(WriteFuture future) {
                    try {
                        finalChannel.close();
                    } catch (IOException e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            });
        }

        return future;
    }

    public final Object getAttachment() {
        return getAttribute("");
    }

    public final Object setAttachment(Object attachment) {
        return setAttribute("", attachment);
    }

    public final Object getAttribute(Object key) {
        return attributes.getAttribute(this, key);
    }

    public final Object getAttribute(Object key, Object defaultValue) {
        return attributes.getAttribute(this, key, defaultValue);
    }

    public final Object setAttribute(Object key, Object value) {
        return attributes.setAttribute(this, key, value);
    }

    public final Object setAttribute(Object key) {
        return attributes.setAttribute(this, key);
    }

    public final Object setAttributeIfAbsent(Object key, Object value) {
        return attributes.setAttributeIfAbsent(this, key, value);
    }

    public final Object removeAttribute(Object key) {
        return attributes.removeAttribute(this, key);
    }

    public final boolean removeAttribute(Object key, Object value) {
        return attributes.removeAttribute(this, key, value);
    }

    public final boolean replaceAttribute(Object key, Object oldValue, Object newValue) {
        return attributes.replaceAttribute(this, key, oldValue, newValue);
    }

    public final boolean containsAttribute(Object key) {
        return attributes.containsAttribute(this, key);
    }

    public final Set<Object> getAttributeKeys() {
        return attributes.getAttributeKeys(this);
    }
    
    protected final IoSessionAttributeMap getAttributeMap() {
        return attributes;
    }

    protected final void setAttributeMap(IoSessionAttributeMap attributes) {
        this.attributes = attributes;
    }
    
    protected final void setWriteRequestQueue(WriteRequestQueue writeRequestQueue) {
        this.writeRequestQueue =
            new CloseRequestAwareWriteRequestQueue(writeRequestQueue);
    }

    public final TrafficMask getTrafficMask() {
        return trafficMask;
    }

    public final void setTrafficMask(TrafficMask trafficMask) {
        if (trafficMask == null) {
            throw new NullPointerException("trafficMask");
        }
        
        if (isClosing() || !isConnected()) {
            return;
        }
        
        getFilterChain().fireFilterSetTrafficMask(trafficMask);
    }
    
    protected final void setTrafficMaskNow(TrafficMask trafficMask) {
        this.trafficMask = trafficMask;
    }

    public final void suspendRead() {
        setTrafficMask(getTrafficMask().and(TrafficMask.READ.not()));
    }

    public final void suspendWrite() {
        setTrafficMask(getTrafficMask().and(TrafficMask.WRITE.not()));
    }

    public final void resumeRead() {
        setTrafficMask(getTrafficMask().or(TrafficMask.READ));
    }

    public final void resumeWrite() {
        setTrafficMask(getTrafficMask().or(TrafficMask.WRITE));
    }

    public final long getReadBytes() {
        return readBytes;
    }

    public final long getWrittenBytes() {
        return writtenBytes;
    }

    public final long getReadMessages() {
        return readMessages;
    }

    public final long getWrittenMessages() {
        return writtenMessages;
    }

    public final double getReadBytesThroughput() {
        return readBytesThroughput;
    }

    public final double getWrittenBytesThroughput() {
        return writtenBytesThroughput;
    }

    public final double getReadMessagesThroughput() {
        return readMessagesThroughput;
    }

    public final double getWrittenMessagesThroughput() {
        return writtenMessagesThroughput;
    }
    
    protected final void updateThroughput(long currentTime) {
        int interval = (int) (currentTime - lastThroughputCalculationTime);
        long minInterval = getConfig().getThroughputCalculationIntervalInMillis();
        if (minInterval == 0 || interval < minInterval) {
            return;
        }
        
        readBytesThroughput = (readBytes - lastReadBytes) * 1000.0 / interval;
        writtenBytesThroughput = (writtenBytes - lastWrittenBytes) * 1000.0 / interval;
        readMessagesThroughput = (readMessages - lastReadMessages) * 1000.0 / interval;
        writtenMessagesThroughput = (writtenMessages - lastWrittenMessages) * 1000.0 / interval;
        
        lastReadBytes = readBytes;
        lastWrittenBytes = writtenBytes;
        lastReadMessages = readMessages;
        lastWrittenMessages = writtenMessages;
        
        lastThroughputCalculationTime = currentTime;
    }

    public final long getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }

    public final int getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    protected final void increaseReadBytesAndMessages(
            Object message, long currentTime) {
        if (message instanceof IoBuffer) {
            IoBuffer b = (IoBuffer) message;
            if (b.hasRemaining()) {
                increaseReadBytes(((IoBuffer) message).remaining(), currentTime);
            } else {
                increaseReadMessages(currentTime);
            }
        } else {
            increaseReadMessages(currentTime);
        }
    }
    
    private void increaseReadBytes(long increment, long currentTime) {
        if (increment <= 0) {
            return;
        }
        
        readBytes += increment;
        lastReadTime = currentTime;
        idleCountForBoth = 0;
        idleCountForRead = 0;

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseReadBytes(increment, currentTime);
        }
    }
    
    private void increaseReadMessages(long currentTime) {
        readMessages++;
        lastWriteTime = currentTime;
        idleCountForBoth = 0;
        idleCountForRead = 0;
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseReadMessages(currentTime);
        }
    }

    protected final void increaseWrittenBytesAndMessages(
            WriteRequest request, long currentTime) {
        
        Object message = request.getMessage();
        if (message instanceof IoBuffer) {
            IoBuffer b = (IoBuffer) message;
            if (b.hasRemaining()) {
                increaseWrittenBytes(((IoBuffer) message).remaining(), currentTime);
            } else {
                increaseWrittenMessages(currentTime);
            }
        } else {
            increaseWrittenMessages(currentTime);
        }
    }
    
    private void increaseWrittenBytes(long increment, long currentTime) {
        if (increment <= 0) {
            return;
        }

        writtenBytes += increment;
        lastWriteTime = currentTime;
        idleCountForBoth = 0;
        idleCountForWrite = 0;

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseWrittenBytes(increment, currentTime);
        }

        increaseScheduledWriteBytes(-increment);
    }

    private void increaseWrittenMessages(long currentTime) {
        writtenMessages++;
        lastWriteTime = currentTime;
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseWrittenMessages(currentTime);
        }

        decreaseScheduledWriteMessages();
    }

    protected final void increaseScheduledWriteBytes(long increment) {
        scheduledWriteBytes.addAndGet(increment);
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseScheduledWriteBytes(increment);
        }
    }

    protected final void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseScheduledWriteMessages();
        }
    }

    private void decreaseScheduledWriteMessages() {
        scheduledWriteMessages.decrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).decreaseScheduledWriteMessages();
        }
    }

    protected final void decreaseScheduledBytesAndMessages(WriteRequest request) {
        Object message = request.getMessage();
        if (message instanceof IoBuffer) {
            IoBuffer b = (IoBuffer) message;
            if (b.hasRemaining()) {
                increaseScheduledWriteBytes(-((IoBuffer) message).remaining());
            } else {
                decreaseScheduledWriteMessages();
            }
        } else {
            decreaseScheduledWriteMessages();
        }
    }

    protected final WriteRequestQueue getWriteRequestQueue() {
        if (writeRequestQueue == null) {
            throw new IllegalStateException();
        }
        return writeRequestQueue;
    }
    
    protected final WriteRequest getCurrentWriteRequest() {
        return currentWriteRequest;
    }
    
    protected final void setCurrentWriteRequest(WriteRequest currentWriteRequest) {
        this.currentWriteRequest = currentWriteRequest;
    }

    protected final void increaseReadBufferSize() {
        int newReadBufferSize = getConfig().getReadBufferSize() << 1;
        if (newReadBufferSize <= getConfig().getMaxReadBufferSize()) {
            getConfig().setReadBufferSize(newReadBufferSize);
        } else {
            getConfig().setReadBufferSize(getConfig().getMaxReadBufferSize());
        }

        deferDecreaseReadBuffer = true;
    }

    protected final void decreaseReadBufferSize() {
        if (deferDecreaseReadBuffer) {
            deferDecreaseReadBuffer = false;
            return;
        }

        if (getConfig().getReadBufferSize() > getConfig().getMinReadBufferSize()) {
            getConfig().setReadBufferSize(getConfig().getReadBufferSize() >>> 1);
        }

        deferDecreaseReadBuffer = true;
    }

    public final long getCreationTime() {
        return creationTime;
    }

    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    public final long getLastReadTime() {
        return lastReadTime;
    }

    public final long getLastWriteTime() {
        return lastWriteTime;
    }

    public final boolean isIdle(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth > 0;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead > 0;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite > 0;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public final boolean isBothIdle() {
        return isIdle(IdleStatus.BOTH_IDLE);
    }

    public final boolean isReaderIdle() {
        return isIdle(IdleStatus.READER_IDLE);
    }

    public final boolean isWriterIdle() {
        return isIdle(IdleStatus.WRITER_IDLE);
    }
    
    public final int getIdleCount(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleCountForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleCountForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleCountForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public final long getLastIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return lastIdleTimeForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return lastIdleTimeForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return lastIdleTimeForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    protected final void increaseIdleCount(IdleStatus status, long currentTime) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth++;
            lastIdleTimeForBoth = currentTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead++;
            lastIdleTimeForRead = currentTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite++;
            lastIdleTimeForWrite = currentTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }

    public final int getBothIdleCount() {
        return getIdleCount(IdleStatus.BOTH_IDLE);
    }

    public final long getLastBothIdleTime() {
        return getLastIdleTime(IdleStatus.BOTH_IDLE);
    }

    public final long getLastReaderIdleTime() {
        return getLastIdleTime(IdleStatus.READER_IDLE);
    }

    public final long getLastWriterIdleTime() {
        return getLastIdleTime(IdleStatus.WRITER_IDLE);
    }

    public final int getReaderIdleCount() {
        return getIdleCount(IdleStatus.READER_IDLE);
    }

    public final int getWriterIdleCount() {
        return getIdleCount(IdleStatus.WRITER_IDLE);
    }

    public SocketAddress getServiceAddress() {
        IoService service = getService();
        if (service instanceof IoAcceptor) {
            return ((IoAcceptor) service).getLocalAddress();
        } else {
            return getRemoteAddress();
        }
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        if (getService() instanceof IoAcceptor) {
            return "(" + getIdAsString() + ": " + getServiceName() + ", server, " +
                    getRemoteAddress() + " => " + getLocalAddress() + ')';
        } else {
            return "(" + getIdAsString() + ": " + getServiceName() + ", client, " +
                    getLocalAddress() + " => " + getRemoteAddress() + ')';
        }
    }
    
    private String getIdAsString() {
        String id = Long.toHexString(getId()).toUpperCase();

        // Somewhat inefficient, but it won't happen that often
        // because an ID is often a big integer.
        while (id.length() < 8) {
            id = '0' + id; // padding
        }
        id = "0x" + id;
        
        return id;
    }

    private String getServiceName() {
        TransportMetadata tm = getTransportMetadata();
        if (tm == null) {
            return "null";
        } else {
            return tm.getProviderName() + ' ' + tm.getName();
        }
    }
    
    private class CloseRequestAwareWriteRequestQueue implements WriteRequestQueue {
        
        private final WriteRequestQueue q;
        
        public CloseRequestAwareWriteRequestQueue(WriteRequestQueue q) {
            this.q = q;
        }

        public synchronized WriteRequest poll(IoSession session) {
            WriteRequest answer = q.poll(session);
            if (answer == CLOSE_REQUEST) {
                AbstractIoSession.this.close();
                dispose(session);
                answer = null;
            }
            return answer;
        }
        
        public void offer(IoSession session, WriteRequest e) {
            q.offer(session, e);
        }

        public boolean isEmpty(IoSession session) {
            return q.isEmpty(session);
        }
        
        public void clear(IoSession session) {
            q.clear(session);
        }

        public void dispose(IoSession session) {
            q.dispose(session);
        }
    }
}
