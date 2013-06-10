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

import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.StatelessProtocolEncoder;

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
public abstract class ByteBufferEncoder<INPUT> implements StatelessProtocolEncoder<INPUT, ByteBuffer> {

    /**
     * Being stateless, this method is left empty
     * 
     * @see ProtocolDecoder#createDecoderState()
     */
    @Override
    public final Void createEncoderState() {
        // stateless !
        return null;
    }

    /**
     * Encodes a message to a {@link ByteBuffer}
     * 
     * @param message
     *            a message to be encoded
     * @return the buffer containing {@link ByteBuffer} representation of the
     *         message
     */
    public ByteBuffer encode(INPUT message) {
        ByteBuffer buffer = ByteBuffer.allocate(getEncodedSize(message));
        int oldPos = buffer.position();
        writeTo(message, buffer);
        buffer.position(oldPos);
        return buffer;
    }

    /**
     * Encodes a message to a {@link ByteBuffer}
     * <p>
     * The actual encoding needs to be implemented in the abstract method
     * {@link ByteBufferEncoder#encode(Object)}
     * </p>
     */

    @Override
    public final ByteBuffer encode(INPUT message, Void context) {
        return encode(message);
    }

    /**
     * 
     * Computes the size of the serialized form of a message in bytes.
     * 
     * @param message
     *            a message to be encoded
     * @return the size of the serialized form of the message
     */
    public abstract int getEncodedSize(INPUT message);

    /**
     * Writes a message on a {@link ByteBuffer}.
     * 
     * <p>
     * n.b. The buffer is expected to have at least a sufficient capacity to
     * handle the serialized form of the message.
     * </p>
     * 
     * @param message
     *            a message to be encoded
     * @param buffer
     *            a target {@link ByteBuffer}
     */
    public abstract void writeTo(INPUT message, ByteBuffer buffer);

}
