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
package org.apache.mina.examples.netcat;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;

/**
 * {@link IoHandler} implementation for NetCat client.  This class extended
 * {@link IoHandlerAdapter} for convenience. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
public class NetCatProtocolHandler extends IoHandlerAdapter
{
    public void sessionOpened( IoSession session )
    {
        // Set reader idle time to 10 seconds.
        // sessionIdle(...) method will be invoked when no data is read
        // for 10 seconds.
        session.getConfig().setIdleTime( IdleStatus.READER_IDLE, 10 );
    }

    public void sessionClosed( IoSession session )
    {
        // Print out total number of bytes read from the remote peer.
        System.err.println( "Total " + session.getReadBytes() + " byte(s)" );
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
        // Close the connection if reader is idle.
        if( status == IdleStatus.READER_IDLE )
            session.close();
    }

    public void dataRead( IoSession session, ByteBuffer buf )
    {
        // Print out read buffer content.
        while( buf.hasRemaining() )
        {
            System.out.print( ( char ) buf.get() );
        }
        System.out.flush();
    }
}