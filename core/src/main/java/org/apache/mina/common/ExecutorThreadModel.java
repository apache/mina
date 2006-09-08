/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.filter.executor.ExecutorFilter;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadModel} which represents a thread model with an {@link Executor}
 * (from <a href="http://dcl.mathcs.emory.edu/util/backport-util-concurrent/">backport-util-concurrent</a>)
 * per service.  The default underlying {@link Executor} is {@link ThreadPoolExecutor},
 * so you can safely downcast the returned {@link Executor} of {@link #getExecutor()} to
 * {@link ThreadPoolExecutor} by default.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ExecutorThreadModel implements ThreadModel
{
    /**
     * Maps a service name to a PooledThreadModel instance.
     * Without this map, we might create extremely many thread pools that leads the system to
     * coma. */
    private static final Map service2model = new HashMap();
    
    /**
     * Returns a {@link ExecutorThreadModel} instance for the specified <tt>serviceName</tt>.
     * Please note that all returned instances will be managed globally; the same instance
     * will be returned if you specified the same service name.  Please try to specify
     * different names for different services.
     * 
     * @param serviceName the name of the service that needs thread pooling
     */
    public static ExecutorThreadModel getInstance( String serviceName )
    {
        if( serviceName == null )
        {
            throw new NullPointerException( "serviceName" );
        }

        ExecutorThreadModel model;
        synchronized( service2model )
        {
            model = ( ExecutorThreadModel ) service2model.get( serviceName );
            if( model == null )
            {
                model = new ExecutorThreadModel( serviceName );
                service2model.put( serviceName, model );
            }
        }
        
        return model;
    }
    
    private final String threadNamePrefix;
    private final ExecutorFilter defaultFilter;
    private ExecutorFilter filter = new ExecutorFilter();

    private ExecutorThreadModel( String threadNamePrefix )
    {
        this.threadNamePrefix = threadNamePrefix;
        
        // Create the default filter
        defaultFilter = new ExecutorFilter();
        ThreadPoolExecutor tpe = ( ThreadPoolExecutor ) defaultFilter.getExecutor();
        final ThreadFactory originalThreadFactory = tpe.getThreadFactory();
        ThreadFactory newThreadFactory = new ThreadFactory()
        {
            private final AtomicInteger threadId = new AtomicInteger( 0 );

            public Thread newThread( Runnable runnable )
            {
                Thread t = originalThreadFactory.newThread( runnable );
                t.setName( ExecutorThreadModel.this.threadNamePrefix + '-' + threadId.incrementAndGet() );
                t.setDaemon( true );
                return t;
            }
        };
        tpe.setThreadFactory( newThreadFactory );
        
        // Set to default.
        setExecutor( null );
    }

    /**
     * Returns the underlying {@link Executor} of this model.
     * You can change various properties such as the number of threads
     * by calling methods of the {@link Executor} implementation.
     */
    public Executor getExecutor()
    {
        return filter.getExecutor();
    }
    
    /**
     * Changes the underlying {@link Executor} of this model.
     * Previous settings such as the number of threads should be configured again.
     * Only newly created {@link IoSession}s will be affected.
     * 
     * @param executor <tt>null</tt> to revert to the default setting
     */
    public void setExecutor( Executor executor )
    {
        if( executor == null )
        {
            filter = defaultFilter;
        }
        else
        {
            filter = new ExecutorFilter( executor );
        }
    }

    public void buildFilterChain( IoFilterChain chain ) throws Exception
    {
        chain.addFirst( ExecutorThreadModel.class.getName(), filter );
    }
}
