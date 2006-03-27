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

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoService;

/**
 * Base implementation of {@link IoService}s.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoService implements IoService
{
    /**
     * Current filter chain builder.
     */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();

    protected BaseIoService()
    {
    }
    
    public Set getManagedSessions( SocketAddress address )
    {
        throw new UnsupportedOperationException();
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
}
