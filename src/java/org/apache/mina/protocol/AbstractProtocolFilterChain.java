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
package org.apache.mina.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolFilter.NextFilter;

/**
 * An abstract implementation of {@link ProtocolFilterChain} that provides
 * common operations for developers to extend protocol layer.
 * <p>
 * All methods has been implemented.  The list of filters is maintained
 * as a doublely linked list.  You can fire any MINA events which is filtered
 * by this chain using these public methods:
 * <ul>
 *   <li></li>
 * </ul>
 * 
 * The only method a developer should implement is {@link #doWrite(ProtocolSession, Object)}.
 * This method is invoked when filter chain is evaluated for
 * {@link ProtocolFilter#filterWrite(NextFilter, ProtocolSession, Object)} and 
 * finally to be written out.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class AbstractProtocolFilterChain implements ProtocolFilterChain
{
    private final Map name2entry = new HashMap();

    private final Map filter2entry = new IdentityHashMap();

    private final Entry head;

    private final Entry tail;

    protected AbstractProtocolFilterChain()
    {
        head = new Entry( null, null, "head", createHeadFilter() );
        tail = new Entry( head, null, "tail", createTailFilter() );
        head.nextEntry = tail;
    }
    
    /**
     * Override this method to create custom head of this filter chain.
     */
    protected ProtocolFilter createHeadFilter()
    {
        return new ProtocolFilter()
        {
            public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
            {
                nextFilter.sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
            {
                nextFilter.sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                    IdleStatus status )
            {
                nextFilter.sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        ProtocolSession session, Throwable cause )
            {
                nextFilter.exceptionCaught( session, cause );
            }

            public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                         Object message )
            {
                nextFilter.messageReceived( session, message );
            }

            public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
            {
                nextFilter.messageSent( session, message );
            }
            
            public void filterWrite( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
            {
                doWrite( session, message );
            }
        };
    }
    
    /**
     * Override this method to create custom head of this filter chain.
     */
    protected ProtocolFilter createTailFilter()
    {
        return new ProtocolFilter()
        {
            public void sessionOpened( NextFilter nextFilter, ProtocolSession session ) throws Exception
            {
                session.getHandler().sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, ProtocolSession session ) throws Exception
            {
                session.getHandler().sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                    IdleStatus status ) throws Exception
            {
                session.getHandler().sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        ProtocolSession session, Throwable cause ) throws Exception
            {
                session.getHandler().exceptionCaught( session, cause );
            }

            public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                         Object message ) throws Exception
            {
                ProtocolHandler handler = session.getHandler();
                handler.messageReceived( session, message );
            }

            public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                     Object message ) throws Exception
            {
                session.getHandler().messageSent( session, message );
            }

            public void filterWrite( NextFilter nextFilter,
                                     ProtocolSession session, Object message ) throws Exception
            {
                nextFilter.filterWrite( session, message );
            }
        };
    }
    
    public ProtocolFilter getChild( String name )
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
                                       ProtocolFilter filter )
    {
        checkAddable( name );
        register( head, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name at the end of this chain.
     */
    public synchronized void addLast( String name,
                                      ProtocolFilter filter )
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
                                        ProtocolFilter filter )
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
                                       ProtocolFilter filter )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name );
        register( baseEntry.prevEntry, name, filter );
    }


    /**
     * Removes the interceptor with the specified name from this chain.
     */
    public synchronized void remove( String name )
    {
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;

        name2entry.remove( name );
        ProtocolFilter filter = entry.filter;
        filter2entry.remove( filter );
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

    private void register( Entry prevEntry, String name, ProtocolFilter filter )
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

    public void sessionOpened( ProtocolSession session )
    {
        Entry head = this.head;
        callNextSessionOpened(head, session);
    }

    private void callNextSessionOpened( Entry entry,
                                        ProtocolSession session)
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

    public void sessionClosed( ProtocolSession session )
    {
        Entry head = this.head;
        callNextSessionClosed(head, session);
    }

    private void callNextSessionClosed( Entry entry,
                                        ProtocolSession session )
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

    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        Entry head = this.head;
        callNextSessionIdle(head, session, status);
    }

    private void callNextSessionIdle( Entry entry,
                                      ProtocolSession session,
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

    public void messageReceived( ProtocolSession session, Object message )
    {
        Entry head = this.head;
        callNextMessageReceived(head, session, message );
    }

    private void callNextMessageReceived( Entry entry,
                                          ProtocolSession session,
                                          Object message )
    {
        try
        {
            entry.filter.messageReceived( entry.nextFilter, session, message );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void messageSent( ProtocolSession session, Object message )
    {
        Entry head = this.head;
        callNextMessageSent(head, session, message);
    }

    private void callNextMessageSent( Entry entry,
                                      ProtocolSession session,
                                      Object message ) 
    {
        try
        {
            entry.filter.messageSent( entry.nextFilter, session, message );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        Entry head = this.head;
        callNextExceptionCaught(head, session, cause);
    }

    private void callNextExceptionCaught( Entry entry,
                                          ProtocolSession session,
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
    
    public void filterWrite( ProtocolSession session, Object message )
    {
        Entry tail = this.tail;
        callPreviousFilterWrite( tail, session, message );
    }

    private void callPreviousFilterWrite( Entry entry,
                                          ProtocolSession session,
                                          Object message )
    {
        if( message == null )
        {
            return;
        }
        
        try
        {
            entry.filter.filterWrite( entry.prevFilter, session, message );
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
    
    protected abstract void doWrite( ProtocolSession session, Object message );

    private class Entry
    {
        private Entry prevEntry;

        private Entry nextEntry;

        private final String name;
        
        private final ProtocolFilter filter;

        private final NextFilter nextFilter;
        
        private final NextFilter prevFilter;

        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, ProtocolFilter filter )
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

                public void sessionOpened( ProtocolSession session )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionOpened( nextEntry, session );
                }

                public void sessionClosed( ProtocolSession session )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionClosed( nextEntry, session );
                }

                public void sessionIdle( ProtocolSession session, IdleStatus status )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionIdle( nextEntry, session, status );
                }

                public void exceptionCaught( ProtocolSession session,
                                            Throwable cause )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextExceptionCaught( nextEntry, session, cause );
                }

                public void messageReceived( ProtocolSession session, Object message )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextMessageReceived( nextEntry, session, message );
                }

                public void messageSent( ProtocolSession session, Object message )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextMessageSent( nextEntry, session, message );
                }
                
                public void filterWrite( ProtocolSession session, Object message )
                {
                    throw new IllegalStateException();
                }
            };
            
            this.prevFilter = new NextFilter()
            {

                public void sessionOpened( ProtocolSession session )
                {
                    throw new IllegalStateException();
                }

                public void sessionClosed( ProtocolSession session )
                {
                    throw new IllegalStateException();
                }

                public void sessionIdle( ProtocolSession session, IdleStatus status )
                {
                    throw new IllegalStateException();
                }

                public void exceptionCaught( ProtocolSession session,
                                            Throwable cause )
                {
                    throw new IllegalStateException();
                }

                public void messageReceived( ProtocolSession session, Object message )
                {
                    throw new IllegalStateException();
                }

                public void messageSent( ProtocolSession session, Object message )
                {
                    throw new IllegalStateException();
                }
                
                public void filterWrite( ProtocolSession session, Object message )
                {
                    Entry nextEntry = Entry.this.prevEntry;
                    callPreviousFilterWrite( nextEntry, session, message );
                }
            };
        }
        
        public String getName()
        {
            return name;
        }
        
        public ProtocolFilter getFilter()
        {
            return filter;
        }
    }
}
