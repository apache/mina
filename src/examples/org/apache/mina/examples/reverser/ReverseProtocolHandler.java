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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;

/**
 * {@link ProtocolHandler} implementation of reverser server protocol.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class ReverseProtocolHandler implements ProtocolHandler
{

    public void sessionOpened( ProtocolSession session )
    {
        System.out.println( session.getRemoteAddress() + " OPENED" );
    }

    public void sessionClosed( ProtocolSession session )
    {
        System.out.println( session.getRemoteAddress() + " CLOSED" );
    }

    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        System.out.println( session.getRemoteAddress() + " IDLE(" + status
                            + ")" );
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        System.out.println( session.getRemoteAddress() + " EXCEPTION" );
        cause.printStackTrace( System.out );

        // Close connection when unexpected exception is caught.
        session.close();
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
        System.out.println( session.getRemoteAddress() + " RCVD: " + message );

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

    public void messageSent( ProtocolSession session, Object message )
    {
        // Invoked the reversed string is actually written to socket channel.
        System.out.println( session.getRemoteAddress() + " SENT: " + message );
    }
}