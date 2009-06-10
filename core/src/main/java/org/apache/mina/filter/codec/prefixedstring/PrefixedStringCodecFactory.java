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
package org.apache.mina.filter.codec.prefixedstring;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import java.nio.charset.Charset;

/**
 * A {@link ProtocolCodecFactory} that performs encoding and decoding
 * of a Java String object using a fixed-length length prefix.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class PrefixedStringCodecFactory implements ProtocolCodecFactory {

    private final PrefixedStringEncoder encoder;

    private final PrefixedStringDecoder decoder;

    public PrefixedStringCodecFactory(Charset charset) {
        encoder = new PrefixedStringEncoder(charset);
        decoder = new PrefixedStringDecoder(charset);
    }

    public PrefixedStringCodecFactory() {
        this(Charset.defaultCharset());
    }

    /**
     * Returns the allowed maximum size of an encoded string.
     * If the size of the encoded String exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.
     * The default value is {@link PrefixedStringEncoder#DEFAULT_MAX_DATA_LENGTH}.
     * <p/>
     * This method does the same job as {@link PrefixedStringEncoder#setMaxDataLength(int)}.
     *
     * @return the allowed maximum size of an encoded string.
     */
    public int getEncoderMaxDataLength() {
        return encoder.getMaxDataLength();
    }

    /**
     * Sets the allowed maximum size of an encoded String.
     * If the size of the encoded String exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.
     * The default value is {@link PrefixedStringEncoder#DEFAULT_MAX_DATA_LENGTH}.
     * <p/>
     * This method does the same job as {@link PrefixedStringEncoder#getMaxDataLength()}.
     *
     * @param maxDataLength allowed maximum size of an encoded String.
     */
    public void setEncoderMaxDataLength(int maxDataLength) {
        encoder.setMaxDataLength(maxDataLength);
    }

    /**
     * Returns the allowed maximum size of a decoded string.
     * <p>
     * This method does the same job as {@link PrefixedStringEncoder#setMaxDataLength(int)}.
     * </p>
     *
     * @return the allowed maximum size of an encoded string.
     * @see #setDecoderMaxDataLength(int)
     */
    public int getDecoderMaxDataLength() {
        return decoder.getMaxDataLength();
    }

    /**
     * Sets the maximum allowed value specified as data length in the decoded data
     * <p>
     * Useful for preventing an OutOfMemory attack by the peer.
     * The decoder will throw a {@link BufferDataException} when data length
     * specified in the incoming data is greater than maxDataLength
     * The default value is {@link PrefixedStringDecoder#DEFAULT_MAX_DATA_LENGTH}.
     *
     * This method does the same job as {@link PrefixedStringDecoder#setMaxDataLength(int)}.
     * </p>
     *
     * @param maxDataLength maximum allowed value specified as data length in the incoming data
     */
    public void setDecoderMaxDataLength(int maxDataLength) {
        decoder.setMaxDataLength(maxDataLength);
    }

    /**
     * Sets the length of the prefix used by the decoder
     *
     * @param prefixLength the length of the length prefix (1, 2, or 4)
     */
    public void setDecoderPrefixLength(int prefixLength) {
        decoder.setPrefixLength(prefixLength);
    }

    /**
     * Gets the length of the length prefix (1, 2, or 4) used by the decoder
     *
     * @return length of the length prefix
     */
    public int getDecoderPrefixLength() {
        return decoder.getPrefixLength();
    }

    /**
     * Sets the length of the prefix used by the encoder
     *
     * @param prefixLength the length of the length prefix (1, 2, or 4)
     */
    public void setEncoderPrefixLength(int prefixLength) {
        encoder.setPrefixLength(prefixLength);
    }

    /**
     * Gets the length of the length prefix (1, 2, or 4) used by the encoder
     *
     * @return length of the length prefix
     */
    public int getEncoderPrefixLength() {
        return encoder.getPrefixLength();
    }

    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }
}
