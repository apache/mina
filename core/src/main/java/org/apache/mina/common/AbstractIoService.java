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
    /**
     * Current filter chain builder.
     */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();

    /**
     * Current handler.
     */
    private IoHandler handler;
    
    private IoSessionAttributeMapFactory sessionAttributeMapFactory =
        new DefaultIoSessionAttributeMapFactory();

    /**
     * Maintains the {@link IoServiceListener}s of this service.
     */
    private final IoServiceListenerSupport listeners;

    private volatile long activationTime;
    private final AtomicLong readBytes = new AtomicLong();
    private final AtomicLong writtenBytes = new AtomicLong();
    private final AtomicLong readMessages = new AtomicLong();
    private final AtomicLong writtenMessages = new AtomicLong();
    private final AtomicLong scheduledWriteBytes = new AtomicLong();
    private final AtomicLong scheduledWriteMessages = new AtomicLong();

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
        this.sessionConfig = sessionConfig;
    }

    public IoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }

    public void setFilterChainBuilder(IoFilterChainBuilder builder) {
        if (builder == null) {
            builder = new DefaultIoFilterChainBuilder();
        }
        filterChainBuilder = builder;
    }

    public DefaultIoFilterChainBuilder getFilterChain() {
        if (filterChainBuilder instanceof DefaultIoFilterChainBuilder) {
            return (DefaultIoFilterChainBuilder) filterChainBuilder;
        } else {
            throw new IllegalStateException(
                    "Current filter chain builder is not a DefaultIoFilterChainBuilder.");
        }
    }

    public void addListener(IoServiceListener listener) {
        getListeners().add(listener);
    }

    public void removeListener(IoServiceListener listener) {
        getListeners().remove(listener);
    }

    public boolean isActive() {
        return getListeners().isActive();
    }

    public Set<IoSession> getManagedSessions() {
        return getListeners().getManagedSessions();
    }

    public IoHandler getHandler() {
        return handler;
    }

    public void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        if (isActive()) {
            throw new IllegalStateException("handler cannot be set while the service is active.");
        }

        this.handler = handler;
    }

    protected IoServiceListenerSupport getListeners() {
        return listeners;
    }

    public IoSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public IoSessionAttributeMapFactory getSessionAttributeMapFactory() {
        return sessionAttributeMapFactory;
    }

    public void setSessionAttributeMapFactory(IoSessionAttributeMapFactory sessionAttributeMapFactory) {
        if (sessionAttributeMapFactory == null) {
            throw new NullPointerException("sessionAttributeMapFactory");
        }

        if (isActive()) {
            throw new IllegalStateException(
                    "sessionAttributeMapFactory cannot be set while the service is active.");
        }

        this.sessionAttributeMapFactory = sessionAttributeMapFactory;
    }

    public long getReadBytes() {
        return readBytes.get();
    }

    protected void increaseReadBytes(long increment) {
        readBytes.addAndGet(increment);
    }

    public long getReadMessages() {
        return readMessages.get();
    }

    protected void increaseReadMessages() {
        readMessages.incrementAndGet();
    }

    public long getScheduledWriteBytes() {
        return scheduledWriteBytes.get();
    }

    protected void increaseScheduledWriteBytes(long increment) {
        scheduledWriteBytes.addAndGet(increment);
    }

    public long getScheduledWriteMessages() {
        return scheduledWriteMessages.get();
    }

    protected void increaseScheduledWriteMessages() {
        scheduledWriteMessages.incrementAndGet();
    }

    protected void decreaseScheduledWriteMessages() {
        scheduledWriteMessages.decrementAndGet();
    }

    public long getActivationTime() {
        return activationTime;
    }

    protected void setActivationTime(long activationTime) {
        this.activationTime = activationTime;
    }

    public long getWrittenBytes() {
        return writtenBytes.get();
    }

    protected void increaseWrittenBytes(long increment) {
        writtenBytes.addAndGet(increment);
    }

    public long getWrittenMessages() {
        return writtenMessages.get();
    }

    protected void increaseWrittenMessages() {
        writtenMessages.incrementAndGet();
    }

    public Set<WriteFuture> broadcast(Object message) {
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
    
    protected void finishSessionInitialization(IoSession session, IoFuture future) {
        // Every property but attributeMap should be set now.
        // Now initialize the attributeMap.  The reason why we initialize
        // the attributeMap at last is to make sure all session properties
        // such as remoteAddress are provided to IoSessionAttributeMapFactory.
        try {
            ((AbstractIoSession) session).setAttributeMap(
                    session.getService().getSessionAttributeMapFactory().getAttributeMap(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException(
                    "Failed to initialize sessionAttributeMap.", e);
        }

        if (future != null && future instanceof ConnectFuture) {
            // DefaultIoFilterChain will notify the future. (We support ConnectFuture only for now).
            session.setAttribute(DefaultIoFilterChain.SESSION_OPENED_FUTURE, future);
        }
    }

    protected static class ServiceOperationFuture extends DefaultIoFuture {
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

        public void setException(Exception cause) {
            setValue(cause);
        }
    }
}
