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
package org.apache.mina.http2.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;

import org.apache.mina.http2.Http2Test;
import org.apache.mina.http2.TestMessages;
import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2HeadersFrameTest extends Http2Test {

    @Test
    public void decodeNoPaddingNoPriority() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.HEADERS_NO_PADDING_NO_PRIORITY_BUFFER);
        Http2HeadersFrame frame = (Http2HeadersFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(1, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
    }
    
    @Test
    public void encodeNoPaddingNoPriority() {
        Http2HeadersFrame frame = TestMessages.HEADERS_NO_PADDING_NO_PRIORITY_FRAME;
       assertArrayEquals(TestMessages.HEADERS_NO_PADDING_NO_PRIORITY_BUFFER, toByteArray(frame.toBuffer())); 
    }
    
    
    @Test
    public void decodePaddingPriority() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.HEADERS_PADDING_PRIORITY_BUFFER);
        Http2HeadersFrame frame = (Http2HeadersFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(23, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0x28, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(10,  frame.getWeight());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void encodePaddingPriority() {
        Http2HeadersFrame frame = TestMessages.HEADERS_PADDING_PRIORITY_FRAME;
                assertArrayEquals(TestMessages.HEADERS_PADDING_PRIORITY_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    @Test
    public void decodePaddingNoPriority() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.HEADERS_PADDING_NO_PRIORITY_BUFFER);
        Http2HeadersFrame frame = (Http2HeadersFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(18, frame.getLength());
        assertEquals(1, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void encodePaddingNoPriority() {
        Http2HeadersFrame frame = TestMessages.HEADERS_PADDING_NO_PRIORITY_FRAME;
                assertArrayEquals(TestMessages.HEADERS_PADDING_NO_PRIORITY_BUFFER, toByteArray(frame.toBuffer()));
    }

}
