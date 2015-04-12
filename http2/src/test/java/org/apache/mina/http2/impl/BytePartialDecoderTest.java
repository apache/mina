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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.mina.http2.impl.BytePartialDecoder;
import org.junit.Test;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BytePartialDecoderTest {

    private static final byte[] SAMPLE_VALUE_1 = new byte[] {0x74, 0x18, 0x4F, 0x68};
    private static final byte[] SAMPLE_VALUE_2 = new byte[] {0x74, 0x18, 0x4F, 0x68, 0x0F};

    @Test
    public void checkSimpleValue() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(SAMPLE_VALUE_1);
        assertTrue(decoder.consume(buffer));
        assertArrayEquals(SAMPLE_VALUE_1, decoder.getValue());
    }
    
    @Test
    public void checkNotenoughData() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {0x00, 0x00});
        assertFalse(decoder.consume(buffer));
    }

    @Test
    public void checkTooMuchData() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(SAMPLE_VALUE_2);
        assertTrue(decoder.consume(buffer));
        assertArrayEquals(SAMPLE_VALUE_1, decoder.getValue());
        assertEquals(1, buffer.remaining());
    }

    @Test
    public void checkDecodingIn2Steps() {
        BytePartialDecoder decoder = new BytePartialDecoder(4);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {SAMPLE_VALUE_2[0], SAMPLE_VALUE_2[1]});
        assertFalse(decoder.consume(buffer));
        buffer = ByteBuffer.wrap(new byte[] {SAMPLE_VALUE_2[2], SAMPLE_VALUE_2[3], SAMPLE_VALUE_2[4]});
        assertTrue(decoder.consume(buffer));
        assertArrayEquals(SAMPLE_VALUE_1, decoder.getValue());
        assertEquals(1, buffer.remaining());
    }
}
