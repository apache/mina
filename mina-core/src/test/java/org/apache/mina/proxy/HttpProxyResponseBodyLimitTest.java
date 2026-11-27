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
package org.apache.mina.proxy;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.proxy.handlers.http.AbstractHttpLogicHandler;
import org.apache.mina.proxy.handlers.http.HttpProxyRequest;
import org.apache.mina.proxy.handlers.http.HttpProxyResponse;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.IoBufferDecoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression tests: sizes declared by the proxy during the HTTP handshake
 * (Content-Length, chunk sizes) drive buffer allocation before any body byte
 * arrives, so they must be bounded. A hostile or compromised proxy answering
 * a CONNECT with "Content-Length: 2000000000" (or a single "7ffffff0" chunk
 * header) would otherwise trigger a ~2 GB allocation for free, taking down
 * the client JVM.
 */
public class HttpProxyResponseBodyLimitTest {

    /** Minimal concrete handler: only the response decoding path is under test. */
    private static class TestHttpLogicHandler extends AbstractHttpLogicHandler {
        boolean responseHandled;

        TestHttpLogicHandler(ProxyIoSession proxyIoSession) {
            super(proxyIoSession);
        }

        @Override
        public void doHandshake(NextFilter nextFilter) {
            // Do nothing
        }

        @Override
        public void handleResponse(HttpProxyResponse response) {
            responseHandled = true;
        }
    }

    private TestHttpLogicHandler newHandler() {
        ProxyIoSession proxyIoSession = new ProxyIoSession(new InetSocketAddress("localhost", 8080),
                new HttpProxyRequest("http://localhost/"));
        proxyIoSession.setSession(new DummySession());

        return new TestHttpLogicHandler(proxyIoSession);
    }

    private static IoBuffer response(String text) {
        return IoBuffer.wrap(text.getBytes());
    }

    @Test
    public void hugeContentLengthIsRejected() throws Exception {
        TestHttpLogicHandler handler = newHandler();

        try {
            handler.messageReceived(null, response("HTTP/1.0 407 Proxy Authentication Required\r\n"
                    + "Proxy-Authenticate: Basic realm=\"test\"\r\n" + "Content-Length: 2000000000\r\n" + "\r\n"));
            fail("expected ProxyAuthException for a 2 GB declared Content-Length");
        } catch (ProxyAuthException expected) {
            assertTrue(expected.getMessage().contains("Content-Length"), expected.getMessage());
        }
    }

    @Test
    public void hugeChunkSizeIsRejected() throws Exception {
        TestHttpLogicHandler handler = newHandler();

        try {
            handler.messageReceived(null, response("HTTP/1.1 407 Proxy Authentication Required\r\n"
                    + "Proxy-Authenticate: Basic realm=\"test\"\r\n" + "Transfer-Encoding: chunked\r\n" + "\r\n"
                    + "7ffffff0\r\n"));
            fail("expected ProxyAuthException for a ~2 GB declared chunk size");
        } catch (ProxyAuthException expected) {
            assertTrue(expected.getMessage().contains("chunk size"), expected.getMessage());
        }
    }

    @Test
    public void reasonableBodyIsStillAccepted() throws Exception {
        TestHttpLogicHandler handler = newHandler();

        handler.messageReceived(null, response("HTTP/1.0 407 Proxy Authentication Required\r\n"
                + "Proxy-Authenticate: Basic realm=\"test\"\r\n" + "Content-Length: 5\r\n" + "\r\n" + "hello"));

        assertTrue(handler.responseHandled, "small entity bodies must still be decoded and handled");
    }

    @Test
    public void contentLengthDecodingIsIncremental() {
        // Guards the lazy-allocation change in IoBufferDecoder: fragmented
        // content must still be reassembled correctly.
        IoBufferDecoder decoder = new IoBufferDecoder(10);

        assertNull(decoder.decodeFully(IoBuffer.wrap("hello".getBytes())), "partial content must not complete decoding");

        IoBuffer decoded = decoder.decodeFully(IoBuffer.wrap("world".getBytes()));
        assertNotNull(decoded);
        assertTrue(decoded.hasRemaining());

        byte[] content = new byte[decoded.remaining()];
        decoded.get(content);
        assertTrue(new String(content).equals("helloworld"), "reassembled content must match");
    }

    @Test
    public void hugeDeclaredContentLengthDoesNotAllocateUpfront() {
        // Before the fix this allocated the full declared length before any
        // body byte arrived. Now allocation grows with the data received.
        IoBufferDecoder decoder = new IoBufferDecoder(Integer.MAX_VALUE - 8);

        assertNull(decoder.decodeFully(IoBuffer.wrap("tiny".getBytes())));
    }
}
