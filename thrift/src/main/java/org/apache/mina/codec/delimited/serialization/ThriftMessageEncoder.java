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

import org.apache.mina.codec.ProtocolEncoderException;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

/**
 * Encode Thrift message into {@link ByteBuffer}
 * 
 * @param <INPUT> the base type for message to encode
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ThriftMessageEncoder<INPUT extends TBase<?, ?>> extends ByteBufferEncoder<INPUT> {
    private final TSerializer serializer = new TSerializer(new TBinaryProtocol.Factory());

    private INPUT lastMessage;

    private byte[] lastBuffer;

    public static <L extends TBase<?, ?>> ThriftMessageEncoder<L> newInstance(Class<L> clazz) {
        return new ThriftMessageEncoder<L>();
    }

    private byte[] prepareBuffer(INPUT message) throws TException {
        if (message != lastMessage) { // NOSONAR, comparing the messages, not their content
            lastBuffer = serializer.serialize(message);
            this.lastMessage = message;
        }
        return lastBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEncodedSize(INPUT message) {
        try {
            return prepareBuffer(message).length;
        } catch (TException e) {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(INPUT message, ByteBuffer buffer) {
        try {
            buffer.put(prepareBuffer(message));
        } catch (TException e) {
            throw new ProtocolEncoderException(e);
        }
    }
}