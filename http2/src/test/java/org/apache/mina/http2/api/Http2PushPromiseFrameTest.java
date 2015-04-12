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
public class Http2PushPromiseFrameTest extends Http2Test {

    @Test
    public void decodeNoPadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PUSH_PROMISE_NO_PADDING_BUFFER);
        Http2PushPromiseFrame frame = (Http2PushPromiseFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(5, frame.getLength());
        assertEquals(5, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getPromisedStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
    }
    
    @Test
    public void encodeNoPadding() {
        Http2PushPromiseFrame frame = TestMessages.PUSH_PROMISE_NO_PADDING_FRAME;
        assertArrayEquals(TestMessages.PUSH_PROMISE_NO_PADDING_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    
    @Test
    public void decodePadding() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PUSH_PROMISE_PADDING_BUFFER);
        Http2PushPromiseFrame frame = (Http2PushPromiseFrame) connection.decode(buffer);
        assertEquals(22, frame.getLength());
        assertEquals(5, frame.getType());
        assertEquals(0x08, frame.getFlags());
        assertEquals(3, frame.getStreamID());
        assertEquals(20, frame.getPromisedStreamID());
        assertEquals(1, frame.getHeaderBlockFragment().length);
        assertEquals(0x0082, frame.getHeaderBlockFragment()[0] & 0x00FF);
        assertEquals(16,  frame.getPadding().length);
        assertArrayEquals(new byte[] {0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x70, 0x61, 0x64, 0x64, 0x69, 0x6E, 0x67, 0x2E}, frame.getPadding());
    }
    
    @Test
    public void encodePadding() {
        Http2PushPromiseFrame frame = TestMessages.PUSH_PROMISE_PADDING_FRAME;
        assertArrayEquals(TestMessages.PUSH_PROMISE_PADDING_BUFFER, toByteArray(frame.toBuffer()));
    }
    
}
