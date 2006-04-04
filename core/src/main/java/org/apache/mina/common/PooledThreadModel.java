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
package org.apache.mina.common;

import org.apache.mina.filter.ThreadPoolFilter;

/**
 * A {@link ThreadModel} which represents a thread model with an independant
 * thread pool per service.
 * <p>
 * Please note that reusing an instance of this model means a thread pool
 * is shared among multiple services.  If don't want to shared a thread pool,
 * please create each instance of this model whenever you bind a service or
 * connect to a remote service.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class PooledThreadModel implements ThreadModel
{
    /**
     * @see ThreadPoolFilter#DEFAULT_MAXIMUM_POOL_SIZE
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = ThreadPoolFilter.DEFAULT_MAXIMUM_POOL_SIZE;

    /**
     * @see ThreadPoolFilter#DEFAULT_KEEP_ALIVE_TIME
     */
    public static final int DEFAULT_KEEP_ALIVE_TIME = ThreadPoolFilter.DEFAULT_KEEP_ALIVE_TIME;
    
    private static int id = 1;
    
    private final ThreadPoolFilter pool = new ThreadPoolFilter();

    public PooledThreadModel()
    {
        this( "AnonymousIoService-" + id++, DEFAULT_MAXIMUM_POOL_SIZE );
    }
    
    public PooledThreadModel( int maxThreads )
    {
        this( "AnonymousIoService-" + id++, maxThreads );
    }

    public PooledThreadModel( String threadNamePrefix )
    {
        this( threadNamePrefix, DEFAULT_MAXIMUM_POOL_SIZE );
    }

    public PooledThreadModel( String threadNamePrefix, int maxThreads )
    {
        setMaximumPoolSize( maxThreads );
        setThreadNamePrefix( threadNamePrefix );
    }

    public String getThreadNamePrefix()
    {
        return pool.getThreadNamePrefix();
    }

    public void setThreadNamePrefix( String threadNamePrefix )
    {
        pool.setThreadNamePrefix( threadNamePrefix );
    }
    
    public int getMaximumPoolSize()
    {
        return pool.getMaximumPoolSize();
    }

    public int getKeepAliveTime()
    {
        return pool.getKeepAliveTime();
    }

    public void setMaximumPoolSize( int maximumPoolSize )
    {
        pool.setMaximumPoolSize( maximumPoolSize );
    }

    public void setKeepAliveTime( int keepAliveTime )
    {
        pool.setKeepAliveTime( keepAliveTime );
    }

    public void buildFilterChain( IoFilterChain chain ) throws Exception
    {
        chain.addFirst( PooledThreadModel.class.getName(), pool );
    }
}
