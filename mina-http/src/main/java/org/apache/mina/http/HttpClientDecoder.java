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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An HTTP decoder
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HttpClientDecoder implements ProtocolDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientCodec.class);

    /**
     * The default maximum size in bytes of an accumulated HTTP response head
     * (status line plus headers). A hostile or broken server that keeps
     * sending header bytes without ever terminating the head (CRLFCRLF)
     * would otherwise make the decoder accumulate an ever-growing buffer,
     * exhausting the heap. It can be changed globally with the
     * <tt>org.apache.mina.http.maxResponseHeadSize</tt> system property, or
     * per decoder with the {@link #HttpClientDecoder(int)} constructor.
     * A value of <tt>0</tt> or less disables the limit (previous, unbounded
     * behavior).
     */
    public static final int DEFAULT_MAX_RESPONSE_HEAD_SIZE = Integer.getInteger(
            "org.apache.mina.http.maxResponseHeadSize", 16 * 1024);

    /** The maximum size in bytes of an accumulated HTTP response head */
    private final int maxResponseHeadSize;

    /** Key for decoder current state */
    private static final String DECODER_STATE_ATT = "http.ds";

    /** Key for the partial HTTP requests head */
    private static final String PARTIAL_HEAD_ATT = "http.ph";

    /** Key for the number of bytes remaining to read for completing the body */
    private static final String BODY_REMAINING_BYTES = "http.brb";
    
    /** Key for indicating chunked data */
    private static final String BODY_CHUNKED = "http.ckd";
    
    /** Key for the number of bytes remaining in the current chunk */
    private static final String CHUNK_REMAINING_BYTES = "http.crb";

    /** Key for a partially received chunk-size or trailer line */
    private static final String PARTIAL_CHUNK_LINE = "http.pcl";

    /** Key indicating that the last (zero-sized) chunk was seen and trailers are being consumed */
    private static final String CHUNK_TRAILER = "http.ctr";

    /** Maximum length of a chunk-size or trailer line, to bound accumulation */
    private static final int MAX_CHUNK_LINE_LENGTH = 4096;

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern RESPONSE_LINE_PATTERN = Pattern.compile(" ");

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
    public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(": ");

    /** Regex to split cookie header following RFC6265 Section 5.4 */
    public static final Pattern COOKIE_SEPARATOR_PATTERN = Pattern.compile(";");

    /**
     * Creates a new HttpClientDecoder instance, limiting the accumulated HTTP
     * response head to {@link #DEFAULT_MAX_RESPONSE_HEAD_SIZE} bytes.
     */
    public HttpClientDecoder() {
        this(DEFAULT_MAX_RESPONSE_HEAD_SIZE);
    }

    /**
     * Creates a new HttpClientDecoder instance
     *
     * @param maxResponseHeadSize the maximum size in bytes of an accumulated
     * HTTP response head (status line plus headers); responses whose head
     * exceeds this size fail with an {@link HttpException}. A value of
     * <tt>0</tt> or less disables the limit.
     */
    public HttpClientDecoder(int maxResponseHeadSize) {
        this.maxResponseHeadSize = maxResponseHeadSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decode(IoSession session, IoBuffer msg, ProtocolDecoderOutput out) {
        DecoderState state = (DecoderState)session.getAttribute(DECODER_STATE_ATT);
        
        if (null == state) {
            state = DecoderState.NEW;
            session.setAttribute(DECODER_STATE_ATT, state);
        }
        
        switch (state) {
            case HEAD:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("decoding HEAD");
                }
                
                // grab the stored a partial HEAD request
                ByteBuffer oldBuffer = (ByteBuffer)session.getAttribute(PARTIAL_HEAD_ATT);
                // concat the old buffer and the new incoming one
                msg = IoBuffer.allocate(oldBuffer.remaining() + msg.remaining()).put(oldBuffer).put(msg).flip();
                // now let's decode like it was a new message
    
            case NEW:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("decoding NEW");
                }
                
                DefaultHttpResponse rp = parseHttpReponseHead(msg.buf());
    
                if (rp == null) {
                    // Refuse to accumulate more than maxResponseHeadSize bytes for a
                    // single response head: a server withholding the CRLFCRLF
                    // terminator must not be able to grow the partial-head buffer
                    // without bound.
                    if ((maxResponseHeadSize > 0) && (msg.remaining() > maxResponseHeadSize)) {
                        session.removeAttribute(PARTIAL_HEAD_ATT);
                        session.removeAttribute(DECODER_STATE_ATT);
                        throw new HttpException(HttpStatus.CLIENT_ERROR_REQUEST_ENTITY_TOO_LARGE,
                                "HTTP response head exceeds " + maxResponseHeadSize + " bytes");
                    }

                    // we copy the incoming BB because it's going to be recycled by the inner IoProcessor for next reads
                    ByteBuffer partial = ByteBuffer.allocate(msg.remaining());
                    partial.put(msg.buf());
                    partial.flip();
                    // no request decoded, we accumulate
                    session.setAttribute(PARTIAL_HEAD_ATT, partial);
                    session.setAttribute(DECODER_STATE_ATT, DecoderState.HEAD);
                    break;
                }

                out.write(rp);
                
                // is it a response with some body content ?
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("response with content");
                }

                session.setAttribute(DECODER_STATE_ATT, DecoderState.BODY);

                String contentLen = rp.getHeader("content-length");

                if (contentLen != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("response with content");
                    }

                    session.setAttribute(BODY_REMAINING_BYTES, Integer.valueOf(contentLen));
                } else if ("chunked".equalsIgnoreCase(rp.getHeader("transfer-encoding"))) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("no content len but chunked");
                    }

                    session.setAttribute(BODY_CHUNKED, Boolean.TRUE);
                } else if ("close".equalsIgnoreCase(rp.getHeader("connection"))) {
                    session.closeNow();
                    break;
                } else {
                    throw new HttpException(HttpStatus.CLIENT_ERROR_LENGTH_REQUIRED, "no content length !");
                }

                // fallthrough, process the body bytes received along with the head

            case BODY:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("decoding BODY: {} bytes", msg.remaining());
                }

                // a chunked body is framed by the server : decode the framing
                if( session.getAttribute(BODY_CHUNKED) != null ) {
                    decodeChunkedBody(session, msg, out);
                    break;
                }

                int chunkSize = msg.remaining();
                
                // send the chunk of body
                if (chunkSize != 0) {
                    IoBuffer wb = IoBuffer.allocate(msg.remaining());
                    wb.put(msg);
                    wb.flip();
                    out.write(wb);
                }
                
                msg.position(msg.limit());
                
                // do we have reach end of body ? manage with content-length
                int remaining = (Integer) session.getAttribute(BODY_REMAINING_BYTES);
                remaining -= chunkSize;
    
                if (remaining <= 0 ) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("end of HTTP body");
                    }
                    
                    session.setAttribute(DECODER_STATE_ATT, DecoderState.NEW);
                    session.removeAttribute(BODY_REMAINING_BYTES);
                    
                    out.write(new HttpEndOfContent());
                } else {
                    session.setAttribute(BODY_REMAINING_BYTES, Integer.valueOf(remaining));
                }
    
                break;
    
            default:
                throw new HttpException(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR, "Unknonwn decoder state : " + state);
        }
    }

    /**
     * Decodes a body sent with "Transfer-Encoding: chunked". Only the chunk payloads
     * are delivered to the next filter : the chunk-size lines, the CRLF terminating
     * each chunk and the optional trailers are consumed by the decoder. A
     * {@link HttpEndOfContent} is emitted once the last (zero-sized) chunk and its
     * trailers have been received. The method consumes at least one byte on each
     * invocation when data remains, so the codec filter can safely call it again
     * while input remains.
     */
    private void decodeChunkedBody(IoSession session, IoBuffer msg, ProtocolDecoderOutput out) {
        Integer chunkRemaining = (Integer) session.getAttribute(CHUNK_REMAINING_BYTES);

        if ((chunkRemaining != null) && (chunkRemaining.intValue() > 0)) {
            // deliver as much of the current chunk payload as we have received
            int toRead = Math.min(chunkRemaining.intValue(), msg.remaining());

            if (toRead == 0) {
                // wait for more data
                return;
            }

            IoBuffer wb = IoBuffer.allocate(toRead);
            int oldLimit = msg.limit();
            msg.limit(msg.position() + toRead);
            wb.put(msg);
            msg.limit(oldLimit);
            wb.flip();
            out.write(wb);

            int left = chunkRemaining.intValue() - toRead;

            if (left == 0) {
                session.removeAttribute(CHUNK_REMAINING_BYTES);
            } else {
                session.setAttribute(CHUNK_REMAINING_BYTES, Integer.valueOf(left));
            }

            return;
        }

        // we are between chunks : accumulate a full line, which is either a
        // chunk-size line, the CRLF terminating the previous chunk or a trailer line
        String partial = (String) session.getAttribute(PARTIAL_CHUNK_LINE);
        StringBuilder line = (partial == null) ? new StringBuilder() : new StringBuilder(partial);
        boolean complete = false;

        while (msg.hasRemaining()) {
            char c = (char) (msg.get() & 0xFF);

            if (c == '\n') {
                complete = true;
                break;
            }

            if (c != '\r') {
                line.append(c);
            }

            if (line.length() > MAX_CHUNK_LINE_LENGTH) {
                throw new HttpException(HttpStatus.CLIENT_ERROR_BAD_REQUEST, "Chunk size line too long");
            }
        }

        if (!complete) {
            // wait for more data
            session.setAttribute(PARTIAL_CHUNK_LINE, line.toString());
            return;
        }

        session.removeAttribute(PARTIAL_CHUNK_LINE);
        String chunkLine = line.toString();

        if (session.getAttribute(CHUNK_TRAILER) != null) {
            // consuming the trailers following the last chunk
            if (chunkLine.length() == 0) {
                // end of the trailers : end of the response
                session.setAttribute(DECODER_STATE_ATT, DecoderState.NEW);
                session.removeAttribute(BODY_CHUNKED);
                session.removeAttribute(CHUNK_TRAILER);
                out.write(new HttpEndOfContent());
            }

            return;
        }

        if (chunkLine.length() == 0) {
            // the CRLF terminating the previous chunk payload
            return;
        }

        // a chunk-size line : an hex size, optionally followed by ';' and extensions
        int semicolon = chunkLine.indexOf(';');
        String sizeString = (semicolon >= 0 ? chunkLine.substring(0, semicolon) : chunkLine).trim();
        int size;

        try {
            size = Integer.parseInt(sizeString, 16);
        } catch (NumberFormatException e) {
            throw new HttpException(HttpStatus.CLIENT_ERROR_BAD_REQUEST, "Invalid chunk size : " + sizeString);
        }

        if (size < 0) {
            throw new HttpException(HttpStatus.CLIENT_ERROR_BAD_REQUEST, "Invalid chunk size : " + sizeString);
        }

        if (size == 0) {
            // last chunk : consume the trailers
            session.setAttribute(CHUNK_TRAILER, Boolean.TRUE);
        } else {
            session.setAttribute(CHUNK_REMAINING_BYTES, Integer.valueOf(size));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(IoSession session) throws Exception {
    }

    private DefaultHttpResponse parseHttpReponseHead(ByteBuffer buffer) {
        String raw = new String(buffer.array(), buffer.position(), buffer.remaining());
        String[] headersAndBody = RAW_VALUE_PATTERN.split(raw, -1);
        
        if (headersAndBody.length <= 1) {
            // we didn't receive the full HTTP head
            return null;
        }

        String[] headerFields = HEADERS_BODY_PATTERN.split(headersAndBody[0]);
        headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

        String requestLine = headerFields[0];
        Map<String, String> generalHeaders = new HashMap<>();

        // the status line is not a header : start at 1
        for (int i = 1; i < headerFields.length; i++) {
            String headerField = headerFields[i];
            int firstColon = headerField.indexOf(':');

            if (firstColon > 0) {
                generalHeaders.put(headerField.substring(0, firstColon).toLowerCase(),
                        headerField.substring(firstColon + 1).trim());
            } else {
                generalHeaders.put(headerField.trim(), "");
            }
        }

        String[] elements = RESPONSE_LINE_PATTERN.split(requestLine);
        int statusCode = Integer.parseInt(elements[1]);
        
        HttpStatus status = HttpStatus.getStatus(statusCode);
        
        HttpVersion version = HttpVersion.fromString(elements[0]);

        // we put the buffer position where we found the beginning of the HTTP body
        buffer.position(buffer.position() + headersAndBody[0].length() + 4);
        
        return new DefaultHttpResponse(version, status, generalHeaders);
    }
}
