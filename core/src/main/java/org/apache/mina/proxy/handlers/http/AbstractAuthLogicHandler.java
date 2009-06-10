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

import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.ProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractAuthLogicHandler.java - Abstract class that handles an authentication 
 * mechanism logic.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public abstract class AbstractAuthLogicHandler {
    private final static Logger logger = LoggerFactory
            .getLogger(AbstractAuthLogicHandler.class);

    /**
     * The request to be handled by the proxy.
     */
    protected ProxyRequest request;

    /**
     * Object that contains all the proxy authentication session informations.
     */
    protected ProxyIoSession proxyIoSession;

    /**
     * The current handshake step.
     */
    protected int step = 0;

    /**
     * Instantiates a handler for the given proxy session.
     * 
     * @param proxyIoSession the proxy session object
     * @throws ProxyAuthException
     */
    protected AbstractAuthLogicHandler(final ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        this.proxyIoSession = proxyIoSession;
        this.request = proxyIoSession.getRequest();

        if (this.request == null || !(this.request instanceof HttpProxyRequest)) {
            throw new IllegalArgumentException(
                    "request parameter should be a non null HttpProxyRequest instance");
        }
    }

    /**
     * Method called at each step of the handshaking process.
     * 
     * @param nextFilter the next filter
     * @throws ProxyAuthException
     */
    public abstract void doHandshake(final NextFilter nextFilter)
            throws ProxyAuthException;

    /**
     * Handles a HTTP response from the proxy server.
     * 
     * @param response The HTTP response.
     * @throws ProxyAuthException
     */
    public abstract void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException;

    /**
     * Sends an HTTP request.
     * 
     * @param nextFilter the next filter
     * @param request the request to write
     * @throws ProxyAuthException
     */
    protected void writeRequest(final NextFilter nextFilter,
            final HttpProxyRequest request) throws ProxyAuthException {
        logger.debug("  sending HTTP request");

        ((AbstractHttpLogicHandler) proxyIoSession.getHandler()).writeRequest(
                nextFilter, request);
    }
    
    /**
     * Try to force proxy connection to be kept alive.
     * 
     * @param headers the request headers
     */
    public static void addKeepAliveHeaders(Map<String, List<String>> headers) {
        StringUtilities.addValueToHeader(headers, "Keep-Alive",
                HttpProxyConstants.DEFAULT_KEEP_ALIVE_TIME, true);
        StringUtilities.addValueToHeader(headers, "Proxy-Connection",
                "keep-Alive", true);
    }
    
}