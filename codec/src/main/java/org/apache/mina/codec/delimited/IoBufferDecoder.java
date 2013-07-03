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
package org.apache.mina.codec.delimited;

import java.nio.ByteBuffer;

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.StatelessProtocolDecoder;
import org.apache.mina.codec.delimited.ints.RawInt32;
import org.apache.mina.codec.delimited.ints.VarInt;

/**
 * Abstract class providing both encoding and decoding methods between a given
 * type and ByteBuffers.
 * 
 * <p>
 * Transcoder is stateless class providing encoding and decoding facilities.
 * Additionally this abstract requires two methods which allows to determine the
 * size of a given message and to write it directly to a previously allocated
 * ByteBuffer.
 * </p>
 * 
 * @param <INPUT>
 *            the type of the messages which will be encoded in ByteBuffers and
 *            decoded from ByteBuffers.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class IoBufferDecoder<INPUT> implements StatelessProtocolDecoder<IoBuffer, INPUT> {
    /**
     * Being stateless, this method is left empty
     * 
     * @see ProtocolDecoder#createDecoderState()
     */
    @Override
    public final Void createDecoderState() {
        // stateless !
        return null;
    }

    /**
     * Decodes a message from a {@link IoBuffer}
     * 
     * <p>
     * When a truncated input is given to this method it <b>may</b> return null.
     * Not all decoder will be able to detect this issue and report it that way.
     * Thanks to prefixing of messages, decoder will only receive appropriately
     * sized ByteBuffers.
     * </p>
     * 
     * <p>
     * n.b. The decoders used for the prefixing (i.e. {@link RawInt32} and
     * {@link VarInt}) <b>have</b> to detect truncated ByteBuffers.
     * </p>
     * 
     * @param input
     *            data to be decoded as a TYPE message
     * @return the decoded message on success, null otherwise
     * 
     * @throws ProtocolDecoderException
     */
    public abstract INPUT decode(IoBuffer input);

    /**
     * Decodes a message from a {@link ByteBuffer}
     * <p>
     * The actual decoding needs to be implemented in the abstract method
     * {@link IoBufferDecoder#decode(ByteBuffer)}
     * </p>
     */
    @Override
    public final INPUT decode(IoBuffer input, Void context) {
        return decode(input);
    }

    /**
     * Being stateless, this method is left empty
     * 
     * @see ProtocolDecoder#finishDecode(Object)
     */
    @Override
    public final void finishDecode(Void context) {
        // stateless !
    }

}
