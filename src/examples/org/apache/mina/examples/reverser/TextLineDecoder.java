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
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * Decodes a text line into a string.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class TextLineDecoder implements ProtocolDecoder
{

    private StringBuffer decodeBuf = new StringBuffer();

    public void decode( ProtocolSession session, ByteBuffer in,
                       ProtocolDecoderOutput out )
            throws ProtocolViolationException
    {
        do
        {
            byte b = in.get();
            switch( b )
            {
            case '\r':
                break;
            case '\n':
                String result = decodeBuf.toString();
                decodeBuf.delete( 0, decodeBuf.length() );
                out.write( result );
                break;
            default:
                decodeBuf.append( ( char ) b );
            }

            // Don't accept too long line
            if( decodeBuf.length() > 256 )
            {
                decodeBuf.delete( 0, decodeBuf.length() );
                throw new ProtocolViolationException( "The line is too long." );
            }
        }
        while( in.hasRemaining() );
    }
}