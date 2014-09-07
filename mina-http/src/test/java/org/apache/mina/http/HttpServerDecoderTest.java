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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpRequest;
import org.junit.Test;

public class HttpServerDecoderTest {
    private static final CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder(); //$NON-NLS-1$

    private static final ProtocolDecoder decoder = new HttpServerDecoder();

    /*
     * Use a single session for all requests in order to test state management better
     */
    private static IoSession session = new DummySession();

    /**
     * Build an IO buffer containing a simple minimal HTTP request.
     * 
     * @param method the HTTP method
     * @param body the option body
     * @return the built IO buffer
     * @throws CharacterCodingException if encoding fails
     */
    protected static IoBuffer getRequestBuffer(String method, String body) throws CharacterCodingException {
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString(method + " / HTTP/1.1\r\nHost: dummy\r\n", encoder);
        
        if (body != null) {
            buffer.putString("Content-Length: " + body.length() + "\r\n\r\n", encoder);
            buffer.putString(body, encoder);
        } else {
            buffer.putString("\r\n", encoder);
        }
        
        buffer.rewind();
        
        return buffer;
    }

    protected static IoBuffer getRequestBuffer(String method) throws CharacterCodingException {
        return getRequestBuffer(method, null);
    }

    /**
     * Execute an HTPP request and return the queue of messages.
     * 
     * @param method the HTTP method
     * @param body the optional body
     * @return the protocol output and its queue of messages
     * @throws Exception if error occurs (encoding,...)
     */
    protected static AbstractProtocolDecoderOutput executeRequest(String method, String body) throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };

        IoBuffer buffer = getRequestBuffer(method, body); //$NON-NLS-1$
        
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        
        return out;
    }

    @Test
    public void testGetRequestWithoutBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("GET", null);
        assertEquals(2, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testGetRequestBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("GET", "body");
        assertEquals(3, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testPutRequestWithoutBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("PUT", null);
        assertEquals(2, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testPutRequestBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("PUT", "body");
        assertEquals(3, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testPostRequestWithoutBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("POST", null);
        assertEquals(2, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testPostRequestBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("POST", "body");
        assertEquals(3, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testDeleteRequestWithoutBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("DELETE", null);
        assertEquals(2, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testDeleteRequestBody() throws Exception {
        AbstractProtocolDecoderOutput out = executeRequest("DELETE", "body");
        assertEquals(3, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }
    
    @Test
    public void testDIRMINA965NoContent() throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("GET / HTTP/1.1\r\nHost: ", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("dummy\r\n\r\n", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        assertEquals(2, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void testDIRMINA965WithContent() throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("GET / HTTP/1.1\r\nHost: ", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("dummy\r\nContent-Length: 1\r\n\r\nA", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        assertEquals(3, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }
    @Test
    public void testDIRMINA965WithContentOnTwoChunks() throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("GET / HTTP/1.1\r\nHost: ", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("dummy\r\nContent-Length: 2\r\n\r\nA", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("B", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        assertEquals(4, out.getMessageQueue().size());
        assertTrue(out.getMessageQueue().poll() instanceof HttpRequest);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof IoBuffer);
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }
    
    @Test
    public void verifyThatHeaderWithoutLeadingSpaceIsSupported() throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("GET / HTTP/1.0\r\nHost:localhost\r\n\r\n", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        assertEquals(2, out.getMessageQueue().size());
        HttpRequest request = (HttpRequest) out.getMessageQueue().poll();
        assertEquals("localhost", request.getHeader("host"));
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void verifyThatLeadingSpacesAreRemovedFromHeader() throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("GET / HTTP/1.0\r\nHost:  localhost\r\n\r\n", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        assertEquals(2, out.getMessageQueue().size());
        HttpRequest request = (HttpRequest) out.getMessageQueue().poll();
        assertEquals("localhost", request.getHeader("host"));
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }

    @Test
    public void verifyThatTrailingSpacesAreRemovedFromHeader() throws Exception {
        AbstractProtocolDecoderOutput out = new AbstractProtocolDecoderOutput() {
            public void flush(NextFilter nextFilter, IoSession session) {
            }
        };
        IoBuffer buffer = IoBuffer.allocate(0).setAutoExpand(true);
        buffer.putString("GET / HTTP/1.0\r\nHost:localhost  \r\n\r\n", encoder);
        buffer.rewind();
        while (buffer.hasRemaining()) {
            decoder.decode(session, buffer, out);
        }
        assertEquals(2, out.getMessageQueue().size());
        HttpRequest request = (HttpRequest) out.getMessageQueue().poll();
        assertEquals("localhost", request.getHeader("host"));
        assertTrue(out.getMessageQueue().poll() instanceof HttpEndOfContent);
    }
}
