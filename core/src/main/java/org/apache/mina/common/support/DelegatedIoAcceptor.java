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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;

/**
 * A delegated {@link IoAcceptor} that wraps the other {@link IoAcceptor}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
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

    public void addListener( IoServiceListener listener )
    {
        delegate.addListener( listener );
    }

    public void removeListener( IoServiceListener listener )
    {
        delegate.removeListener( listener );
    }

    public void bind() throws IOException
    {
        delegate.bind();
    }

    public SocketAddress getLocalAddress()
    {
        return delegate.getLocalAddress();
    }

    public boolean isDisconnectOnUnbind()
    {
        return delegate.isDisconnectOnUnbind();
    }

    public IoSession newSession( SocketAddress remoteAddress )
    {
        return delegate.newSession( remoteAddress );
    }

    public void setDisconnectOnUnbind( boolean disconnectOnUnbind )
    {
        delegate.setDisconnectOnUnbind( disconnectOnUnbind );
    }

    public void setLocalAddress( SocketAddress localAddress )
    {
        delegate.setLocalAddress( localAddress );
    }

    public void unbind()
    {
        delegate.unbind();
    }

    public IoHandler getHandler()
    {
        return delegate.getHandler();
    }

    public Set<IoSession> getManagedSessions()
    {
        return delegate.getManagedSessions();
    }

    public IoSessionConfig getSessionConfig()
    {
        return delegate.getSessionConfig();
    }

    public void setHandler( IoHandler handler )
    {
        delegate.setHandler( handler );
    }

    public boolean isBound()
    {
        return delegate.isBound();
    }

    public TransportType getTransportType() {
        return delegate.getTransportType();
    }
}
