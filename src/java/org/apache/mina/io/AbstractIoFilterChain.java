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
package org.apache.mina.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoFilter.NextFilter;

/**
 * An abstract implementation of {@link IoFilterChain} that provides
 * common operations for developers to support specific transport types.
 * <p>
 * All methods has been implemented.  The list of filters is maintained
 * as a doublely linked list.  You can fire any MINA events which is filtered
 * by this chain using these public methods:
 * <ul>
 *   <li></li>
 * </ul>
 * 
 * The only method a developer should implement is {@link #doWrite(IoSession, ByteBuffer, Object)}.
 * This method is invoked when filter chain is evaluated for
 * {@link IoFilter#filterWrite(NextFilter, IoSession, ByteBuffer, Object)} and 
 * finally to be written to the underlying transport layer (e.g. socket)
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoFilterChain implements IoFilterChain
{
    private final Map name2entry = new HashMap();

    private final Map filter2entry = new IdentityHashMap();

    private final Entry head;
    
    private final Entry tail;

    protected AbstractIoFilterChain()
    {
        head = new Entry( null, null, "head", createHeadFilter() );
        tail = new Entry( head, null, "tail", createTailFilter() );
        head.nextEntry = tail;
    }
    
    /**
     * Override this method to create custom head of this filter chain.
     */
    protected IoFilter createHeadFilter()
    {
        return new IoFilter()
        {
            public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
            {
                nextFilter.sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
            {
                nextFilter.sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, IoSession session,
                                    IdleStatus status ) throws Exception
            {
                nextFilter.sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        IoSession session, Throwable cause ) throws Exception
            {
                nextFilter.exceptionCaught( session, cause );
            }

            public void dataRead( NextFilter nextFilter, IoSession session,
                                 ByteBuffer buf ) throws Exception
            {
                nextFilter.dataRead( session, buf );
            }

            public void dataWritten( NextFilter nextFilter, IoSession session,
                                    Object marker ) throws Exception
            {
                nextFilter.dataWritten( session, marker );
            }
            
            public void filterWrite( NextFilter nextFilter, IoSession session,
                                     ByteBuffer buf, Object marker ) throws Exception
            {
                doWrite( session, buf, marker );
            }
        };
    }
    
    /**
     * Override this method to create custom tail of this filter chain.
     */
    protected IoFilter createTailFilter()
    {
        return new IoFilter()
        {
            public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
            {
                session.getHandler().sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
            {
                session.getHandler().sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, IoSession session,
                                    IdleStatus status ) throws Exception
            {
                session.getHandler().sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        IoSession session, Throwable cause ) throws Exception
            {
                session.getHandler().exceptionCaught( session, cause );
            }

            public void dataRead( NextFilter nextFilter, IoSession session,
                                 ByteBuffer buf ) throws Exception
            {
                IoHandler handler = session.getHandler();
                handler.dataRead( session, buf );
                buf.release();
            }

            public void dataWritten( NextFilter nextFilter, IoSession session,
                                    Object marker ) throws Exception
            {
                session.getHandler().dataWritten( session, marker );
            }

            public void filterWrite( NextFilter nextFilter,
                                     IoSession session, ByteBuffer buf, Object marker ) throws Exception
            {
                nextFilter.filterWrite( session, buf, marker );
            }
        };
    }
    
    public IoFilter getChild( String name )
    {
        Entry e = ( Entry ) name2entry.get( name );
        if ( e == null )
        {
            return null;
        }
        return e.filter;
    }
    
    /**
     * Adds the specified interceptor with the specified name at the beginning of this chain.
     */
    public synchronized void addFirst( String name,
                                       IoFilter filter )
    {
        checkAddable( name );
        register( head, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name at the end of this chain.
     */
    public synchronized void addLast( String name,
                                      IoFilter filter )
    {
        checkAddable( name );
        register( tail.prevEntry, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name just before the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addBefore( String baseName,
                                        String name,
                                        IoFilter filter )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name );
        register( baseEntry, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name just after the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addAfter( String baseName,
                                       String name,
                                       IoFilter filter )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name );
        register( baseEntry.prevEntry, name, filter );
    }


    /**
     * Removes the interceptor with the specified name from this chain.
     */
    public synchronized IoFilter remove( String name )
    {
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;

        name2entry.remove( name );
        IoFilter filter = entry.filter;
        filter2entry.remove( filter );
        
        return filter;
    }


    /**
     * Removes all interceptors added to this chain.
     */
    public synchronized void clear()
    {
        Iterator it = new ArrayList( name2entry.keySet() ).iterator();
        while ( it.hasNext() )
        {
            this.remove( ( String ) it.next() );
        }
    }

    private void register( Entry prevEntry, String name, IoFilter filter )
    {
        Entry newEntry = new Entry( prevEntry, prevEntry.nextEntry, name, filter );
        prevEntry.nextEntry.prevEntry = newEntry;
        prevEntry.nextEntry = newEntry;
        name2entry.put( name, newEntry );
        filter2entry.put( filter, newEntry );
    }

    /**
     * Throws an exception when the specified interceptor name is not registered in this chain.
     *
     * @return An interceptor entry with the specified name.
     */
    private Entry checkOldName( String baseName )
    {
        Entry e = ( Entry ) name2entry.get( baseName );
        if ( e == null )
        {
            throw new IllegalArgumentException( "Unknown interceptor name:" +
                    baseName );
        }
        return e;
    }


    /**
     * Checks the specified interceptor name is already taken and throws an exception if already taken.
     */
    private void checkAddable( String name )
    {
        if ( name2entry.containsKey( name ) )
        {
            throw new IllegalArgumentException( "Other interceptor is using name '" + name + "'" );
        }
    }

    public void sessionOpened( IoSession session )
    {
        Entry head = this.head;
        callNextSessionOpened(head, session);
    }

    private void callNextSessionOpened( Entry entry,
                                        IoSession session)
    {
        try
        {
            entry.filter.sessionOpened( entry.nextFilter, session );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void sessionClosed( IoSession session )
    {
        Entry head = this.head;
        callNextSessionClosed(head, session);
    }

    private void callNextSessionClosed( Entry entry,
                                        IoSession session )
    {
        try
        {
            entry.filter.sessionClosed( entry.nextFilter, session );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
        Entry head = this.head;
        callNextSessionIdle(head, session, status);
    }

    private void callNextSessionIdle( Entry entry,
                                      IoSession session,
                                      IdleStatus status )
    {
        try
        {
            entry.filter.sessionIdle( entry.nextFilter, session, status );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void dataRead( IoSession session, ByteBuffer buf )
    {
        Entry head = this.head;
        callNextDataRead(head, session, buf);
    }

    private void callNextDataRead( Entry entry,
                                   IoSession session,
                                   ByteBuffer buf )
    {
        try
        {
            entry.filter.dataRead( entry.nextFilter, session, buf );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void dataWritten( IoSession session, Object marker )
    {
        Entry head = this.head;
        callNextDataWritten(head, session, marker);
    }

    private void callNextDataWritten( Entry entry,
                                      IoSession session,
                                      Object marker ) 
    {
        try
        {
            entry.filter.dataWritten( entry.nextFilter, session, marker );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        Entry head = this.head;
        callNextExceptionCaught(head, session, cause);
    }

    private void callNextExceptionCaught( Entry entry,
                                          IoSession session,
                                          Throwable cause )
    {
        try
        {
            entry.filter.exceptionCaught( entry.nextFilter, session, cause );
        }
        catch( Throwable e )
        {
            e.printStackTrace();
        }
    }
    
    public void filterWrite( IoSession session, ByteBuffer buf, Object marker )
    {
        Entry tail = this.tail;
        callPreviousFilterWrite( tail, session, buf, marker );
    }

    private void callPreviousFilterWrite( Entry entry,
                                          IoSession session,
                                          ByteBuffer buf, Object marker )
    {
        if( buf == null )
        {
            return;
        }
        
        try
        {
            entry.filter.filterWrite( entry.nextFilter, session, buf, marker );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public List getChildren()
    {
        List list = new ArrayList();
        Entry e = head.nextEntry;
        while( e != tail )
        {
            list.add( e.filter );
            e = e.nextEntry;
        }

        return list;
    }

    public List getChildrenReversed()
    {
        List list = new ArrayList();
        Entry e = tail.prevEntry;
        while( e != head )
        {
            list.add( e.filter );
            e = e.prevEntry;
        }
        return list;
    }
    
    protected abstract void doWrite( IoSession session, ByteBuffer buffer, Object marker );

    private class Entry
    {
        private Entry prevEntry;

        private Entry nextEntry;

        private final String name;
        
        private final IoFilter filter;

        private final NextFilter nextFilter;
        
        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, IoFilter filter )
        {
            if( filter == null )
            {
                throw new NullPointerException( "filter" );
            }
            if( name == null )
            {
                throw new NullPointerException( "name" );
            }
            
            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.name = name;
            this.filter = filter;
            this.nextFilter = new NextFilter()
            {

                public void sessionOpened( IoSession session )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionOpened( nextEntry, session );
                }

                public void sessionClosed( IoSession session )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionClosed( nextEntry, session );
                }

                public void sessionIdle( IoSession session, IdleStatus status )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionIdle( nextEntry, session, status );
                }

                public void exceptionCaught( IoSession session,
                                            Throwable cause )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextExceptionCaught( nextEntry, session, cause );
                }

                public void dataRead( IoSession session, ByteBuffer buf )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextDataRead( nextEntry, session, buf );
                }

                public void dataWritten( IoSession session, Object marker )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextDataWritten( nextEntry, session, marker );
                }
                
                public void filterWrite( IoSession session, ByteBuffer buf, Object marker )
                {
                    Entry nextEntry = Entry.this.prevEntry;
                    callPreviousFilterWrite( nextEntry, session, buf, marker );
                }
            };
        }
        
        public String getName()
        {
            return name;
        }
        
        public IoFilter getFilter()
        {
            return filter;
        }
    }
}
