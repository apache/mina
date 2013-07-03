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
package org.apache.mina.codec.delimited.ints;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;

/**
 * A {@link VarInt.Encoder} and {@link VarInt.Decoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class VarIntEncodingTest extends IntEncodingTest {

    @Override
    public IoBufferDecoder<Integer> newDecoderInstance() {
        return new VarInt().getDecoder();
    }

    @Override
    public ByteBufferEncoder<Integer> newEncoderInstance() {
        return new VarInt().getEncoder();
    }

    @Override
    public Map<Integer, ByteBuffer> getEncodingSamples() {
        Map<Integer, ByteBuffer> map = new HashMap<Integer, ByteBuffer>();

        map.put(0, ByteBuffer.wrap(new byte[] { 0 }));
        map.put(1 | 2 << 7 | 3 << 14 | 4 << 21 | 5 << 28,
                ByteBuffer.wrap(new byte[] { 1 | (byte) 0x80, 2 | (byte) 0x80, 3 | (byte) 0x80, 4 | (byte) 0x80, 5 }));
        return map;
    }

    @Override
    public Iterable<ByteBuffer> getIllegalBuffers() {
        List<ByteBuffer> list = new LinkedList<ByteBuffer>();
        list.add(ByteBuffer.wrap(new byte[] { 0 | (byte) 0x80, 0 | (byte) 0x80, 0 | (byte) 0x80, 0 | (byte) 0x80,
                1 << 4 }));
        return list;
    }

}
