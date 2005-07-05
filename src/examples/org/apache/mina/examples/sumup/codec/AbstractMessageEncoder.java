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
import org.apache.mina.examples.sumup.message.AbstractMessage;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.codec.MessageEncoder;

/**
 * A {@link MessageEncoder} that encodes message header and forwards
 * the encoding of body to a subclass.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class AbstractMessageEncoder implements MessageEncoder
{
    private final int type;

    protected AbstractMessageEncoder( int type )
    {
        this.type = type;
    }

    public void encode( ProtocolSession session, Object message, ProtocolEncoderOutput out ) throws ProtocolViolationException
    {
        AbstractMessage m = ( AbstractMessage ) message ;
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        buf.setAutoExpand( true ); // Enable auto-expand for easier encoding
        
        // Encode a header
        buf.putShort( ( short ) type );
        buf.putInt( m.getSequence() );
        
        // Encode a body
        encodeBody( session, m, buf );
        buf.flip();
        out.write( buf );
    }
    
    protected abstract void encodeBody( ProtocolSession session, AbstractMessage message, ByteBuffer out );
}
