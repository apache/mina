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
import org.apache.mina.http2.api.Http2RstStreamFrame.Http2RstStreamFrameBuilder;
import org.apache.mina.http2.impl.Http2Connection;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2RstStreamFrameTest extends Http2Test {

    @Test
    public void decodeNoExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_NO_EXTRA_PAYLOAD_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(4, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getErrorCode());
    }
    
    @Test
    public void encodeNoExtraPayload() {
        Http2RstStreamFrame frame = TestMessages.RST_STREAM_NO_EXTRA_PAYLOAD_FRAME;
        assertArrayEquals(TestMessages.RST_STREAM_NO_EXTRA_PAYLOAD_BUFFER, toByteArray(frame.toBuffer()));
    }

    @Test
    public void decodeHighestValueNoExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(4, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
    
    @Test
    public void encodeHighestValueNoExtraPayload() {
        Http2RstStreamFrame frame = TestMessages.RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_FRAME;
        assertArrayEquals(TestMessages.RST_STREAM_HIGHEST_VALUE_NO_EXTRA_PAYLOAD_BUFFER, toByteArray(frame.toBuffer()));
    }

    @Test
    public void decodeExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_EXTRA_PAYLOAD_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(256, frame.getErrorCode());
    }

    @Test
    public void decodeHighestValueExtraPayload() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.RST_STREAM_EXTRA_PAYLOAD_HIGHEST_BUFFER);
        Http2RstStreamFrame frame = (Http2RstStreamFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(6, frame.getLength());
        assertEquals(3, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(32, frame.getStreamID());
        assertEquals(0x00FFFFFFFFL, frame.getErrorCode());
    }
}
