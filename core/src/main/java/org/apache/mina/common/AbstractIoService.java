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

import java.util.Set;


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

    /**
     * Maintains the {@link IoServiceListener}s of this service.
     */
    private final IoServiceListenerSupport listeners;

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

        this.handler = handler;
    }

    protected IoServiceListenerSupport getListeners() {
        return listeners;
    }

    public IoSessionConfig getSessionConfig() {
        return sessionConfig;
    }
    
    protected static class ServiceOperationFuture extends DefaultIoFuture {
        public ServiceOperationFuture() {
            super(null);
        }
        
        public boolean isDone() {
            return (getValue() == Boolean.TRUE);
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
