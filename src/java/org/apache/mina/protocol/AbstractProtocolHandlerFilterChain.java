package org.apache.mina.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.FilterChainType;
import org.apache.mina.common.IdleStatus;

public abstract class AbstractProtocolHandlerFilterChain implements ProtocolHandlerFilterChain
{
    private final ProtocolHandlerFilter HEAD_FILTER = new ProtocolHandlerFilter()
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
            if( AbstractProtocolHandlerFilterChain.this.parent == null )
            {
                // write only when root filter chain traversal is finished.
                doWrite( session, message );
            }
        }
    };
    
    private static final ProtocolHandlerFilter TAIL_FILTER = new ProtocolHandlerFilter()
    {
        public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
        {
            session.getHandler().sessionOpened( session );
        }

        public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
        {
            session.getHandler().sessionClosed( session );
        }

        public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                IdleStatus status )
        {
            session.getHandler().sessionIdle( session, status );
        }

        public void exceptionCaught( NextFilter nextFilter,
                                    ProtocolSession session, Throwable cause )
        {
            session.getHandler().exceptionCaught( session, cause );
        }

        public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
        {
            ProtocolHandler handler = session.getHandler();
            handler.messageReceived( session, message );
        }

        public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                 Object message )
        {
            session.getHandler().messageSent( session, message );
        }

        public void filterWrite( NextFilter nextFilter,
                                 ProtocolSession session, Object message )
        {
            nextFilter.filterWrite( session, message );
        }
    };

    private AbstractProtocolHandlerFilterChain parent;
    
    private final FilterChainType type; 
    
    private final Map name2entry = new HashMap();

    private final Map filter2entry = new IdentityHashMap();

    private final Entry head;

    private final Entry tail;

    protected AbstractProtocolHandlerFilterChain( FilterChainType type )
    {
        if( type == null )
        {
            throw new NullPointerException( "type" );
        }
        
        this.type = type;
        
        head = new Entry( null, null, "head", HEAD_FILTER );
        tail = new Entry( head, null, "tail", TAIL_FILTER );
        head.nextEntry = tail;
    }
    
    public ProtocolHandlerFilterChain getRoot()
    {
        AbstractProtocolHandlerFilterChain current = this;
        while( current.parent != null )
        {
            current = current.parent;
        }
        return current;
    }
    
    public ProtocolHandlerFilterChain getParent()
    {
        return parent;
    }
    
    public FilterChainType getType()
    {
        return type;
    }
    
    public ProtocolHandlerFilter getChild( String name )
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
                                       ProtocolHandlerFilter filter )
    {
        checkAddable( name, filter );
        register( head, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name at the end of this chain.
     */
    public synchronized void addLast( String name,
                                      ProtocolHandlerFilter filter )
    {
        checkAddable( name, filter );
        register( tail.prevEntry, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name just before the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addBefore( String baseName,
                                        String name,
                                        ProtocolHandlerFilter filter )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name, filter );
        register( baseEntry, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name just after the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addAfter( String baseName,
                                       String name,
                                       ProtocolHandlerFilter filter )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name, filter );
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
        ProtocolHandlerFilter filter = entry.filter;
        filter2entry.remove( filter );
        if ( filter instanceof AbstractProtocolHandlerFilterChain )
        {
            ( ( AbstractProtocolHandlerFilterChain ) filter ).parent = null;
        }
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

    private void register( Entry prevEntry, String name, ProtocolHandlerFilter filter )
    {
        Entry newEntry = new Entry( prevEntry, prevEntry.nextEntry, name, filter );
        prevEntry.nextEntry.prevEntry = newEntry;
        prevEntry.nextEntry = newEntry;
        name2entry.put( name, newEntry );
        filter2entry.put( filter, newEntry );
        if ( filter instanceof AbstractProtocolHandlerFilterChain )
        {
            ( ( AbstractProtocolHandlerFilterChain ) filter ).parent = this;
        }
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
    private void checkAddable( String name, ProtocolHandlerFilter filter )
    {
        if ( name2entry.containsKey( name ) )
        {
            throw new IllegalArgumentException( "Other interceptor is using name '" + name + "'" );
        }

        if ( filter instanceof AbstractProtocolHandlerFilterChain )
        {
            if ( ( ( AbstractProtocolHandlerFilterChain ) filter ).parent != null )
            {
                throw new IllegalArgumentException( "This interceptor chain has its parent already." );
            }
        }
    }

    public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
    {
        Entry head = this.head;
        callNextSessionOpened(head, nextFilter, session);
    }

    private void callNextSessionOpened( Entry entry,
                                        NextFilter nextFilter, ProtocolSession session)
    {
        try
        {
            if( nextFilter == null )
            {
                entry.filter.sessionOpened( entry.nextFilter, session );
            }
            else if ( type == FilterChainType.PREPROCESS )
            {
                entry.filter.sessionOpened( entry.nextFilter, session );
                nextFilter.sessionOpened( session );
            }
            else // POSTPROCESS
            {
                nextFilter.sessionOpened( session );
                entry.filter.sessionOpened( entry.nextFilter, session );
            }
                
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
    {
        Entry head = this.head;
        callNextSessionClosed(head, nextFilter, session);
    }

    private void callNextSessionClosed( Entry entry,
                                        NextFilter nextFilter, ProtocolSession session )
    {
        try
        {
            if( nextFilter == null )
            {
                entry.filter.sessionClosed( entry.nextFilter, session );
            }
            else if( type == FilterChainType.PREPROCESS )
            {
                entry.filter.sessionClosed( entry.nextFilter, session );
                nextFilter.sessionClosed( session );
            }
            else // POSTPROCESS
            {
                nextFilter.sessionClosed( session );
                entry.filter.sessionClosed( entry.nextFilter, session );
            }
                
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void sessionIdle( NextFilter nextFilter, ProtocolSession session, IdleStatus status )
    {
        Entry head = this.head;
        callNextSessionIdle(head, nextFilter, session, status);
    }

    private void callNextSessionIdle( Entry entry,
                                      NextFilter nextFilter, ProtocolSession session,
                                      IdleStatus status )
    {
        try
        {
            if( nextFilter == null )
            {
                entry.filter.sessionIdle( entry.nextFilter, session, status );
            }
            else if( type == FilterChainType.PREPROCESS )
            {
                entry.filter.sessionIdle( entry.nextFilter, session, status );
                nextFilter.sessionIdle( session, status );
            }
            else // POSTPROCESS
            {
                nextFilter.sessionIdle( session, status );
                entry.filter.sessionIdle( entry.nextFilter, session, status );
            }
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void messageReceived( NextFilter nextFilter, ProtocolSession session, Object message )
    {
        Entry head = this.head;
        callNextMessageReceived(head, nextFilter, session, message );
    }

    private void callNextMessageReceived( Entry entry,
                                   NextFilter nextFilter, ProtocolSession session,
                                   Object message )
    {
        try
        {
            if( nextFilter == null )
            {
                entry.filter.messageReceived( entry.nextFilter, session, message );
            }
            else if( type == FilterChainType.PREPROCESS )
            {
                entry.filter.messageReceived( entry.nextFilter, session, message );
                nextFilter.messageReceived( session, message );
            }
            else // POSTPROCESS
            {
                nextFilter.messageReceived( session, message );
                entry.filter.messageReceived( entry.nextFilter, session, message );
            }
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void messageSent( NextFilter nextFilter, ProtocolSession session, Object message )
    {
        Entry head = this.head;
        callNextMessageSent(head, nextFilter, session, message);
    }

    private void callNextMessageSent( Entry entry,
                                      NextFilter nextFilter, ProtocolSession session,
                                      Object message ) 
    {
        try
        {
            if( nextFilter == null )
            {
                entry.filter.messageSent( entry.nextFilter, session, message );
            }
            else if( type == FilterChainType.PREPROCESS )
            {
                entry.filter.messageSent( entry.nextFilter, session, message );
                nextFilter.messageSent( session, message );
            }
            else // POSTPROCESS
            {
                nextFilter.messageSent( session, message );
                entry.filter.messageSent( entry.nextFilter, session, message );
            }
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
        }
    }

    public void exceptionCaught( NextFilter nextFilter, ProtocolSession session, Throwable cause )
    {
        Entry head = this.head;
        callNextExceptionCaught(head, nextFilter, session, cause);
    }

    private void callNextExceptionCaught( Entry entry,
                                          NextFilter nextFilter, ProtocolSession session,
                                          Throwable cause )
    {
        try
        {
            if( nextFilter == null )
            {
                entry.filter.exceptionCaught( entry.nextFilter, session, cause );
            }
            else if( type == FilterChainType.PREPROCESS )
            {
                entry.filter.exceptionCaught( entry.nextFilter, session, cause );
                nextFilter.exceptionCaught( session, cause );
            }
            else // POSTPROCESS
            {
                entry.filter.exceptionCaught( entry.nextFilter, session, cause );
                nextFilter.exceptionCaught( session, cause );
            }
        }
        catch( Throwable e )
        {
            e.printStackTrace();
        }
    }
    
    public void filterWrite( NextFilter nextFilter,
                             ProtocolSession session, Object message )
    {
        Entry tail = this.tail;
        callPreviousFilterWrite( tail, nextFilter, session, message );
    }

    private void callPreviousFilterWrite( Entry entry,
                                          NextFilter prevFilter, ProtocolSession session,
                                          Object message )
    {
        if( message == null )
        {
            return;
        }
        
        try
        {
            if( prevFilter == null )
            {
                entry.filter.filterWrite( entry.prevFilter, session, message );
            }
            else if( type == FilterChainType.PREPROCESS )
            {
                entry.filter.filterWrite( entry.prevFilter, session, message );
                prevFilter.filterWrite( session, message );
            }
            else // POSTPROCESS
            {
                entry.filter.filterWrite( entry.prevFilter, session, message );
                prevFilter.filterWrite( session, message );
            }
        }
        catch( Throwable e )
        {
            fireExceptionCaught( session, e );
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
        while( e != null );

        return list;

    }
    
    private void fireExceptionCaught( ProtocolSession session, Throwable cause )
    {
        try
        {
            getRoot().exceptionCaught( null, session, cause );
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }
    
    protected abstract void doWrite( ProtocolSession session, Object message );

    private class Entry
    {
        private Entry prevEntry;

        private Entry nextEntry;

        private final String name;
        
        private final ProtocolHandlerFilter filter;

        private final NextFilter nextFilter;
        
        private final NextFilter prevFilter;

        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, ProtocolHandlerFilter filter )
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
                    callNextSessionOpened( nextEntry, null, session );
                }

                public void sessionClosed( ProtocolSession session )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionClosed( nextEntry, null, session );
                }

                public void sessionIdle( ProtocolSession session, IdleStatus status )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextSessionIdle( nextEntry, null, session, status );
                }

                public void exceptionCaught( ProtocolSession session,
                                            Throwable cause )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextExceptionCaught( nextEntry, null, session, cause );
                }

                public void messageReceived( ProtocolSession session, Object message )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextMessageReceived( nextEntry, null, session, message );
                }

                public void messageSent( ProtocolSession session, Object message )
                {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextMessageSent( nextEntry, null, session, message );
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
                    callPreviousFilterWrite( nextEntry, null, session, message );
                }
            };
        }
        
        public String getName()
        {
            return name;
        }
        
        public ProtocolHandlerFilter getFilter()
        {
            return filter;
        }
    }
}
