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

import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.ConsumeToCrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.ConsumeToTerminatorDecodingState;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingStateMachine;
import org.apache.mina.filter.codec.statemachine.LinearWhitespaceSkippingState;

/**
 * Decodes the Headers of HTTP requests.
 * <code>HttpHeaderDecoder</code> employs several sub-decoders - each taking
 * the responsibility of decoding a specific part of the header.<br/>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
abstract class HttpHeaderDecodingState extends DecodingStateMachine {

    private final CharsetDecoder asciiDecoder =
        HttpCodecUtils.US_ASCII_CHARSET.newDecoder();
    private final CharsetDecoder defaultDecoder =
        HttpCodecUtils.DEFAULT_CHARSET.newDecoder();

    private Map<String, List<String>> headers;
    private String lastHeaderName;
    private StringBuilder lastHeaderValue;

    HttpHeaderDecodingState() {
        //cookieParser = new CookieParser();
    }

    @Override
    protected DecodingState init() throws Exception {
        headers = new LinkedHashMap<String, List<String>>();
        return FIND_EMPTY_LINE;
    }

    @Override
    protected void destroy() throws Exception {
        headers = null;
        lastHeaderName = null;
        lastHeaderValue = null;
    }

    private final DecodingState FIND_EMPTY_LINE = new CrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            if (foundCRLF) {
                out.write(headers);
                return null;
            } else {
                return READ_HEADER_NAME;
            }
        }
    };

    private final DecodingState READ_HEADER_NAME =
        new ConsumeToTerminatorDecodingState((byte) ':') {
        @Override
        protected DecodingState finishDecode(IoBuffer product,
                ProtocolDecoderOutput out) throws Exception {
            lastHeaderName = product.getString(asciiDecoder);
            return AFTER_READ_HEADER_NAME;
        }
    };

    private final DecodingState AFTER_READ_HEADER_NAME = new LinearWhitespaceSkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            lastHeaderValue = new StringBuilder();
            return READ_HEADER_VALUE;
        }
    };

    private final DecodingState READ_HEADER_VALUE = new ConsumeToCrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(IoBuffer product,
                ProtocolDecoderOutput out) throws Exception {
            String value = product.getString(defaultDecoder);
            if (lastHeaderValue.length() == 0) {
                lastHeaderValue.append(value);
            } else {
                lastHeaderValue.append(' ');
                lastHeaderValue.append(value);
            }
            return AFTER_READ_HEADER_VALUE;
        }
    };

    private final DecodingState AFTER_READ_HEADER_VALUE = new LinearWhitespaceSkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            if (skippedBytes == 0) {
                List<String> values = headers.get(lastHeaderName);
                if (values == null) {
                    values = new ArrayList<String>();
                    headers.put(lastHeaderName, values);
                }
                values.add(lastHeaderValue.toString());
                return FIND_EMPTY_LINE;
            } else {
                return READ_HEADER_VALUE;
            }
        }
    };
}
