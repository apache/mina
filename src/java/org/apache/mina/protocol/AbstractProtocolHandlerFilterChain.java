package org.apache.mina.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            if( parent == null )
            {
                // write only when root filter chain traversal is finished.
                doWrite( session, message );
            }
        }
    };
    
    private final ProtocolHandlerFilter NEXT_FILTER = new ProtocolHandlerFilter()
    {
        public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.nextFilter.sessionOpened( session );
            }
            nextFilter.sessionOpened( session );
        }

        public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.nextFilter.sessionClosed( session );
            }
            nextFilter.sessionClosed( session );
        }

        public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                IdleStatus status )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.nextFilter.sessionIdle( session, status );
            }
            nextFilter.sessionIdle( session, status );
        }

        public void exceptionCaught( NextFilter nextFilter,
                                    ProtocolSession session, Throwable cause )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.nextFilter.exceptionCaught( session, cause );
            }
            nextFilter.exceptionCaught( session, cause );
        }

        public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.nextFilter.messageReceived( session, message );
            }
            nextFilter.messageReceived( session, message );
        }

        public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                 Object message )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.nextFilter.messageSent( session, message );
            }
            nextFilter.messageSent( session, message );
        }
        
        public void filterWrite( NextFilter nextFilter, ProtocolSession session,
                                 Object message )
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.filter2entry.get( AbstractProtocolHandlerFilterChain.this );
                e.prevFilter.filterWrite( session, message );
            }
            nextFilter.filterWrite( session, message );
        }
    };
    
    private final ProtocolHandlerFilter TAIL_FILTER = new ProtocolHandlerFilter()
    {
        public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
        {
            if( parent == null )
            {
                session.getHandler().sessionOpened( session );
            }
        }

        public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
        {
            if( parent == null )
            {
                session.getHandler().sessionClosed( session );
            }
        }

        public void sessionIdle( NextFilter nextFilter, ProtocolSession session,
                                IdleStatus status )
        {
            if( parent == null )
            {
                session.getHandler().sessionIdle( session, status );
            }
        }

        public void exceptionCaught( NextFilter nextFilter,
                                    ProtocolSession session, Throwable cause )
        {
            if( parent == null )
            {
                session.getHandler().exceptionCaught( session, cause );
            }
        }

        public void messageReceived( NextFilter nextFilter, ProtocolSession session,
                                     Object message )
        {
            if( parent == null )
            {
                ProtocolHandler handler = session.getHandler();
                handler.messageReceived( session, message );
            }
        }

        public void messageSent( NextFilter nextFilter, ProtocolSession session,
                                 Object message )
        {
            if( parent == null )
            {
                session.getHandler().messageSent( session, message );
            }
        }

        public void filterWrite( NextFilter nextFilter,
                                 ProtocolSession session, Object message )
        {
            nextFilter.filterWrite( session, message );
        }
    };

    private final boolean root;
    
    private AbstractProtocolHandlerFilterChain parent;
    
    private final Map name2entry = new HashMap();

    private final Map filter2entry = new IdentityHashMap();

    private final Entry head;

    private final Entry tail;

    protected AbstractProtocolHandlerFilterChain( boolean root )
    {
        this.root = root;
        
        head = new Entry( null, null, "head", HEAD_FILTER );
        tail = new Entry( head, null, "tail", TAIL_FILTER );
        head.nextEntry = tail;
        
        if( !root )
        {
            register( head, ProtocolHandlerFilterChain.NEXT_FILTER, NEXT_FILTER );
        }
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
        if ( filter instanceof AbstractProtocolHandlerFilterChain )
        {
            if( !this.getClass().isAssignableFrom( filter.getClass() ) )
            {
                throw new IllegalArgumentException( "Incompatible chain" );
            }
            if( ( ( AbstractProtocolHandlerFilterChain ) filter ).root )
            {
                throw new IllegalArgumentException( "Root chain cannot be added." );
            }
            if( ( ( AbstractProtocolHandlerFilterChain ) filter ).parent != null )
            {
                throw new IllegalArgumentException( "Already added to other parent chain." );
            }

            ( ( AbstractProtocolHandlerFilterChain ) filter ).parent = this;
        }
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
            fireExceptionCaught( session, e );
        }
    }

    public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
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
            fireExceptionCaught( session, e );
        }
    }

    public void sessionIdle( NextFilter nextFilter, ProtocolSession session, IdleStatus status )
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
            fireExceptionCaught( session, e );
        }
    }

    public void messageReceived( NextFilter nextFilter, ProtocolSession session, Object message )
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
            fireExceptionCaught( session, e );
        }
    }

    public void messageSent( NextFilter nextFilter, ProtocolSession session, Object message )
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
            fireExceptionCaught( session, e );
        }
    }

    public void exceptionCaught( NextFilter nextFilter, ProtocolSession session, Throwable cause )
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
    
    public void filterWrite( NextFilter nextFilter,
                             ProtocolSession session, Object message )
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
        
        public ProtocolHandlerFilter getFilter()
        {
            return filter;
        }
    }
}
