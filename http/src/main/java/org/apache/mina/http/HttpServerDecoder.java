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
package org.apache.mina.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.http.api.HttpContentChunk;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpPdu;
import org.apache.mina.http.api.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In charge of decoding received bytes into HTTP message.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HttpServerDecoder implements ProtocolDecoder<ByteBuffer, HttpPdu, HttpDecoderState> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerDecoder.class);

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");

    /** Regex to parse out QueryString from HttpRequest */
    public static final Pattern QUERY_STRING_PATTERN = Pattern.compile("\\?");

    /** Regex to parse out parameters from query string */
    public static final Pattern PARAM_STRING_PATTERN = Pattern.compile("\\&|;");

    /** Regex to parse out key/value pairs */
    public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("=");

    /** Regex to parse raw headers and body */
    public static final Pattern RAW_VALUE_PATTERN = Pattern.compile("\\r\\n\\r\\n");

    /** Regex to parse raw headers from body */
    public static final Pattern HEADERS_BODY_PATTERN = Pattern.compile("\\r\\n");

    /** Regex to parse header name and value */
    public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(":");

    /** Regex to split cookie header following RFC6265 Section 5.4 */
    public static final Pattern COOKIE_SEPARATOR_PATTERN = Pattern.compile(";");

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpDecoderState createDecoderState() {
        return new HttpDecoderState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpPdu decode(ByteBuffer msg, HttpDecoderState context) {
        LOG.debug("decode : {}", msg);
        if (msg.remaining() <= 0) {
            return null;
        }
        switch (context.getState()) {
        case HEAD:
            LOG.debug("decoding HEAD");
            // concat the old buffer and the new incoming one
            msg = ByteBuffer.allocate(context.getPartial().remaining() + msg.remaining()).put(context.getPartial())
                    .put(msg);
            msg.flip();
            // now let's decode like it was a new message

        case NEW:
            LOG.debug("decoding NEW");
            HttpRequestImpl rq = parseHttpRequestHead(msg);

            if (rq == null) {
                // we copy the incoming BB because it's going to be recycled by the inner IoProcessor for next reads
                context.setPartial(ByteBuffer.allocate(msg.remaining()));
                context.getPartial().put(msg);
                context.getPartial().flip();
            } else {
                return rq;
            }
            return null;
        case BODY:
            LOG.debug("decoding BODY");
            int chunkSize = msg.remaining();
            // send the chunk of body
            HttpContentChunk chunk = new HttpContentChunk(msg);
            // do we have reach end of body ?
            context.setRemainingBytes(context.getRemainingBytes() - chunkSize);

            if (context.getRemainingBytes() <= 0) {
                LOG.debug("end of HTTP body");
                context.setState(DecoderState.NEW);
                context.setRemainingBytes(0);
                context.setState(DecoderState.DONE);
                return chunk;

            }
            break;
        case DONE:
            return new HttpEndOfContent();
        default:
            throw new IllegalStateException("Unknonwn decoder state : " + context.getState());
        }

        return null;
    }

    private HttpRequestImpl parseHttpRequestHead(ByteBuffer buffer) {
        String raw = new String(buffer.array(), 0, buffer.limit(), Charset.forName("ISO-8859-1"));
        String[] headersAndBody = RAW_VALUE_PATTERN.split(raw, -1);

        if (headersAndBody.length <= 1) {
            // we didn't receive the full HTTP head
            return null;
        }

        String[] headerFields = HEADERS_BODY_PATTERN.split(headersAndBody[0]);
        headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

        String requestLine = headerFields[0];
        Map<String, String> generalHeaders = new HashMap<String, String>();

        for (int i = 1; i < headerFields.length; i++) {
            String[] header = HEADER_VALUE_PATTERN.split(headerFields[i]);
            generalHeaders.put(header[0].toLowerCase(), header[1].trim());
        }

        String[] elements = REQUEST_LINE_PATTERN.split(requestLine);
        HttpMethod method = HttpMethod.valueOf(elements[0]);
        HttpVersion version = HttpVersion.fromString(elements[2]);
        String[] pathFrags = QUERY_STRING_PATTERN.split(elements[1]);
        String requestedPath = pathFrags[0];

        // we put the buffer position where we found the beginning of the HTTP body
        buffer.position(headersAndBody[0].length() + 4);

        return new HttpRequestImpl(version, method, requestedPath, generalHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishDecode(HttpDecoderState context) {

    }
}
