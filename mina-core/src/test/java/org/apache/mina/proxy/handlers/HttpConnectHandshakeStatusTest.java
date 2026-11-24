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
package org.apache.mina.proxy.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.http.AbstractHttpLogicHandler;
import org.apache.mina.proxy.handlers.http.HttpProxyRequest;
import org.apache.mina.proxy.handlers.http.HttpProxyResponse;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.junit.Test;

/**
 * Regression tests for the HTTP CONNECT handshake status handling:
 * <ul>
 * <li>a 3xx response is a redirect/failure and must never be treated as an
 * established tunnel (previously it completed the handshake and the queued
 * application data was flushed to the proxy's HTTP front-end),</li>
 * <li>on success the receive buffer must stay positioned after the consumed
 * handshake bytes instead of being rewound to 0, which replayed the proxy
 * response into the application's decoder chain.</li>
 * </ul>
 */
public class HttpConnectHandshakeStatusTest {

    /**
     * Minimal concrete handler that records the responses routed to
     * {@link #handleResponse(HttpProxyResponse)}.
     */
    private static class TestLogicHandler extends AbstractHttpLogicHandler {
        HttpProxyResponse lastResponse;

        TestLogicHandler(ProxyIoSession proxyIoSession) {
            super(proxyIoSession);
        }

        @Override
        public void doHandshake(NextFilter nextFilter) throws ProxyAuthException {
            // no-op for the test
        }

        @Override
        public void handleResponse(HttpProxyResponse response) throws ProxyAuthException {
            lastResponse = response;
        }
    }

    private static TestLogicHandler createHandler() {
        ProxyIoSession proxyIoSession = new ProxyIoSession(new InetSocketAddress("127.0.0.1", 8080),
                new HttpProxyRequest("host.example:443"));
        proxyIoSession.setSession(new DummySession());

        return new TestLogicHandler(proxyIoSession);
    }

    private static IoBuffer wrap(String data) throws Exception {
        return IoBuffer.wrap(data.getBytes("ISO-8859-1"));
    }

    @Test
    public void redirectResponseDoesNotEstablishTunnel() throws Exception {
        TestLogicHandler handler = createHandler();

        IoBuffer buf = wrap("HTTP/1.0 302 Found\r\nLocation: http://portal.example/\r\n\r\n");
        handler.messageReceived(null, buf);

        assertFalse("a 3xx CONNECT response must not complete the handshake", handler.isHandshakeComplete());
        assertNotNull("the 3xx response must be routed to handleResponse()", handler.lastResponse);
        assertEquals(302, handler.lastResponse.getStatusCode());
    }

    @Test
    public void successfulResponseLeavesBufferAfterHandshakeBytes() throws Exception {
        TestLogicHandler handler = createHandler();

        String response = "HTTP/1.0 200 Connection established\r\n\r\n";
        IoBuffer buf = wrap(response + "TUNNELED");

        try {
            handler.messageReceived(null, buf);
        } catch (ProxyAuthException e) {
            // The unit-test ProxyIoSession has no connector wired, so the
            // handshake-complete notification fails after the buffer has been
            // positioned; only the buffer state matters here.
        }

        assertTrue(handler.isHandshakeComplete());
        assertEquals("the buffer must be positioned after the consumed handshake bytes", response.length(),
                buf.position());
        assertEquals("only genuine post-tunnel data may remain", "TUNNELED",
                buf.getString(java.nio.charset.StandardCharsets.ISO_8859_1.newDecoder()));
    }
}
