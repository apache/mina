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
package org.apache.mina.codec.delimited.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.util.ByteBufferOutputStream;

/**
 * Encoder providing the built-in Java-serialization.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class JavaNativeMessageEncoder<OUT extends Serializable> extends ByteBufferEncoder<OUT> {
    @Override
    public ByteBuffer encode(OUT message) {
        // avoid the copy done in Transcoder
        return serialize(message);
    }

    private OUT lastObject;

    private ByteBuffer lastSerialized;

    @Override
    public int getEncodedSize(OUT message) {
        return serialize(message).remaining();
    }

    private ByteBuffer serialize(OUT message) {
        if (message != lastObject) {
            ByteBufferOutputStream ebbosa = new ByteBufferOutputStream();
            ebbosa.setElastic(true);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(ebbosa);
                oos.writeObject(message);
                oos.close();
                lastObject = message;
                lastSerialized = ebbosa.getByteBuffer();
            } catch (IOException e) {
                throw new IllegalStateException("Serialization exception", e);
            }
        }
        return lastSerialized;
    }

    @Override
    public void writeTo(OUT message, ByteBuffer buffer) {
        buffer.put(serialize(message));
    }
}