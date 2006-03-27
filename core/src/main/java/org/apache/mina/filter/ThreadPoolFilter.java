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
package org.apache.mina.filter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.BlockingQueue;
import org.apache.mina.util.ByteBufferUtil;
import org.apache.mina.util.IdentityHashSet;
import org.apache.mina.util.Queue;
import org.apache.mina.util.Stack;

/**
 * A Thread-pooling filter.  This filter forwards {@link IoHandler} events
 * to its thread pool.
 * <p>
 * This is an implementation of
 * <a href="http://deuce.doc.wustl.edu/doc/pspdfs/lf.pdf">Leader/Followers
 * thread pool</a> by Douglas C. Schmidt et al.
 * </p>
 * <p>
 * Use the {@link #start()} and {@link #stop()} methods to force this filter
 * to start/stop processing events. Alternatively, {@link #start()} will be
 * called automatically the first time an instance of this filter is added
 * to a filter chain. Calling {@link #stop()} is not required either since
 * all workers are daemon threads which means that any workers still alive
 * when the JVM terminates will die automatically.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ThreadPoolFilter extends IoFilterAdapter
{
    /**
     * Default maximum size of thread pool (16).
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = 16;

    /**
     * Default keep-alive time of thread pool (1 min).
     */
    public static final int DEFAULT_KEEP_ALIVE_TIME = 60 * 1000;

    /**
     * A queue which contains {@link Integer}s which represents reusable
     * thread IDs.  {@link Worker} first checks this queue and then
     * uses {@link #threadId} when no reusable thread ID is available.
     */
    private static final Queue threadIdReuseQueue = new Queue();
    private static int threadId = 0;
    
    private static int acquireThreadId()
    {
        synchronized( threadIdReuseQueue )
        {
            Integer id = ( Integer ) threadIdReuseQueue.pop();
            if( id == null )
            {
                return ++ threadId;
            }
            else
            {
                return id.intValue();
            }
        }
    }
    
    private static void releaseThreadId( int id )
    {
        synchronized( threadIdReuseQueue )
        {
            threadIdReuseQueue.push( new Integer( id ) );
        }
    }

    private String threadNamePrefix;
    private final Map buffers = new IdentityHashMap();
    private final BlockingQueue unfetchedSessionBuffers = new BlockingQueue();
    private final Set allSessionBuffers = new IdentityHashSet();

    private Worker leader;
    private final Stack followers = new Stack();
    private final Set allWorkers = new IdentityHashSet();

    private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
    private int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;

    private boolean shuttingDown;

    private int poolSize;
    private final Object poolSizeLock = new Object();

    /**
     * Creates a new instance of this filter with default thread pool settings.
     */
    public ThreadPoolFilter()
    {
        this( "IoThreadPool" );
    }
    
    /**
     * Creates a new instance of this filter with the specified thread name prefix
     * and other default settings.
     * @param threadNamePrefix the prefix of the thread names this pool will create.
     */
    public ThreadPoolFilter( String threadNamePrefix )
    {
        setThreadNamePrefix( threadNamePrefix );
    }
    
    public String getThreadNamePrefix()
    {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix( String threadNamePrefix )
    {
        if( threadNamePrefix == null )
        {
            throw new NullPointerException( "threadNamePrefix" );
        }
        threadNamePrefix = threadNamePrefix.trim();
        if( threadNamePrefix.length() == 0 )
        {
            throw new IllegalArgumentException( "threadNamePrefix is empty." );
        }
        this.threadNamePrefix = threadNamePrefix;
        
        synchronized( poolSizeLock )
        {
            for( Iterator i = allWorkers.iterator(); i.hasNext(); )
            {
                ( ( Worker ) i.next() ).updateName();
            }
        }
    }
    
    public int getPoolSize()
    {
        synchronized( poolSizeLock )
        {
            return poolSize;
        }
    }

    public int getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    public int getKeepAliveTime()
    {
        return keepAliveTime;
    }

    public void setMaximumPoolSize( int maximumPoolSize )
    {
        if( maximumPoolSize <= 0 )
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
    }

    public void setKeepAliveTime( int keepAliveTime )
    {
        this.keepAliveTime = keepAliveTime;
    }

    public void onPreAdd( IoFilterChain parent, String name, NextFilter nextFilter ) 
        throws Exception
    {
        if( leader == null )
        {
            start();
        }
    }

    /**
     * Starts handling new events. This will be called automatically if not
     * already called the first time this filter is added to a filter chain. 
     */
    public void start()
    {
        shuttingDown = false;
        leader = new Worker();
        leader.start();
        leader.lead();
    }

    /**
     * Stops handling events and terminates all active worker threads.
     */
    public void stop()
    {
        shuttingDown = true;
        int expectedPoolSize = 0;
        while( getPoolSize() != expectedPoolSize )
        {
            List allWorkers;
            synchronized( poolSizeLock )
            {
                allWorkers = new ArrayList( this.allWorkers );
            }
            
            // You may not interrupt the current thread.
            if( allWorkers.remove( Thread.currentThread() ) )
            {
                expectedPoolSize = 1;
            }
            
            for( Iterator i = allWorkers.iterator(); i.hasNext(); )
            {
                Worker worker = ( Worker ) i.next();
                while( worker.isAlive() )
                {
                    worker.interrupt();
                    try
                    {
                        // This timeout will help us from 
                        // infinite lock-up and interrupt workers again.
                        worker.join( 100 );
                    }
                    catch( InterruptedException e )
                    {
                    }
                }
            }
        }
        
        this.allSessionBuffers.clear();
        this.unfetchedSessionBuffers.clear();
        this.buffers.clear();
        this.followers.clear();
        this.leader = null;
    }

    private void increasePoolSize( Worker worker )
    {
        synchronized( poolSizeLock )
        {
            poolSize++;
            allWorkers.add( worker );
        }
    }

    private void decreasePoolSize( Worker worker )
    {
        synchronized( poolSizeLock )
        {
            poolSize--;
            allWorkers.remove( worker );
        }
    }

    private void fireEvent( NextFilter nextFilter, IoSession session,
                              EventType type, Object data )
    {
        final BlockingQueue unfetchedSessionBuffers = this.unfetchedSessionBuffers;
        final Set allSessionBuffers = this.allSessionBuffers;
        final Event event = new Event( type, nextFilter, data );

        synchronized( unfetchedSessionBuffers )
        {
            final SessionBuffer buf = getSessionBuffer( session );
            final Queue eventQueue = buf.eventQueue;

            synchronized( buf )
            {
                eventQueue.push( event );
            }

            if( !allSessionBuffers.contains( buf ) )
            {
                allSessionBuffers.add( buf );
                unfetchedSessionBuffers.push( buf );
            }
        }
    }
    
    /**
     * Implement this method to fetch (or pop) a {@link SessionBuffer} from
     * the given <tt>unfetchedSessionBuffers</tt>.  The default implementation
     * simply pops the buffer from it.  You could prioritize the fetch order.
     * 
     * @return A non-null {@link SessionBuffer}
     */
    protected SessionBuffer fetchSessionBuffer( Queue unfetchedSessionBuffers )
    {
        return ( SessionBuffer ) unfetchedSessionBuffers.pop();
    }

    private SessionBuffer getSessionBuffer( IoSession session )
    {
        final Map buffers = this.buffers;
        SessionBuffer buf;
        synchronized( buffers )
        {
            buf = ( SessionBuffer ) buffers.get( session );
            if( buf == null )
            {
                buf = new SessionBuffer( session );
                buffers.put( session, buf );
            }
        }
        return buf;
    }

    private void removeSessionBuffer( SessionBuffer buf )
    {
        final Map buffers = this.buffers;
        final IoSession session = buf.session;
        synchronized( buffers )
        {
            buffers.remove( session );
        }
    }

    protected static class SessionBuffer
    {
        private final IoSession session;

        private final Queue eventQueue = new Queue();

        private SessionBuffer( IoSession session )
        {
            this.session = session;
        }
        
        public IoSession getSession()
        {
            return session;
        }
        
        public Queue getEventQueue()
        {
            return eventQueue;
        }
    }

    private class Worker extends Thread
    {
        private final int id;
        private final Object promotionLock = new Object();
        private boolean dead;

        private Worker()
        {
            int id = acquireThreadId();
            this.id = id;
            updateName();
            increasePoolSize( this );
            setDaemon( true );
        }
        
        public void updateName()
        {
            this.setName( threadNamePrefix + '-' + id );
        }

        public boolean lead()
        {
            final Object promotionLock = this.promotionLock;
            synchronized( promotionLock )
            {
                if( dead )
                {
                    return false;
                }

                leader = this;
                promotionLock.notify();
            }
            
            return true;
        }

        public void run()
        {
            for( ;; )
            {
                if( !waitForPromotion() )
                    break;

                SessionBuffer buf = fetchBuffer();
                giveUpLead();
                if( buf == null )
                {
                    break;
                }

                processEvents( buf );
                follow();
                releaseBuffer( buf );
            }

            decreasePoolSize( this );
            releaseThreadId( id );
        }

        private SessionBuffer fetchBuffer()
        {
            BlockingQueue unfetchedSessionBuffers = ThreadPoolFilter.this.unfetchedSessionBuffers;
            synchronized( unfetchedSessionBuffers )
            {
                while( !shuttingDown )
                {
                    try
                    {
                        unfetchedSessionBuffers.waitForNewItem();
                    }
                    catch( InterruptedException e )
                    {
                        continue;
                    }

                    return ThreadPoolFilter.this.fetchSessionBuffer( unfetchedSessionBuffers );
                }
            }

            return null;
        }

        private void processEvents( SessionBuffer buf )
        {
            final IoSession session = buf.session;
            final Queue eventQueue = buf.eventQueue;
            for( ;; )
            {
                Event event;
                synchronized( buf )
                {
                    event = ( Event ) eventQueue.pop();
                    if( event == null )
                        break;
                }
                processEvent( event.getNextFilter(), session,
                              event.getType(), event.getData() );
            }
        }

        private void follow()
        {
            final Object promotionLock = this.promotionLock;
            final Stack followers = ThreadPoolFilter.this.followers;
            synchronized( promotionLock )
            {
                if( this != leader )
                {
                    synchronized( followers )
                    {
                        followers.push( this );
                    }
                }
            }
        }

        private void releaseBuffer( SessionBuffer buf )
        {
            final BlockingQueue unfetchedSessionBuffers = ThreadPoolFilter.this.unfetchedSessionBuffers;
            final Set allSessionBuffers = ThreadPoolFilter.this.allSessionBuffers;
            final Queue eventQueue = buf.eventQueue;

            synchronized( unfetchedSessionBuffers )
            {
                if( eventQueue.isEmpty() )
                {
                    allSessionBuffers.remove( buf );
                    removeSessionBuffer( buf );
                }
                else
                {
                    unfetchedSessionBuffers.push( buf );
                }
            }
        }

        private boolean waitForPromotion()
        {
            final Object promotionLock = this.promotionLock;

            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            
            synchronized( promotionLock )
            {
                while( this != leader && !shuttingDown )
                {
                    // Calculate remaining keep-alive time
                    int keepAliveTime = getKeepAliveTime();
                    if( keepAliveTime > 0 )
                    {
                        keepAliveTime -= ( currentTime - startTime );
                    }
                    else
                    {
                        keepAliveTime = Integer.MAX_VALUE;
                    }
                    
                    // Break the loop if there's no remaining keep-alive time.
                    if( keepAliveTime <= 0 )
                    {
                        break;
                    }

                    // Wait for promotion
                    try
                    {
                        promotionLock.wait( keepAliveTime );
                    }
                    catch( InterruptedException e )
                    {
                    }

                    // Update currentTime for the next iteration
                    currentTime = System.currentTimeMillis();
                }

                boolean timeToLead = this == leader && !shuttingDown;

                if( !timeToLead )
                {
                    // time to die
                    synchronized( followers )
                    {
                        followers.remove( this );
                    }

                    // Mark as dead explicitly when we've got promotionLock.
                    dead = true;
                }

                return timeToLead;
            }
        }

        private void giveUpLead()
        {
            final Stack followers = ThreadPoolFilter.this.followers;
            Worker worker;
            do
            {
                synchronized( followers )
                {
                    worker = ( Worker ) followers.pop();
                }

                if( worker == null )
                {
                    // Increase the number of threads if we
                    // are not shutting down and we can increase the number.
                    if( !shuttingDown
                        && getPoolSize() < getMaximumPoolSize() )
                    {
                        worker = new Worker();
                        worker.lead();
                        worker.start();
                    }

                    // This loop should end because:
                    // 1) lead() is called already,
                    // 2) or it is shutting down and there's no more threads left.
                    break;
                }
            }
            while( !worker.lead() );
        }
    }

    protected static class EventType
    {
        public static final EventType OPENED = new EventType( "OPENED" );

        public static final EventType CLOSED = new EventType( "CLOSED" );

        public static final EventType READ = new EventType( "READ" );

        public static final EventType WRITTEN = new EventType( "WRITTEN" );

        public static final EventType RECEIVED = new EventType( "RECEIVED" );

        public static final EventType SENT = new EventType( "SENT" );

        public static final EventType IDLE = new EventType( "IDLE" );

        public static final EventType EXCEPTION = new EventType( "EXCEPTION" );

        private final String value;
        
        private EventType( String value )
        {
            this.value = value;
        }
        
        public String toString()
        {
            return value;
        }
    }
    
    protected static class Event
    {
        private final EventType type;
        private final NextFilter nextFilter;
        private final Object data;
        
        public Event( EventType type, NextFilter nextFilter, Object data )
        {
            this.type = type;
            this.nextFilter = nextFilter;
            this.data = data;
        }

        public Object getData()
        {
            return data;
        }
        

        public NextFilter getNextFilter()
        {
            return nextFilter;
        }
        

        public EventType getType()
        {
            return type;
        }
    }
    
    public void sessionCreated( NextFilter nextFilter, IoSession session )
    {
        nextFilter.sessionCreated( session );
    }
    
    public void sessionOpened( NextFilter nextFilter,
                              IoSession session )
    {
        fireEvent( nextFilter, session, EventType.OPENED, null );
    }

    public void sessionClosed( NextFilter nextFilter,
                              IoSession session )
    {
        fireEvent( nextFilter, session, EventType.CLOSED, null );
    }

    public void sessionIdle( NextFilter nextFilter,
                            IoSession session, IdleStatus status )
    {
        fireEvent( nextFilter, session, EventType.IDLE, status );
    }

    public void exceptionCaught( NextFilter nextFilter,
                                IoSession session, Throwable cause )
    {
        fireEvent( nextFilter, session, EventType.EXCEPTION, cause );
    }

    public void messageReceived( NextFilter nextFilter,
                                 IoSession session, Object message )
    {
        ByteBufferUtil.acquireIfPossible( message );
        fireEvent( nextFilter, session, EventType.RECEIVED, message );
    }

    public void messageSent( NextFilter nextFilter,
                             IoSession session, Object message )
    {
        ByteBufferUtil.acquireIfPossible( message );
        fireEvent( nextFilter, session, EventType.SENT, message );
    }

    protected void processEvent( NextFilter nextFilter,
                                 IoSession session, EventType type,
                                 Object data )
    {
        if( type == EventType.RECEIVED )
        {
            nextFilter.messageReceived( session, data );
            ByteBufferUtil.releaseIfPossible( data );
        }
        else if( type == EventType.SENT )
        {
            nextFilter.messageSent( session, data );
            ByteBufferUtil.releaseIfPossible( data );
        }
        else if( type == EventType.EXCEPTION )
        {
            nextFilter.exceptionCaught( session, ( Throwable ) data );
        }
        else if( type == EventType.IDLE )
        {
            nextFilter.sessionIdle( session, ( IdleStatus ) data );
        }
        else if( type == EventType.OPENED )
        {
            nextFilter.sessionOpened( session );
        }
        else if( type == EventType.CLOSED )
        {
            nextFilter.sessionClosed( session );
        }
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest )
    {
        nextFilter.filterWrite( session, writeRequest );
    }

    public void filterClose( NextFilter nextFilter, IoSession session ) throws Exception
    {
        nextFilter.filterClose( session );
    }
}