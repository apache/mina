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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.IoBufferDecoder;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessage;

/**
 * An alternative decoder for protobuf which allows the use various target
 * classes with the same decoder.
 * 
 * This decoder converts incoming {@link IoBuffer} into
 * {@link ProtobufSerializedMessage}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtobufDynamicMessageDecoder extends
        IoBufferDecoder<ProtobufDynamicMessageDecoder.ProtobufSerializedMessage> {

    public static ProtobufDynamicMessageDecoder newInstance() {
        return new ProtobufDynamicMessageDecoder();
    }

    @Override
    public ProtobufSerializedMessage decode(IoBuffer input) {
        return new ProtobufSerializedMessage(input);
    }

    public static final class ProtobufSerializedMessage {
        private final IoBuffer input;

        public ProtobufSerializedMessage(IoBuffer input) {
            this.input = input;
        }

        @SuppressWarnings("unchecked")
        public <L extends GeneratedMessage> L get(Class<L> clazz, ExtensionRegistryLite registry)
                throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method parseMethod = clazz.getDeclaredMethod("parseFrom", InputStream.class, ExtensionRegistryLite.class);
            return (L) parseMethod.invoke(null, input.asInputStream(), registry);
        }

        public <L extends GeneratedMessage> L get(Class<L> clazz) throws NoSuchMethodException, IllegalAccessException,
                InvocationTargetException {
            return get(clazz, ExtensionRegistryLite.getEmptyRegistry());
        }
    }
}
