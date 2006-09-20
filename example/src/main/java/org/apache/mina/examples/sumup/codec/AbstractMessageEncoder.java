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
package org.apache.mina.examples.sumup.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.examples.sumup.message.AbstractMessage;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

/**
 * A {@link MessageEncoder} that encodes message header and forwards
 * the encoding of body to a subclass.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractMessageEncoder implements MessageEncoder
{
    private final int type;

    protected AbstractMessageEncoder( int type )
    {
        this.type = type;
    }

    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws Exception
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
    
    protected abstract void encodeBody( IoSession session, AbstractMessage message, ByteBuffer out );
}
