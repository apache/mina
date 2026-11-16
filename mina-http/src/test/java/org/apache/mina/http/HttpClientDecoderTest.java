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
package org.apache.mina.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpResponse;
import org.apache.mina.http.api.HttpStatus;
import org.junit.Test;

public class HttpClientDecoderTest {
    private static final Charset ASCII = Charset.forName("US-ASCII");

    private static final CharsetEncoder encoder = ASCII.newEncoder();

    protected static class ProtocolDecoderQueue extends AbstractProtocolDecoderOutput {
        public Queue<Object> getQueue() {
            return this.messageQueue;
        }
    }

    /**
     * Feeds one network read into the decoder, calling it again while data
     * remains, like ProtocolCodecFilter does.
     */
    private static void decode(ProtocolDecoder decoder, IoSession session, String data, ProtocolDecoderQueue out)
            throws Exception {
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString(data, encoder);
        buffer.flip();

        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
    }

    private static String asString(Object message) throws Exception {
        return ((IoBuffer) message).getString(ASCII.newDecoder());
    }

    @Test
    public void testResponseHeadSplitAcrossTwoReads() throws Exception {
        ProtocolDecoder decoder = new HttpClientDecoder();
        IoSession session = new DummySession();
        ProtocolDecoderQueue out = new ProtocolDecoderQueue();

        decode(decoder, session, "HTTP/1.1 200 OK\r\nContent-Le", out);
        assertTrue("no message expected before the full head is received", out.getQueue().isEmpty());

        decode(decoder, session, "ngth: 2\r\n\r\nAB", out);

        assertEquals(3, out.getQueue().size());
        HttpResponse response = (HttpResponse) out.getQueue().poll();
        assertEquals(HttpStatus.SUCCESS_OK, response.getStatus());
        assertEquals("2", response.getHeader("content-length"));
        assertEquals("AB", asString(out.getQueue().poll()));
        assertTrue(out.getQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testHeaderWithoutSpaceAfterColon() throws Exception {
        ProtocolDecoder decoder = new HttpClientDecoder();
        IoSession session = new DummySession();
        ProtocolDecoderQueue out = new ProtocolDecoderQueue();

        decode(decoder, session, "HTTP/1.1 200 OK\r\nContent-Length:2\r\nX-Test:foo\r\n\r\nAB", out);

        assertEquals(3, out.getQueue().size());
        HttpResponse response = (HttpResponse) out.getQueue().poll();
        assertEquals(HttpStatus.SUCCESS_OK, response.getStatus());
        assertEquals("2", response.getHeader("content-length"));
        assertEquals("foo", response.getHeader("x-test"));
        assertEquals("AB", asString(out.getQueue().poll()));
        assertTrue(out.getQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testChunkedBody() throws Exception {
        ProtocolDecoder decoder = new HttpClientDecoder();
        IoSession session = new DummySession();
        ProtocolDecoderQueue out = new ProtocolDecoderQueue();

        decode(decoder, session,
                "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n", out);

        assertEquals(4, out.getQueue().size());
        HttpResponse response = (HttpResponse) out.getQueue().poll();
        assertEquals(HttpStatus.SUCCESS_OK, response.getStatus());
        assertEquals("abc", asString(out.getQueue().poll()));
        assertEquals("de", asString(out.getQueue().poll()));
        assertTrue("end of content must be signaled after the zero chunk",
                out.getQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testChunkedBodySplitAcrossReads() throws Exception {
        ProtocolDecoder decoder = new HttpClientDecoder();
        IoSession session = new DummySession();
        ProtocolDecoderQueue out = new ProtocolDecoderQueue();

        decode(decoder, session, "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3", out);
        decode(decoder, session, "\r\nab", out);
        decode(decoder, session, "c\r\n0\r\n\r\n", out);

        assertEquals(4, out.getQueue().size());
        HttpResponse response = (HttpResponse) out.getQueue().poll();
        assertEquals(HttpStatus.SUCCESS_OK, response.getStatus());
        assertEquals("ab", asString(out.getQueue().poll()));
        assertEquals("c", asString(out.getQueue().poll()));
        assertTrue(out.getQueue().poll() instanceof HttpEndOfContent);
    }
}
