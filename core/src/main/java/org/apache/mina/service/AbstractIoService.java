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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLContext;

import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoServiceListener;
import org.apache.mina.api.IoSession;
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
    
    /** The service mode : secured or not */
    private ServiceMode mode;
    
    /** The SSLContext instance */
    private SSLContext sslContext;

    private final Map<Long, IoSession> managedSessions = new ConcurrentHashMap<Long, IoSession>();

    /**
     * The handler, the interface with the application part.
     */
    private IoHandler handler;

    /**
     * Placeholder for storing all the listeners added
     */
    private final List<IoServiceListener> listeners = new CopyOnWriteArrayList<IoServiceListener>();

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
     * The Service secured mode
     */
    protected enum ServiceMode {
        /** SSL/TLS activated */
        SECURED,
        /** SSL/TLS not activated */
        NOT_SECURED
    }

    /**
     * Create an AbstractIoService
     */
    protected AbstractIoService() {
        state = ServiceState.NONE;
        mode = ServiceMode.NOT_SECURED;
    }

    @Override
    public Map<Long, IoSession> getManagedSessions() {
        return managedSessions;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void addListener(IoServiceListener listener) {
        if (listener != null) {
            listeners.add(listener);
            return;
        }

        LOG.warn("Trying to add Null Listener");
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void removeListener(IoServiceListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final IoHandler getHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    public final void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        // TODO: check the service state, we should not be able to set the handler
        // if the service is already started
        /*
         * if (isActive()) { throw new IllegalStateException( "handler cannot be set while the service is active."); }
         */

        this.handler = handler;
    }

    /**
     * @return true if the IoService is active
     */
    public boolean isActive() {
        return state == ServiceState.ACTIVE;
    }

    /**
     * @return true if the IoService is being disposed
     */
    public boolean isDisposing() {
        return state == ServiceState.DISPOSING;
    }

    /**
     * @return true if the IoService is disposed
     */
    public boolean isDisposed() {
        return state == ServiceState.DISPOSED;
    }

    /**
     * @return true if the IoService is suspended
     */
    public boolean isSuspended() {
        return state == ServiceState.SUSPENDED;
    }

    /**
     * @return true if the IoService is created
     */
    public boolean isCreated() {
        return state == ServiceState.CREATED;
    }

    /**
     * Sets the IoService state to CREATED.
     */
    protected void setCreated() {
        state = ServiceState.CREATED;
    }

    /**
     * Sets the IoService state to ACTIVE.
     */
    protected void setActive() {
        state = ServiceState.ACTIVE;
    }

    /**
     * Sets the IoService state to DISPOSED.
     */
    protected void setDisposed() {
        state = ServiceState.DISPOSED;
    }

    /**
     * Sets the IoService state to DISPOSING.
     */
    protected void setDisposing() {
        state = ServiceState.DISPOSING;
    }

    /**
     * Sets the IoService state to SUSPENDED.
     */
    protected void setSuspended() {
        state = ServiceState.SUSPENDED;
    }

    /**
     * Initialize the IoService state
     */
    protected void initState() {
        state = ServiceState.NONE;
    }

    /**
     * Inform all current the listeners of the service activation.
     */
    protected void fireServiceActivated() {
        for (IoServiceListener listener : listeners) {
            listener.serviceActivated(this);
        }
    }

    /**
     * Inform all current the listeners of the service desactivation.
     */
    protected void fireServiceInactivated() {
        for (IoServiceListener listener : listeners) {
            listener.serviceInactivated(this);
        }
    }

    public void fireSessionCreated(IoSession session) {
        for (IoServiceListener listener : listeners) {
            listener.sessionCreated(session);
        }
    }

    public void fireSessionDestroyed(IoSession session) {
        for (IoServiceListener listener : listeners) {
            listener.sessionDestroyed(session);
        }
    }

    private IoFilter[] filters;

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFilter[] getFilters() {
        return filters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFilters(IoFilter... filters) {
        this.filters = filters;
    }
    
    /**
     * Tells if the service provide some encryption (SSL/TLS)
     * @return <code>true</code> if the service is secured
     */
    public boolean isSecured() {
        return mode == ServiceMode.SECURED;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setSecured(boolean secured) {
        if (secured) {
            mode = ServiceMode.SECURED;
        } else {
            mode = ServiceMode.NOT_SECURED;
        }
    }
    
    public void addSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }
    
    /**
     * {@inheritDoc}
     */
    public SSLContext getSslContext() {
        return sslContext;
    }
}