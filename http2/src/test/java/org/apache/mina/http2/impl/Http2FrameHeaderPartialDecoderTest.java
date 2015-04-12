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
package org.apache.mina.http2.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder;
import org.apache.mina.http2.impl.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2FrameHeaderPartialDecoderTest {

    @Test
    public void checkStandardValue() {
        Http2FrameHeadePartialDecoder decoder = new Http2FrameHeadePartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        0x00, 0x00, 0x00, 0x01 /*streamID*/});
        assertTrue(decoder.consume(buffer));
        Http2FrameHeader header = decoder.getValue();
        assertNotNull(header);
        assertEquals(0, header.getLength());
        assertEquals(0, header.getType());
        assertEquals(0, header.getFlags());
        assertEquals(1, header.getStreamID());
    }

    @Test
    public void checkReservedBitIsNotTransmitted() {
        Http2FrameHeadePartialDecoder decoder = new Http2FrameHeadePartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x00, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        (byte)0x80, 0x00, 0x00, 0x01 /*streamID*/});
        assertTrue(decoder.consume(buffer));
        Http2FrameHeader header = decoder.getValue();
        assertNotNull(header);
        assertEquals(0, header.getLength());
        assertEquals(0, header.getType());
        assertEquals(0, header.getFlags());
        assertEquals(1, header.getStreamID());
    }
    
    @Test
    public void checkPayLoadIsTransmitted() {
        Http2FrameHeadePartialDecoder decoder = new Http2FrameHeadePartialDecoder();
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00, 0x01, /*length*/
                                                        0x00, /*type*/
                                                        0x00, /*flags*/
                                                        (byte)0x80, 0x00, 0x00, 0x01, /*streamID*/
                                                        0x40});
        assertTrue(decoder.consume(buffer));
        Http2FrameHeader header = decoder.getValue();
        assertNotNull(header);
        assertEquals(1, header.getLength());
        assertEquals(0, header.getType());
        assertEquals(0, header.getFlags());
        assertEquals(1, header.getStreamID());
    }

}
