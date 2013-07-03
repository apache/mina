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

import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.IoBufferDecoder;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessage;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class ProtobufMessageDecoder<IN extends GeneratedMessage> extends IoBufferDecoder<IN> {
    private final Method parseMethod;

    private final ExtensionRegistryLite registry;

    public static <TYPE extends GeneratedMessage> ProtobufMessageDecoder<TYPE> newInstance(Class<TYPE> c)
            throws NoSuchMethodException {
        return newInstance(c, ExtensionRegistryLite.getEmptyRegistry());
    }

    public static <TYPE extends GeneratedMessage> ProtobufMessageDecoder<TYPE> newInstance(Class<TYPE> c,
            ExtensionRegistryLite registry) throws NoSuchMethodException {
        return new ProtobufMessageDecoder<TYPE>(c, registry);
    }

    private ProtobufMessageDecoder(Class<IN> clazz, ExtensionRegistryLite registry) throws NoSuchMethodException {
        super();
        parseMethod = clazz.getDeclaredMethod("parseFrom", InputStream.class, ExtensionRegistryLite.class);
        this.registry = registry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IN decode(IoBuffer input) {
        try {
            return (IN) parseMethod.invoke(null, input.asInputStream(), registry);
        } catch (Exception e) {
            throw new ProtocolDecoderException(e);
        }
    }
}
