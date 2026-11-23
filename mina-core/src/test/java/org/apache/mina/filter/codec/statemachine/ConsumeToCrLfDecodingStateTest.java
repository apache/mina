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
package org.apache.mina.filter.codec.statemachine;

import java.nio.charset.StandardCharsets;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link ConsumeToCrLfDecodingState}, in particular that the decoded
 * product does not depend on how the input was fragmented.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ConsumeToCrLfDecodingStateTest {

    private static final ProtocolDecoderOutput NULL_OUT = new ProtocolDecoderOutput() {
        @Override
        public void write(Object message) {
            // Do nothing
        }

        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            // Do nothing
        }
    };

    /**
     * Feeds the given chunks into a fresh decoding state and returns the
     * product produced when the CRLF terminator is reached.
     */
    private static String decode(String... chunks) throws Exception {
        final StringBuilder result = new StringBuilder();

        ConsumeToCrLfDecodingState state = new ConsumeToCrLfDecodingState() {
            @Override
            protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) {
                byte[] bytes = new byte[product.remaining()];
                product.get(bytes);
                result.append(new String(bytes, StandardCharsets.US_ASCII));
                return this;
            }
        };

        for (String chunk : chunks) {
            state.decode(IoBuffer.wrap(chunk.getBytes(StandardCharsets.US_ASCII)), NULL_OUT);
        }

        return result.toString();
    }

    @Test
    public void testUnfragmented() throws Exception {
        assertEquals("abc", decode("abc\r\n"));
        assertEquals("abc\rdef", decode("abc\rdef\r\n"));
        assertEquals("abc\r", decode("abc\r\r\n"));
        assertEquals("", decode("\r\n"));
    }

    @Test
    public void testTerminatorSplitAcrossChunks() throws Exception {
        assertEquals("abc", decode("abc\r", "\n"));
        assertEquals("abc", decode("abc", "\r\n"));
        assertEquals("abc", decode("abc", "\r", "\n"));
        assertEquals("", decode("\r", "\n"));
    }

    /**
     * A bare CR followed by a non-LF byte must produce the same product
     * whether or not a chunk boundary falls right after the CR.
     */
    @Test
    public void testBareCrAtChunkBoundaryIsPreserved() throws Exception {
        assertEquals("abc\rdef", decode("abc\r", "def\r\n"));
        assertEquals("abc\r", decode("abc\r", "\r\n"));
        assertEquals("abc\rdef", decode("ab", "c\r", "de", "f\r\n"));
    }

    /**
     * An empty chunk arriving after a chunk ending in CR must not eat any
     * accumulated data.
     */
    @Test
    public void testEmptyChunkAfterCrDoesNotDropData() throws Exception {
        assertEquals("abc\rdef", decode("abc\r", "", "def\r\n"));
        assertEquals("abc", decode("abc\r", "", "\n"));
    }

    /**
     * Every possible split of the same input must yield the same product.
     */
    @Test
    public void testFragmentationInvariance() throws Exception {
        String input = "abc\rdef\r\n";

        for (int split = 1; split < input.length(); split++) {
            assertEquals("abc\rdef",
                    decode(input.substring(0, split), input.substring(split)),
                    "split at " + split);
        }
    }
}
