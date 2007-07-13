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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Represents results from {@link MessageDecoder}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see MessageDecoder
 */
public class MessageDecoderResult {
    /**
     * Represents a result from {@link MessageDecoder#decodable(IoSession, ByteBuffer)}
     * and {@link MessageDecoder#decode(IoSession, ByteBuffer, ProtocolDecoderOutput)}.
     * Please refer to each method's documentation for detailed explanation.
     */
    public static MessageDecoderResult OK = new MessageDecoderResult("OK");

    /**
     * Represents a result from {@link MessageDecoder#decodable(IoSession, ByteBuffer)}
     * and {@link MessageDecoder#decode(IoSession, ByteBuffer, ProtocolDecoderOutput)}.
     * Please refer to each method's documentation for detailed explanation.
     */
    public static MessageDecoderResult NEED_DATA = new MessageDecoderResult(
            "NEED_DATA");

    /**
     * Represents a result from {@link MessageDecoder#decodable(IoSession, ByteBuffer)}
     * and {@link MessageDecoder#decode(IoSession, ByteBuffer, ProtocolDecoderOutput)}.
     * Please refer to each method's documentation for detailed explanation.
     */
    public static MessageDecoderResult NOT_OK = new MessageDecoderResult(
            "NOT_OK");

    private final String name;

    private MessageDecoderResult(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}