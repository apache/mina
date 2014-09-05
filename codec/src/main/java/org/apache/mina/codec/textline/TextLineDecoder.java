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
package org.apache.mina.codec.textline;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.ProtocolDecoderException;

/**
 * A {@link ProtocolDecoder} which decodes a text line into a string.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class TextLineDecoder implements ProtocolDecoder<ByteBuffer, String, TextLineDecoder.Context> {
    private final Charset charset;

    /** The delimiter used to determinate when a line has been fully decoded */
    private final LineDelimiter delimiter;

    /** An ByteBuffer containing the delimiter */
    private ByteBuffer delimBuf;

    /** The default maximum Line length. Default to 1024. */
    private int maxLineLength = 1024;

    /** The default maximum buffer length. Default to 128 chars. */
    private int bufferLength = 128;

    /**
     * Creates a new instance with the current default {@link Charset} and
     * {@link LineDelimiter#AUTO} delimiter.
     */
    public TextLineDecoder() {
        this(LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the current default {@link Charset} and the
     * specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(String delimiter) {
        this(new LineDelimiter(delimiter));
    }

    /**
     * Creates a new instance with the current default {@link Charset} and the
     * specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(LineDelimiter delimiter) {
        this(Charset.defaultCharset(), delimiter);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt> and
     * {@link LineDelimiter#AUTO} delimiter.
     */
    public TextLineDecoder(Charset charset) {
        this(charset, LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt> and the
     * specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(Charset charset, String delimiter) {
        this(charset, new LineDelimiter(delimiter));
    }

    /**
     * Creates a new instance with the specified <tt>charset</tt> and the
     * specified <tt>delimiter</tt>.
     */
    public TextLineDecoder(Charset charset, LineDelimiter delimiter) {
        if (charset == null) {
            throw new IllegalArgumentException("charset parameter shuld not be null");
        }

        if (delimiter == null) {
            throw new IllegalArgumentException("delimiter parameter should not be null");
        }

        this.charset = charset;
        this.delimiter = delimiter;

        // Convert delimiter to ByteBuffer if not done yet.
        if (delimBuf == null) {
            ByteBuffer tmp = charset.encode(CharBuffer.wrap(delimiter.getValue()));
            tmp.rewind();
            delimBuf = tmp;
        }
    }

    /**
     * Returns the allowed maximum size of the line to be decoded. If the size
     * of the line to be decoded exceeds this value, the decoder will throw a
     * {@link BufferDataException}. The default value is <tt>1024</tt> (1KB).
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the line to be decoded. If the size of
     * the line to be decoded exceeds this value, the decoder will throw a
     * {@link BufferDataException}. The default value is <tt>1024</tt> (1KB).
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength (" + maxLineLength + ") should be a positive value");
        }

        this.maxLineLength = maxLineLength;
    }

    /**
     * Sets the default buffer size. This buffer is used in the Context to store
     * the decoded line.
     * 
     * @param bufferLength
     *            The default bufer size
     */
    public void setBufferLength(int bufferLength) {
        if (bufferLength <= 0) {
            throw new IllegalArgumentException("bufferLength (" + maxLineLength + ") should be a positive value");

        }

        this.bufferLength = bufferLength;
    }

    /**
     * Returns the allowed buffer size used to store the decoded line in the
     * Context instance.
     */
    public int getBufferLength() {
        return bufferLength;
    }

    @Override
    public Context createDecoderState() {
        return new Context(bufferLength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decode(ByteBuffer in, Context ctx) {
        if (LineDelimiter.AUTO.equals(delimiter)) {
            return decodeAuto(ctx, in);
        } else {
            return decodeNormal(ctx, in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishDecode(Context ctx) {
    }

    /**
     * Decode a line using the default delimiter on the current system
     */
    private String decodeAuto(Context ctx, ByteBuffer in) {
        String decoded = null;
        int matchCount = ctx.getMatchCount();

        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();

        while (in.hasRemaining() && decoded == null) {
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

                ctx.append(in);

                in.limit(oldLimit);
                in.position(pos);

                try {
                    if (ctx.getOverflowLength() == 0) {
                        ByteBuffer buf = ctx.getBuffer();
                        buf.flip();
                        buf.limit(buf.limit() - matchCount);

                        CharsetDecoder decoder = ctx.getDecoder();
                        CharBuffer buffer = decoder.decode(buf);
                        decoded = buffer.toString();
                    } else {
                        int overflowPosition = ctx.getOverflowLength();
                        throw new IllegalStateException("Line is too long: " + overflowPosition);
                    }
                } catch (CharacterCodingException cce) {
                    throw new ProtocolDecoderException(cce);
                } finally {
                    ctx.reset();
                }
                oldPos = pos;
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        ctx.append(in);

        ctx.setMatchCount(matchCount);
        return decoded;
    }

    /**
     * Decode a line using the delimiter defined by the caller
     * 
     * @return
     */
    private String decodeNormal(Context ctx, ByteBuffer in) {
        String decoded = null;
        int matchCount = ctx.getMatchCount();

        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();

        while (in.hasRemaining() && decoded == null) {
            byte b = in.get();

            if (delimBuf.get(matchCount) == b) {
                matchCount++;

                if (matchCount == delimBuf.limit()) {
                    // Found a match.
                    int pos = in.position();
                    in.limit(pos);
                    in.position(oldPos);

                    ctx.append(in);

                    in.limit(oldLimit);
                    in.position(pos);

                    try {
                        if (ctx.getOverflowLength() == 0) {
                            ByteBuffer buf = ctx.getBuffer();
                            buf.flip();
                            buf.limit(buf.limit() - matchCount);

                            CharsetDecoder decoder = ctx.getDecoder();
                            CharBuffer buffer = decoder.decode(buf);
                            decoded = new String(buffer.array());
                        } else {
                            int overflowLength = ctx.getOverflowLength();
                            throw new IllegalStateException("Line is too long: " + overflowLength);
                        }
                    } catch (CharacterCodingException cce) {
                        throw new ProtocolDecoderException(cce);
                    } finally {
                        ctx.reset();
                    }

                    oldPos = pos;
                    matchCount = 0;
                }
            } else {
                // fix for DIRMINA-506 & DIRMINA-536
                in.position(Math.max(0, in.position() - matchCount));
                matchCount = 0;
            }
        }

        // Put remainder to buf.
        in.position(oldPos);
        ctx.append(in);

        ctx.setMatchCount(matchCount);
        return decoded;
    }

    /**
     * A Context used during the decoding of a lin. It stores the decoder, the
     * temporary buffer containing the decoded line, and other status flags.
     * 
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory
     *         Project</a>
     * @version $Rev$, $Date$
     */
    public class Context {
        /** The decoder */
        private final CharsetDecoder decoder;

        /** The temporary buffer containing the decoded line */
        private ByteBuffer buf;

        /** The number of lines found so far */
        private int matchCount = 0;

        /**
         * Overflow length
         */
        private int overflowLength = 0;

        /** Create a new Context object with a default buffer */
        private Context(int bufferLength) {
            decoder = charset.newDecoder();
            buf = ByteBuffer.allocate(bufferLength);
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

        public int getOverflowLength() {
            return overflowLength;
        }

        public void reset() {
            overflowLength = 0;
            matchCount = 0;
            decoder.reset();
            buf.clear();
        }

        private void ensureSpace(int size) {
            if (buf.position() + size > buf.capacity()) {
                ByteBuffer b = ByteBuffer.allocate(buf.position() + size + bufferLength);
                buf.flip();
                b.put(buf);
                buf = b;
            }
        }

        public void append(ByteBuffer in) {
            if (buf.position() > maxLineLength - in.remaining()) {
                overflowLength = buf.position() + in.remaining();
                buf.clear();
                discard(in);
            } else {
                ensureSpace(in.remaining());
                getBuffer().put(in);
            }
        }

        private void discard(ByteBuffer in) {
            in.position(in.limit());
        }
    }

}