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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.ConsumeToDynamicTerminatorDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;

/**
 * Decodes HTTP version.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
abstract class HttpVersionDecodingState implements DecodingState {

    private final CharsetDecoder asciiDecoder = HttpCodecUtils.US_ASCII_CHARSET.newDecoder();
    
    private final DecodingState READ_PROTOCOL_VERSION = new ConsumeToDynamicTerminatorDecodingState() {
        @Override
        protected DecodingState finishDecode(IoBuffer product,
                ProtocolDecoderOutput out) throws Exception {
            String versionStr = null;
            HttpVersion version = null;
            try {
                versionStr = product.getString(asciiDecoder);
                version = HttpVersion.valueOf(versionStr);
            } catch (CharacterCodingException e) {
                // Will take care down the
            }
            
            if (version == null) {
                if (versionStr != null) {
                    versionStr = product.getHexDump();
                }

                HttpCodecUtils.throwDecoderException(
                        "Unsupported HTTP version: " + versionStr,
                        HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED);
            }
            
            return HttpVersionDecodingState.this.finishDecode(version, out);
        }

        @Override
        protected boolean isTerminator(byte b) {
            return Character.isWhitespace(b);
        }
    };
    
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        DecodingState nextState = READ_PROTOCOL_VERSION.decode(in, out);
        if (nextState == READ_PROTOCOL_VERSION) {
            return this;
        } else {
            return nextState;
        }
    }

    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        throw new ProtocolDecoderException(
                "Unexpected end of session while waiting for a HTTP version field.");
    }

    protected abstract DecodingState finishDecode(
            HttpVersion version, ProtocolDecoderOutput out) throws Exception;
}
