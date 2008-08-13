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
package org.apache.mina.proxy.handlers.http;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.ProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractAuthLogicHandler.java - Abstract class that handles an authentication mechanism logic.
 * 
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a>
 * @version $Id: $
 */
public abstract class AbstractAuthLogicHandler {
    private final static Logger logger = LoggerFactory
            .getLogger(AbstractAuthLogicHandler.class);

    /**
     * The request the proxy has to handle.
     */
    protected ProxyRequest request;

    /**
     * Object that contains all the proxy authentication session informations.
     */
    protected ProxyIoSession proxyIoSession;

    /**
     * The current step in the handshake.
     */
    protected int step = 0;

    protected AbstractAuthLogicHandler(final ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        this.proxyIoSession = proxyIoSession;
        this.request = proxyIoSession.getRequest();
    }

    /**
     * Called on each step of the handshaking process.
     */
    public abstract void doHandshake(final NextFilter nextFilter)
            throws ProxyAuthException;

    /**
     * Handles a HTTP response from the proxy server.
     * 
     * @param response The response.
     */
    public abstract void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException;

    protected void writeRequest(final NextFilter nextFilter,
            final HttpProxyRequest request) throws ProxyAuthException {
        logger.debug("  sending HTTP request");

        ((AbstractHttpLogicHandler) proxyIoSession.getHandler()).writeRequest(
                nextFilter, request);
    }
}