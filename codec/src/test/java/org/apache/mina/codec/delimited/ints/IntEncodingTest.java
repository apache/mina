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
package org.apache.mina.codec.delimited.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.mina.codec.ProtocolDecoderException;
import org.junit.Before;
import org.junit.Test;

abstract public class IntEncodingTest {

    protected IntTranscoder encoder;

    protected IntTranscoder decoder;

    public abstract IntTranscoder newDecoderInstance();

    public abstract IntTranscoder newEncoderInstance();

    public abstract Map<Integer, ByteBuffer> getEncodingSamples();

    public abstract Iterable<ByteBuffer> getIllegalBuffers();

    @Before
    public void prepareDecoder() {
        decoder = newDecoderInstance();
        encoder = newEncoderInstance();
    }

    @Test
    public void testTruncatedValues() {
        for (int value : new int[] { 0, 1, 127, 128, 65536, 198649, Integer.MAX_VALUE }) {

            ByteBuffer buffer = encoder.encode(value, null);

            for (int i = 0; i < buffer.remaining(); i++) {
                ByteBuffer partialBuffer = buffer.slice();
                partialBuffer.limit(partialBuffer.position() + i);
                try {
                    assertNull(decoder.decode(partialBuffer, null));
                } catch (ProtocolDecoderException e) {
                    fail("Should not throw exception");
                }
            }
        }
    }

    @Test
    public void testSizedValues() {
        for (int value : new int[] { 0, 1, 127, 128, 65536, 198649, Integer.MAX_VALUE }) {
            ByteBuffer buffer = encoder.encode(value, null);

            try {
                assertEquals(value, decoder.decode(buffer, null).intValue());
            } catch (ProtocolDecoderException e) {
                fail("Should not throw exception");
            }
        }
    }

    @Test
    public void testExtendedValues() {
        for (int value : new int[] { 0, 1, 127, 128, 65536, 198649, Integer.MAX_VALUE }) {

            ByteBuffer buffer = encoder.encode(value, null);

            for (int i = 1; i < 5; i++) {
                int size = buffer.remaining() + i;
                ByteBuffer extendedBuffer = ByteBuffer.allocate(size);
                int start = extendedBuffer.position();
                extendedBuffer.put(buffer.slice());
                extendedBuffer.position(start);
                extendedBuffer.limit(start + size);

                try {
                    decoder.decode(extendedBuffer, null);
                    assertEquals(i, extendedBuffer.remaining());
                } catch (ProtocolDecoderException e) {
                    fail("Should not throw exception");
                }
            }
        }
    }

    @Test
    public void testSamples() {
        Map<Integer, ByteBuffer> samples = getEncodingSamples();
        for (Integer val : samples.keySet()) {
            assertEquals(samples.get(val), encoder.encode(val, null));
            try {
                assertEquals(val, decoder.decode(samples.get(val), null));
            } catch (ProtocolDecoderException e) {
                fail("Should not throw exception");
            }
        }
    }

    @Test
    public void testOverflow() {

        for (ByteBuffer buffer : getIllegalBuffers())
            try {
                decoder.decode(buffer, null);
                fail("Should throw an overflow exception");
            } catch (ProtocolDecoderException e) {
                // fine
            }
    }

    @Test
    public void testNegativeValues() {
        ByteBuffer zero = encoder.encode(0, null);
        for (int i : new int[] { -1, -127, Integer.MIN_VALUE })
            assertEquals(zero, encoder.encode(i, null));
    }
}
