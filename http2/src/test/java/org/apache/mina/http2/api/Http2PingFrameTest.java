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
public class Http2PingFrameTest extends Http2Test {

    @Test
    public void decode() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PING_STANDARD_BUFFER);
        Http2PingFrame frame = (Http2PingFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(6, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}, frame.getData());
    }
    
    @Test
    public void encode() {
        Http2PingFrame frame = TestMessages.PING_STANDARD_FRAME;
        assertArrayEquals(TestMessages.PING_STANDARD_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    @Test
    public void decodeExtraData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PING_EXTRA_DATA_BUFFER);
        Http2PingFrame frame = (Http2PingFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(9, frame.getLength());
        assertEquals(6, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, frame.getData());
    }
    
    @Test
    public void encodeExtraData() {
        Http2PingFrame frame = TestMessages.PING_EXTRA_DATA_FRAME;
        assertArrayEquals(TestMessages.PING_EXTRA_DATA_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    @Test
    public void decodeNotEnoughData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.PING_NO_ENOUGH_DATA_BUFFER);
        Http2PingFrame frame = (Http2PingFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(1, frame.getLength());
        assertEquals(6, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertArrayEquals(new byte[] {0x00}, frame.getData());
    }
    
    @Test
    public void encodeNotEnoughData() {
        Http2PingFrame frame = TestMessages.PING_NO_ENOUGH_DATA_FRAME;
        assertArrayEquals(TestMessages.PING_NO_ENOUGH_DATA_BUFFER, toByteArray(frame.toBuffer()));
    }
}
