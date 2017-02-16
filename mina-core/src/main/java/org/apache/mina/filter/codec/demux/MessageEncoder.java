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
package org.apache.mina.filter.codec.demux;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Encodes a certain type of messages.
 * <p>
 * We didn't provide any <tt>dispose</tt> method for {@link MessageEncoder}
 * because it can give you  performance penalty in case you have a lot of
 * message types to handle.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see DemuxingProtocolEncoder
 * @see MessageEncoderFactory
 * 
 * @param <T> The message type
 */
public interface MessageEncoder<T> {
    /**
     * Encodes higher-level message objects into binary or protocol-specific data.
     * MINA invokes {@link #encode(IoSession, Object, ProtocolEncoderOutput)}
     * method with message which is popped from the session write queue, and then
     * the encoder implementation puts encoded {@link IoBuffer}s into
     * {@link ProtocolEncoderOutput}.
     *
     * @param session The current session 
     * @param message The message to encode
     * @param out The instance of {@link ProtocolEncoderOutput} that will receive the encoded message
     * @throws Exception if the message violated protocol specification
     */
    void encode(IoSession session, T message, ProtocolEncoderOutput out) throws Exception;
}
