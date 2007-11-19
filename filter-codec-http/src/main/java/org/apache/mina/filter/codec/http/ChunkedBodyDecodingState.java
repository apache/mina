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
package org.apache.mina.filter.codec.http;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.ConsumeToDynamicTerminatorDecodingState;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingStateMachine;
import org.apache.mina.filter.codec.statemachine.FixedLengthDecodingState;
import org.apache.mina.filter.codec.statemachine.SkippingState;

/**
 * A decoder which decodes the body of HTTP Requests having
 * a "chunked" transfer-coding.
 *
 * This decoder does <i>not</i> decode trailing entity-headers - it simply
 * discards them. Tomcat currently does the same - so this is probably
 * the most stable approach for now.<br/>
 * If the need arises to decode them in the future, we simply need to employ a
 * <code>HttpHeaderDecoder</code> following the last chunk - yielding
 * headers for the encountered trailing entity-headers.<p/>
 *
 * This decoder decodes the following format:
 *
 * <pre>
 *      Chunked-Body   = *chunk
 *                       last-chunk
 *                       trailer
 *                       CRLF
 *      chunk          = chunk-size [ chunk-extension ] CRLF
 *                       chunk-data CRLF
 *      chunk-size     = 1*HEX
 *      last-chunk     = 1*("0") [ chunk-extension ] CRLF
 *      chunk-extension= *( ";" chunk-ext-name [ "=" chunk-ext-val ] )
 *      chunk-ext-name = token
 *      chunk-ext-val  = token | quoted-string
 *      chunk-data     = chunk-size(OCTET)
 *      trailer        = *(entity-header CRLF)
 * </pre>
 *
 * <code>ChunkedBodyDecoder</code> employs a <code>SharedBytesAllocator</code>
 * to enable the content of each decoded chunk to contribute to a single
 * <code>Bytes</code>. This enables all chunks to be read without requiring
 * copying.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
abstract class ChunkedBodyDecodingState extends DecodingStateMachine {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    private final CharsetDecoder asciiDecoder = US_ASCII.newDecoder();

    private int lastChunkLength;

    private boolean chunkHasExtension;

    ChunkedBodyDecodingState() {
    }

    @Override
    protected void destroy() throws Exception {
    }

    @Override
    protected DecodingState init() throws Exception {
        chunkHasExtension = false;
        return READ_CHUNK_LENGTH;
    }

    private final DecodingState READ_CHUNK_LENGTH = new ConsumeToDynamicTerminatorDecodingState() {
        @Override
        protected DecodingState finishDecode(IoBuffer product,
                ProtocolDecoderOutput out) throws Exception {
            if (!product.hasRemaining()) {
                HttpCodecUtils
                        .throwDecoderException("Expected a chunk length.");
            }

            String length = product.getString(asciiDecoder);
            lastChunkLength = Integer.parseInt(length, 16);
            if (chunkHasExtension) {
                return SKIP_CHUNK_EXTENSION;
            }
            return AFTER_SKIP_CHUNK_EXTENSION.decode(IoBuffer
                    .wrap(new byte[] { HttpCodecUtils.CR }), out);
        }

        @Override
        protected boolean isTerminator(byte b) {
            if (!(b >= '0' && b <= '9' || b >= 'a' && b <= 'f' || b >= 'A'
                    && b <= 'F')) {
                if (b == HttpCodecUtils.CR || b == HttpCodecUtils.SEMI_COLON) {
                    chunkHasExtension = b == HttpCodecUtils.SEMI_COLON;
                    return true;
                }
                throw new IllegalArgumentException();
            }
            return false;
        }
    };

    private final DecodingState SKIP_CHUNK_EXTENSION = new SkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            return AFTER_SKIP_CHUNK_EXTENSION;
        }

        @Override
        protected boolean canSkip(byte b) {
            return b != HttpCodecUtils.CR;
        }
    };

    private final DecodingState AFTER_SKIP_CHUNK_EXTENSION = new CrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            if (!foundCRLF) {
                HttpCodecUtils.throwDecoderException(
                        "Expected CRLF at the end of chunk extension.",
                        HttpResponseStatus.BAD_REQUEST);
            }

            if (lastChunkLength <= 0) {
                return FIND_END_OF_TRAILER;
            } else {
                return new FixedLengthDecodingState(lastChunkLength) {
                    @Override
                    protected DecodingState finishDecode(IoBuffer readData,
                            ProtocolDecoderOutput out) throws Exception {
                        out.write(readData);
                        // Reset the state.
                        lastChunkLength = 0;
                        return AFTER_CHUNK_DATA;
                    }
                };
            }
        }
    };

    private final DecodingState AFTER_CHUNK_DATA = new CrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            if (!foundCRLF) {
                HttpCodecUtils.throwDecoderException(
                        "Expected CRLF after a chunk data.",
                        HttpResponseStatus.BAD_REQUEST);

            }
            chunkHasExtension = false;
            return READ_CHUNK_LENGTH;
        }
    };

    private final DecodingState FIND_END_OF_TRAILER = new CrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            if (foundCRLF) {
                return null; // Finish
            } else {
                return SKIP_ENTITY_HEADER;
            }
        }
    };

    private final DecodingState SKIP_ENTITY_HEADER = new SkippingState() {

        @Override
        protected boolean canSkip(byte b) {
            return b != HttpCodecUtils.CR;
        }

        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            return AFTER_SKIP_ENTITY_HEADER;
        }
    };

    private final DecodingState AFTER_SKIP_ENTITY_HEADER = new CrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            return FIND_END_OF_TRAILER;
        }
    };
}
