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
package org.apache.mina.io.filter;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.Session;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoFilter;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.BaseThreadPool;
import org.apache.mina.util.EventType;
import org.apache.mina.util.ThreadPool;

/**
 * A Thread-pooling filter.  This filter forwards {@link IoHandler} events 
 * to its thread pool.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see ThreadPool
 * @see BaseThreadPool
 */
public class IoThreadPoolFilter extends BaseThreadPool implements ThreadPool, IoFilter
{
    /**
     * Creates a new instanceof this filter with default thread pool settings.
     * You'll have to invoke {@link #start()} method to start threads actually.
     */
    public IoThreadPoolFilter()
    {
    }

    public void sessionOpened( NextFilter nextFilter, IoSession session )
    {
        fireEvent( nextFilter, session, EventType.OPENED, null );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session )
    {
        fireEvent( nextFilter, session, EventType.CLOSED, null );
    }

    public void sessionIdle( NextFilter nextFilter, IoSession session,
                            IdleStatus status )
    {
        fireEvent( nextFilter, session, EventType.IDLE, status );
    }

    public void exceptionCaught( NextFilter nextFilter, IoSession session,
                                Throwable cause )
    {
        fireEvent( nextFilter, session, EventType.EXCEPTION, cause );
    }

    public void dataRead( NextFilter nextFilter, IoSession session,
                          ByteBuffer buf )
    {
        // MINA will release the buffer if this method returns.
        buf.acquire();
        fireEvent( nextFilter, session, EventType.READ, buf );
    }

    public void dataWritten( NextFilter nextFilter, IoSession session,
                            Object marker )
    {
        fireEvent( nextFilter, session, EventType.WRITTEN, marker );
    }

    protected void processEvent( Object nextFilter0, Session session0,
                                 EventType type, Object data )
    {
        NextFilter nextFilter = ( NextFilter ) nextFilter0;
        IoSession session = ( IoSession ) session0;
        if( type == EventType.READ )
        {
            ByteBuffer buf = ( ByteBuffer ) data;
            nextFilter.dataRead( session, buf );
            buf.release();
        }
        else if( type == EventType.WRITTEN )
        {
            nextFilter.dataWritten( session, data );
        }
        else if( type == EventType.EXCEPTION )
        {
            nextFilter.exceptionCaught( session, ( Throwable ) data );
        }
        else if( type == EventType.IDLE )
        {
            nextFilter.sessionIdle( session, ( IdleStatus ) data );
        }
        else if( type == EventType.OPENED )
        {
            nextFilter.sessionOpened( session );
        }
        else if( type == EventType.CLOSED )
        {
            nextFilter.sessionClosed( session );
        }
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker )
    {
        nextFilter.filterWrite( session, buf, marker );
    }
}