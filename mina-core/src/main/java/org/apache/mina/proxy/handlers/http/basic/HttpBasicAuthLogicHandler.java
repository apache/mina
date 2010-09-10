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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.http.AbstractAuthLogicHandler;
import org.apache.mina.proxy.handlers.http.HttpProxyConstants;
import org.apache.mina.proxy.handlers.http.HttpProxyRequest;
import org.apache.mina.proxy.handlers.http.HttpProxyResponse;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.StringUtilities;
import org.apache.mina.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpBasicAuthLogicHandler.java - HTTP Basic authentication mechanism logic handler.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpBasicAuthLogicHandler extends AbstractAuthLogicHandler {
    private final static Logger logger = LoggerFactory
            .getLogger(HttpBasicAuthLogicHandler.class);

    /**
     * {@inheritDoc}
     */
    public HttpBasicAuthLogicHandler(final ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        super(proxyIoSession);

        ((HttpProxyRequest) request).checkRequiredProperties(
                HttpProxyConstants.USER_PROPERTY,
                HttpProxyConstants.PWD_PROPERTY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doHandshake(final NextFilter nextFilter)
            throws ProxyAuthException {
        logger.debug(" doHandshake()");

        if (step > 0) {
            throw new ProxyAuthException("Authentication request already sent");
        }

        // Send request
        HttpProxyRequest req = (HttpProxyRequest) request;
        Map<String, List<String>> headers = req.getHeaders() != null ? req
                .getHeaders() : new HashMap<String, List<String>>();

        String username = req.getProperties().get(
                HttpProxyConstants.USER_PROPERTY);
        String password = req.getProperties().get(
                HttpProxyConstants.PWD_PROPERTY);

        StringUtilities.addValueToHeader(headers, "Proxy-Authorization",
                "Basic " + createAuthorization(username, password), true);

        addKeepAliveHeaders(headers);
        req.setHeaders(headers);

        writeRequest(nextFilter, req);
        step++;
    }

    /**
     * Computes the authorization header value.
     * 
     * @param username the user name
     * @param password the user password
     * @return the authorization header value as a string
     */
    public static String createAuthorization(final String username,
            final String password) {
        return new String(Base64.encodeBase64((username + ":" + password)
                .getBytes()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException {
        if (response.getStatusCode() != 407) {
            throw new ProxyAuthException("Received error response code ("
                    + response.getStatusLine() + ").");
        }
    }
}