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

/**
 * An interface for a pool of threads, capable of handling units of work that are <code>Runnable</code>.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 397284 $, $Date: 2006-04-26 20:09:11Z $
 */
public interface ThreadPool
{
    /**
     * Default maximum size of thread pool (16).
     */
    static int DEFAULT_MAXIMUM_POOL_SIZE = 16;

    /**
     * Default keep-alive time of thread pool (1 min).
     */
    static int DEFAULT_KEEP_ALIVE_TIME = 60 * 1000;

    /**
     * Submit a <code>Runnable</code> to this thread pool.
     *
     * @param runnable <code>Runnable</code> to submit to this pool
     */
    void submit( Runnable runnable );

    /**
     * Initialize this pool
     */
    void init();

    /**
     * Destroy this pool.
     */
    void destroy();
    
    /**
     * Returns <tt>true</tt> if and if only this pool is started.
     */
    boolean isStarted();
    
    /**
     * Returns the name prefix string of the threads that this pool creates.
     * 
     * @return <tt>null</tt> if not supported
     */
    String getThreadNamePrefix();

    /**
     * Sets the name prefix string of the threads that this pool creates.
     * This method does nothing if this property is not supported.
     */
    void setThreadNamePrefix( String threadNamePrefix );

    /**
     * Returns the current number of the threads which are serving the submissions.
     */
    int getPoolSize();

    /**
     * Returns the maximum number of the threads in this pool.
     * 
     * @return <tt>-1</tt> if not supported
     */
    int getMaximumPoolSize();

    /**
     * Returns the keep-alive time (milliseconds) of the threads in this pool.
     * 
     * @return <tt>-1</tt> if not supported
     */
    int getKeepAliveTime();

    /**
     * Sets the maximum number of the threads in this pool.
     * This method does nothing if this property is not supported.
     */
    void setMaximumPoolSize( int maximumPoolSize );

    
    /**
     * Sets the keep-alive time (milliseconds) of the threads in this pool.
     * This method does nothing if this property is not supported.
     */
    void setKeepAliveTime( int keepAliveTime );
}
