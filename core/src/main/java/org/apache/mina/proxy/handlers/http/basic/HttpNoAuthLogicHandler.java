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
package org.apache.mina.proxy.handlers.http.basic;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.http.AbstractAuthLogicHandler;
import org.apache.mina.proxy.handlers.http.HttpProxyRequest;
import org.apache.mina.proxy.handlers.http.HttpProxyResponse;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpNoAuthLogicHandler.java - HTTP 'no auth' mechanism logic handler.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpNoAuthLogicHandler extends AbstractAuthLogicHandler {
    private final static Logger logger = LoggerFactory
            .getLogger(HttpNoAuthLogicHandler.class);

    /**
     * {@inheritDoc}
     */
    public HttpNoAuthLogicHandler(final ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        super(proxyIoSession);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doHandshake(final NextFilter nextFilter)
            throws ProxyAuthException {
        logger.debug(" doHandshake()");

        // Just send the request, no authentication needed
        writeRequest(nextFilter, (HttpProxyRequest) request);
        step++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException {
        // Should never get here !
        throw new ProxyAuthException("Received error response code ("
                + response.getStatusLine() + ").");
    }
}