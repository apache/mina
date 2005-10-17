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

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.Session;

/**
 * A base implementation of Thread-pooling filters.
 * This filter forwards events to its thread pool.  This is an implementation of
 * <a href="http://deuce.doc.wustl.edu/doc/pspdfs/lf.pdf">Leader/Followers
 * thread pool</a> by Douglas C. Schmidt et al.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseThreadPool implements ThreadPool
{
    /**
     * Default maximum size of thread pool (2G).
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = Integer.MAX_VALUE;

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

    private final String threadNamePrefix;
    private final Map buffers = new IdentityHashMap();
    private final Stack followers = new Stack();
    private final BlockingQueue unfetchedSessionBuffers = new BlockingQueue();
    private final Set allSessionBuffers = new HashSet();

    private Worker leader;

    private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
    private int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;

    private boolean started;
    private boolean shuttingDown;

    private int poolSize;
    private final Object poolSizeLock = new Object();

    /**
     * Creates a new instance with default thread pool settings.
     * You'll have to invoke {@link #start()} method to start threads actually.
     *
     * @param threadNamePrefix the prefix of the thread names this pool will create.
     */
    public BaseThreadPool( String threadNamePrefix )
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
    }
    
    public String getThreadNamePrefix()
    {
        return threadNamePrefix;
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

    public synchronized void start()
    {
        if( started )
            return;

        shuttingDown = false;

        leader = new Worker();
        leader.start();
        leader.lead();

        started = true;
    }

    public synchronized void stop()
    {
        if( !started )
            return;

        shuttingDown = true;
        Worker lastLeader = null;
        for( ;; )
        {
            Worker leader = this.leader;
            if( lastLeader == leader )
                break;

            while( leader.isAlive() )
            {
                leader.interrupt();
                try
                {
                    leader.join();
                }
                catch( InterruptedException e )
                {
                }
            }

            lastLeader = leader;
        }

        started = false;
    }

    private void increasePoolSize()
    {
        synchronized( poolSizeLock )
        {
            poolSize++;
        }
    }

    private void decreasePoolSize()
    {
        synchronized( poolSizeLock )
        {
            poolSize--;
        }
    }

    protected void fireEvent( Object nextFilter, Session session,
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
     * Implement this method to forward events to <tt>nextFilter</tt>.
     */
    protected abstract void processEvent( Object nextFilter, Session session,
                                          EventType type, Object data );

    
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

    private SessionBuffer getSessionBuffer( Session session )
    {
        final Map buffers = this.buffers;
        SessionBuffer buf = ( SessionBuffer ) buffers.get( session );
        if( buf == null )
        {
            synchronized( buffers )
            {
                buf = ( SessionBuffer ) buffers.get( session );
                if( buf == null )
                {
                    buf = new SessionBuffer( session );
                    buffers.put( session, buf );
                }
            }
        }
        return buf;
    }

    private void removeSessionBuffer( SessionBuffer buf )
    {
        final Map buffers = this.buffers;
        final Session session = buf.session;
        synchronized( buffers )
        {
            buffers.remove( session );
        }
    }

    protected static class SessionBuffer
    {
        private final Session session;

        private final Queue eventQueue = new Queue();

        private SessionBuffer( Session session )
        {
            this.session = session;
        }
        
        public Session getSession()
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
            this.setName( threadNamePrefix + '-' + id );
            increasePoolSize();
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

            decreasePoolSize();
            releaseThreadId( id );
        }

        private SessionBuffer fetchBuffer()
        {
            BlockingQueue unfetchedSessionBuffers = BaseThreadPool.this.unfetchedSessionBuffers;
            synchronized( unfetchedSessionBuffers )
            {
                for( ;; )
                {
                    try
                    {
                        unfetchedSessionBuffers.waitForNewItem();
                    }
                    catch( InterruptedException e )
                    {
                        if( shuttingDown )
                        {
                            return null;
                        }
                        else
                        {
                            continue;
                        }
                    }

                    return BaseThreadPool.this.fetchSessionBuffer( unfetchedSessionBuffers );
                }
            }
        }

        private void processEvents( SessionBuffer buf )
        {
            final Session session = buf.session;
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
            final Stack followers = BaseThreadPool.this.followers;
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
            final BlockingQueue unfetchedSessionBuffers = BaseThreadPool.this.unfetchedSessionBuffers;
            final Set allSessionBuffers = BaseThreadPool.this.allSessionBuffers;
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

            final long startTime = System.currentTimeMillis();
            long currentTime = startTime;
            
            synchronized( promotionLock )
            {
                while( this != leader )
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

                boolean timeToLead = this == leader;

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
            final Stack followers = BaseThreadPool.this.followers;
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
}