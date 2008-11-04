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
package org.apache.mina.core.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.DefaultFileRegion;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.DefaultCloseFuture;
import org.apache.mina.core.future.DefaultReadFuture;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteException;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.util.CircularQueue;
import org.apache.mina.util.ExceptionMonitor;


/**
 * Base implementation of {@link IoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoSession implements IoSession {

    private static final AttributeKey READY_READ_FUTURES_KEY =
        new AttributeKey(AbstractIoSession.class, "readyReadFutures");
    
    private static final AttributeKey WAITING_READ_FUTURES_KEY =
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
    
    // The Session creation's time */
    private final long creationTime;

    /**
     * A future that will be set 'closed' when the connection is closed.
     */
    private final CloseFuture closeFuture = new DefaultCloseFuture(this);

    private volatile boolean closing;
    private volatile TrafficMask trafficMask = TrafficMask.ALL;

    // Status variables
    private final AtomicBoolean scheduledForFlush = new AtomicBoolean();
    private final AtomicInteger scheduledWriteBytes = new AtomicInteger();
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

    /** The list of IoFilter for the incoming chain */
    protected List<IoFilter> incomingChain;

    /** The list of IoFilter for the outgoing chain */
    protected List<IoFilter> outgoingChain;


    /**
     * TODO Add method documentation
     */
    protected AbstractIoSession() {
        // Initialize all the Session counters to the current time 
        long currentTime = System.currentTimeMillis();
        creationTime = currentTime;
        lastThroughputCalculationTime = currentTime;
        lastReadTime = currentTime;
        lastWriteTime = currentTime;
        lastIdleTimeForBoth = currentTime;
        lastIdleTimeForRead = currentTime;
        lastIdleTimeForWrite = currentTime;
        
        // TODO add documentation
        closeFuture.addListener(SCHEDULED_COUNTER_RESETTER);
    }

    /**
     * {@inheritDoc}
     * 
     * TODO this method implementation is totally wrong. It has to
     * be rewritten.
     */
    public final long getId() {
        return hashCode() & 0xFFFFFFFFL;
    }

    /**
     * TODO Add method documentation
     */
    public abstract IoProcessor getProcessor();

    /**
     * {@inheritDoc}
     */
    public final boolean isConnected() {
        return !closeFuture.isClosed();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isClosing() {
        return closing || closeFuture.isClosed();
    }

    /**
     * {@inheritDoc}
     */
    public final CloseFuture getCloseFuture() {
        return closeFuture;
    }

    /**
     * TODO Add method documentation
     */
    public final boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }

    /**
     * TODO Add method documentation
     */
    public final boolean setScheduledForFlush(boolean flag) {
        if (flag) {
            return scheduledForFlush.compareAndSet(false, true);
        } else {
            scheduledForFlush.set(false);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final CloseFuture close(boolean rightNow) {
        if (rightNow) {
            return close();
        } else {
            return closeOnFlush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public final CloseFuture close() {
        synchronized (lock) {
            if (isClosing()) {
                return closeFuture;
            } else {
                closing = true;
            }
        }

        getFirstFilterIn().filterClose(this);
        return closeFuture;
    }

    /**
     * {@inheritDoc}
     */
    public final CloseFuture closeOnFlush() {
        getWriteRequestQueue().offer(this, CLOSE_REQUEST);
        getProcessor().flush(this);
        return closeFuture;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * TODO Add method documentation
     */
    public final void offerReadFuture(Object message) {
        newReadFuture().setRead(message);
    }

    /**
     * TODO Add method documentation
     */
    public final void offerFailedReadFuture(Throwable exception) {
        newReadFuture().setException(exception);
    }

    /**
     * TODO Add method documentation
     */
    public final void offerClosedReadFuture() {
        Queue<ReadFuture> readyReadFutures = getReadyReadFutures();
        synchronized (readyReadFutures) {
            newReadFuture().setClosed();
        }
    }

    /**
     * TODO Add method documentation
     */
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

    /**
     * TODO Add method documentation
     */
    private Queue<ReadFuture> getReadyReadFutures() {
        Queue<ReadFuture> readyReadFutures =
            (Queue<ReadFuture>) getAttribute(READY_READ_FUTURES_KEY);
        if (readyReadFutures == null) {
            readyReadFutures = new CircularQueue<ReadFuture>();

            Queue<ReadFuture> oldReadyReadFutures =
                (Queue<ReadFuture>) setAttributeIfAbsent(
                        READY_READ_FUTURES_KEY, readyReadFutures);
            if (oldReadyReadFutures != null) {
                readyReadFutures = oldReadyReadFutures;
            }
        }
        return readyReadFutures;
    }

    /**
     * TODO Add method documentation
     */
    private Queue<ReadFuture> getWaitingReadFutures() {
        Queue<ReadFuture> waitingReadyReadFutures =
            (Queue<ReadFuture>) getAttribute(WAITING_READ_FUTURES_KEY);
        if (waitingReadyReadFutures == null) {
            waitingReadyReadFutures = new CircularQueue<ReadFuture>();

            Queue<ReadFuture> oldWaitingReadyReadFutures =
                (Queue<ReadFuture>) setAttributeIfAbsent(
                        WAITING_READ_FUTURES_KEY, waitingReadyReadFutures);
            if (oldWaitingReadyReadFutures != null) {
                waitingReadyReadFutures = oldWaitingReadyReadFutures;
            }
        }
        return waitingReadyReadFutures;
    }

    /**
     * {@inheritDoc}
     */
    public final WriteFuture write(Object message) {
        return write(message, null);
    }

    /**
     * {@inheritDoc}
     */
    public List<IoFilter> getFilterChainIn() {
        return incomingChain;
    }

    /**
     * {@inheritDoc}
     */
    public List<IoFilter> getFilterChainOut() {
        return outgoingChain;
    }

    /**
     * Store a copy of the given incoming chain 
     * @param chain The chain to copy
     */
    public void setFilterChainIn(List<IoFilter> chain) {
    	incomingChain = new ArrayList<IoFilter>(chain.size());
    	
    	for (IoFilter filter:chain) {
    		incomingChain.add(filter);
    	}
    }

    /**
     * Store a copy of the given outgoing chain 
     * @param chain The chain to copy
     */
    public void setFilterChainOut(List<IoFilter> chain) {
    	outgoingChain = new ArrayList<IoFilter>(chain.size());
    	
    	for (IoFilter filter:chain) {
    		outgoingChain.add(filter);
    	}
    }

    /**
     * Get the first filter in the incoming chain
     * @return The first filter in the chain
     */
    public IoFilter getFirstFilterIn() {
    	return getFilterChainIn().get(0);
    }

    /**
     * Get the first filter in the incoming chain
     * @return The first filter in the chain
     */
    public IoFilter getFirstFilterOut() {
    	return getFilterChainOut().get(0);
    }

    
    /**
     * {@inheritDoc}
     */
    public final WriteFuture write(Object message, SocketAddress remoteAddress) {
        if (message == null) {
            throw new NullPointerException("message");
        }

        // We can't send a message to a connected session if we don't have 
        // the remote address
        if (!getTransportMetadata().isConnectionless() &&
                remoteAddress != null) {
            throw new UnsupportedOperationException();
        }

        
        // If the session has been closed or is closing, we can't either
        // send a message to the remote side. We generate a future
        // containing an exception.
        if (isClosing() || !isConnected()) {
            WriteFuture future = new DefaultWriteFuture(this);
            WriteRequest request = new DefaultWriteRequest(message, future, remoteAddress);
            WriteException writeException = new WriteToClosedSessionException(request);
            future.setException(writeException);
            return future;
        }

        FileChannel openedFileChannel = null;
        
        // TODO: remove this code as soon as we use InputStream
        // instead of Object for the message.
        try {
            if (message instanceof IoBuffer
                    && !((IoBuffer) message).hasRemaining()) {
                // Nothing to write : probably an error in the user code
                throw new IllegalArgumentException(
                "message is empty. Forgot to call flip()?");
            } else if (message instanceof FileChannel) {
                FileChannel fileChannel = (FileChannel) message;
                message = new DefaultFileRegion(fileChannel, 0, fileChannel.size());
            } else if (message instanceof File) {
                File file = (File) message;
                openedFileChannel = new FileInputStream(file).getChannel();
                message = new DefaultFileRegion(openedFileChannel, 0, openedFileChannel.size());
            }
        } catch (IOException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
            return DefaultWriteFuture.newNotWrittenFuture(this, e);
        }

        // Now, we can write the message. First, create a future
        WriteFuture writeFuture = new DefaultWriteFuture(this);
        WriteRequest writeRequest = new DefaultWriteRequest(message, writeFuture, remoteAddress);
        
        // Then, get the chain and inject the WriteRequest into it
        getFirstFilterOut().filterWrite(this, writeRequest);

        // TODO : This is not our business ! The caller has created a FileChannel,
        // he has to close it !
        if (openedFileChannel != null) {
            // If we opened a FileChannel, it needs to be closed when the write has completed
            final FileChannel finalChannel = openedFileChannel;
            writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                public void operationComplete(WriteFuture future) {
                    try {
                        finalChannel.close();
                    } catch (IOException e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    }
                }
            });
        }

        // Return the WriteFuture.
        return writeFuture;
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttachment() {
        return getAttribute("");
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttachment(Object attachment) {
        return setAttribute("", attachment);
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }

    /**
     * {@inheritDoc}
     */
    public final Object getAttribute(Object key, Object defaultValue) {
        return attributes.getAttribute(this, key, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttribute(Object key, Object value) {
        return attributes.setAttribute(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttribute(Object key) {
        return setAttribute(key, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttributeIfAbsent(Object key, Object value) {
        return attributes.setAttributeIfAbsent(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final Object setAttributeIfAbsent(Object key) {
        return setAttributeIfAbsent(key, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    public final Object removeAttribute(Object key) {
        return attributes.removeAttribute(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean removeAttribute(Object key, Object value) {
        return attributes.removeAttribute(this, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean replaceAttribute(Object key, Object oldValue, Object newValue) {
        return attributes.replaceAttribute(this, key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsAttribute(Object key) {
        return attributes.containsAttribute(this, key);
    }

    /**
     * {@inheritDoc}
     */
    public final Set<Object> getAttributeKeys() {
        return attributes.getAttributeKeys(this);
    }

    /**
     * TODO Add method documentation
     */
    public final IoSessionAttributeMap getAttributeMap() {
        return attributes;
    }

    /**
     * TODO Add method documentation
     */
    public final void setAttributeMap(IoSessionAttributeMap attributes) {
        this.attributes = attributes;
    }

    /**
     * TODO Add method documentation
     */
    public final void setWriteRequestQueue(WriteRequestQueue writeRequestQueue) {
        this.writeRequestQueue =
            new CloseRequestAwareWriteRequestQueue(writeRequestQueue);
    }

    /**
     * {@inheritDoc}
     */
    public final TrafficMask getTrafficMask() {
        return trafficMask;
    }

    /**
     * {@inheritDoc}
     */
    public final void setTrafficMask(TrafficMask trafficMask) {
        if (trafficMask == null) {
            throw new NullPointerException("trafficMask");
        }

        if (isClosing() || !isConnected()) {
            return;
        }

        //getFirstFilterIn().filterSetTrafficMask(this, trafficMask);
    }

    /**
     * TODO Add method documentation
     */
    public final void setTrafficMaskNow(TrafficMask trafficMask) {
        this.trafficMask = trafficMask;
    }

    /**
     * {@inheritDoc}
     */
    public final void suspendRead() {
        setTrafficMask(getTrafficMask().and(TrafficMask.READ.not()));
    }

    /**
     * {@inheritDoc}
     */
    public final void suspendWrite() {
        setTrafficMask(getTrafficMask().and(TrafficMask.WRITE.not()));
    }

    /**
     * {@inheritDoc}
     */
    public final void resumeRead() {
        setTrafficMask(getTrafficMask().or(TrafficMask.READ));
    }

    /**
     * {@inheritDoc}
     */
    public final void resumeWrite() {
        setTrafficMask(getTrafficMask().or(TrafficMask.WRITE));
    }

    /**
     * {@inheritDoc}
     */
    public final long getReadBytes() {
        return readBytes;
    }

    /**
     * {@inheritDoc}
     */
    public final long getWrittenBytes() {
        return writtenBytes;
    }

    /**
     * {@inheritDoc}
     */
    public final long getReadMessages() {
        return readMessages;
    }

    /**
     * {@inheritDoc}
     */
    public final long getWrittenMessages() {
        return writtenMessages;
    }

    /**
     * {@inheritDoc}
     */
    public final double getReadBytesThroughput() {
        return readBytesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final double getWrittenBytesThroughput() {
        return writtenBytesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final double getReadMessagesThroughput() {
        return readMessagesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final double getWrittenMessagesThroughput() {
        return writtenMessagesThroughput;
    }

    /**
     * {@inheritDoc}
     */
    public final void updateThroughput(long currentTime, boolean force) {
        int interval = (int) (currentTime - lastThroughputCalculationTime);

        long minInterval = getConfig().getThroughputCalculationIntervalInMillis();
        if (minInterval == 0 || interval < minInterval) {
            if (!force) {
                return;
            }
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

    /**
     * {@inheritDoc}
     */
    public final long getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }

    /**
     * {@inheritDoc}
     */
    public final int getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    /**
     * TODO Add method documentation
     */
    protected void setScheduledWriteBytes(int byteCount){
        scheduledWriteBytes.set(byteCount);
    }

    /**
     * TODO Add method documentation
     */
    protected void setScheduledWriteMessages(int messages) {
        scheduledWriteMessages.set(messages);
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseReadBytes(long increment, long currentTime) {
        if (increment <= 0) {
            return;
        }

        readBytes += increment;
        lastReadTime = currentTime;
        idleCountForBoth = 0;
        idleCountForRead = 0;

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseReadBytes(increment, currentTime);
        }
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseReadMessages(long currentTime) {
        readMessages++;
        lastReadTime = currentTime;
        idleCountForBoth = 0;
        idleCountForRead = 0;

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseReadMessages(currentTime);
        }
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseWrittenBytes(int increment, long currentTime) {
        if (increment <= 0) {
            return;
        }

        writtenBytes += increment;
        lastWriteTime = currentTime;
        idleCountForBoth = 0;
        idleCountForWrite = 0;

        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseWrittenBytes(increment, currentTime);
        }

        increaseScheduledWriteBytes(-increment);
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseWrittenMessages(
            WriteRequest request, long currentTime) {
        Object message = request.getMessage();
        if (message instanceof IoBuffer) {
            IoBuffer b = (IoBuffer) message;
            if (b.hasRemaining()) {
                return;
            }
        }

        writtenMessages++;
        lastWriteTime = currentTime;
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseWrittenMessages(currentTime);
        }

        decreaseScheduledWriteMessages();
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseScheduledWriteBytes(int increment) {
        scheduledWriteBytes.addAndGet(increment);
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseScheduledWriteBytes(increment);
        }
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().increaseScheduledWriteMessages();
        }
    }

    /**
     * TODO Add method documentation
     */
    private void decreaseScheduledWriteMessages() {
        scheduledWriteMessages.decrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).getStatistics().decreaseScheduledWriteMessages();
        }
    }

    /**
     * TODO Add method documentation
     */
    public final void decreaseScheduledBytesAndMessages(WriteRequest request) {
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

    /**
     * {@inheritDoc}
     */
    public final WriteRequestQueue getWriteRequestQueue() {
        if (writeRequestQueue == null) {
            throw new IllegalStateException();
        }
        return writeRequestQueue;
    }

    /**
     * {@inheritDoc}
     */
    public final WriteRequest getCurrentWriteRequest() {
        return currentWriteRequest;
    }

    /**
     * {@inheritDoc}
     */
    public final Object getCurrentWriteMessage() {
        WriteRequest req = getCurrentWriteRequest();
        if (req == null) {
            return null;
        }
        return req.getMessage();
    }

    /**
     * {@inheritDoc}
     */
    public final void setCurrentWriteRequest(WriteRequest currentWriteRequest) {
        this.currentWriteRequest = currentWriteRequest;
    }

    /**
     * TODO Add method documentation
     */
    public final void increaseReadBufferSize() {
        int newReadBufferSize = getConfig().getReadBufferSize() << 1;
        if (newReadBufferSize <= getConfig().getMaxReadBufferSize()) {
            getConfig().setReadBufferSize(newReadBufferSize);
        } else {
            getConfig().setReadBufferSize(getConfig().getMaxReadBufferSize());
        }

        deferDecreaseReadBuffer = true;
    }

    /**
     * TODO Add method documentation
     */
    public final void decreaseReadBufferSize() {
        if (deferDecreaseReadBuffer) {
            deferDecreaseReadBuffer = false;
            return;
        }

        if (getConfig().getReadBufferSize() > getConfig().getMinReadBufferSize()) {
            getConfig().setReadBufferSize(getConfig().getReadBufferSize() >>> 1);
        }

        deferDecreaseReadBuffer = true;
    }

    /**
     * {@inheritDoc}
     */
    public final long getCreationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastReadTime() {
        return lastReadTime;
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastWriteTime() {
        return lastWriteTime;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public final boolean isBothIdle() {
        return isIdle(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isReaderIdle() {
        return isIdle(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWriterIdle() {
        return isIdle(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final int getIdleCount(IdleStatus status) {
        if (getConfig().getIdleTime(status) == 0) {
            if (status == IdleStatus.BOTH_IDLE) {
                idleCountForBoth = 0;
            }

            if (status == IdleStatus.READER_IDLE) {
                idleCountForRead = 0;
            }

            if (status == IdleStatus.WRITER_IDLE) {
                idleCountForWrite = 0;
            }
        }

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

    /**
     * {@inheritDoc}
     */
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

    /**
     * TODO Add method documentation
     */
    public final void increaseIdleCount(IdleStatus status, long currentTime) {
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

    /**
     * {@inheritDoc}
     */
    public final int getBothIdleCount() {
        return getIdleCount(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastBothIdleTime() {
        return getLastIdleTime(IdleStatus.BOTH_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastReaderIdleTime() {
        return getLastIdleTime(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final long getLastWriterIdleTime() {
        return getLastIdleTime(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final int getReaderIdleCount() {
        return getIdleCount(IdleStatus.READER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public final int getWriterIdleCount() {
        return getIdleCount(IdleStatus.WRITER_IDLE);
    }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getServiceAddress() {
        IoService service = getService();
        if (service instanceof IoAcceptor) {
            return ((IoAcceptor) service).getLocalAddress();
        } else {
            return getRemoteAddress();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     * TODO This is a ridiculous implementation. Need to be replaced.
     */
    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * TODO Add method documentation
     */
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

    /**
     * TODO Add method documentation
     */
    private String getServiceName() {
        TransportMetadata tm = getTransportMetadata();
        if (tm == null) {
            return "null";
        } else {
            return tm.getProviderName() + ' ' + tm.getName();
        }
    }

    /**
     * TODO Add method documentation. Name is ridiculously too long.
     */
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
