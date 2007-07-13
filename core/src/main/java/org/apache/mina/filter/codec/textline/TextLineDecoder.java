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
package org.apache.mina.filter.codec.textline;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.common.BufferDataException;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.util.CharsetUtil;

/**
 * A {@link ProtocolDecoder} which decodes a text line into a string.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class TextLineDecoder implements ProtocolDecoder {
    private static final String CONTEXT = TextLineDecoder.class.getName()
            + ".context";

    private final Charset charset;

    private final LineDelimiter delimiter;

    private ByteBuffer delimBuf;

    private int maxLineLength = 1024;

    /**
     * Creates a new instance with the current default {@link Charset}
     * and {@link LineDelimiter#AUTO} delimiter.
     */
    public TextLineDecoder() {
        this(CharsetUtil.getDefaultCharset(), LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt>
     * and {@link LineDelimiter#AUTO} delimiter.
     */
    public TextLineDecoder(Charset charset) {
        this(charset, LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the specified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(Charset charset, LineDelimiter delimiter) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (delimiter == null) {
            throw new NullPointerException("delimiter");
        }

        this.charset = charset;
        this.delimiter = delimiter;
    }

    /**
     * Returns the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength: "
                    + maxLineLength);
        }

        this.maxLineLength = maxLineLength;
    }

    public void decode(IoSession session, ByteBuffer in,
            ProtocolDecoderOutput out) throws Exception {
        Context ctx = getContext(session);

        if (LineDelimiter.AUTO.equals(delimiter)) {
            ctx.setMatchCount(decodeAuto(in, ctx.getBuffer(), ctx
                    .getMatchCount(), ctx.getDecoder(), out));
        } else {
            ctx.setMatchCount(decodeNormal(in, ctx.getBuffer(), ctx
                    .getMatchCount(), ctx.getDecoder(), out));
        }
    }

    private Context getContext(IoSession session) {
        Context ctx;
        ctx = (Context) session.getAttribute(CONTEXT);
        if (ctx == null) {
            ctx = new Context();
            session.setAttribute(CONTEXT, ctx);
        }
        return ctx;
    }

    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
    }

    public void dispose(IoSession session) throws Exception {
        Context ctx = (Context) session.getAttribute(CONTEXT);
        if (ctx != null) {
            ctx.getBuffer().release();
            session.removeAttribute(CONTEXT);
        }
    }

    private int decodeAuto(ByteBuffer in, ByteBuffer buf, int matchCount,
            CharsetDecoder decoder, ProtocolDecoderOutput out)
            throws CharacterCodingException {
        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();
        while (in.hasRemaining()) {
            byte b = in.get();
            boolean matched = false;
            switch (b) {
            case '\r':
                // Might be Mac, but we don't auto-detect Mac EOL
                // to avoid confusion.
                matchCount++;
                break;
            case '\n':
                // UNIX
                matchCount++;
                matched = true;
                break;
            default:
                matchCount = 0;
            }

            if (matched) {
                // Found a match.
                int pos = in.position();
                in.limit(pos);
                in.position(oldPos);

                buf.put(in);
                if (buf.position() > maxLineLength) {
                    throw new BufferDataException("Line is too long: "
                            + buf.position());
                }
                buf.flip();
                buf.limit(buf.limit() - matchCount);
                out.write(buf.getString(decoder));
                buf.clear();

                in.limit(oldLimit);
                in.position(pos);
                oldPos = pos;
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        buf.put(in);

        return matchCount;
    }

    private int decodeNormal(ByteBuffer in, ByteBuffer buf, int matchCount,
            CharsetDecoder decoder, ProtocolDecoderOutput out)
            throws CharacterCodingException {
        // Convert delimiter to ByteBuffer if not done yet.
        if (delimBuf == null) {
            ByteBuffer tmp = ByteBuffer.allocate(2).setAutoExpand(true);
            tmp.putString(delimiter.getValue(), charset.newEncoder());
            tmp.flip();
            delimBuf = tmp;
        }

        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();
        while (in.hasRemaining()) {
            byte b = in.get();
            if (delimBuf.get(matchCount) == b) {
                matchCount++;
                if (matchCount == delimBuf.limit()) {
                    // Found a match.
                    int pos = in.position();
                    in.limit(pos);
                    in.position(oldPos);

                    buf.put(in);
                    if (buf.position() > maxLineLength) {
                        throw new BufferDataException("Line is too long: "
                                + buf.position());
                    }
                    buf.flip();
                    buf.limit(buf.limit() - matchCount);
                    out.write(buf.getString(decoder));
                    buf.clear();

                    in.limit(oldLimit);
                    in.position(pos);
                    oldPos = pos;
                    matchCount = 0;
                }
            } else {
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        buf.put(in);

        return matchCount;
    }

    private class Context {
        private final CharsetDecoder decoder;

        private final ByteBuffer buf;

        private int matchCount = 0;

        private Context() {
            decoder = charset.newDecoder();
            buf = ByteBuffer.allocate(80).setAutoExpand(true);
        }

        public CharsetDecoder getDecoder() {
            return decoder;
        }

        public ByteBuffer getBuffer() {
            return buf;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }
    }
}