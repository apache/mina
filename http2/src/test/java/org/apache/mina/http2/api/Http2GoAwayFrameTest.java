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
public class Http2GoAwayFrameTest extends Http2Test {


    @Test
    public void decodeNoAdditionalData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getLastStreamID());
        assertEquals(0x010203, frame.getErrorCode());
    }
    
    @Test
    public void encodeWNoAdditionalData() {
        Http2GoAwayFrame frame = TestMessages.GOAWAY_NO_DATA_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_NO_DATA_BUFFER, toByteArray(frame.toBuffer()));
    }

    @Test
    public void decodeHighestLastStreamIDNoAdditionalData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(0x7FFFFFFF, frame.getLastStreamID());
        assertEquals(0x010203, frame.getErrorCode());
    }
    
    @Test
    public void encodeHighestLastStreamIDWNoAdditionalData() {
        Http2GoAwayFrame frame = TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    @Test
    public void decodeHighestLastStreamIDReservedBitSetNoAdditionalData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_HIGHEST_STREAMID_RESERVED_BIT_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(0x7FFFFFFF, frame.getLastStreamID());
        assertEquals(0x010203, frame.getErrorCode());
    }
    
    @Test
    public void decodeHighestErrorCodeNoAdditionalData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(8, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(0x7FFFFFFF, frame.getLastStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
    
    @Test
    public void encodeHighestErrorCodeNoAdditionalData() {
        Http2GoAwayFrame frame = TestMessages.GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_NO_DATA_HIGHEST_ERROR_CODE_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    @Test
    public void decodeAdditionalData() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.GOAWAY_DATA_BUFFER);
        Http2GoAwayFrame frame = (Http2GoAwayFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(9, frame.getLength());
        assertEquals(7, frame.getType());
        assertEquals(0, frame.getFlags());
        assertEquals(1, frame.getStreamID());
        assertEquals(256, frame.getLastStreamID());
        assertEquals(0x00010203, frame.getErrorCode());
        assertArrayEquals(new byte[] {0x01}, frame.getData());
    }
    
    @Test
    public void encodeAdditionalData() {
        Http2GoAwayFrame frame = TestMessages.GOAWAY_DATA_FRAME;
        assertArrayEquals(TestMessages.GOAWAY_DATA_BUFFER, toByteArray(frame.toBuffer()));
    }
}
