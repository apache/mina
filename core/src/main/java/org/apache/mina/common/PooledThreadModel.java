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
    private String threadNamePrefix;
    private int keepAliveTime;
    private int maximumPoolSize;

    public PooledThreadModel()
    {
        this( "AnonymousIoService-" + id++, DEFAULT_MAXIMUM_POOL_SIZE );
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
        return threadNamePrefix;
    }

    public void setThreadNamePrefix( String threadNamePrefix )
    {
        this.threadNamePrefix = threadNamePrefix;
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
        this.maximumPoolSize = maximumPoolSize;
    }

    public void setKeepAliveTime( int keepAliveTime )
    {
        this.keepAliveTime = keepAliveTime;
    }

    public void buildFilterChain( IoFilterChain chain ) throws Exception
    {
        ThreadPoolFilter filter = new ThreadPoolFilter( threadNamePrefix );
        filter.setKeepAliveTime( keepAliveTime );
        filter.setMaximumPoolSize( maximumPoolSize );
        chain.addFirst( PooledThreadModel.class.getName(), filter );
    }
}
