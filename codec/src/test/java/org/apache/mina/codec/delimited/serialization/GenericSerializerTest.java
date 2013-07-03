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

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.junit.Test;

/**
 * A {@link ByteBufferEncoder} and {@link IoBufferDecoder} test. 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class GenericSerializerTest<T> {

    public abstract IoBufferDecoder<T> getDecoder() throws Exception;

    public abstract ByteBufferEncoder<T> getEncoder() throws Exception;

    public abstract List<T> getObjects();

    @Test
    public void testSerialization() throws Exception {
        IoBufferDecoder<T> decoder = getDecoder();
        ByteBufferEncoder<T> encoder = getEncoder();
        for (T object : getObjects()) {
            assertEquals(object, decoder.decode(IoBuffer.wrap(encoder.encode(object))));
        }
    }

    @Test
    public void testEncodedSize() throws Exception {
        IoBufferDecoder<T> decoder = getDecoder();
        ByteBufferEncoder<T> encoder = getEncoder();
        for (T object : getObjects()) {
            int size = encoder.getEncodedSize(object);
            ByteBuffer out = ByteBuffer.allocate(size);
            encoder.writeTo(object, out);            
            assertEquals(size, out.position());
            out.rewind();
            assertEquals(object, decoder.decode(IoBuffer.wrap(out)));
        }
    }
}
