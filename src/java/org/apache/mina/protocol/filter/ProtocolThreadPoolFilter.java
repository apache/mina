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
package org.apache.mina.protocol.filter;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.Session;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolFilter;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.util.BaseThreadPool;
import org.apache.mina.util.EventType;
import org.apache.mina.util.ThreadPool;

/**
 * A Thread-pooling filter.  This filter forwards {@link ProtocolHandler} events
 * to its thread pool.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see ThreadPool
 * @see BaseThreadPool
 */
public class ProtocolThreadPoolFilter extends BaseThreadPool implements ThreadPool, ProtocolFilter
{

    /**
     * Creates a new instanceof this filter with default thread pool settings.
     * You'll have to invoke {@link #start()} method to start threads actually.
     */
    public ProtocolThreadPoolFilter()
    {
        this( "ProtocolThreadPool" );
    }

    /**
     * Creates a new instance of this filter with default thread pool settings.
     * You'll have to invoke {@link #start()} method to start threads actually.
     * 
     * @param threadNamePrefix the prefix of the thread names this pool will create.
     */
    public ProtocolThreadPoolFilter( String threadNamePrefix )
    {
        super( threadNamePrefix );
    }

    public void sessionOpened( NextFilter nextFilter,
                              ProtocolSession session )
    {
        fireEvent( nextFilter, session, EventType.OPENED, null );
    }

    public void sessionClosed( NextFilter nextFilter,
                              ProtocolSession session )
    {
        fireEvent( nextFilter, session, EventType.CLOSED, null );
    }

    public void sessionIdle( NextFilter nextFilter,
                            ProtocolSession session, IdleStatus status )
    {
        fireEvent( nextFilter, session, EventType.IDLE, status );
    }

    public void exceptionCaught( NextFilter nextFilter,
                                ProtocolSession session, Throwable cause )
    {
        fireEvent( nextFilter, session, EventType.EXCEPTION, cause );
    }

    public void messageReceived( NextFilter nextFilter,
                                ProtocolSession session, Object message )
    {
        fireEvent( nextFilter, session, EventType.RECEIVED, message );
    }

    public void messageSent( NextFilter nextFilter,
                            ProtocolSession session, Object message )
    {
        fireEvent( nextFilter, session, EventType.SENT, message );
    }

    protected void processEvent( Object nextFilter0,
                               Session session0, EventType type,
                               Object data )
    {
        NextFilter nextFilter = ( NextFilter ) nextFilter0;
        ProtocolSession session = ( ProtocolSession ) session0;

        if( type == EventType.RECEIVED )
        {
            nextFilter.messageReceived( session, data );
        }
        else if( type == EventType.SENT )
        {
            nextFilter.messageSent( session, data );
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

    public void filterWrite( NextFilter nextFilter, ProtocolSession session, Object message )
    {
        nextFilter.filterWrite( session, message );
    }
}