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
package org.apache.mina.api;

import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.mina.service.IoHandler;

/**
 * Base interface for all {@link IoServer}s and {@link IoClient}s that provide I/O service and manage {@link IoSession}
 * s.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoService {

    /**
     * Returns the map of all sessions which are currently managed by this service. The key of map is the
     * {@link IoSession#getId() ID} of the session.
     * 
     * @return the sessions. An empty collection if there's no session.
     */
    Map<Long, IoSession> getManagedSessions();

    /**
     * Adds an {@link IoServiceListener} that listens any events related with this service.
     */
    void addListener(IoServiceListener listener);

    /**
     * Removed an existing {@link IoServiceListener} that listens any events related with this service.
     */
    void removeListener(IoServiceListener listener);

    /**
     * Returns the handler which will handle all the connections managed by this service.
     */

    IoHandler getHandler();

    /**
     * Sets the handler which will handle all connections managed by this service. The handler can only be set before
     * the service is started.
     */
    void setHandler(IoHandler handler);

    /**
     * Get the list of filters installed on this service
     * 
     * @return
     */
    IoFilter[] getFilters();

    /**
     * Set the list of filters for this service. Must be called before the service is bound/connected
     */
    void setFilters(IoFilter... filters);

    /**
     * Returns the default configuration of the new {@link IoSession}s
     * created by this service.
     */
    IoSessionConfig getSessionConfig();
    
    /**
     * Tells if the service provide some encryption (SSL/TLS)
     * @return <code>true</code> if the service is secured
     */
    boolean isSecured();
    
    /**
     * Set the mode to use, either secured or not secured
     * @param secured The mode to use
     */
    void setSecured(boolean secured);
    
    /**
     * Inject a {@link SSLContex} valid for the service. This {@link SSLContex} will be used
     * by the SSLEngine to handle secured connections.<br/>
     * The {@link SSLContex} must have been created and initialized before being injected in
     * the service.
     * @param sslContext The configured {@link SSLContex}.
     */
    void addSslContext(SSLContext sslContext);
    
    /**
     * @return The {@link SSLContext} instance stored in the service.
     */
    SSLContext getSslContext();
}
