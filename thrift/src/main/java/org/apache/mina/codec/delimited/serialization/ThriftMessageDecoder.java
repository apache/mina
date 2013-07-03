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


import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

/**
 * Decode {@link IoBuffer} into Thrift messages.
 * 
 * @param <OUTPUT> the base type for decoded messages.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ThriftMessageDecoder<OUTPUT extends TBase<?, ?>> extends IoBufferDecoder<OUTPUT> {
    private final TDeserializer deserializer = new TDeserializer(new TBinaryProtocol.Factory());

    private final Class<OUTPUT> clazz;

    /**
     * Create thrift message decoder
     * 
     * @param clazz the base class for decoded messages
     */
    public ThriftMessageDecoder(Class<OUTPUT> clazz) {
        super();
        this.clazz = clazz;
    }

    public static <L extends TBase<?, ?>> ThriftMessageDecoder<L> newInstance(Class<L> clazz) {
        return new ThriftMessageDecoder<L>(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OUTPUT decode(IoBuffer input) {
        OUTPUT object;
        try {
            byte array[] = new byte[input.remaining()];
            input.get(array);
            object = clazz.newInstance();
            deserializer.deserialize(object, array);
            return object;
        } catch (TException e) {
            throw new ProtocolDecoderException(e);
        } catch (InstantiationException e) {
            throw new ProtocolDecoderException(e);
        } catch (IllegalAccessException e) {
            throw new ProtocolDecoderException(e);
        }
    }

}