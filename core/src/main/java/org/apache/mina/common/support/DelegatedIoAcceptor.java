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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;

/**
 * A delegated {@link IoAcceptor} that wraps the other {@link IoAcceptor}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DelegatedIoAcceptor implements IoAcceptor
{
    protected IoAcceptor delegate;
    
    /**
     * Creates a new instance.
     */
    protected DelegatedIoAcceptor()
    {
    }
    
    /**
     * Sets the delegate.  This method should be invoked before any operations
     * is requested.
     */
    protected void init( IoAcceptor delegate )
    {
        this.delegate = delegate;
    }
    
    public void bind( SocketAddress address, IoHandler handler ) throws IOException
    {
        delegate.bind( address, handler );
    }

    public void bind( SocketAddress address, IoHandler handler, IoServiceConfig config ) throws IOException
    {
        delegate.bind( address, handler, config );
    }

    public void unbind( SocketAddress address )
    {
        delegate.unbind( address );
    }
    
    public void unbindAll()
    {
        delegate.unbindAll();
    }
    
    public boolean isBound( SocketAddress address )
    {
        return delegate.isBound( address );
    }

    public Set getManagedSessions( SocketAddress address )
    {
        return delegate.getManagedSessions( address );
    }

    public IoSession newSession( SocketAddress remoteAddress, SocketAddress localAddress )
    {
        return delegate.newSession( remoteAddress, localAddress );
    }

    public IoServiceConfig getDefaultConfig()
    {
        return delegate.getDefaultConfig();
    }

    public IoFilterChainBuilder getFilterChainBuilder()
    {
        return delegate.getFilterChainBuilder();
    }

    public void setFilterChainBuilder( IoFilterChainBuilder builder )
    {
        delegate.setFilterChainBuilder( builder );
    }

    public DefaultIoFilterChainBuilder getFilterChain()
    {
        return delegate.getFilterChain();
    }
}
