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
package org.apache.mina.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.ProtocolSession;

/**
 * Manages the list of {@link ProtocolHandlerFilter}s.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolHandlerFilterManager
{
    private static final ProtocolHandlerFilter FINAL_FILTER = new ProtocolHandlerFilter()
    {

        public void sessionOpened( ProtocolHandler nextHandler,
                                  ProtocolSession session )
        {
            session.getHandler().sessionOpened( session );
        }

        public void sessionClosed( ProtocolHandler nextHandler,
                                  ProtocolSession session )
        {
            session.getHandler().sessionClosed( session );
        }

        public void sessionIdle( ProtocolHandler nextHandler,
                                ProtocolSession session, IdleStatus status )
        {
            session.getHandler().sessionIdle( session, status );
        }

        public void exceptionCaught( ProtocolHandler nextHandler,
                                    ProtocolSession session, Throwable cause )
        {
            session.getHandler().exceptionCaught( session, cause );
        }

        public void messageReceived( ProtocolHandler nextHandler,
                                    ProtocolSession session, Object message )
        {
            session.getHandler().messageReceived( session, message );
        }

        public void messageSent( ProtocolHandler nextHandler,
                                ProtocolSession session, Object message )
        {
            session.getHandler().messageSent( session, message );
        }

        public Object filterWrite( ProtocolSession session, Object message )
        {
            return message;
        }
    };

    private final Entry[] entries;

    private final int minPriority;

    private final int maxPriority;

    public ProtocolHandlerFilterManager( int minPriority, int maxPriority )
    {
        this.minPriority = minPriority;
        this.maxPriority = maxPriority;

        entries = new Entry[ maxPriority - minPriority + 2 ];
        entries[ 0 ] = new Entry( minPriority - 1, FINAL_FILTER );
    }

    public ProtocolHandlerFilterManager()
    {
        this( ProtocolHandlerFilter.MIN_PRIORITY,
                ProtocolHandlerFilter.MAX_PRIORITY );
    }

    public synchronized void addFilter( int priority,
                                       ProtocolHandlerFilter filter )
    {
        if( priority < minPriority || priority > maxPriority )
        {
            throw new IllegalArgumentException( "priority: " + priority
                    + " (should be " + minPriority + '~' + maxPriority + ')' );
        }

        if( entries[ priority - minPriority + 1 ] == null )
        {
            entries[ priority - minPriority + 1 ] = new Entry( priority, filter );
        }
        else
        {
            throw new IllegalArgumentException(
                    "Other filter is registered with priority " + priority
                            + " already." );
        }
    }

    public synchronized boolean removeFilter( ProtocolHandlerFilter filter )
    {
        for( int i = entries.length - 1; i > 0; i -- )
        {
            if( entries[ i ] != null && filter == entries[ i ].filter )
            {
                entries[ i ] = null;
                return true;
            }
        }

        return false;
    }

    public synchronized void removeAllFilters()
    {
        Arrays.fill( entries, 1, entries.length, null );
    }

    private Entry findNextEntry( int currentPriority )
    {
        currentPriority -= minPriority;

        for( ; currentPriority >= 0; currentPriority -- )
        {
            Entry e = entries[ currentPriority ];
            if( e != null )
            {
                return e;
            }
        }

        throw new InternalError();
    }

    public void fireSessionOpened( ProtocolSession session )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.sessionOpened( entry.nextHandler, session );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireSessionClosed( ProtocolSession session )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.sessionClosed( entry.nextHandler, session );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireSessionIdle( ProtocolSession session, IdleStatus status )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.sessionIdle( entry.nextHandler, session, status );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireMessageSent( ProtocolSession session, Object message )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.messageSent( entry.nextHandler, session, message );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireMessageReceived( ProtocolSession session, Object message )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter
                    .messageReceived( entry.nextHandler, session, message );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireExceptionCaught( ProtocolSession session, Throwable cause )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.exceptionCaught( entry.nextHandler, session, cause );
        }
        catch( Throwable e )
        {
            e.printStackTrace();
        }
    }

    public void write( ProtocolSession session, WriteCommand cmd,
                      Object message )
    {
        for( int i = 0; i < entries.length; i ++ )
        {
            Entry e = entries[ i ];
            if( e == null )
            {
                continue;
            }

            message = e.filter.filterWrite( session, message );
            if( message == null )
            {
                return;
            }
        }

        cmd.execute( message );
    }

    public List getAllFilters()
    {
        List list = new ArrayList( maxPriority - minPriority + 1 );
        for( int priority = maxPriority + 1;; )
        {
            Entry e = findNextEntry( priority );
            if( e.priority < minPriority )
            {
                break;
            }

            priority = e.priority;
            list.add( e.filter );
        }

        return list;
    }

    private class Entry
    {
        private final int priority;

        private final ProtocolHandlerFilter filter;

        private final ProtocolHandler nextHandler;

        private Entry( int priority, ProtocolHandlerFilter filter )
        {
            if( filter == null )
                throw new NullPointerException( "filter" );
            this.priority = priority;
            this.filter = filter;
            this.nextHandler = new ProtocolHandler()
            {

                public void sessionOpened( ProtocolSession session )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.sessionOpened(
                                nextEntry.nextHandler, session );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this
                                .fireExceptionCaught( session, e );
                    }
                }

                public void sessionClosed( ProtocolSession session )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.sessionClosed(
                                nextEntry.nextHandler, session );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this
                                .fireExceptionCaught( session, e );
                    }
                }

                public void sessionIdle( ProtocolSession session,
                                        IdleStatus status )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.sessionIdle( nextEntry.nextHandler,
                                session, status );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this
                                .fireExceptionCaught( session, e );
                    }
                }

                public void exceptionCaught( ProtocolSession session,
                                            Throwable cause )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.exceptionCaught(
                                nextEntry.nextHandler, session, cause );
                    }
                    catch( Throwable e )
                    {
                        e.printStackTrace();
                    }
                }

                public void messageReceived( ProtocolSession session,
                                            Object message )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.messageReceived(
                                nextEntry.nextHandler, session, message );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this
                                .fireExceptionCaught( session, e );
                    }
                }

                public void messageSent( ProtocolSession session,
                                        Object message )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.messageSent( nextEntry.nextHandler,
                                session, message );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this
                                .fireExceptionCaught( session, e );
                    }
                }
            };
        }
    }

    public static interface WriteCommand
    {
        void execute( Object message );
    }
}
