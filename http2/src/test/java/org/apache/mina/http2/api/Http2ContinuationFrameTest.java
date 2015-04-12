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
public class Http2ContinuationFrameTest extends Http2Test {

    @Test
    public void decodeNoHeaderFragment() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.CONTINUATION_NO_HEADER_FRAGMENT_BUFFER);
        Http2ContinuationFrame frame = (Http2ContinuationFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(0, frame.getLength());
        assertEquals(9, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(0, frame.getHeaderBlockFragment().length);
    }
    
    @Test
    public void encodeNoHeaderFragment() {
        Http2ContinuationFrame frame = TestMessages.CONTINUATION_NO_HEADER_FRAGMENT_FRAME;
        assertArrayEquals(TestMessages.CONTINUATION_NO_HEADER_FRAGMENT_BUFFER, toByteArray(frame.toBuffer()));
    }
    
    @Test
    public void decodeHeaderFragment() {
        Http2Connection connection = new Http2Connection();
        ByteBuffer buffer = ByteBuffer.wrap(TestMessages.CONTINUATION_HEADER_FRAGMENT_BUFFER);
        Http2ContinuationFrame frame = (Http2ContinuationFrame) connection.decode(buffer);
        assertNotNull(frame);
        assertEquals(10, frame.getLength());
        assertEquals(9, frame.getType());
        assertEquals(0x00, frame.getFlags());
        assertEquals(50, frame.getStreamID());
        assertEquals(10, frame.getHeaderBlockFragment().length);
        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, frame.getHeaderBlockFragment());
    }

    @Test
    public void encodeHeaderFragment() {
        Http2ContinuationFrame frame = TestMessages.CONTINUATION_HEADER_FRAGMENT_FRAME;
        assertArrayEquals(TestMessages.CONTINUATION_HEADER_FRAGMENT_BUFFER, toByteArray(frame.toBuffer()));
    }
}
