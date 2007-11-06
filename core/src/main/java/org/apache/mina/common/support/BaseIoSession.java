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
package org.apache.mina.common.support;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TrafficMask;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.IoFilter.WriteRequest;

/**
 * Base implementation of {@link IoSession}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoSession implements IoSession {
    
    private static final IoFutureListener SCHEDULED_COUNTER_RESETTER =
        new IoFutureListener() {
            public void operationComplete(IoFuture future) {
                BaseIoSession s = (BaseIoSession) future.getSession();
                s.scheduledWriteBytes.set(0);
                s.scheduledWriteRequests.set(0);
            }
        };
    
    private final Object lock = new Object();

    private final Map<String, Object> attributes = Collections
            .synchronizedMap(new HashMap<String, Object>(8));

    private final long creationTime;

    /**
     * A future that will be set 'closed' when the connection is closed.
     */
    private final CloseFuture closeFuture = new DefaultCloseFuture(this);
    
    private final AtomicBoolean scheduledForFlush = new AtomicBoolean();
    
    private final AtomicInteger scheduledWriteBytes = new AtomicInteger();

    private final AtomicInteger scheduledWriteRequests = new AtomicInteger();

    private volatile boolean closing;

    // Configuration variables
    private int idleTimeForRead;

    private int idleTimeForWrite;

    private int idleTimeForBoth;

    private int writeTimeout;

    private TrafficMask trafficMask = TrafficMask.ALL;

    // Status variables
    private long readBytes;

    private long writtenBytes;

    private long readMessages;

    private long writtenMessages;

    private long lastReadTime;

    private long lastWriteTime;

    private int idleCountForBoth;

    private int idleCountForRead;

    private int idleCountForWrite;

    private long lastIdleTimeForBoth;

    private long lastIdleTimeForRead;

    private long lastIdleTimeForWrite;

    protected BaseIoSession() {
        creationTime = lastReadTime = lastWriteTime = lastIdleTimeForBoth = lastIdleTimeForRead = lastIdleTimeForWrite = System
                .currentTimeMillis();
        closeFuture.addListener(SCHEDULED_COUNTER_RESETTER);
    }

    public boolean isConnected() {
        return !closeFuture.isClosed();
    }

    public boolean isClosing() {
        return closing || closeFuture.isClosed();
    }

    public CloseFuture getCloseFuture() {
        return closeFuture;
    }
    
    public boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }
    
    public boolean setScheduledForFlush(boolean flag) {
        if (flag) {
            return scheduledForFlush.compareAndSet(false, true);
        } else {
            scheduledForFlush.set(false);
            return true;
        }
    }

    public CloseFuture close() {
        synchronized (lock) {
            if (isClosing()) {
                return closeFuture;
            } else {
                closing = true;
            }
        }

        close0();
        return closeFuture;
    }

    /**
     * Implement this method to perform real close operation.
     * By default, this method is implemented to set the future to
     * 'closed' immediately.
     */
    protected void close0() {
        closeFuture.setClosed();
    }

    public WriteFuture write(Object message) {
        return write(message, null);
    }

    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        if (isClosing() ) {
            return DefaultWriteFuture.newNotWrittenFuture(this);
        }

        WriteFuture future = new DefaultWriteFuture(this);
        write0(new WriteRequest(message, future, remoteAddress));

        return future;
    }

    /**
     * Implement this method to perform real write operation with
     * the specified <code>writeRequest</code>.
     *
     * By default, this method is implemented to set the future to
     * 'not written' immediately.
     *
     * @param writeRequest Write request to make
     */
    protected void write0(WriteRequest writeRequest) {
        writeRequest.getFuture().setWritten(false);
    }

    public Object getAttachment() {
        return getAttribute("");
    }

    public Object setAttachment(Object attachment) {
        return setAttribute("", attachment);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Object setAttribute(String key, Object value) {
        if (value == null) {
            return removeAttribute(key);
        } else {
            return attributes.put(key, value);
        }
    }

    public Object setAttribute(String key) {
        return setAttribute(key, Boolean.TRUE);
    }

    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    public boolean containsAttribute(String key) {
        return getAttribute(key) != null;
    }

    public Set<String> getAttributeKeys() {
        synchronized (attributes) {
            return new HashSet<String>(attributes.keySet());
        }
    }

    public int getIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE)
            return idleTimeForBoth;

        if (status == IdleStatus.READER_IDLE)
            return idleTimeForRead;

        if (status == IdleStatus.WRITER_IDLE)
            return idleTimeForWrite;

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public long getIdleTimeInMillis(IdleStatus status) {
        return getIdleTime(status) * 1000L;
    }

    public void setIdleTime(IdleStatus status, int idleTime) {
        if (idleTime < 0)
            throw new IllegalArgumentException("Illegal idle time: " + idleTime);

        if (status == IdleStatus.BOTH_IDLE)
            idleTimeForBoth = idleTime;
        else if (status == IdleStatus.READER_IDLE)
            idleTimeForRead = idleTime;
        else if (status == IdleStatus.WRITER_IDLE)
            idleTimeForWrite = idleTime;
        else
            throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public long getWriteTimeoutInMillis() {
        return writeTimeout * 1000L;
    }

    public void setWriteTimeout(int writeTimeout) {
        if (writeTimeout < 0)
            throw new IllegalArgumentException("Illegal write timeout: "
                    + writeTimeout);
        this.writeTimeout = writeTimeout;
    }

    public TrafficMask getTrafficMask() {
        return trafficMask;
    }

    public void setTrafficMask(TrafficMask trafficMask) {
        if (trafficMask == null) {
            throw new NullPointerException("trafficMask");
        }

        if (this.trafficMask == trafficMask) {
            return;
        }

        this.trafficMask = trafficMask;
        updateTrafficMask();
    }

    public void suspendRead() {
        setTrafficMask(getTrafficMask().and(TrafficMask.READ.not()));
    }

    public void suspendWrite() {
        setTrafficMask(getTrafficMask().and(TrafficMask.WRITE.not()));
    }

    public void resumeRead() {
        setTrafficMask(getTrafficMask().or(TrafficMask.READ));
    }

    public void resumeWrite() {
        setTrafficMask(getTrafficMask().or(TrafficMask.WRITE));
    }

    /**
     * Signals the {@link IoService} that the {@link TrafficMask} of this
     * session has been changed.
     */
    protected abstract void updateTrafficMask();

    public long getReadBytes() {
        return readBytes;
    }

    public long getWrittenBytes() {
        return writtenBytes;
    }

    public long getWrittenWriteRequests() {
        return writtenMessages;
    }

    public long getReadMessages() {
        return readMessages;
    }

    public long getWrittenMessages() {
        return writtenMessages;
    }
    
    public int getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }
    
    public int getScheduledWriteRequests() {
        return scheduledWriteRequests.get();
    }

    public void increaseReadBytes(int increment) {
        if (increment > 0) {
            readBytes += increment;
            lastReadTime = System.currentTimeMillis();
            idleCountForBoth = 0;
            idleCountForRead = 0;
        }
    }

    public void increaseWrittenBytes(int increment) {
        if (increment > 0) {
            writtenBytes += increment;
            lastWriteTime = System.currentTimeMillis();
            idleCountForBoth = 0;
            idleCountForWrite = 0;
            
            scheduledWriteBytes.addAndGet(-increment);
        }
    }
    
    public void increaseReadMessages() {
        readMessages++;
        lastReadTime = System.currentTimeMillis();
    }

    public void increaseWrittenMessages() {
        writtenMessages++;
        lastWriteTime = System.currentTimeMillis();
        scheduledWriteRequests.decrementAndGet();
    }
    
    public void increaseScheduledWriteBytes(int increment) {
        scheduledWriteBytes.addAndGet(increment);
    }

    public void increaseScheduledWriteRequests() {
        scheduledWriteRequests.incrementAndGet();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public boolean isIdle(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE)
            return idleCountForBoth > 0;

        if (status == IdleStatus.READER_IDLE)
            return idleCountForRead > 0;

        if (status == IdleStatus.WRITER_IDLE)
            return idleCountForWrite > 0;

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public int getIdleCount(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE)
            return idleCountForBoth;

        if (status == IdleStatus.READER_IDLE)
            return idleCountForRead;

        if (status == IdleStatus.WRITER_IDLE)
            return idleCountForWrite;

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public long getLastIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE)
            return lastIdleTimeForBoth;

        if (status == IdleStatus.READER_IDLE)
            return lastIdleTimeForRead;

        if (status == IdleStatus.WRITER_IDLE)
            return lastIdleTimeForWrite;

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public void increaseIdleCount(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth++;
            lastIdleTimeForBoth = System.currentTimeMillis();
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead++;
            lastIdleTimeForRead = System.currentTimeMillis();
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite++;
            lastIdleTimeForWrite = System.currentTimeMillis();
        } else
            throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    @Override
    public String toString() {
        return "(" + getTransportType() + ", R: " + getRemoteAddress()
                + ", L: " + getLocalAddress() + ", S: " + getServiceAddress()
                + ')';
    }
}
