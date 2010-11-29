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
package org.apache.mina.proxy.handlers.socks;

import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.ByteUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socks4LogicHandler.java - SOCKS4/SOCKS4a authentication mechanisms logic handler.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class Socks4LogicHandler extends AbstractSocksLogicHandler {

    private final static Logger logger = LoggerFactory
            .getLogger(Socks4LogicHandler.class);

    /**
     * {@inheritDoc}
     */
    public Socks4LogicHandler(final ProxyIoSession proxyIoSession) {
        super(proxyIoSession);
    }

    /**
     * Perform the handshake.
     * 
     * @param nextFilter the next filter
     */
    public void doHandshake(final NextFilter nextFilter) {
        logger.debug(" doHandshake()");

        // Send request
        writeRequest(nextFilter, request);
    }

    /**
     * Encode a SOCKS4/SOCKS4a request and writes it to the next filter
     * so it can be sent to the proxy server.
     * 
     * @param nextFilter the next filter
     * @param request the request to send.
     */
    protected void writeRequest(final NextFilter nextFilter,
            final SocksProxyRequest request) {
        try {
            boolean isV4ARequest = Arrays.equals(request.getIpAddress(),
                    SocksProxyConstants.FAKE_IP); 
            byte[] userID = request.getUserName().getBytes("ASCII");
            byte[] host = isV4ARequest ? request.getHost().getBytes("ASCII")
                    : null;

            int len = 9 + userID.length;

            if (isV4ARequest) {
                len += host.length + 1;
            }

            IoBuffer buf = IoBuffer.allocate(len);

            buf.put(request.getProtocolVersion());
            buf.put(request.getCommandCode());
            buf.put(request.getPort());
            buf.put(request.getIpAddress());
            buf.put(userID);
            buf.put(SocksProxyConstants.TERMINATOR);

            if (isV4ARequest) {
                buf.put(host);
                buf.put(SocksProxyConstants.TERMINATOR);
            }

            if (isV4ARequest) {
                logger.debug("  sending SOCKS4a request");
            } else {
                logger.debug("  sending SOCKS4 request");
            }

            buf.flip();
            writeData(nextFilter, buf);
        } catch (Exception ex) {
            closeSession("Unable to send Socks request: ", ex);
        }
    }

    /**
     * Handle incoming data during the handshake process. Should consume only the
     * handshake data from the buffer, leaving any extra data in place.
     * 
     * @param nextFilter the next filter
     * @param buf the server response data buffer
     */
    public void messageReceived(final NextFilter nextFilter,
            final IoBuffer buf) {
        try {
            if (buf.remaining() >= SocksProxyConstants.SOCKS_4_RESPONSE_SIZE) {
                handleResponse(buf);
            }
        } catch (Exception ex) {
            closeSession("Proxy handshake failed: ", ex);
        }
    }

    /**
     * Handle a SOCKS4/SOCKS4a response from the proxy server. Test
     * the response buffer reply code and call {@link #setHandshakeComplete()}
     * if access is granted.
     * 
     * @param buf the buffer holding the server response data.
     * @throws exception if server response is malformed or if request is rejected
     * by the proxy server.
     */
    protected void handleResponse(final IoBuffer buf) throws Exception {
        byte first = buf.get(0);

        if (first != 0) {
            throw new Exception("Socks response seems to be malformed");
        }

        byte status = buf.get(1);

        // Consumes all the response data from the buffer
        buf.position(buf.position() + SocksProxyConstants.SOCKS_4_RESPONSE_SIZE);
        
        if (status == SocksProxyConstants.V4_REPLY_REQUEST_GRANTED) {
            setHandshakeComplete();
        } else {
            throw new Exception("Proxy handshake failed - Code: 0x"
                    + ByteUtilities.asHex(new byte[] { status }) + " ("
                    + SocksProxyConstants.getReplyCodeAsString(status) + ")");
        }
    }
}