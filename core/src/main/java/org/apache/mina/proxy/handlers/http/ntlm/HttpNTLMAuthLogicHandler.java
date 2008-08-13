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
package org.apache.mina.proxy.handlers.http.ntlm;

import java.io.IOException;
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
 * HttpNTLMAuthLogicHandler.java - HTTP NTLM authentication mechanism logic handler.
 * 
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a>
 * @version $Id: $
 */
public class HttpNTLMAuthLogicHandler extends AbstractAuthLogicHandler {

    private final static Logger logger = LoggerFactory
            .getLogger(HttpNTLMAuthLogicHandler.class);

    /**
     * The challenge provided by the server.
     */
    private byte[] challengePacket = null;

    public HttpNTLMAuthLogicHandler(final ProxyIoSession proxyIoSession)
            throws ProxyAuthException {
        super(proxyIoSession);

        if (request == null || !(request instanceof HttpProxyRequest)) {
            throw new IllegalArgumentException(
                    "request parameter should be a non null HttpProxyRequest instance");
        }

        HttpProxyRequest req = (HttpProxyRequest) request;
        req.checkRequiredProperty(HttpProxyConstants.USER_PROPERTY);
        req.checkRequiredProperty(HttpProxyConstants.PWD_PROPERTY);
        req.checkRequiredProperty(HttpProxyConstants.DOMAIN_PROPERTY);
        req.checkRequiredProperty(HttpProxyConstants.WORKSTATION_PROPERTY);
    }

    @Override
    public void doHandshake(NextFilter nextFilter) throws ProxyAuthException {
        logger.debug(" doHandshake()");

        if (step > 0 && challengePacket == null) {
            throw new IllegalStateException("Challenge packet not received");
        } else {
            HttpProxyRequest req = (HttpProxyRequest) request;
            Map<String, List<String>> headers = req.getHeaders() != null ? req
                    .getHeaders() : new HashMap<String, List<String>>();

            String domain = req.getProperties().get(
                    HttpProxyConstants.DOMAIN_PROPERTY);
            String workstation = req.getProperties().get(
                    HttpProxyConstants.WORKSTATION_PROPERTY);

            if (step > 0) {
                logger.debug("  sending NTLM challenge response");

                byte[] challenge = NTLMUtilities
                        .extractChallengeFromType2Message(challengePacket);
                int serverFlags = NTLMUtilities
                        .extractFlagsFromType2Message(challengePacket);

                String username = req.getProperties().get(
                        HttpProxyConstants.USER_PROPERTY);
                String password = req.getProperties().get(
                        HttpProxyConstants.PWD_PROPERTY);

                byte[] authenticationPacket = NTLMUtilities.createType3Message(
                        username, password, challenge, domain, workstation,
                        serverFlags, null);

                StringUtilities.addValueToHeader(headers,
                        "Proxy-Authorization", "NTLM "
                                + new String(Base64
                                        .encodeBase64(authenticationPacket)),
                        true);

            } else {
                logger.debug("  sending HTTP request");

                byte[] negotiationPacket = NTLMUtilities.createType1Message(
                        workstation, domain, null, null);
                StringUtilities
                        .addValueToHeader(
                                headers,
                                "Proxy-Authorization",
                                "NTLM "
                                        + new String(
                                                Base64
                                                        .encodeBase64(negotiationPacket)),
                                true);
            }

            StringUtilities.addValueToHeader(headers, "Keep-Alive",
                    HttpProxyConstants.DEFAULT_KEEP_ALIVE_TIME, true);
            StringUtilities.addValueToHeader(headers, "Proxy-Connection",
                    "keep-Alive", true);
            req.setHeaders(headers);

            writeRequest(nextFilter, req);
            step++;
        }
    }

    /**
     * Returns the value of the NTLM Proxy-Authenticate header.
     */
    private String getNTLMHeader(final HttpProxyResponse response) {
        List<String> values = response.getHeaders().get("Proxy-Authenticate");

        for (String s : values) {
            if (s.startsWith("NTLM")) {
                return s;
            }
        }

        return null;
    }

    @Override
    public void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException {
        if (step == 0) {
            String challengeResponse = getNTLMHeader(response);
            step = 1;

            if (challengeResponse == null || challengeResponse.length() < 5) {
                // Nothing to handle at this step. Just need to send a reply type 1 message in doHandshake().
                return;
            }

            // else there was no step 0 so continue to step 1.
        }

        if (step == 1) {
            // Header should be like this
            // Proxy-Authenticate: NTLM still_some_more_stuff
            String challengeResponse = getNTLMHeader(response);

            if (challengeResponse == null || challengeResponse.length() < 5) {
                throw new ProxyAuthException(
                        "Unexpected error while reading server challenge !");
            }

            try {
                challengePacket = Base64
                        .decodeBase64(challengeResponse.substring(5).getBytes(
                                proxyIoSession.getCharsetName()));
            } catch (IOException e) {
                throw new ProxyAuthException(
                        "Unable to decode the base64 encoded NTLM challenge", e);
            }
            step = 2;
        } else {
            throw new ProxyAuthException("Received unexpected response code ("
                    + response.getStatusLine() + ").");
        }
    }
}