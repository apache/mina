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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Base implementation of {@link IoService}s.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoService implements IoService {
    private static final IoServiceListener SERVICE_ACTIVATION_LISTENER =
        new IoServiceListener() {
            public void serviceActivated(IoService service) {
                // Update lastIoTime.
                AbstractIoService s = (AbstractIoService) service;
                s.setLastReadTime(s.getActivationTime());
                s.setLastWriteTime(s.getActivationTime());
                s.lastThroughputCalculationTime = s.getActivationTime();
                
                // Start idleness notification.
                IdleStatusChecker.getInstance().addService(s);
            }

            public void serviceDeactivated(IoService service) {
                IdleStatusChecker.getInstance().removeService(
                        (AbstractIoService) service);
            }

            public void serviceIdle(IoService service, IdleStatus idleStatus) {}
            public void sessionCreated(IoSession session) {}
            public void sessionDestroyed(IoSession session) {}
    };
    
    /**
     * Current filter chain builder.
     */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();

    /**
     * Current handler.
     */
    private IoHandler handler;
    
    private IoSessionDataStructureFactory sessionDataStructureFactory =
        new DefaultIoSessionDataStructureFactory();

    /**
     * Maintains the {@link IoServiceListener}s of this service.
     */
    private final IoServiceListenerSupport listeners;
    
    private final Object disposalLock = new Object();
    private volatile boolean disposing;
    private volatile boolean disposed;
    private IoFuture disposalFuture;

    private final AtomicLong readBytes = new AtomicLong();
    private final AtomicLong writtenBytes = new AtomicLong();
    private final AtomicLong readMessages = new AtomicLong();
    private final AtomicLong writtenMessages = new AtomicLong();
    private long lastReadTime;
    private long lastWriteTime;
    
    private final AtomicLong scheduledWriteBytes = new AtomicLong();
    private final AtomicLong scheduledWriteMessages = new AtomicLong();

    private final Object throughputCalculationLock = new Object();
    private int throughputCalculationInterval = 3;

    private long lastThroughputCalculationTime;
    private long lastReadBytes;
    private long lastWrittenBytes;
    private long lastReadMessages;
    private long lastWrittenMessages;
    private double readBytesThroughput;
    private double writtenBytesThroughput;
    private double readMessagesThroughput;
    private double writtenMessagesThroughput;

    private final Object idlenessCheckLock = new Object();
    private int idleTimeForRead;
    private int idleTimeForWrite;
    private int idleTimeForBoth;

    private int idleCountForBoth;
    private int idleCountForRead;
    private int idleCountForWrite;

    private long lastIdleTimeForBoth;
    private long lastIdleTimeForRead;
    private long lastIdleTimeForWrite;
    
    /**
     * The default {@link IoSessionConfig} which will be used to configure new sessions.
     */
    private IoSessionConfig sessionConfig;

    protected AbstractIoService(IoSessionConfig sessionConfig) {
        if (sessionConfig == null) {
            throw new NullPointerException("sessionConfig");
        }

        if (!getTransportMetadata().getSessionConfigType().isAssignableFrom(
                sessionConfig.getClass())) {
            throw new IllegalArgumentException("sessionConfig type: "
                    + sessionConfig.getClass() + " (expected: "
                    + getTransportMetadata().getSessionConfigType() + ")");
        }

        this.listeners = new IoServiceListenerSupport(this);
        this.listeners.add(SERVICE_ACTIVATION_LISTENER);
        this.sessionConfig = sessionConfig;
    }

    public final IoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }

    public final void setFilterChainBuilder(IoFilterChainBuilder builder) {
        if (builder == null) {
            builder = new DefaultIoFilterChainBuilder();
        }
        filterChainBuilder = builder;
    }

    public final DefaultIoFilterChainBuilder getFilterChain() {
        if (filterChainBuilder instanceof DefaultIoFilterChainBuilder) {
            return (DefaultIoFilterChainBuilder) filterChainBuilder;
        } else {
            throw new IllegalStateException(
                    "Current filter chain builder is not a DefaultIoFilterChainBuilder.");
        }
    }

    public final void addListener(IoServiceListener listener) {
        listeners.add(listener);
    }

    public final void removeListener(IoServiceListener listener) {
        listeners.remove(listener);
    }

    public final boolean isActive() {
        return listeners.isActive();
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

        IoFuture disposalFuture;
        synchronized (disposalLock) {
            disposalFuture = this.disposalFuture;
            if (!disposing) {
                disposing = true;
                try {
                    this.disposalFuture = disposalFuture = dispose0();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    if (disposalFuture == null) {
                        disposed = true;
                    }
                }
            }
        }
        
        if (disposalFuture != null) {
            disposalFuture.awaitUninterruptibly();
        }

        disposed = true;
    }
    
    /**
     * Implement this method to release any acquired resources.  This method
     * is invoked only once by {@link #dispose()}.
     */
    protected abstract IoFuture dispose0() throws Exception;

    public final Set<IoSession> getManagedSessions() {
        return listeners.getManagedSessions();
    }

    public final long getCumulativeManagedSessionCount() {
        return listeners.getCumulativeManagedSessionCount();
    }

    public final int getLargestManagedSessionCount() {
        return listeners.getLargestManagedSessionCount();
    }

    public final int getManagedSessionCount() {
        return listeners.getManagedSessionCount();
    }

    public final IoHandler getHandler() {
        return handler;
    }

    public final void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        if (isActive()) {
            throw new IllegalStateException("handler cannot be set while the service is active.");
        }

        this.handler = handler;
    }

    public IoSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public final IoSessionDataStructureFactory getSessionDataStructureFactory() {
        return sessionDataStructureFactory;
    }

    public final void setSessionDataStructureFactory(IoSessionDataStructureFactory sessionDataStructureFactory) {
        if (sessionDataStructureFactory == null) {
            throw new NullPointerException("sessionDataStructureFactory");
        }

        if (isActive()) {
            throw new IllegalStateException(
                    "sessionDataStructureFactory cannot be set while the service is active.");
        }

        this.sessionDataStructureFactory = sessionDataStructureFactory;
    }

    public final long getReadBytes() {
        return readBytes.get();
    }

    protected final void increaseReadBytes(long increment, long currentTime) {
        readBytes.addAndGet(increment);
        lastReadTime = currentTime;
        idleCountForBoth = 0;
        idleCountForRead = 0;
    }

    public final long getReadMessages() {
        return readMessages.get();
    }

    protected final void increaseReadMessages(long currentTime) {
        readMessages.incrementAndGet();
        lastReadTime = currentTime;
        idleCountForBoth = 0;
        idleCountForRead = 0;
    }

    public final int getThroughputCalculationInterval() {
        return throughputCalculationInterval;
    }

    public final void setThroughputCalculationInterval(int throughputCalculationInterval) {
        if (throughputCalculationInterval < 0) {
            throw new IllegalArgumentException(
                    "throughputCalculationInterval: " + throughputCalculationInterval);
        }

        this.throughputCalculationInterval = throughputCalculationInterval;
    }
    
    public final long getThroughputCalculationIntervalInMillis() {
        return throughputCalculationInterval * 1000L;
    }
    
    public final double getReadBytesThroughput() {
        resetThroughput();
        return readBytesThroughput;
    }

    public final double getWrittenBytesThroughput() {
        resetThroughput();
        return writtenBytesThroughput;
    }

    public final double getReadMessagesThroughput() {
        resetThroughput();
        return readMessagesThroughput;
    }

    public final double getWrittenMessagesThroughput() {
        resetThroughput();
        return writtenMessagesThroughput;
    }
    
    private void resetThroughput() {
        if (getManagedSessionCount() == 0) {
            readBytesThroughput = 0;
            writtenBytesThroughput = 0;
            readMessagesThroughput = 0;
            writtenMessagesThroughput = 0;
        }
    }

    private void updateThroughput(long currentTime) {
        synchronized (throughputCalculationLock) {
            int interval = (int) (currentTime - lastThroughputCalculationTime);
            long minInterval = getThroughputCalculationIntervalInMillis();
            if (minInterval == 0 || interval < minInterval) {
                return;
            }
            
            long readBytes = this.readBytes.get();
            long writtenBytes = this.writtenBytes.get();
            long readMessages = this.readMessages.get();
            long writtenMessages = this.writtenMessages.get();
            
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
    }
    
    public final long getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }

    protected final void increaseScheduledWriteBytes(long increment) {
        scheduledWriteBytes.addAndGet(increment);
    }

    public final long getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    protected final void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
    }

    protected final void decreaseScheduledWriteMessages() {
        scheduledWriteMessages.decrementAndGet();
    }

    public final long getActivationTime() {
        return listeners.getActivationTime();
    }

    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    public final long getLastReadTime() {
        return lastReadTime;
    }

    protected final void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public final long getLastWriteTime() {
        return lastWriteTime;
    }
    
    protected final void setLastWriteTime(long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
    }

    public final long getWrittenBytes() {
        return writtenBytes.get();
    }

    protected final void increaseWrittenBytes(long increment, long currentTime) {
        writtenBytes.addAndGet(increment);
        lastWriteTime = currentTime;
        idleCountForBoth = 0;
        idleCountForWrite = 0;
    }

    public final long getWrittenMessages() {
        return writtenMessages.get();
    }

    protected final void increaseWrittenMessages(long currentTime) {
        writtenMessages.incrementAndGet();
        lastWriteTime = currentTime;
        idleCountForBoth = 0;
        idleCountForWrite = 0;
    }

    public final int getIdleTime(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleTimeForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleTimeForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleTimeForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }

    public final long getIdleTimeInMillis(IdleStatus status) {
        return getIdleTime(status) * 1000L;
    }

    public final void setIdleTime(IdleStatus status, int idleTime) {
        if (idleTime < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTime);
        }
        
        if (status == IdleStatus.BOTH_IDLE) {
            idleTimeForBoth = idleTime;
        } else if (status == IdleStatus.READER_IDLE) {
            idleTimeForRead = idleTime;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleTimeForWrite = idleTime;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
        
        if (idleTime == 0) {
            if (status == IdleStatus.BOTH_IDLE) {
                idleCountForBoth = 0;
            } else if (status == IdleStatus.READER_IDLE) {
                idleCountForRead = 0;
            } else if (status == IdleStatus.WRITER_IDLE) {
                idleCountForWrite = 0;
            }
        }
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

    private void increaseIdleCount(IdleStatus status, long currentTime) {
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
    
    protected final void notifyIdleness(long currentTime) {
        updateThroughput(currentTime);
        
        synchronized (idlenessCheckLock) {
            notifyIdleness(
                    currentTime,
                    getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                    IdleStatus.BOTH_IDLE, Math.max(
                            getLastIoTime(),
                            getLastIdleTime(IdleStatus.BOTH_IDLE)));
            
            notifyIdleness(
                    currentTime,
                    getIdleTimeInMillis(IdleStatus.READER_IDLE),
                    IdleStatus.READER_IDLE, Math.max(
                            getLastReadTime(),
                            getLastIdleTime(IdleStatus.READER_IDLE)));
            
            notifyIdleness(
                    currentTime,
                    getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                    IdleStatus.WRITER_IDLE, Math.max(
                            getLastWriteTime(),
                            getLastIdleTime(IdleStatus.WRITER_IDLE)));
        }
    }
    
    private void notifyIdleness(
            long currentTime, long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && currentTime - lastIoTime >= idleTime) {
            increaseIdleCount(status, currentTime);
            listeners.fireServiceIdle(status);
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
    
    public final int getBothIdleTime() {
        return getIdleTime(IdleStatus.BOTH_IDLE);
    }

    public final long getBothIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.BOTH_IDLE);
    }

    public final int getReaderIdleTime() {
        return getIdleTime(IdleStatus.READER_IDLE);
    }

    public final long getReaderIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.READER_IDLE);
    }

    public final int getWriterIdleTime() {
        return getIdleTime(IdleStatus.WRITER_IDLE);
    }

    public final long getWriterIdleTimeInMillis() {
        return getIdleTimeInMillis(IdleStatus.WRITER_IDLE);
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

    public final void setBothIdleTime(int idleTime) {
        setIdleTime(IdleStatus.BOTH_IDLE, idleTime);
    }

    public final void setReaderIdleTime(int idleTime) {
        setIdleTime(IdleStatus.READER_IDLE, idleTime);
    }

    public final void setWriterIdleTime(int idleTime) {
        setIdleTime(IdleStatus.WRITER_IDLE, idleTime);
    }

    public final Set<WriteFuture> broadcast(Object message) {
        // Convert to Set.  We do not return a List here because only the 
        // direct caller of MessageBroadcaster knows the order of write
        // operations.
        final List<WriteFuture> futures = IoUtil.broadcast(
                message, getManagedSessions());
        return new AbstractSet<WriteFuture>() {
            @Override
            public Iterator<WriteFuture> iterator() {
                return futures.iterator();
            }

            @Override
            public int size() {
                return futures.size();
            }
        };
    }
    
    protected final IoServiceListenerSupport getListeners() {
        return listeners;
    }
    
    protected final void finishSessionInitialization(IoSession session, IoFuture future) {
        // Update lastIoTime if needed.
        if (getLastReadTime() == 0) {
            setLastReadTime(getActivationTime());
        }
        if (getLastWriteTime() == 0) {
            setLastWriteTime(getActivationTime());
        }

        // Every property but attributeMap should be set now.
        // Now initialize the attributeMap.  The reason why we initialize
        // the attributeMap at last is to make sure all session properties
        // such as remoteAddress are provided to IoSessionDataStructureFactory.
        try {
            ((AbstractIoSession) session).setAttributeMap(
                    session.getService().getSessionDataStructureFactory().getAttributeMap(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException(
                    "Failed to initialize an attributeMap.", e);
        }

        try {
            ((AbstractIoSession) session).setWriteRequestQueue(
                    session.getService().getSessionDataStructureFactory().getWriteRequestQueue(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException(
                    "Failed to initialize a writeRequestQueue.", e);
        }

        if (future != null && future instanceof ConnectFuture) {
            // DefaultIoFilterChain will notify the future. (We support ConnectFuture only for now).
            session.setAttribute(DefaultIoFilterChain.SESSION_OPENED_FUTURE, future);
        }
        
        finishSessionInitialization0(session, future);
    }
    
    /**
     * Implement this method to perform additional tasks required for session
     * initialization. Do not call this method directly;
     * {@link #finishSessionInitialization(IoSession, IoFuture)} will call
     * this method instead.
     */
    @SuppressWarnings("unused")
    protected void finishSessionInitialization0(IoSession session, IoFuture future) {}

    protected static final class ServiceOperationFuture extends DefaultIoFuture {
        public ServiceOperationFuture() {
            super(null);
        }

        public boolean isDone() {
            return getValue() == Boolean.TRUE;
        }

        public void setDone() {
            setValue(Boolean.TRUE);
        }

        public Exception getException() {
            if (getValue() instanceof Exception) {
                return (Exception) getValue();
            } else {
                return null;
            }
        }

        public void setException(Exception exception) {
            if (exception == null) {
                throw new NullPointerException("exception");
            }
            setValue(exception);
        }
    }
}
