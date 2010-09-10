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
package org.apache.mina.proxy.utils;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.textline.LineDelimiter;

/**
 * IoBufferDecoder.java - Handles an {@link IoBuffer} decoder which supports 
 * two methods : 
 * - dynamic delimiter decoding
 * - fixed length content reading
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class IoBufferDecoder {

    /**
     * The class holding the decoding context.
     */
    public class DecodingContext {

        /**
         * The buffered data.
         */
        private IoBuffer decodedBuffer;

        /**
         * The delimiter in use. Set if delimiter mode is in use.
         */
        private IoBuffer delimiter;

        /**
         * The currently matched bytes of the delimiter. 
         */
        private int matchCount = 0;
        
        /**
         * Holds the current content length of decoded data if in
         * content-length mode.
         */
        private int contentLength = -1;

        /**
         * Resets the decoding state.
         */
        public void reset() {
            contentLength = -1;
            matchCount = 0;
            decodedBuffer = null;
        }

        public int getContentLength() {
            return contentLength;
        }

        public void setContentLength(int contentLength) {
            this.contentLength = contentLength;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }

        public IoBuffer getDecodedBuffer() {
            return decodedBuffer;
        }

        public void setDecodedBuffer(IoBuffer decodedBuffer) {
            this.decodedBuffer = decodedBuffer;
        }

        public IoBuffer getDelimiter() {
            return delimiter;
        }

        public void setDelimiter(IoBuffer delimiter) {
            this.delimiter = delimiter;
        }
    }

    /**
     * The decoding context.
     */
    private DecodingContext ctx = new DecodingContext();

    /**
     * Creates a new instance that uses specified <tt>delimiter</tt> byte array as a
     * message delimiter.
     * 
     * @param delimiter an array of characters which delimits messages
     */
    public IoBufferDecoder(byte[] delimiter) {
        setDelimiter(delimiter, true);
    }

    /**
     * Creates a new instance that will read messages of <tt>contentLength</tt> bytes.
     * 
     * @param contentLength the exact length to read
     */
    public IoBufferDecoder(int contentLength) {
        setContentLength(contentLength, false);
    }

    /**
     * Sets the the length of the content line to be decoded.
     * When set, it overrides the dynamic delimiter setting and content length 
     * method will be used for decoding on the next decodeOnce call.
     * The default value is <tt>-1</tt>.
     * 
     * @param contentLength the content length to match
     * @param resetMatchCount delimiter matching is reset if true
     */
    public void setContentLength(int contentLength, boolean resetMatchCount) {
        if (contentLength <= 0) {
            throw new IllegalArgumentException("contentLength: "
                    + contentLength);
        }

        ctx.setContentLength(contentLength);
        if (resetMatchCount) {
            ctx.setMatchCount(0);
        }
    }

    /**
     * Dynamically sets a new delimiter. Next time 
     * {@link IoBufferDecoder#decodeOnce(IoSession, int) } will be called it will use the new 
     * delimiter. Delimiter matching is reset only if <tt>resetMatchCount</tt> is true but 
     * decoding will continue from current position.
     * 
     * NB : Delimiter {@link LineDelimiter#AUTO} is not allowed.
     * 
     * @param delim the new delimiter as a byte array
     * @param resetMatchCount delimiter matching is reset if true
     */
    public void setDelimiter(byte[] delim, boolean resetMatchCount) {
        if (delim == null) {
            throw new IllegalArgumentException("Null delimiter not allowed");
        }

        // Convert delimiter to IoBuffer.
        IoBuffer delimiter = IoBuffer.allocate(delim.length);
        delimiter.put(delim);
        delimiter.flip();

        ctx.setDelimiter(delimiter);
        ctx.setContentLength(-1);
        if (resetMatchCount) {
            ctx.setMatchCount(0);
        }
    }

    /**
     * Will return null unless it has enough data to decode. If <code>contentLength</code>
     * is set then it tries to retrieve <code>contentLength</code> bytes from the buffer
     * otherwise it will scan the buffer to find the data <code>delimiter</code> and return
     * all the data and the trailing delimiter.
     * 
     * @param in the data to decode
     */
    public IoBuffer decodeFully(IoBuffer in) {
        int contentLength = ctx.getContentLength();
        IoBuffer decodedBuffer = ctx.getDecodedBuffer();

        int oldLimit = in.limit();

        // Retrieve fixed length content
        if (contentLength > -1) {
            if (decodedBuffer == null) {
                decodedBuffer = IoBuffer.allocate(contentLength).setAutoExpand(
                        true);
            }

            // If not enough data to complete the decoding
            if (in.remaining() < contentLength) {
                int readBytes = in.remaining();
                decodedBuffer.put(in);
                ctx.setDecodedBuffer(decodedBuffer);
                ctx.setContentLength(contentLength - readBytes);
                return null;

            }

            int newLimit = in.position() + contentLength;
            in.limit(newLimit);
            decodedBuffer.put(in);
            decodedBuffer.flip();
            in.limit(oldLimit);
            ctx.reset();

            return decodedBuffer;
        }

        // Not a fixed length matching so try to find a delimiter match
        int oldPos = in.position();
        int matchCount = ctx.getMatchCount();
        IoBuffer delimiter = ctx.getDelimiter();

        while (in.hasRemaining()) {
            byte b = in.get();
            if (delimiter.get(matchCount) == b) {
                matchCount++;
                if (matchCount == delimiter.limit()) {
                    // Found a match.
                    int pos = in.position();
                    in.position(oldPos);

                    in.limit(pos);

                    if (decodedBuffer == null) {
                        decodedBuffer = IoBuffer.allocate(in.remaining())
                                .setAutoExpand(true);
                    }

                    decodedBuffer.put(in);
                    decodedBuffer.flip();

                    in.limit(oldLimit);
                    ctx.reset();

                    return decodedBuffer;
                }
            } else {
                in.position(Math.max(0, in.position() - matchCount));
                matchCount = 0;
            }
        }

        // Copy remainder from buf.
        if (in.remaining() > 0) {
            in.position(oldPos);
            decodedBuffer.put(in);
            in.position(in.limit());
        }

        // Save decoding state
        ctx.setMatchCount(matchCount);
        ctx.setDecodedBuffer(decodedBuffer);

        return decodedBuffer;
    }
}