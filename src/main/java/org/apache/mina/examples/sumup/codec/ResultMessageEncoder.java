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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.examples.sumup.message.AbstractMessage;
import org.apache.mina.examples.sumup.message.ResultMessage;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.codec.MessageEncoder;

/**
 * A {@link MessageEncoder} that encodes {@link ResultMessage}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class ResultMessageEncoder extends AbstractMessageEncoder
{
    private static final Set TYPES;
    
    static
    {
        Set types = new HashSet();
        types.add( ResultMessage.class );
        TYPES = Collections.unmodifiableSet( types );
    }

    public ResultMessageEncoder()
    {
        super( Constants.RESULT );
    }

    protected void encodeBody( ProtocolSession session, AbstractMessage message, ByteBuffer out )
    {
        ResultMessage m = ( ResultMessage ) message;
        if( m.isOk() )
        {
            out.putShort( ( short ) Constants.RESULT_OK );
            out.putInt( m.getValue() );
        }
        else
        {
            out.putShort( ( short ) Constants.RESULT_ERROR );
        }
    }

    public Set getMessageTypes()
    {
        return TYPES;
    }

}
