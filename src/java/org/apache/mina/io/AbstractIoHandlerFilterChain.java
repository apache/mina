package org.apache.mina.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoHandlerFilter.NextFilter;

public abstract class AbstractIoHandlerFilterChain implements IoHandlerFilterChain
{
    private final IoHandlerFilter HEAD_FILTER = new IoHandlerFilter()
    {
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

        public void dataRead( NextFilter nextFilter, IoSession session,
                             ByteBuffer buf )
        {
            nextFilter.dataRead( session, buf );
        }

        public void dataWritten( NextFilter nextFilter, IoSession session,
                                Object marker )
        {
            nextFilter.dataWritten( session, marker );
        }
        
        public void filterWrite( NextFilter nextFilter, IoSession session,
                                 ByteBuffer buf, Object marker )
        {
            doWrite( session, buf, marker );
        }
    };
    
    private final IoHandlerFilter TAIL_FILTER = new IoHandlerFilter()
    {
        public void sessionOpened( NextFilter nextFilter, IoSession session )
        {
            session.getHandler().sessionOpened( session );
        }

        public void sessionClosed( NextFilter nextFilter, IoSession session )
        {
            session.getHandler().sessionClosed( session );
        }

        public void sessionIdle( NextFilter nextFilter, IoSession session,
                                IdleStatus status )
        {
            session.getHandler().sessionIdle( session, status );
        }

        public void exceptionCaught( NextFilter nextFilter,
                                    IoSession session, Throwable cause )
        {
            session.getHandler().exceptionCaught( session, cause );
        }

        public void dataRead( NextFilter nextFilter, IoSession session,
                             ByteBuffer buf )
        {
            IoHandler handler = session.getHandler();
            handler.dataRead( session, buf );
            buf.release();
        }

        public void dataWritten( NextFilter nextFilter, IoSession session,
                                Object marker )
        {
            session.getHandler().dataWritten( session, marker );
        }

        public void filterWrite( NextFilter nextFilter,
                                 IoSession session, ByteBuffer buf, Object marker )
        {
            nextFilter.filterWrite( session, buf, marker );
        }
    };
    
    private final Map name2entry = new HashMap();

    private final Map filter2entry = new IdentityHashMap();

    private final Entry head;
    
    private final Entry tail;

    protected AbstractIoHandlerFilterChain()
    {
        head = new Entry( null, null, "head", HEAD_FILTER );
        tail = new Entry( head, null, "tail", TAIL_FILTER );
        head.nextEntry = tail;
    }
    
    public IoHandlerFilter getChild( String name )
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
                                       IoHandlerFilter filter )
    {
        checkAddable( name );
        register( head, name, filter );
    }


    /**
     * Adds the specified interceptor with the specified name at the end of this chain.
     */
    public synchronized void addLast( String name,
                                      IoHandlerFilter filter )
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
                                        IoHandlerFilter filter )
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
                                       IoHandlerFilter filter )
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
        IoHandlerFilter filter = entry.filter;
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

    private void register( Entry prevEntry, String name, IoHandlerFilter filter )
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
            entry.filter.filterWrite( entry.prevFilter, session, buf, marker );
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
        
        private final IoHandlerFilter filter;

        private final NextFilter nextFilter;
        
        private final NextFilter prevFilter;

        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, IoHandlerFilter filter )
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
                    throw new IllegalStateException();
                }
            };
            
            this.prevFilter = new NextFilter()
            {

                public void sessionOpened( IoSession session )
                {
                    throw new IllegalStateException();
                }

                public void sessionClosed( IoSession session )
                {
                    throw new IllegalStateException();
                }

                public void sessionIdle( IoSession session, IdleStatus status )
                {
                    throw new IllegalStateException();
                }

                public void exceptionCaught( IoSession session,
                                            Throwable cause )
                {
                    throw new IllegalStateException();
                }

                public void dataRead( IoSession session, ByteBuffer buf )
                {
                    throw new IllegalStateException();
                }

                public void dataWritten( IoSession session, Object marker )
                {
                    throw new IllegalStateException();
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
        
        public IoHandlerFilter getFilter()
        {
            return filter;
        }
    }
}
