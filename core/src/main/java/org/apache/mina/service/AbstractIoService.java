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
package org.apache.mina.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for {@link IoService}s.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoService implements IoService {

    /** A logger for this class */
    static final Logger LOG = LoggerFactory.getLogger(AbstractIoService.class);

    /** The service state */
    private ServiceState state;

    /** The placeholder of managed open sessions */
    private final Map<Long, IoSession> managedSessions = new ConcurrentHashMap<Long, IoSession>();

    /** the default session configuration */
    protected IoSessionConfig config;

    /** The high level business logic */
    private IoHandler handler;

    /** Filters chain */
    private IoFilter[] filters = new IoFilter[0];

    /** used for executing IoHandler event in another pool of thread (not in the low level I/O one) */
    protected final IoHandlerExecutor ioHandlerExecutor;

    /**
     * The Service states
     */
    protected enum ServiceState {
        /** Initial state */
        NONE,
        /** The service has been created */
        CREATED,
        /** The service is started */
        ACTIVE,
        /** The service has been suspended */
        SUSPENDED,
        /** The service is being stopped */
        DISPOSING,
        /** The service is stopped */
        DISPOSED
    }

    /**
     * Create an AbstractIoService
     * 
     * @param eventExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O one).
     *        Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    protected AbstractIoService(final IoHandlerExecutor eventExecutor) {
        this.state = ServiceState.NONE;
        this.ioHandlerExecutor = eventExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, IoSession> getManagedSessions() {
        return this.managedSessions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIoHandler(final IoHandler handler) {
        this.handler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoHandler getIoHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoHandlerExecutor getIoHandlerExecutor() {
        return ioHandlerExecutor;
    }

    /**
     * @return true if the IoService is active
     */
    public boolean isActive() {
        return this.state == ServiceState.ACTIVE;
    }

    /**
     * @return true if the IoService is being disposed
     */
    public boolean isDisposing() {
        return this.state == ServiceState.DISPOSING;
    }

    /**
     * @return true if the IoService is disposed
     */
    public boolean isDisposed() {
        return this.state == ServiceState.DISPOSED;
    }

    /**
     * @return true if the IoService is suspended
     */
    public boolean isSuspended() {
        return this.state == ServiceState.SUSPENDED;
    }

    /**
     * @return true if the IoService is created
     */
    public boolean isCreated() {
        return this.state == ServiceState.CREATED;
    }

    /**
     * Sets the IoService state to CREATED.
     */
    protected void setCreated() {
        this.state = ServiceState.CREATED;
    }

    /**
     * Sets the IoService state to ACTIVE.
     */
    protected void setActive() {
        this.state = ServiceState.ACTIVE;
    }

    /**
     * Sets the IoService state to DISPOSED.
     */
    protected void setDisposed() {
        this.state = ServiceState.DISPOSED;
    }

    /**
     * Sets the IoService state to DISPOSING.
     */
    protected void setDisposing() {
        this.state = ServiceState.DISPOSING;
    }

    /**
     * Sets the IoService state to SUSPENDED.
     */
    protected void setSuspended() {
        this.state = ServiceState.SUSPENDED;
    }

    /**
     * Initialize the IoService state
     */
    protected void initState() {
        this.state = ServiceState.NONE;
    }

    /**
     * Inform all current the listeners of the service activation.
     */
    protected void fireServiceActivated() {
        if (handler != null) {
            handler.serviceActivated(this);
        }
    }

    /**
     * Inform all current the listeners of the service desactivation.
     */
    protected void fireServiceInactivated() {
        if (handler != null) {
            handler.serviceInactivated(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFilter[] getFilters() {
        return this.filters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFilters(final IoFilter... filters) {
        this.filters = filters;
    }
}