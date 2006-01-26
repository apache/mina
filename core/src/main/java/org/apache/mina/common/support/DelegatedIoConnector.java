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

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;

/**
 * A delegated {@link IoConnector} that wraps the other {@link IoConnector}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DelegatedIoConnector implements IoConnector
{
    protected IoConnector delegate;

    /**
     * Creates a new instance.
     */
    protected DelegatedIoConnector()
    {
    }

    /**
     * Sets the delegate.  This method should be invoked before any operation
     * is requested.
     */
    protected void init( IoConnector delegate )
    {
        this.delegate = delegate;
    }
    
    public ConnectFuture connect( SocketAddress address, IoHandler handler )
    {
        return delegate.connect( address, handler );
    }

    public ConnectFuture connect( SocketAddress address, IoHandler handler, IoConnectorConfig config )
    {
        return delegate.connect( address, handler, config );
    }

    public ConnectFuture connect( SocketAddress address, SocketAddress localAddress,
                                  IoHandler handler )
    {
        return delegate.connect( address, localAddress, handler );
    }

    public ConnectFuture connect( SocketAddress address, SocketAddress localAddress,
                                  IoHandler handler, IoConnectorConfig config )
    {
        return delegate.connect( address, localAddress, handler, config );
    }

    public Set getManagedSessions( SocketAddress address )
    {
        return delegate.getManagedSessions( address );
    }
    
    public IoServiceConfig getDefaultConfig()
    {
        return delegate.getDefaultConfig();
    }
}
