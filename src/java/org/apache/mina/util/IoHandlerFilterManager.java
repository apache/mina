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

    private final Entry[] entries;

    private final int minPriority;

    private final int maxPriority;

    public IoHandlerFilterManager( int minPriority, int maxPriority )
    {
        this.minPriority = minPriority;
        this.maxPriority = maxPriority;

        entries = new Entry[ maxPriority - minPriority + 2 ];
        entries[ 0 ] = new Entry( minPriority - 1, FINAL_FILTER );
    }

    public IoHandlerFilterManager()
    {
        this( IoHandlerFilter.MIN_PRIORITY, IoHandlerFilter.MAX_PRIORITY );
    }

    public synchronized void addFilter( int priority, IoHandlerFilter filter )
    {
        if( priority < minPriority || priority > maxPriority )
        {
            throw new IllegalArgumentException( "priority: " + priority
                    + " (should be " + minPriority + '~' + maxPriority + ')' );
        }

        if( entries[ priority - minPriority + 1 ] == null )
        {
            entries[ priority - minPriority + 1 ] = new Entry( priority,
                    filter );
        }
        else
        {
            throw new IllegalArgumentException(
                    "Other filter is registered with priority " + priority
                            + " already." );
        }
    }

    public synchronized boolean removeFilter( IoHandlerFilter filter )
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

    public void fireSessionOpened( IoSession session )
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

    public void fireSessionClosed( IoSession session )
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

    public void fireSessionIdle( IoSession session, IdleStatus status )
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

    public void fireDataRead( IoSession session, ByteBuffer buf )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.dataRead( entry.nextHandler, session, buf );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireDataWritten( IoSession session, Object marker )
    {
        Entry entry = findNextEntry( maxPriority + 1 );
        try
        {
            entry.filter.dataWritten( entry.nextHandler, session, marker );
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void fireExceptionCaught( IoSession session, Throwable cause )
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

    public void write( IoSession session, WriteCommand cmd, ByteBuffer buf,
                      Object marker )
    {
        ByteBuffer newBuf;
        for( int i = 0; i < entries.length; i ++ )
        {
            Entry e = entries[ i ];
            if( e == null )
            {
                continue;
            }

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
        }

        cmd.execute( buf, marker );
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

        private final IoHandlerFilter filter;

        private final IoHandler nextHandler;

        private Entry( int priority, IoHandlerFilter filter )
        {
            if( filter == null )
                throw new NullPointerException( "filter" );
            this.priority = priority;
            this.filter = filter;
            this.nextHandler = new IoHandler()
            {

                public void sessionOpened( IoSession session )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.sessionOpened(
                                nextEntry.nextHandler, session );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void sessionClosed( IoSession session )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.sessionClosed(
                                nextEntry.nextHandler, session );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void sessionIdle( IoSession session, IdleStatus status )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.sessionIdle( nextEntry.nextHandler,
                                session, status );
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

                public void dataRead( IoSession session, ByteBuffer buf )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.dataRead( nextEntry.nextHandler,
                                session, buf );
                    }
                    catch( Throwable e )
                    {
                        IoHandlerFilterManager.this.fireExceptionCaught(
                                session, e );
                    }
                }

                public void dataWritten( IoSession session, Object marker )
                {
                    Entry nextEntry = findNextEntry( Entry.this.priority );
                    try
                    {
                        nextEntry.filter.dataWritten( nextEntry.nextHandler,
                                session, marker );
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
