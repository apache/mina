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

import java.io.Serializable;

import org.apache.mina.common.BufferDataException;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * A {@link ProtocolDecoder} which deserializes {@link Serializable} Java
 * objects using {@link ByteBuffer#getObject(ClassLoader)}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ObjectSerializationDecoder extends CumulativeProtocolDecoder {
    private final ClassLoader classLoader;

    private int maxObjectSize = 1048576; // 1MB

    /**
     * Creates a new instance with the {@link ClassLoader} of
     * the current thread.
     */
    public ObjectSerializationDecoder() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a new instance with the specified {@link ClassLoader}.
     */
    public ObjectSerializationDecoder(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        this.classLoader = classLoader;
    }

    /**
     * Returns the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     */
    public int getMaxObjectSize() {
        return maxObjectSize;
    }

    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     */
    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: "
                    + maxObjectSize);
        }

        this.maxObjectSize = maxObjectSize;
    }

    protected boolean doDecode(IoSession session, ByteBuffer in,
            ProtocolDecoderOutput out) throws Exception {
        if (!in.prefixedDataAvailable(4, maxObjectSize)) {
            return false;
        }

        out.write(in.getObject(classLoader));
        return true;
    }
}
