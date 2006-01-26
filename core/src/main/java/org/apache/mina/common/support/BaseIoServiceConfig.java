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
package org.apache.mina.common.support;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoServiceConfig;

/**
 * A base implementation of {@link IoServiceConfig}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoServiceConfig implements IoServiceConfig, Cloneable
{
    /**
     * Current filter chain builder.
     */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();

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
    
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw ( InternalError ) new InternalError().initCause( e );
        }
    }
}
