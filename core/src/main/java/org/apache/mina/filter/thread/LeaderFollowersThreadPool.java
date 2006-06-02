/*
 *   @(#) $Id:  $
 *
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.mina.filter.thread;

import org.apache.mina.util.BlockingQueue;
import org.apache.mina.util.IdentityHashSet;
import org.apache.mina.util.Queue;
import org.apache.mina.util.Stack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is an implementation of
 * <a href="http://deuce.doc.wustl.edu/doc/pspdfs/lf.pdf">Leader/Followers
 * thread pool</a> by Douglas C. Schmidt et al.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 350169 $, $Date: 2005-12-01 00:17:41 -0500 (Thu, 01 Dec 2005) $
 */
public class LeaderFollowersThreadPool implements ThreadPool
{
    /**
     * A queue which contains {@link Integer}s which represents reusable
     * thread IDs.  {@link LeaderFollowersThreadPool.Worker} first checks this queue and then
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
    private final BlockingQueue unfetchedRunnables = new BlockingQueue();

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
    public LeaderFollowersThreadPool()
    {
        this( "LeaderFollowerThreadPool" );
    }

    /**
     * Creates a new instance of this filter with the specified thread name prefix
     * and other default settings.
     * @param threadNamePrefix the prefix of the thread names this pool will create.
     */
    public LeaderFollowersThreadPool( String threadNamePrefix )
    {
        this( threadNamePrefix, DEFAULT_MAXIMUM_POOL_SIZE );
    }

    /**
     * Creates a new instance of this filter with the specified thread name prefix
     * and other default settings.
     * @param threadNamePrefix the prefix of the thread names this pool will create.
     * @param maximumPoolSize Maximum size of thread pool
     */
    public LeaderFollowersThreadPool( String threadNamePrefix, int maximumPoolSize )
    {
        setThreadNamePrefix( threadNamePrefix );
        setMaximumPoolSize( maximumPoolSize );
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
    
    public boolean isStarted()
    {
        return leader != null;
    }

    public void init()
    {
        shuttingDown = false;
        leader = new Worker();
        leader.start();
        leader.lead();
    }

    public void destroy()
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

        this.unfetchedRunnables.clear();
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

    /**
     * Implement this method to fetch (or pop) a {@link Runnable} from
     * the given <tt>unfetchedRunnables</tt>.  The default implementation
     * simply pops the Runnable from it.  You could prioritize the fetch order.
     *
     * @return A non-null {@link Runnable}
     */
    protected Runnable fetchRunnable( Queue unfetchedSessionBuffers )
    {
        return ( Runnable ) unfetchedSessionBuffers.pop();
    }

    public void submit( Runnable runnable )
    {
        synchronized( unfetchedRunnables )
        {
            unfetchedRunnables.add( runnable );
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

                Runnable runnable = fetchRunnable();
                giveUpLead();
                if( runnable == null )
                {
                    break;
                }

                runnable.run();
                // previously, follow() occured between the two bits here, dunno how much of a
                //difference that makes
                follow();
            }

            decreasePoolSize( this );
            releaseThreadId( id );
        }

        private Runnable fetchRunnable()
        {
            BlockingQueue unfetchedRunnables = LeaderFollowersThreadPool.this.unfetchedRunnables;

            synchronized( unfetchedRunnables )
            {
                while( !shuttingDown )
                {
                    try
                    {
                        unfetchedRunnables.waitForNewItem();
                    }
                    catch( InterruptedException e )
                    {
                        continue;
                    }

                    return LeaderFollowersThreadPool.this.fetchRunnable( unfetchedRunnables );
                }
            }

            return null;
        }


        private void follow()
        {
            final Object promotionLock = this.promotionLock;
            final Stack followers = LeaderFollowersThreadPool.this.followers;
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
            final Stack followers = LeaderFollowersThreadPool.this.followers;
            LeaderFollowersThreadPool.Worker worker;
            do
            {
                synchronized( followers )
                {
                    worker = ( LeaderFollowersThreadPool.Worker ) followers.pop();
                }

                if( worker == null )
                {
                    // Increase the number of threads if we
                    // are not shutting down and we can increase the number.
                    if( !shuttingDown
                        && getPoolSize() < getMaximumPoolSize() )
                    {
                        worker = new LeaderFollowersThreadPool.Worker();
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
