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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpSmartProxyHandler.java - HTTP proxy handler that automatically handles forwarding a request 
 * to the appropriate authentication mechanism logic handler.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpSmartProxyHandler extends AbstractHttpLogicHandler {
    private final static Logger logger = LoggerFactory
            .getLogger(HttpSmartProxyHandler.class);

    /**
     * Has the HTTP proxy request been sent ?
     */
    private boolean requestSent = false;

    /**
     * The automatically selected http authentication logic handler. 
     */
    private AbstractAuthLogicHandler authHandler;

    public HttpSmartProxyHandler(final ProxyIoSession proxyIoSession) {
        super(proxyIoSession);
    }

    /**
     * Performs the handshake processing.
     * 
     * @param nextFilter the next filter
     */
    public void doHandshake(final NextFilter nextFilter)
            throws ProxyAuthException {
        logger.debug(" doHandshake()");

        if (authHandler != null) {
            authHandler.doHandshake(nextFilter);
        } else {
            if (requestSent) {
                // Safety check
                throw new ProxyAuthException(
                        "Authentication request already sent");
            }

            logger.debug("  sending HTTP request");

            // Compute request headers
            HttpProxyRequest req = (HttpProxyRequest) getProxyIoSession()
                    .getRequest();
            Map<String, List<String>> headers = req.getHeaders() != null ? req
                    .getHeaders() : new HashMap<String, List<String>>();

            AbstractAuthLogicHandler.addKeepAliveHeaders(headers);
            req.setHeaders(headers);

            // Write request to the proxy
            writeRequest(nextFilter, req);
            requestSent = true;
        }
    }

    /**
     * Automatic selection of the authentication algorithm. If <code>preferedOrder</code> is set then
     * algorithms are selected from the list order otherwise the algorithm tries to select the most 
     * secured algorithm available first.
     * 
     * @param response the proxy response
     */
    private void autoSelectAuthHandler(final HttpProxyResponse response)
            throws ProxyAuthException {
        // Get the Proxy-Authenticate header
        List<String> values = response.getHeaders().get("Proxy-Authenticate");
        ProxyIoSession proxyIoSession = getProxyIoSession();

        if (values == null || values.size() == 0) {
            authHandler = HttpAuthenticationMethods.NO_AUTH
                    .getNewHandler(proxyIoSession);

        } else if (getProxyIoSession().getPreferedOrder() == null) {
            // No preference order set for auth mechanisms
            int method = -1;

            // Test which auth mechanism to use. First found is the first used
            // that's why we test in a decreasing security quality order.
            for (String proxyAuthHeader : values) {
                proxyAuthHeader = proxyAuthHeader.toLowerCase();

                if (proxyAuthHeader.contains("ntlm")) {
                    method = HttpAuthenticationMethods.NTLM.getId();
                    break;
                } else if (proxyAuthHeader.contains("digest")
                        && method != HttpAuthenticationMethods.NTLM.getId()) {
                    method = HttpAuthenticationMethods.DIGEST.getId();
                } else if (proxyAuthHeader.contains("basic") && method == -1) {
                    method = HttpAuthenticationMethods.BASIC.getId();
                }
            }

            if (method != -1) {
                try {
                    authHandler = HttpAuthenticationMethods.getNewHandler(
                            method, proxyIoSession);
                } catch (Exception ex) {
                    logger.debug("Following exception occured:", ex);
                }
            }

            if (authHandler == null) {
                authHandler = HttpAuthenticationMethods.NO_AUTH
                        .getNewHandler(proxyIoSession);
            }

        } else {
            for (HttpAuthenticationMethods method : proxyIoSession
                    .getPreferedOrder()) {
                if (authHandler != null) {
                    break;
                }

                if (method == HttpAuthenticationMethods.NO_AUTH) {
                    authHandler = HttpAuthenticationMethods.NO_AUTH
                            .getNewHandler(proxyIoSession);
                    break;
                }

                for (String proxyAuthHeader : values) {
                    proxyAuthHeader = proxyAuthHeader.toLowerCase();

                    try {
                        // test which auth mechanism to use
                        if (proxyAuthHeader.contains("basic")
                                && method == HttpAuthenticationMethods.BASIC) {
                            authHandler = HttpAuthenticationMethods.BASIC
                                    .getNewHandler(proxyIoSession);
                            break;
                        } else if (proxyAuthHeader.contains("digest")
                                && method == HttpAuthenticationMethods.DIGEST) {
                            authHandler = HttpAuthenticationMethods.DIGEST
                                    .getNewHandler(proxyIoSession);
                            break;
                        } else if (proxyAuthHeader.contains("ntlm")
                                && method == HttpAuthenticationMethods.NTLM) {
                            authHandler = HttpAuthenticationMethods.NTLM
                                    .getNewHandler(proxyIoSession);
                            break;
                        }
                    } catch (Exception ex) {
                        logger.debug("Following exception occured:", ex);
                    }
                }
            }

        }

        if (authHandler == null) {
            throw new ProxyAuthException(
                    "Unknown authentication mechanism(s): " + values);
        }
    }

    /**
     * Handle a HTTP response from the proxy server.
     * 
     * @param response The proxy response.
     */
    @Override
    public void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException {
        if (!isHandshakeComplete()
                && ("close".equalsIgnoreCase(StringUtilities
                        .getSingleValuedHeader(response.getHeaders(),
                                "Proxy-Connection")) || "close"
                        .equalsIgnoreCase(StringUtilities
                                .getSingleValuedHeader(response.getHeaders(),
                                        "Connection")))) {
            getProxyIoSession().setReconnectionNeeded(true);
        }

        if (response.getStatusCode() == 407) {
            if (authHandler == null) {
                autoSelectAuthHandler(response);
            }
            authHandler.handleResponse(response);
        } else {
            throw new ProxyAuthException("Error: unexpected response code "
                    + response.getStatusLine() + " received from proxy.");
        }
    }
}