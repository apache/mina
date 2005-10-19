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
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} implementation for echo server. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class EchoProtocolHandler extends IoHandlerAdapter
{
    private static final Logger log = LoggerFactory.getLogger( EchoProtocolHandler.class );

    public void sessionCreated( IoSession session )
    {
        SessionConfig cfg = session.getConfig();
        if( cfg instanceof SocketSessionConfig )
        {
            ( ( SocketSessionConfig ) cfg ).setSessionReceiveBufferSize( 2048 );
        }
        
        cfg.setIdleTime( IdleStatus.BOTH_IDLE, 10 );
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
        log.info(
                "*** IDLE #" +
                session.getIdleCount( IdleStatus.BOTH_IDLE ) +
                " ***" );
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