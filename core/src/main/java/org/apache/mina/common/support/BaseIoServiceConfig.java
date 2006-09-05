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
package org.apache.mina.common.support;

import java.lang.reflect.Method;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.PooledThreadModel;
import org.apache.mina.common.ThreadModel;

/**
 * A base implementation of {@link IoServiceConfig}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoServiceConfig implements IoServiceConfig, Cloneable
{
    /**
     * Current filter chain builder.
     */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
    
    /**
     * The default thread model.
     */
    private final ThreadModel defaultThreadModel = PooledThreadModel.getInstance("AnonymousIoService");
    
    /**
     * Current thread model.
     */
    private ThreadModel threadModel = defaultThreadModel;

    public BaseIoServiceConfig()
    {
        super();
    }

    public IoFilterChainBuilder getFilterChainBuilder()
    {
        return filterChainBuilder;
    }

    public void setFilterChainBuilder( IoFilterChainBuilder builder )
    {
        if( builder == null )
        {
            builder = new DefaultIoFilterChainBuilder();
        }
        filterChainBuilder = builder;
    }
    
    public DefaultIoFilterChainBuilder getFilterChain()
    {
        if( filterChainBuilder instanceof DefaultIoFilterChainBuilder )
        {
            return ( DefaultIoFilterChainBuilder ) filterChainBuilder;
        }
        else
        {
            throw new IllegalStateException(
                    "Current filter chain builder is not a DefaultIoFilterChainBuilder." );
        }
    }
    
    public ThreadModel getThreadModel()
    {
        return threadModel;
    }

    public void setThreadModel( ThreadModel threadModel )
    {
        if( threadModel == null )
        {
            // We reuse the previous default model to prevent too much
            // daemon threads are created.
            threadModel = defaultThreadModel;
        }
        this.threadModel = threadModel;
    }

    public Object clone()
    {
        BaseIoServiceConfig ret;
        try
        {
            ret = ( BaseIoServiceConfig ) super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw ( InternalError ) new InternalError().initCause( e );
        }
        
        
        // Try to clone the chain builder.
        try
        {
            Method cloneMethod = this.filterChainBuilder.getClass().getMethod( "clone", null );
            if( cloneMethod.isAccessible() )
            {
                ret.filterChainBuilder = ( IoFilterChainBuilder ) cloneMethod.invoke( this.filterChainBuilder, null );
            }
        }
        catch( Exception e )
        {
        }
        
        return ret;
    }
}
