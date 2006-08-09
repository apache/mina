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

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.filter.thread.ThreadPool;
import org.apache.mina.filter.thread.ThreadPoolFilter;

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
     * @see ThreadPool#DEFAULT_MAXIMUM_POOL_SIZE
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = ThreadPool.DEFAULT_MAXIMUM_POOL_SIZE;

    /**
     * @see ThreadPool#DEFAULT_KEEP_ALIVE_TIME
     */
    public static final int DEFAULT_KEEP_ALIVE_TIME = ThreadPool.DEFAULT_KEEP_ALIVE_TIME;
    
    /**
     * Maps a service name to a PooledThreadModel instance.
     * Without this map, we might create extremely many thread pools that leads the system to
     * coma. */
    private static final Map service2model = new HashMap();
    
    /**
     * Returns a {@link PooledThreadModel} instance for the specified <tt>serviceName</tt>.
     * @param serviceName the name of the service that needs thread pooling
     */
    public static PooledThreadModel getInstance( String serviceName )
    {
    	if( serviceName == null )
    	{
    		throw new NullPointerException( "serviceName" );
    	}

    	PooledThreadModel model;
    	synchronized( service2model )
    	{
    		model = ( PooledThreadModel ) service2model.get( serviceName );
    		if( model == null )
    		{
    			model = new PooledThreadModel( serviceName );
    			service2model.put( serviceName, model );
    		}
    	}
    	
    	return model;
    }
    
    private final ThreadPoolFilter filter = new ThreadPoolFilter();
    private final IoFilter proxy = new ReferenceCountingIoFilter( filter );

    private PooledThreadModel( String threadNamePrefix )
    {
        this( threadNamePrefix, DEFAULT_MAXIMUM_POOL_SIZE );
    }

    private PooledThreadModel( String threadNamePrefix, int maxThreads )
    {
        setMaximumPoolSize( maxThreads );
        setThreadNamePrefix( threadNamePrefix );
    }

    public String getThreadNamePrefix()
    {
        return filter.getThreadPool().getThreadNamePrefix();
    }

    public void setThreadNamePrefix( String threadNamePrefix )
    {
        filter.getThreadPool().setThreadNamePrefix( threadNamePrefix );
    }
    
    public int getMaximumPoolSize()
    {
        return filter.getThreadPool().getMaximumPoolSize();
    }

    public int getKeepAliveTime()
    {
        return filter.getThreadPool().getKeepAliveTime();
    }

    public void setMaximumPoolSize( int maximumPoolSize )
    {
        filter.getThreadPool().setMaximumPoolSize( maximumPoolSize );
    }

    public void setKeepAliveTime( int keepAliveTime )
    {
        filter.getThreadPool().setKeepAliveTime( keepAliveTime );
    }

    public void buildFilterChain( IoFilterChain chain ) throws Exception
    {
        chain.addFirst( PooledThreadModel.class.getName(), proxy );
    }
}
