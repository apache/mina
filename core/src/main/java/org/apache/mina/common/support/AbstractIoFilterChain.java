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
package org.apache.mina.common.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterLifeCycleException;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.util.ByteBufferUtil;
import org.apache.mina.util.SessionLog;

/**
 * An abstract implementation of {@link IoFilterChain} that provides
 * common operations for developers to implement their own transport layer.
 * <p>
 * The only method a developer should implement is
 * {@link #doWrite(IoSession, IoFilter.WriteRequest)}.  This method is invoked
 * when filter chain is evaluated for
 * {@link IoFilter#filterWrite(NextFilter, IoSession, IoFilter.WriteRequest)} and 
 * finally to be written out.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoFilterChain implements IoFilterChain
{
    private final IoSession session;

    private final Map name2entry = new HashMap();
    
    private final EntryImpl head;
    private final EntryImpl tail;

    protected AbstractIoFilterChain( IoSession session )
    {
        if( session == null )
        {
            throw new NullPointerException( "session" );
        }
        
        this.session = session;
        head = new EntryImpl( null, null, "head", createHeadFilter() );
        tail = new EntryImpl( head, null, "tail", createTailFilter() );
        head.nextEntry = tail;
    }
    
    /**
     * Override this method to create custom head of this filter chain.
     */
    protected IoFilter createHeadFilter()
    {
        return new IoFilterAdapter()
        {
            public void sessionCreated( NextFilter nextFilter, IoSession session )
            {
                nextFilter.sessionCreated( session );
            }

            public void sessionOpened( NextFilter nextFilter, IoSession session )
            {
                nextFilter.sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, IoSession session )
            {
                nextFilter.sessionClosed( session );
            }

            public void sessionIdle( NextFilter nextFilter, IoSession session,
                                    IdleStatus status )
            {
                nextFilter.sessionIdle( session, status );
            }

            public void exceptionCaught( NextFilter nextFilter,
                                        IoSession session, Throwable cause )
            {
                nextFilter.exceptionCaught( session, cause );
            }

            public void messageReceived( NextFilter nextFilter, IoSession session,
                                         Object message )
            {
                nextFilter.messageReceived( session, message );
            }

            public void messageSent( NextFilter nextFilter, IoSession session,
                                     Object message )
            {
                nextFilter.messageSent( session, message );
            }
            
            public void filterWrite( NextFilter nextFilter, IoSession session,
                                     WriteRequest writeRequest ) throws Exception
            {
                if( session.getTransportType().getEnvelopeType().isAssignableFrom( writeRequest.getMessage().getClass() ) )
                {
                    doWrite( session, writeRequest );
                }
                else
                {
                    throw new IllegalStateException(
                            "Write requests must be transformed to " + 
                            session.getTransportType().getEnvelopeType() +
                            ": " + writeRequest );
                }
            }

            public void filterClose( NextFilter nextFilter, IoSession session, CloseFuture closeFuture ) throws Exception
            {
                doClose( session, closeFuture );
            }
        };
    }
    
    /**
     * Override this method to create custom head of this filter chain.
     */
    protected IoFilter createTailFilter()
    {
        return new IoFilterAdapter()
        {
            public void sessionCreated( NextFilter nextFilter, IoSession session ) throws Exception
            {
                session.getHandler().sessionCreated( session );
            }
            public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
            {
                session.getHandler().sessionOpened( session );
            }

            public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
            {
                try
                {
                    session.getHandler().sessionClosed( session );
                }
                finally
                {
                    // Remove all filters.
                    session.getFilterChain().clear();
                }
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

            public void messageReceived( NextFilter nextFilter, IoSession session,
                                         Object message ) throws Exception
            {
                try
                {
                    session.getHandler().messageReceived( session, message );
                }
                finally
                {
                    ByteBufferUtil.releaseIfPossible( message );
                }
            }

            public void messageSent( NextFilter nextFilter, IoSession session,
                                     Object message ) throws Exception
            {
                try
                {
                    session.getHandler().messageSent( session, message );
                }
                finally
                {
                    ByteBufferUtil.releaseIfPossible( message );
                }
            }

            public void filterWrite( NextFilter nextFilter,
                                     IoSession session, WriteRequest writeRequest ) throws Exception
            {
                nextFilter.filterWrite( session, writeRequest );
            }

            public void filterClose( NextFilter nextFilter, IoSession session, CloseFuture closeFuture ) throws Exception
            {
                nextFilter.filterClose( session, closeFuture );
            }
        };
    }
    
    public IoSession getSession()
    {
        return session;
    }
    
    public Entry getEntry( String name )
    {
        Entry e = ( Entry ) name2entry.get( name );
        if ( e == null )
        {
            return null;
        }
        return e;
    }
    
    public IoFilter get( String name )
    {
        Entry e = getEntry( name );
        if( e == null )
        {
            return null;
        }
        
        return e.getFilter();
    }
    
    public NextFilter getNextFilter( String name )
    {
        Entry e = getEntry( name );
        if( e == null )
        {
            return null;
        }
        
        return e.getNextFilter();
    }
    
    public synchronized void addFirst( String name,
                                       IoFilter filter )
    {
        checkAddable( name );
        register( head, name, filter );
    }

    public synchronized void addLast( String name,
                                      IoFilter filter )
    {
        checkAddable( name );
        register( tail.prevEntry, name, filter );
    }

    public synchronized void addBefore( String baseName,
                                        String name,
                                        IoFilter filter )
    {
        EntryImpl baseEntry = checkOldName( baseName );
        checkAddable( name );
        register( baseEntry.prevEntry, name, filter );
    }

    public synchronized void addAfter( String baseName,
                                       String name,
                                       IoFilter filter )
    {
        EntryImpl baseEntry = checkOldName( baseName );
        checkAddable( name );
        register( baseEntry, name, filter );
    }

    public synchronized IoFilter remove( String name )
    {
        EntryImpl entry = checkOldName( name );
        deregister( entry );
        return entry.getFilter();
    }

    public synchronized void clear() throws Exception
    {
        Iterator it = new ArrayList( name2entry.keySet() ).iterator();
        while ( it.hasNext() )
        {
            this.remove( ( String ) it.next() );
        }
    }

    private void register( EntryImpl prevEntry, String name, IoFilter filter )
    {
        EntryImpl newEntry = new EntryImpl( prevEntry, prevEntry.nextEntry, name, filter );

        IoFilterLifeCycleManager lifeCycleManager = IoFilterLifeCycleManager.getInstance();
        
        synchronized( lifeCycleManager )
        {
            lifeCycleManager.callInitIfNecessary( filter );

            try
            {
                lifeCycleManager.callOnPreAdd( this, name, filter, newEntry.getNextFilter() );
            }
            finally
            {
                lifeCycleManager.callDestroyIfNecessary( filter );
            }

            prevEntry.nextEntry.prevEntry = newEntry;
            prevEntry.nextEntry = newEntry;
            name2entry.put( name, newEntry );

            try
            {
                lifeCycleManager.callOnPostAdd( this, name, filter, newEntry.getNextFilter() );
            }
            catch( IoFilterLifeCycleException e )
            {
                deregister0( newEntry );
                throw e;
            }
            finally
            {
                lifeCycleManager.callDestroyIfNecessary( filter );
            }
        }
    }
    
    private void deregister( EntryImpl entry )
    {
        IoFilter filter = entry.getFilter();
        IoFilterLifeCycleManager lifeCycleManager = IoFilterLifeCycleManager.getInstance();
        
        lifeCycleManager.callOnPreRemove( this, entry.getName(), filter, entry.getNextFilter() );
        
        deregister0( entry );

        try
        {
            lifeCycleManager.callOnPostRemove(
                    this, entry.getName(), filter, entry.getNextFilter() );
        }
        finally
        {
            lifeCycleManager.callDestroyIfNecessary( filter );
        }
    }

    private void deregister0( EntryImpl entry )
    {
        EntryImpl prevEntry = entry.prevEntry;
        EntryImpl nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;

        name2entry.remove( entry.name );
    }

    /**
     * Throws an exception when the specified filter name is not registered in this chain.
     *
     * @return An filter entry with the specified name.
     */
    private EntryImpl checkOldName( String baseName )
    {
        EntryImpl e = ( EntryImpl ) name2entry.get( baseName );
        if ( e == null )
        {
            throw new IllegalArgumentException( "Unknown filter name:" +
                    baseName );
        }
        return e;
    }


    /**
     * Checks the specified filter name is already taken and throws an exception if already taken.
     */
    private void checkAddable( String name )
    {
        if ( name2entry.containsKey( name ) )
        {
            throw new IllegalArgumentException( "Other filter is using the same name '" + name + "'" );
        }
    }

    public void sessionCreated( IoSession session )
    {
        Entry head = this.head;
        callNextSessionCreated(head, session);
    }

    private void callNextSessionCreated( Entry entry, IoSession session )
    {
        try
        {
            entry.getFilter().sessionCreated( entry.getNextFilter(), session );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
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
            entry.getFilter().sessionOpened( entry.getNextFilter(), session );
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
            entry.getFilter().sessionClosed( entry.getNextFilter(), session );
                
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
            entry.getFilter().sessionIdle( entry.getNextFilter(), session, status );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void messageReceived( IoSession session, Object message )
    {
        Entry head = this.head;
        callNextMessageReceived(head, session, message );
    }

    private void callNextMessageReceived( Entry entry,
                                          IoSession session,
                                          Object message )
    {
        try
        {
            entry.getFilter().messageReceived( entry.getNextFilter(), session, message );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void messageSent( IoSession session, Object message )
    {
        Entry head = this.head;
        callNextMessageSent(head, session, message);
    }

    private void callNextMessageSent( Entry entry,
                                      IoSession session,
                                      Object message ) 
    {
        try
        {
            entry.getFilter().messageSent( entry.getNextFilter(), session, message );
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
            entry.getFilter().exceptionCaught( entry.getNextFilter(), session, cause );
        }
        catch( Throwable e )
        {
            SessionLog.warn(
                    session,
                    "Unexpected exception from exceptionCaught handler.", e );
        }
    }
    
    public void filterWrite( IoSession session, WriteRequest writeRequest )
    {
        Entry tail = this.tail;
        callPreviousFilterWrite( tail, session, writeRequest );
    }

    private void callPreviousFilterWrite( Entry entry,
                                          IoSession session,
                                          WriteRequest writeRequest )
    {
        try
        {
            entry.getFilter().filterWrite( entry.getNextFilter(), session, writeRequest );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public void filterClose( IoSession session, CloseFuture closeFuture )
    {
        Entry tail = this.tail;
        callPreviousFilterClose( tail, session, closeFuture );
    }

    private void callPreviousFilterClose( Entry entry,
                                          IoSession session,
                                          CloseFuture closeFuture )
    {
        try
        {
            entry.getFilter().filterClose( entry.getNextFilter(), session, closeFuture );
        }
        catch( Throwable e )
        {
            exceptionCaught( session, e );
        }
    }

    public List getAll()
    {
        List list = new ArrayList();
        EntryImpl e = head.nextEntry;
        while( e != tail )
        {
            list.add( e );
            e = e.nextEntry;
        }

        return list;
    }

    public List getAllReversed()
    {
        List list = new ArrayList();
        EntryImpl e = tail.prevEntry;
        while( e != head )
        {
            list.add( e );
            e = e.prevEntry;
        }
        return list;
    }
    
    public boolean contains( String name )
    {
        return getEntry( name ) != null;
    }

    public boolean contains( IoFilter filter )
    {
        EntryImpl e = head.nextEntry;
        while( e != tail )
        {
            if( e.getFilter() == filter )
            {
                return true;
            }
            e = e.nextEntry;
        }
        return false;
    }

    public boolean contains( Class filterType )
    {
        EntryImpl e = head.nextEntry;
        while( e != tail )
        {
            if( filterType.isAssignableFrom( e.getFilter().getClass() ) )
            {
                return true;
            }
            e = e.nextEntry;
        }
        return false;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "{ " );
        
        boolean empty = true;
        
        EntryImpl e = head.nextEntry;
        while( e != tail )
        {
            if( !empty )
            {
                buf.append( ", " );
            }
            else
            {
                empty = false;
            }
            
            buf.append( '(' );
            buf.append( e.getName() );
            buf.append( ':' );
            buf.append( e.getFilter() );
            buf.append( ')' );

            e = e.nextEntry;
        }

        if( empty )
        {
            buf.append( "empty" );
        }
        
        buf.append( " }" );
        
        return buf.toString();
    }

    
    protected void finalize() throws Throwable
    {
        try
        {
            this.clear();
        }
        finally
        {
            super.finalize();
        }
    }

    protected abstract void doWrite( IoSession session, WriteRequest writeRequest ) throws Exception;
    
    protected abstract void doClose( IoSession session, CloseFuture closeFuture ) throws Exception;

    private class EntryImpl implements Entry
    {
        private EntryImpl prevEntry;

        private EntryImpl nextEntry;

        private final String name;
        
        private final IoFilter filter;

        private final NextFilter nextFilter;
        
        private EntryImpl( EntryImpl prevEntry, EntryImpl nextEntry,
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
                public void sessionCreated( IoSession session )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionCreated( nextEntry, session );
                }

                public void sessionOpened( IoSession session )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionOpened( nextEntry, session );
                }

                public void sessionClosed( IoSession session )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionClosed( nextEntry, session );
                }

                public void sessionIdle( IoSession session, IdleStatus status )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextSessionIdle( nextEntry, session, status );
                }

                public void exceptionCaught( IoSession session,
                                            Throwable cause )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextExceptionCaught( nextEntry, session, cause );
                }

                public void messageReceived( IoSession session, Object message )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextMessageReceived( nextEntry, session, message );
                }

                public void messageSent( IoSession session, Object message )
                {
                    Entry nextEntry = EntryImpl.this.nextEntry;
                    callNextMessageSent( nextEntry, session, message );
                }
                
                public void filterWrite( IoSession session, WriteRequest writeRequest )
                {
                    Entry nextEntry = EntryImpl.this.prevEntry;
                    callPreviousFilterWrite( nextEntry, session, writeRequest );
                }

                public void filterClose( IoSession session, CloseFuture closeFuture )
                {
                    Entry nextEntry = EntryImpl.this.prevEntry;
                    callPreviousFilterClose( nextEntry, session, closeFuture );
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
        
        public NextFilter getNextFilter()
        {
            return nextFilter;
        }
    }
}
