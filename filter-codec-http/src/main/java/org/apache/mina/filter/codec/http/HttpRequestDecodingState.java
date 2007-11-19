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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingStateMachine;
import org.apache.mina.filter.codec.statemachine.FixedLengthDecodingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses HTTP requests.
 * Clients should register a <code>HttpRequestParserListener</code>
 * in order to receive notifications at important stages of request
 * building.<br/>
 *
 * <code>HttpRequestParser</code>s should not be built for each request
 * as each parser constructs an underlying state machine which is
 * relatively costly to build.<br/> Instead, parsers should be pooled.<br/>
 *
 * Note, however, that a parser <i>must</i> be <code>prepare</code>d before
 * each new parse.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
abstract class HttpRequestDecodingState extends DecodingStateMachine {

    private static final Logger LOG = LoggerFactory
            .getLogger(HttpRequestDecodingState.class);

    private static final String HEADER_COOKIE = "Cookie";

    /**
     * The header which provides a requests transfer coding
     */
    private static final String TRANSFER_CODING = "transfer-encoding";

    /**
     * The chunked coding
     */
    private static final String CHUNKED = "chunked";

    /**
     * The header which provides a requests content length
     */
    private static final String CONTENT_LENGTH = "content-length";

    /**
     * Indicates the start of a coding extension
     */
    private static final char EXTENSION_CHAR = ';';

    /**
     * The request we are building
     */
    private MutableHttpRequest request;

    private boolean parseCookies = true;

    public boolean isParseCookies() {
        return parseCookies;
    }

    public void setParseCookies(boolean parseCookies) {
        this.parseCookies = parseCookies;
    }

    @Override
    protected DecodingState init() throws Exception {
        request = new DefaultHttpRequest();
        return SKIP_EMPTY_LINES;
    }

    @Override
    protected void destroy() throws Exception {
    }

    private final DecodingState SKIP_EMPTY_LINES = new CrLfDecodingState() {

        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            if (foundCRLF) {
                return this;
            } else {
                return READ_REQUEST_LINE;
            }
        }
    };

    private final DecodingState READ_REQUEST_LINE = new HttpRequestLineDecodingState() {
        @Override
        protected DecodingState finishDecode(List<Object> childProducts,
                ProtocolDecoderOutput out) throws Exception {
            URI requestUri = (URI) childProducts.get(1);
            request.setMethod((HttpMethod) childProducts.get(0));
            request.setRequestUri(requestUri);
            request.setProtocolVersion((HttpVersion) childProducts.get(2));
            request.setParameters(requestUri.getRawQuery());
            return READ_HEADERS;
        }
    };

    private final DecodingState READ_HEADERS = new HttpHeaderDecodingState() {
        @Override
        @SuppressWarnings("unchecked")
        protected DecodingState finishDecode(List<Object> childProducts,
                ProtocolDecoderOutput out) throws Exception {
            Map<String, List<String>> headers = (Map<String, List<String>>) childProducts
                    .get(0);
            if (parseCookies) {
                List<String> cookies = headers.remove(HEADER_COOKIE);
                if (cookies != null && !cookies.isEmpty()) {
                    if (cookies.size() > 1) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Ignoring extra cookie headers: "
                                    + cookies.subList(1, cookies.size()));
                        }
                    }
                    request.setCookies(cookies.get(0));
                }
            }
            request.setHeaders(headers);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Decoded header: " + request.getHeaders());
            }

            // Select appropriate body decoding state.
            boolean isChunked = false;
            if (request.getProtocolVersion() == HttpVersion.HTTP_1_1) {
                LOG.debug("Request is HTTP 1/1. Checking for transfer coding");
                isChunked = isChunked(request);
            } else {
                LOG.debug("Request is not HTTP 1/1. Using content length");
            }
            DecodingState nextState;
            if (isChunked) {
                LOG.debug("Using chunked decoder for request");
                nextState = new ChunkedBodyDecodingState() {
                    @Override
                    protected DecodingState finishDecode(
                            List<Object> childProducts,
                            ProtocolDecoderOutput out) throws Exception {
                        if (childProducts.size() != 1) {
                            int chunkSize = 0;
                            for (Object product : childProducts) {
                                IoBuffer chunk = (IoBuffer) product;
                                chunkSize += chunk.remaining();
                            }

                            IoBuffer body = IoBuffer.allocate(chunkSize);
                            for (Object product : childProducts) {
                                IoBuffer chunk = (IoBuffer) product;
                                body.put(chunk);
                            }
                            body.flip();
                            request.setContent(body);
                        } else {
                            request.setContent((IoBuffer) childProducts.get(0));
                        }

                        out.write(request);
                        return null;
                    }
                };
            } else {
                int length = getContentLength(request);
                if (length > 0) {
                    if (LOG.isDebugEnabled()) {
                        LOG
                                .debug("Using fixed length decoder for request with length "
                                        + length);
                    }
                    nextState = new FixedLengthDecodingState(length) {
                        @Override
                        protected DecodingState finishDecode(IoBuffer readData,
                                ProtocolDecoderOutput out) throws Exception {
                            request.setContent(readData);
                            out.write(request);
                            return null;
                        }
                    };
                } else {
                    LOG.debug("No entity body for this request");
                    out.write(request);
                    nextState = null;
                }
            }
            return nextState;
        }

        /**
         * Obtains the content length from the specified request
         *
         * @param request  The request
         * @return         The content length, or 0 if not specified
         * @throws HttpDecoderException If an invalid content length is specified
         */
        private int getContentLength(HttpRequest request)
                throws ProtocolDecoderException {
            int length = 0;
            String lengthValue = request.getHeader(CONTENT_LENGTH);
            if (lengthValue != null) {
                try {
                    length = Integer.parseInt(lengthValue);
                } catch (NumberFormatException e) {
                    HttpCodecUtils.throwDecoderException(
                            "Invalid content length: " + length,
                            HttpResponseStatus.BAD_REQUEST);
                }
            }
            return length;
        }

        /**
         * Determines whether a specified request employs a chunked
         * transfer coding
         *
         * @param request  The request
         * @return         <code>true</code> iff the request employs a
         *                 chunked transfer coding
         * @throws HttpDecoderException
         *                 If the request employs an unsupported coding
         */
        private boolean isChunked(HttpRequest request)
                throws ProtocolDecoderException {
            boolean isChunked = false;
            String coding = request.getHeader(TRANSFER_CODING);
            if (coding != null) {
                int extensionIndex = coding.indexOf(EXTENSION_CHAR);
                if (extensionIndex != -1) {
                    coding = coding.substring(0, extensionIndex);
                }
                if (CHUNKED.equalsIgnoreCase(coding)) {
                    isChunked = true;
                } else {
                    // As we only support chunked encoding, any other encoding
                    // is unsupported
                    HttpCodecUtils.throwDecoderException(
                            "Unknown transfer coding " + coding,
                            HttpResponseStatus.NOT_IMPLEMENTED);
                }
            }
            return isChunked;
        }
    };
}
