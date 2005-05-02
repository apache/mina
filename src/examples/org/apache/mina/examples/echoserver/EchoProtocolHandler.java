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
package org.apache.mina.examples.echoserver;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.socket.SocketSessionConfig;

/**
 * {@link IoHandler} implementation for echo server. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class EchoProtocolHandler extends IoHandlerAdapter
{
    public void sessionOpened( IoSession session )
    {
        SessionConfig cfg = session.getConfig();
        if( cfg instanceof SocketSessionConfig )
        {
            ( ( SocketSessionConfig ) cfg ).setSessionReceiveBufferSize( 2048 );
        }
    }

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        session.close();
    }

    public void dataRead( IoSession session, ByteBuffer rb )
    {
        // Write the received data back to remote peer
        ByteBuffer wb = ByteBuffer.allocate( rb.remaining() );
        wb.put( rb );
        wb.flip();
        session.write( wb, null );
    }
}