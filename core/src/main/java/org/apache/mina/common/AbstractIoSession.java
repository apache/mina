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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Base implementation of {@link IoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoSession implements IoSession {

    private static final IoFutureListener SCHEDULED_COUNTER_RESETTER =
        new IoFutureListener() {
            public void operationComplete(IoFuture future) {
                AbstractIoSession s = (AbstractIoSession) future.getSession();
                s.scheduledWriteBytes.set(0);
                s.scheduledWriteMessages.set(0);
            }
    };

    /**
     * An internal write request object that triggers session close.
     * @see #writeRequestQueue
     */
    private static final WriteRequest CLOSE_REQUEST =
        new DefaultWriteRequest(new Object());

    private final Object lock = new Object();

    private final Map<Object, Object> attributes = Collections
            .synchronizedMap(new HashMap<Object, Object>(4));

    private final Queue<WriteRequest> writeRequestQueue =
        new ConcurrentLinkedQueue<WriteRequest>() {
            private static final long serialVersionUID = -3899506857975733565L;

            // Discard close request offered by closeOnFlush() silently.
            @Override
            public WriteRequest peek() {
                WriteRequest answer = super.peek();
                if (answer == CLOSE_REQUEST) {
                    AbstractIoSession.this.close();
                    clear();
                    answer = null;
                }
                return answer;
            }

            @Override
            public WriteRequest poll() {
                WriteRequest answer = super.poll();
                if (answer == CLOSE_REQUEST) {
                    AbstractIoSession.this.close();
                    clear();
                    answer = null;
                }
                return answer;
            }
    };

    private final long creationTime;

    /**
     * A future that will be set 'closed' when the connection is closed.
     */
    private final CloseFuture closeFuture = new DefaultCloseFuture(this);

    private volatile boolean closing;

    private TrafficMask trafficMask = TrafficMask.ALL;

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

    private int idleCountForBoth;

    private int idleCountForRead;

    private int idleCountForWrite;

    private long lastIdleTimeForBoth;

    private long lastIdleTimeForRead;

    private long lastIdleTimeForWrite;

    protected AbstractIoSession() {
        creationTime = lastReadTime = lastWriteTime =
            lastIdleTimeForBoth = lastIdleTimeForRead =
                lastIdleTimeForWrite = System.currentTimeMillis();
        closeFuture.addListener(SCHEDULED_COUNTER_RESETTER);
    }

    protected abstract IoProcessor getProcessor();

    public boolean isConnected() {
        return !closeFuture.isClosed();
    }

    public boolean isClosing() {
        return closing || closeFuture.isClosed();
    }

    public CloseFuture getCloseFuture() {
        return closeFuture;
    }

    protected boolean isScheduledForFlush() {
        return scheduledForFlush.get();
    }

    protected boolean setScheduledForFlush(boolean flag) {
        if (flag) {
            return scheduledForFlush.compareAndSet(false, true);
        } else {
            scheduledForFlush.set(false);
            return true;
        }
    }
    
    public CloseFuture close(boolean rightNow) {
        if (rightNow) {
            return close();
        } else {
            return closeOnFlush();
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

        getFilterChain().fireFilterClose();
        return closeFuture;
    }
    
    public CloseFuture closeOnFlush() {
        getWriteRequestQueue().offer(CLOSE_REQUEST);
        getProcessor().flush(this);
        return closeFuture;
    }

    public WriteFuture write(Object message) {
        return write(message, null);
    }

    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        
        if (!getTransportMetadata().isConnectionless() &&
                remoteAddress != null) {
            throw new UnsupportedOperationException();
        }

        if (isClosing() || !isConnected()) {
            return DefaultWriteFuture.newNotWrittenFuture(this);
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
                return DefaultWriteFuture.newNotWrittenFuture(this);
            }
        } else if (message instanceof File) {
            File file = (File) message;
            try {
                channel = new FileInputStream(file).getChannel();
            } catch (IOException e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
                return DefaultWriteFuture.newNotWrittenFuture(this);
            }
        }

        WriteFuture future = new DefaultWriteFuture(this);
        getFilterChain().fireFilterWrite(
                new DefaultWriteRequest(message, future, remoteAddress));

        if (message instanceof File) {
            final FileChannel finalChannel = channel;
            future.addListener(new IoFutureListener() {
                public void operationComplete(IoFuture future) {
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

    public Object getAttachment() {
        return getAttribute("");
    }

    public Object setAttachment(Object attachment) {
        return setAttribute("", attachment);
    }

    public Object getAttribute(Object key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        return attributes.get(key);
    }

    public Object getAttribute(Object key, Object defaultValue) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (defaultValue == null) {
            return attributes.get(key);
        }

        Object answer = attributes.get(key);
        if (answer == null) {
            return defaultValue;
        } else {
            return answer;
        }
    }

    public Object setAttribute(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (value == null) {
            return attributes.remove(key);
        } else {
            return attributes.put(key, value);
        }
    }

    public Object setAttribute(Object key) {
        return setAttribute(key, Boolean.TRUE);
    }

    public Object setAttributeIfAbsent(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (value == null) {
            return null;
        }

        Object oldValue;
        synchronized (attributes) {
            oldValue = attributes.get(key);
            if (oldValue == null) {
                attributes.put(key, value);
            }
        }
        return oldValue;
    }

    public Object removeAttribute(Object key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        return attributes.remove(key);
    }

    public boolean removeAttribute(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (value == null) {
            return false;
        }

        synchronized (attributes) {
            if (value.equals(attributes.get(key))) {
                attributes.remove(key);
                return true;
            }
        }

        return false;
    }

    public boolean replaceAttribute(Object key, Object oldValue, Object newValue) {
        synchronized (attributes) {
            Object actualOldValue = attributes.get(key);
            if (actualOldValue == null) {
                return false;
            }

            if (actualOldValue.equals(oldValue)) {
                attributes.put(key, newValue);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean containsAttribute(Object key) {
        return attributes.containsKey(key);
    }

    public Set<Object> getAttributeKeys() {
        synchronized (attributes) {
            return new HashSet<Object>(attributes.keySet());
        }
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
        getProcessor().updateTrafficMask(this);
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

    public long getReadBytes() {
        return readBytes;
    }

    public long getWrittenBytes() {
        return writtenBytes;
    }

    public long getReadMessages() {
        return readMessages;
    }

    public long getWrittenMessages() {
        return writtenMessages;
    }

    public long getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }

    public int getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    protected void increaseReadBytes(long increment) {
        if (increment > 0) {
            readBytes += increment;
            lastReadTime = System.currentTimeMillis();
            idleCountForBoth = 0;
            idleCountForRead = 0;
            
            if (getService() instanceof AbstractIoService) {
                ((AbstractIoService) getService()).increaseReadBytes(increment);
            }
        }
    }

    protected void increaseWrittenBytes(long increment) {
        if (increment > 0) {
            writtenBytes += increment;
            lastWriteTime = System.currentTimeMillis();
            idleCountForBoth = 0;
            idleCountForWrite = 0;

            scheduledWriteBytes.addAndGet(-increment);
            
            if (getService() instanceof AbstractIoService) {
                ((AbstractIoService) getService()).increaseWrittenBytes(increment);
            }
        }
    }

    protected void increaseReadMessages() {
        readMessages++;
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseReadMessages();
        }
    }

    protected void increaseWrittenMessages() {
        writtenMessages++;
        scheduledWriteMessages.decrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseWrittenMessages();
        }
    }

    protected void increaseScheduledWriteBytes(long increment) {
        scheduledWriteBytes.addAndGet(increment);
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseScheduledWriteBytes(increment);
        }
    }

    protected void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseScheduledWriteMessages();
        }
    }

    protected Queue<WriteRequest> getWriteRequestQueue() {
        return writeRequestQueue;
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

    public int getIdleCount(IdleStatus status) {
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

    public long getLastIdleTime(IdleStatus status) {
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

    protected void increaseIdleCount(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            idleCountForBoth++;
            lastIdleTimeForBoth = System.currentTimeMillis();
        } else if (status == IdleStatus.READER_IDLE) {
            idleCountForRead++;
            lastIdleTimeForRead = System.currentTimeMillis();
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleCountForWrite++;
            lastIdleTimeForWrite = System.currentTimeMillis();
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
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
        return System.identityHashCode(this);
    }

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        if (getService() instanceof IoAcceptor) {
            return "(" + getServiceName() + ", server, " +
                    getRemoteAddress() + " => " +
                    getLocalAddress() + ')';
        } else {
            return "(" + getServiceName() + ", client, " +
                    getLocalAddress() + " => " +
                    getRemoteAddress() + ')';
        }
    }

    private String getServiceName() {
        TransportMetadata tm = getTransportMetadata();
        if (tm == null) {
            return "null";
        } else {
            return tm.getName();
        }
    }
}
