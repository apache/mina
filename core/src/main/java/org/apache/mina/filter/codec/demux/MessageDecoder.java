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
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Decodes a certain type of messages.
 * <p>
 * We didn't provide any <tt>dispose</tt> method for {@link MessageDecoder}
 * because it can give you  performance penalty in case you have a lot of
 * message types to handle.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see DemuxingProtocolDecoder
 * @see MessageDecoderFactory
 */
public interface MessageDecoder {
    /**
     * Represents a result from {@link #decodable(IoSession, IoBuffer)} and
     * {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}.  Please
     * refer to each method's documentation for detailed explanation.
     */
    static MessageDecoderResult OK = MessageDecoderResult.OK;

    /**
     * Represents a result from {@link #decodable(IoSession, IoBuffer)} and
     * {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}.  Please
     * refer to each method's documentation for detailed explanation.
     */
    static MessageDecoderResult NEED_DATA = MessageDecoderResult.NEED_DATA;

    /**
     * Represents a result from {@link #decodable(IoSession, IoBuffer)} and
     * {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}.  Please
     * refer to each method's documentation for detailed explanation.
     */
    static MessageDecoderResult NOT_OK = MessageDecoderResult.NOT_OK;

    /**
     * Checks the specified buffer is decodable by this decoder.
     *
     * @return {@link #OK} if this decoder can decode the specified buffer.
     *         {@link #NOT_OK} if this decoder cannot decode the specified buffer.
     *         {@link #NEED_DATA} if more data is required to determine if the
     *         specified buffer is decodable ({@link #OK}) or not decodable
     *         {@link #NOT_OK}.
     */
    MessageDecoderResult decodable(IoSession session, IoBuffer in);

    /**
     * Decodes binary or protocol-specific content into higher-level message objects.
     * MINA invokes {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)}
     * method with read data, and then the decoder implementation puts decoded
     * messages into {@link ProtocolDecoderOutput}.
     *
     * @return {@link #OK} if you finished decoding messages successfully.
     *         {@link #NEED_DATA} if you need more data to finish decoding current message.
     *         {@link #NOT_OK} if you cannot decode current message due to protocol specification violation.
     *
     * @throws Exception if the read data violated protocol specification
     */
    MessageDecoderResult decode(IoSession session, IoBuffer in,
            ProtocolDecoderOutput out) throws Exception;

    /**
     * Invoked when the specified <tt>session</tt> is closed while this decoder was
     * parsing the data.  This method is useful when you deal with the protocol which doesn't
     * specify the length of a message such as HTTP response without <tt>content-length</tt>
     * header. Implement this method to process the remaining data that
     * {@link #decode(IoSession, IoBuffer, ProtocolDecoderOutput)} method didn't process
     * completely.
     *
     * @throws Exception if the read data violated protocol specification
     */
    void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception;
}
