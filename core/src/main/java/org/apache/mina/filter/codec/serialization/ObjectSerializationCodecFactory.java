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
package org.apache.mina.filter.codec.serialization;

import org.apache.mina.common.BufferDataException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * A {@link ProtocolCodecFactory} that serializes and deserializes Java objects.
 * This codec is very useful when you have to prototype your application rapidly
 * without any specific codec.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ObjectSerializationCodecFactory implements ProtocolCodecFactory {
    private final ObjectSerializationEncoder encoder;

    private final ObjectSerializationDecoder decoder;

    /**
     * Creates a new instance with the {@link ClassLoader} of
     * the current thread.
     */
    public ObjectSerializationCodecFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a new instance with the specified {@link ClassLoader}.
     */
    public ObjectSerializationCodecFactory(ClassLoader classLoader) {
        encoder = new ObjectSerializationEncoder();
        decoder = new ObjectSerializationDecoder(classLoader);
    }

    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    public ProtocolDecoder getDecoder() {
        return decoder;
    }

    /**
     * Returns the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * <p>
     * This method does the same job with {@link ObjectSerializationEncoder#getMaxObjectSize()}.
     */
    public int getEncoderMaxObjectSize() {
        return encoder.getMaxObjectSize();
    }

    /**
     * Sets the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * <p>
     * This method does the same job with {@link ObjectSerializationEncoder#setMaxObjectSize(int)}.
     */
    public void setEncoderMaxObjectSize(int maxObjectSize) {
        encoder.setMaxObjectSize(maxObjectSize);
    }

    /**
     * Returns the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     * <p>
     * This method does the same job with {@link ObjectSerializationDecoder#getMaxObjectSize()}.
     */
    public int getDecoderMaxObjectSize() {
        return decoder.getMaxObjectSize();
    }

    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     * <p>
     * This method does the same job with {@link ObjectSerializationDecoder#setMaxObjectSize(int)}.
     */
    public void setDecoderMaxObjectSize(int maxObjectSize) {
        decoder.setMaxObjectSize(maxObjectSize);
    }
}
