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
import java.util.Iterator;
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

    private static volatile int threadId = 0;

    private final Map buffers = new IdentityHashMap();

    private final Stack followers = new Stack();

    private final BlockingSet readySessionBuffers = new BlockingSet();

    private final Set busySessionBuffers = new HashSet();

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
     */
    protected BaseThreadPool()
    {
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
        final BlockingSet readySessionBuffers = this.readySessionBuffers;
        final Set busySessionBuffers = this.busySessionBuffers;
        final Event event = new Event( type, nextFilter, data );

        synchronized( readySessionBuffers )
        {
            final SessionBuffer buf = getSessionBuffer( session );
            final Queue eventQueue = buf.eventQueue;

            synchronized( buf )
            {
                eventQueue.push( event );
            }

            if( !busySessionBuffers.contains( buf ) )
            {
                busySessionBuffers.add( buf );
                readySessionBuffers.add( buf );
            }
        }
    }

    /**
     * Implement this method to forward events to <tt>nextFilter</tt>.
     */
    protected abstract void processEvent( Object nextFilter, Session session,
                                          EventType type, Object data );

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

    private static class SessionBuffer
    {
        private final Session session;

        private final Queue eventQueue = new Queue();

        private SessionBuffer( Session session )
        {
            this.session = session;
        }
    }

    private class Worker extends Thread
    {
        private final Object promotionLock = new Object();

        private Worker()
        {
            super( "IoThreadPool-" + ( threadId++ ) );
            increasePoolSize();
        }

        public void lead()
        {
            final Object promotionLock = this.promotionLock;
            synchronized( promotionLock )
            {
                leader = this;
                promotionLock.notify();
            }
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
        }

        private SessionBuffer fetchBuffer()
        {
            SessionBuffer buf = null;
            BlockingSet readySessionBuffers = BaseThreadPool.this.readySessionBuffers;
            synchronized( readySessionBuffers )
            {
                do
                {
                    buf = null;
                    try
                    {
                        readySessionBuffers.waitForNewItem();
                    }
                    catch( InterruptedException e )
                    {
                        break;
                    }

                    Iterator it = readySessionBuffers.iterator();
                    if( !it.hasNext() )
                    {
                        // exceeded keepAliveTime
                        break;
                    }

                    do
                    {
                        buf = null;
                        buf = ( SessionBuffer ) it.next();
                        it.remove();
                    }
                    while( buf != null && buf.eventQueue.isEmpty()
                           && it.hasNext() );
                }
                while( buf != null && buf.eventQueue.isEmpty() );
            }

            return buf;
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
            final BlockingSet readySessionBuffers = BaseThreadPool.this.readySessionBuffers;
            final Set busySessionBuffers = BaseThreadPool.this.busySessionBuffers;
            final Queue eventQueue = buf.eventQueue;

            synchronized( readySessionBuffers )
            {
                if( eventQueue.isEmpty() )
                {
                    busySessionBuffers.remove( buf );
                    removeSessionBuffer( buf );
                }
                else
                {
                    readySessionBuffers.add( buf );
                }
            }
        }

        private boolean waitForPromotion()
        {
            final Object promotionLock = this.promotionLock;

            synchronized( promotionLock )
            {
                if( this != leader )
                {
                    try
                    {
                        int keepAliveTime = getKeepAliveTime();
                        if( keepAliveTime > 0 )
                        {
                            promotionLock.wait( keepAliveTime );
                        }
                        else
                        {
                            promotionLock.wait();
                        }
                    }
                    catch( InterruptedException e )
                    {
                    }
                }

                boolean timeToLead = this == leader;

                if( !timeToLead )
                {
                    // time to die
                    synchronized( followers )
                    {
                        followers.remove( this );
                    }
                }

                return timeToLead;
            }
        }

        private void giveUpLead()
        {
            final Stack followers = BaseThreadPool.this.followers;
            Worker worker;
            synchronized( followers )
            {
                worker = ( Worker ) followers.pop();
            }

            if( worker != null )
            {
                worker.lead();
            }
            else
            {
                if( !shuttingDown )
                {
                    synchronized( BaseThreadPool.this )
                    {
                        if( !shuttingDown
                            && getPoolSize() < getMaximumPoolSize() )
                        {
                            worker = new Worker();
                            worker.start();
                            worker.lead();
                        }
                    }
                }
            }
        }
    }
}