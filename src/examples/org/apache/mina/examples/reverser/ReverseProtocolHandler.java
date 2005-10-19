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

import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerAdapter;
import org.apache.mina.protocol.ProtocolSession;

/**
 * {@link ProtocolHandler} implementation of reverser server protocol.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class ReverseProtocolHandler extends ProtocolHandlerAdapter
{
    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        // Close connection when unexpected exception is caught.
        session.close();
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
        // Reverse reveiced string
        String str = message.toString();
        StringBuffer buf = new StringBuffer( str.length() );
        for( int i = str.length() - 1; i >= 0; i-- )
        {
            buf.append( str.charAt( i ) );
        }

        // and write it back.
        session.write( buf.toString() );
    }
}