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
package org.apache.mina.examples.reverser;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * Encodes a string into a text line which ends with <code>"\r\n"</code>.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class TextLineEncoder implements ProtocolEncoder
{

    public void encode( ProtocolSession session, Object message,
                       ProtocolEncoderOutput out )
            throws ProtocolViolationException
    {

        String val = message.toString();
        // Don't accept too long strings.
        if( val.length() > 256 )
        {
            throw new ProtocolViolationException(
                                                  "Cannot encode too long string." );
        }

        val += "\r\n";

        ByteBuffer buf = ByteBuffer.allocate( val.length() );
        for( int i = 0; i < val.length(); i++ )
        {
            buf.put( ( byte ) val.charAt( i ) );
        }

        buf.flip();
        out.write( buf );
    }
}