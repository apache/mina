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
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;

/**
 * {@link IoHandler} implementation for echo server. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class EchoProtocolHandler implements IoHandler
{
    public void sessionOpened( IoSession session )
    {
        System.out.println( Thread.currentThread().getName() + ' '
                            + session.getRemoteAddress() + ": OPEN" );
    }

    public void sessionClosed( IoSession session )
    {
        System.out.println( Thread.currentThread().getName() + ' '
                            + session.getRemoteAddress() + ": CLOSED" );
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
        System.out.println( Thread.currentThread().getName() + ' '
                            + session.getRemoteAddress() + ": IDLE" );
    }

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        System.out.println( Thread.currentThread().getName() + ' '
                            + session.getRemoteAddress() + ": EXCEPTION" );
        cause.printStackTrace( System.out );
        session.close();
    }

    public void dataRead( IoSession session, ByteBuffer rb )
    {
        // Write the received data back to remote peer
        ByteBuffer wb = ByteBuffer.allocate( rb.remaining() );
        wb.put( rb );
        wb.flip();
        System.out.println( Thread.currentThread().getName() + ' '
                            + session.getRemoteAddress() + ": READ ("
                            + wb.remaining() + "B)" );
        session.write( wb, null );
    }

    public void dataWritten( IoSession session, Object marker )
    {
        System.out.println( Thread.currentThread().getName() + ' '
                            + session.getRemoteAddress() + ": WRITTEN" );
    }
}