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
package org.apache.mina.examples.sumup.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.examples.sumup.message.AbstractMessage;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

/**
 * A {@link MessageDecoder} that decodes message header and forwards
 * the decoding of body to a subclass.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractMessageDecoder implements MessageDecoder
{
    private final int type;
    
    private int sequence;
    private boolean readHeader;
    
    protected AbstractMessageDecoder( int type )
    {
        this.type = type;
    }

    public MessageDecoderResult decodable( IoSession session, ByteBuffer in )
    {
        // Return NEED_DATA if the whole header is not read yet.
        if( in.remaining() < Constants.HEADER_LEN )
        {
            return MessageDecoderResult.NEED_DATA;
        }
        
        // Return OK if type and bodyLength matches.
        if( type == in.getShort() )
        {
            return MessageDecoderResult.OK;
        }
        
        // Return NOT_OK if not matches.
        return MessageDecoderResult.NOT_OK;
    }

    public MessageDecoderResult decode( IoSession session, ByteBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        // Try to skip header if not read.
        if( !readHeader )
        {
            in.getShort(); // Skip 'type'.
            sequence = in.getInt(); // Get 'sequence'.
            readHeader = true;
        }
        
        // Try to decode body
        AbstractMessage m = decodeBody( session, in );
        // Return NEED_DATA if the body is not fully read.
        if( m == null )
        {
            return MessageDecoderResult.NEED_DATA;
        }
        else
        {
            readHeader = false; // reset readHeader for the next decode
        }
        m.setSequence( sequence );
        out.write( m );
        
        return MessageDecoderResult.OK; 
    }
    
    /**
     * @return <tt>null</tt> if the whole body is not read yet
     */
    protected abstract AbstractMessage decodeBody( IoSession session, ByteBuffer in );
}
