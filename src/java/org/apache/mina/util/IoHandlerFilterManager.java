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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.io.IoAdapter;

/**
 * Manages the list of {@link IoHandlerFilter}s.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerFilterManager
{
    private static final IoHandlerFilter FINAL_FILTER = new IoHandlerFilter()
    {

        public void sessionOpened( IoHandler nextHandler, IoSession session )
        {
            session.getHandler().sessionOpened( session );
        }

        public void sessionClosed( IoHandler nextHandler, IoSession session )
        {
            session.getHandler().sessionClosed( session );
        }

        public void sessionIdle( IoHandler nextHandler, IoSession session,
                                IdleStatus status )
        {
            session.getHandler().sessionIdle( session, status );
        }

        public void exceptionCaught( IoHandler nextHandler,
                                    IoSession session, Throwable cause )
        {
            session.getHandler().exceptionCaught( session, cause );
        }

        public void dataRead( IoHandler nextHandler, IoSession session,
                             ByteBuffer buf )
        {
            IoHandler handler = session.getHandler();
            handler.dataRead( session, buf );
            if( !IoAdapter.IO_HANDLER_TYPE.isAssignableFrom( handler
                    .getClass() ) )
            {
                ByteBuffer.release( buf );
            }
        }

        public void dataWritten( IoHandler nextHandler, IoSession session,
                                Object marker )
        {
            session.getHandler().dataWritten( session, marker );
        }

        public ByteBuffer filterWrite( IoSession session, ByteBuffer buf )
        {
            return buf;
        }
    };

    private Entry head = new Entry( null, null, Integer.MIN_VALUE,
            FINAL_FILTER, true );

    private final Entry tail = head;

    public IoHandlerFilterManager()
    {
    }

    public synchronized void addFilter( int priority, boolean hidden, IoHandlerFilter filter )
    {
        Entry e = head;
        Entry prevEntry = null;
        for( ;; )
        {
            if( e.priority < priority )
            {
                Entry newEntry = new Entry( prevEntry, e, priority, filter, hidden );
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

    public synchronized void removeFilter( IoHandlerFilter filter )
    {
        if( filter == tail )
        {
            throw new IllegalArgumentException(
                    "Cannot remove the internal filter." );
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

    public synchronized void removeAllFilters()
    {
        tail.prevEntry = null;
        tail.nextEntry = null;
        head = tail;
    }

    public void fireSessionOpened( IoSession session )
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

    public void fireSessionClosed( IoSession session )
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

    public void fireSessionIdle( IoSession session, IdleStatus status )
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

    public void fireDataRead( IoSession session, ByteBuffer buf )
    {
        Entry head = this.head;
        try
        {
            head.filter.dataRead( head.nextHandler, session, buf );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireDataWritten( IoSession session, Object marker )
    {
        Entry head = this.head;
        try
        {
            head.filter.dataWritten( head.nextHandler, session, marker );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireExceptionCaught( IoSession session, Throwable cause )
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

    public void write( IoSession session, WriteCommand cmd, ByteBuffer buf,
                      Object marker )
    {
        Entry e = tail;
        ByteBuffer newBuf;
        do
        {
            newBuf = e.filter.filterWrite( session, buf );
            if( buf != newBuf )
            {
                // Original buffer is replaced with new filtered buffer;
                // let's release the old one.
                ByteBuffer.release( buf );
            }
            else if( newBuf == null )
            {
                return;
            }
            buf = newBuf;
            e = e.prevEntry;
        }
        while( e != null );

        cmd.execute( buf, marker );
    }

    public List getAllFilters()
    {
        List list = new ArrayList();
        Entry e = head;
        do
        {
            if( !e.hidden )
            {
                list.add( e.filter );
            }
            e = e.nextEntry;
        }
        while( e != null );

        return list;
    }

    public List getAllFiltersReversed()
    {
        List list = new ArrayList();
        Entry e = tail;
        do
        {
            if( !e.hidden )
            {
                list.add( e.filter );
            }
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
        
        private final boolean hidden;

        private final IoHandlerFilter filter;

        private final IoHandler nextHandler;

        private Entry( Entry prevEntry, Entry nextEntry, int priority,
                      IoHandlerFilter filter, boolean hidden )
        {
            if( filter == null )
                throw new NullPointerException( "filter" );
            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.priority = priority;
            this.filter = filter;
            this.hidden = hidden;
            this.nextHandler = new IoHandler()
            {

                public void sessionOpened( IoSession session )
                {
                    try
                    {
                        Entry.this.nextEntry.filter.sessionOpened(
                                Entry.this.nextEntry.nextHandler, session );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void sessionClosed( IoSession session )
                {
                    try
                    {
                        Entry.this.nextEntry.filter.sessionClosed(
                                Entry.this.nextEntry.nextHandler, session );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void sessionIdle( IoSession session, IdleStatus status )
                {
                    try
                    {
                        Entry.this.nextEntry.filter.sessionIdle(
                                Entry.this.nextEntry.nextHandler, session,
                                status );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void exceptionCaught( IoSession session,
                                            Throwable cause )
                {
                    try
                    {
                        Entry.this.nextEntry.filter.exceptionCaught(
                                Entry.this.nextEntry.nextHandler, session,
                                cause );
                    }
                    catch( Throwable e )
                    {
                        e.printStackTrace();
                    }
                }

                public void dataRead( IoSession session, ByteBuffer buf )
                {
                    try
                    {
                        Entry.this.nextEntry.filter.dataRead(
                                Entry.this.nextEntry.nextHandler, session,
                                buf );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void dataWritten( IoSession session, Object marker )
                {
                    try
                    {
                        Entry.this.nextEntry.filter.dataWritten(
                                Entry.this.nextEntry.nextHandler, session,
                                marker );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }
            };
        }
    }

    public static interface WriteCommand
    {
        void execute( ByteBuffer buf, Object marker );
    }
}
