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

    private Entry head = new Entry( null, null, Integer.MIN_VALUE,
                                    FINAL_FILTER );

    private Entry tail = head;

    public ProtocolHandlerFilterManager()
    {
    }

    public synchronized void addFilter( int priority,
                                       ProtocolHandlerFilter filter )
    {
        Entry e = head;
        Entry prevEntry = null;
        for( ;; )
        {
            if( e.priority < priority )
            {
                Entry newEntry = new Entry( prevEntry, e, priority, filter );
                if( prevEntry == null )
                {
                    head = newEntry;
                }
                else
                {
                    prevEntry.nextEntry.prevEntry = newEntry;
                    prevEntry.nextEntry = newEntry;
                }
                break;
            }
            else if( e.priority == priority )
            {
                throw new IllegalArgumentException(
                                                    "Other filter is registered with priority "
                                                                                                                                                                                                                + priority
                                                                                                                                                                                                                + " already." );
            }
            prevEntry = e;
            e = e.nextEntry;
        }
    }

    public synchronized void removeFilter( ProtocolHandlerFilter filter )
    {
    	if( filter == tail )
    	{
    		throw new IllegalArgumentException(
    				"Cannot remove the internal filter.");
    	}

        Entry e = head;
        Entry prevEntry = null;
        for( ;; )
        {
            if( e.nextEntry == null )
            {
                break;
            }
            else if( e.filter == filter )
            {
                if( prevEntry == null )
                {
                    // e is head
                    e.nextEntry.prevEntry = null;
                    head = e.nextEntry;
                }
                else
                {
                    e.nextEntry.prevEntry = prevEntry;
                    prevEntry.nextEntry = e.nextEntry;
                }
                break;
            }
            prevEntry = e;
            e = e.nextEntry;
        }
    }

    public synchronized void removeAllFilters() {
    	tail.prevEntry = null;
    	tail.nextEntry = null;
    	head = tail;
    }

    public void fireSessionOpened( ProtocolSession session )
    {
        Entry head = this.head;
        try
        {
            head.filter.sessionOpened( head.nextHandler, session );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireSessionClosed( ProtocolSession session )
    {
        Entry head = this.head;
        try
        {
            head.filter.sessionClosed( head.nextHandler, session );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireSessionIdle( ProtocolSession session, IdleStatus status )
    {
        Entry head = this.head;
        try
        {
            head.filter.sessionIdle( head.nextHandler, session, status );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireMessageSent( ProtocolSession session, Object message )
    {
        Entry head = this.head;
        try
        {
            head.filter.messageSent( head.nextHandler, session, message );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireMessageReceived( ProtocolSession session, Object message )
    {
        Entry head = this.head;
        try
        {
            head.filter.messageReceived( head.nextHandler, session, message );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireExceptionCaught( ProtocolSession session, Throwable cause )
    {
        Entry head = this.head;
        try
        {
            head.filter.exceptionCaught( head.nextHandler, session, cause );
        }
        catch( Throwable e )
        {
            e.printStackTrace();
        }
    }

    public void write( ProtocolSession session, WriteCommand cmd, Object message )
    {
        Entry e = tail;
        do
        {
            message = e.filter.filterWrite( session, message );
            e = e.prevEntry;
        }
        while( e != null );

        cmd.execute( message );
    }

    public List filters()
    {
        List list = new ArrayList();
        Entry e = head;
        do
        {
            list.add( e.filter );
            e = e.nextEntry;
        }
        while( e != null );

        return list;
    }

    public List filtersReversed()
    {
        List list = new ArrayList();
        Entry e = tail;
        do
        {
            list.add( e.filter );
            e = e.prevEntry;
        }
        while( e != null );

        return list;
    }

    private class Entry
    {
        private Entry prevEntry;

        private Entry nextEntry;

        private final int priority;

        private final ProtocolHandlerFilter filter;

        private final ProtocolHandler nextHandler;

        private Entry( Entry prevEntry, Entry nextEntry, int priority,
                      ProtocolHandlerFilter filter )
        {
            if( filter == null )
                throw new NullPointerException( "filter" );
            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.priority = priority;
            this.filter = filter;
            this.nextHandler = new ProtocolHandler()
            {

                public void sessionOpened( ProtocolSession session )
                {
                    try
                    {
                        Entry.this.nextEntry.filter
                                .sessionOpened(
                                                Entry.this.nextEntry.nextHandler,
                                                session );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this.fireExceptionCaught( session, e );
                    }
                }

                public void sessionClosed( ProtocolSession session )
                {
                    try
                    {
                        Entry.this.nextEntry.filter
                                .sessionClosed(
                                                Entry.this.nextEntry.nextHandler,
                                                session );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this.fireExceptionCaught( session, e );
                    }
                }

                public void sessionIdle( ProtocolSession session,
                                        IdleStatus status )
                {
                    try
                    {
                        Entry.this.nextEntry.filter
                                .sessionIdle(
                                              Entry.this.nextEntry.nextHandler,
                                              session, status );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this.fireExceptionCaught( session, e );
                    }
                }

                public void exceptionCaught( ProtocolSession session,
                                            Throwable cause )
                {
                    try
                    {
                        Entry.this.nextEntry.filter
                                .exceptionCaught(
                                                  Entry.this.nextEntry.nextHandler,
                                                  session, cause );
                    }
                    catch( Throwable e )
                    {
                        e.printStackTrace();
                    }
                }

                public void messageReceived( ProtocolSession session,
                                            Object message )
                {
                    try
                    {
                        Entry.this.nextEntry.filter
                                .messageReceived(
                                                  Entry.this.nextEntry.nextHandler,
                                                  session, message );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this.fireExceptionCaught( session, e );
                    }
                }

                public void messageSent( ProtocolSession session,
                                        Object message )
                {
                    try
                    {
                        Entry.this.nextEntry.filter
                                .messageSent(
                                              Entry.this.nextEntry.nextHandler,
                                              session, message );
                    }
                    catch( Throwable e )
                    {
                        ProtocolHandlerFilterManager.this.fireExceptionCaught( session, e );
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