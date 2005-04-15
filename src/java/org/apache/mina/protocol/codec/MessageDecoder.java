/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.protocol.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * Decodes specific messages.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see DemuxingProtocolCodecFactory
 */
public interface MessageDecoder {
    /**
     * Represents a result from {@link #decodable(ProtocolSession, ByteBuffer)} and
     * {@link #decode(ProtocolSession, ByteBuffer, ProtocolDecoderOutput)}.  Please
     * refer to each method's documentation for detailed explanation.
     */
    static MessageDecoderResult OK = MessageDecoderResult.OK;

    /**
     * Represents a result from {@link #decodable(ProtocolSession, ByteBuffer)} and
     * {@link #decode(ProtocolSession, ByteBuffer, ProtocolDecoderOutput)}.  Please
     * refer to each method's documentation for detailed explanation.
     */
    static MessageDecoderResult NEED_DATA = MessageDecoderResult.NEED_DATA;

    /**
     * Represents a result from {@link #decodable(ProtocolSession, ByteBuffer)} and
     * {@link #decode(ProtocolSession, ByteBuffer, ProtocolDecoderOutput)}.  Please
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
    MessageDecoderResult decodable( ProtocolSession session, ByteBuffer in );
    
    /**
     * Decodes binary or protocol-specific content into higher-level message objects.
     * MINA invokes {@link #decode(ProtocolSession, ByteBuffer, ProtocolDecoderOutput)}
     * method with read data, and then the decoder implementation puts decoded
     * messages into {@link ProtocolDecoderOutput}.
     * 
     * @return {@link #OK} if you finished decoding messages successfully.
     *         {@link #NEED_DATA} if you need more data to finish decoding current message.
     *         {@link #NOT_OK} if you cannot decode current message due to protocol specification violation.
     *         
     * @throws ProtocolViolationException if the read data violated protocol
     *                                    specification 
     */
    MessageDecoderResult decode( ProtocolSession session, ByteBuffer in,
                         ProtocolDecoderOutput out ) throws ProtocolViolationException;
}
