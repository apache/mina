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
package org.apache.mina.proxy.handlers.http.digest;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
 * HttpDigestAuthLogicHandler.java - HTTP Digest authentication mechanism logic handler. 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpDigestAuthLogicHandler extends AbstractAuthLogicHandler {

    private final static Logger logger = LoggerFactory
            .getLogger(HttpDigestAuthLogicHandler.class);

    /**
     * The challenge directives provided by the server.
     */
    private HashMap<String, String> directives = null;

    /**
     * The response received to the last request.
     */
    private HttpProxyResponse response;

    private static SecureRandom rnd;

    static {
        // Initialize secure random generator 
        try {
            rnd = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpDigestAuthLogicHandler(final ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        super(proxyIoSession);

        ((HttpProxyRequest) request).checkRequiredProperties(
                HttpProxyConstants.USER_PROPERTY,
                HttpProxyConstants.PWD_PROPERTY);
    }

    @Override
    public void doHandshake(NextFilter nextFilter) throws ProxyAuthException {
        logger.debug(" doHandshake()");

        if (step > 0 && directives == null) {
            throw new ProxyAuthException(
                    "Authentication challenge not received");
        }
        
        HttpProxyRequest req = (HttpProxyRequest) request;
        Map<String, List<String>> headers = req.getHeaders() != null ? req
                .getHeaders() : new HashMap<String, List<String>>();

        if (step > 0) {
            logger.debug("  sending DIGEST challenge response");

            // Build a challenge response
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("username", req.getProperties().get(
                    HttpProxyConstants.USER_PROPERTY));
            StringUtilities.copyDirective(directives, map, "realm");
            StringUtilities.copyDirective(directives, map, "uri");
            StringUtilities.copyDirective(directives, map, "opaque");
            StringUtilities.copyDirective(directives, map, "nonce");
            String algorithm = StringUtilities.copyDirective(directives,
                    map, "algorithm");

            // Check for a supported algorithm
            if (algorithm != null && !"md5".equalsIgnoreCase(algorithm)
                    && !"md5-sess".equalsIgnoreCase(algorithm)) {
                throw new ProxyAuthException(
                        "Unknown algorithm required by server");
            }

            // Check for a supported qop
            String qop = directives.get("qop");
            if (qop != null) {
                StringTokenizer st = new StringTokenizer(qop, ",");
                String token = null;

                while (st.hasMoreTokens()) {
                    String tk = st.nextToken();
                    if ("auth".equalsIgnoreCase(token)) {
                        break;
                    }

                    int pos = Arrays.binarySearch(
                            DigestUtilities.SUPPORTED_QOPS, tk);
                    if (pos > -1) {
                        token = tk;
                    }
                }

                if (token != null) {
                    map.put("qop", token);

                    byte[] nonce = new byte[8];
                    rnd.nextBytes(nonce);

                    try {
                        String cnonce = new String(Base64
                                .encodeBase64(nonce), proxyIoSession
                                .getCharsetName());
                        map.put("cnonce", cnonce);
                    } catch (UnsupportedEncodingException e) {
                        throw new ProxyAuthException(
                                "Unable to encode cnonce", e);
                    }
                } else {
                    throw new ProxyAuthException(
                            "No supported qop option available");
                }
            }

            map.put("nc", "00000001");
            map.put("uri", req.getHttpURI());

            // Compute the response
            try {
                map.put("response", DigestUtilities
                        .computeResponseValue(proxyIoSession.getSession(),
                                map, req.getHttpVerb().toUpperCase(),
                                req.getProperties().get(
                                        HttpProxyConstants.PWD_PROPERTY),
                                proxyIoSession.getCharsetName(), response
                                        .getBody()));

            } catch (Exception e) {
                throw new ProxyAuthException(
                        "Digest response computing failed", e);
            }

            // Prepare the challenge response header and add it to the 
            // request we will send
            StringBuilder sb = new StringBuilder("Digest ");
            boolean addSeparator = false;

            for (String key : map.keySet()) {

                if (addSeparator) {
                    sb.append(", ");
                } else {
                    addSeparator = true;
                }

                boolean quotedValue = !"qop".equals(key)
                        && !"nc".equals(key);
                sb.append(key);
                if (quotedValue) {
                    sb.append("=\"").append(map.get(key)).append('\"');
                } else {
                    sb.append('=').append(map.get(key));
                }
            }

            StringUtilities.addValueToHeader(headers,
                    "Proxy-Authorization", sb.toString(), true);
        }

        addKeepAliveHeaders(headers);
        req.setHeaders(headers);

        writeRequest(nextFilter, req);
        step++;
    }

    @Override
    public void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException {
        this.response = response;

        if (step == 0) {
            if (response.getStatusCode() != 401
                    && response.getStatusCode() != 407) {
                throw new ProxyAuthException(
                        "Received unexpected response code ("
                                + response.getStatusLine() + ").");
            }

            // Header should look like this
            // Proxy-Authenticate: Digest still_some_more_stuff
            List<String> values = response.getHeaders().get(
                    "Proxy-Authenticate");
            String challengeResponse = null;

            for (String s : values) {
                if (s.startsWith("Digest")) {
                    challengeResponse = s;
                    break;
                }
            }

            if (challengeResponse == null) {
                throw new ProxyAuthException(
                        "Server doesn't support digest authentication method !");
            }

            try {
                directives = StringUtilities.parseDirectives(challengeResponse
                        .substring(7).getBytes(proxyIoSession.getCharsetName()));
            } catch (Exception e) {
                throw new ProxyAuthException(
                        "Parsing of server digest directives failed", e);
            }
            step = 1;
        } else {
            throw new ProxyAuthException("Received unexpected response code ("
                    + response.getStatusLine() + ").");
        }
    }
}