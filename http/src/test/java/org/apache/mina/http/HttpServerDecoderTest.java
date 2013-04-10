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

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.http.api.HttpPdu;
import org.junit.Test;

/**
 * Test class for HttpServerDecoderTest
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HttpServerDecoderTest {

    @Test
    public void verifyThatHeaderWithoutLeadingSpaceIsSupported() throws UnsupportedEncodingException,
            ProtocolDecoderException {
        String reqStr = "GET / HTTP/1.0\r\nHost:localhost\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.allocate(reqStr.length());
        buffer.put(reqStr.getBytes("US-ASCII"));
        buffer.rewind();
        HttpServerDecoder decoder = new HttpServerDecoder();
        HttpDecoderState state = decoder.createDecoderState();
        HttpPdu pdus = decoder.decode(buffer, state);
        assertNotNull(pdus);

        assertEquals("localhost", ((HttpRequestImpl) pdus).getHeader("host"));
    }

    @Test
    public void verifyThatLeadingSpacesAreRemovedFromHeader() throws UnsupportedEncodingException,
            ProtocolDecoderException {
        String reqStr = "GET / HTTP/1.0\r\nHost:  localhost\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.allocate(reqStr.length());
        buffer.put(reqStr.getBytes("US-ASCII"));
        buffer.rewind();
        HttpServerDecoder decoder = new HttpServerDecoder();
        HttpDecoderState state = decoder.createDecoderState();
        HttpPdu pdus = decoder.decode(buffer, state);
        assertNotNull(pdus);
        assertEquals("localhost", ((HttpRequestImpl) pdus).getHeader("host"));
    }

    @Test
    public void verifyThatTrailingSpacesAreRemovedFromHeader() throws UnsupportedEncodingException,
            ProtocolDecoderException {
        String reqStr = "GET / HTTP/1.0\r\nHost:localhost  \r\n\r\n";
        ByteBuffer buffer = ByteBuffer.allocate(reqStr.length());
        buffer.put(reqStr.getBytes("US-ASCII"));
        buffer.rewind();
        HttpServerDecoder decoder = new HttpServerDecoder();
        HttpDecoderState state = decoder.createDecoderState();
        HttpPdu pdus = decoder.decode(buffer, state);
        assertNotNull(pdus);
        assertEquals("localhost", ((HttpRequestImpl) pdus).getHeader("host"));
    }
}
