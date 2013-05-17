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

import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

public class ThriftMessageDecoder<IN extends TBase<?, ?>> extends ByteBufferDecoder<IN> {
    private TDeserializer deserializer = new TDeserializer(new TBinaryProtocol.Factory());

    private final Class<IN> clazz;

    public static <L extends TBase<?, ?>> ThriftMessageDecoder<L> newInstance(Class<L> clazz) {
        return new ThriftMessageDecoder<L>(clazz);
    }

    @Override
    public IN decode(ByteBuffer input) throws ProtocolDecoderException {
        IN object;
        try {
            byte array[] = new byte[input.remaining()];
            input.get(array);
            object = clazz.newInstance();
            deserializer.deserialize(object, array);
            return object;
        } catch (Exception e) {
            return null;
        }
    }

    public ThriftMessageDecoder(Class<IN> clazz) {
        super();
        this.clazz = clazz;
    }
}