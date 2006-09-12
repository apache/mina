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
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.BaseIoConnectorConfig;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.transport.vmpipe.support.VmPipe;
import org.apache.mina.transport.vmpipe.support.VmPipeSessionImpl;
import org.apache.mina.util.AnonymousSocketAddress;

/**
 * Connects to {@link IoHandler}s which is bound on the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeConnector extends BaseIoConnector
{
    private static final IoSessionConfig CONFIG = new BaseIoSessionConfig() {};
    private final IoServiceConfig defaultConfig = new BaseIoConnectorConfig()
    {
        public IoSessionConfig getSessionConfig()
        {
            return CONFIG;
        }
    };

    public ConnectFuture connect( SocketAddress address, IoHandler handler, IoServiceConfig config ) 
    {
        return connect( address, null, handler, config );
    }

    public ConnectFuture connect( SocketAddress address, SocketAddress localAddress, IoHandler handler, IoServiceConfig config )
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );
        if( ! ( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                                                "address must be VmPipeAddress." );

        if( config == null )
        {
            config = getDefaultConfig();
        }

        VmPipe entry = ( VmPipe ) VmPipeAcceptor.boundHandlers.get( address );
        if( entry == null )
        {
            return DefaultConnectFuture.newFailedFuture(
                    new IOException( "Endpoint unavailable: " + address ) );
        }

        DefaultConnectFuture future = new DefaultConnectFuture();
        try
        {
            VmPipeSessionImpl session =
                new VmPipeSessionImpl(
                        this,
                        config,
                        getListeners(),
                        new Object(), // lock
                        new AnonymousSocketAddress(),
                        handler,
                        entry );
            future.setSession( session );
        }
        catch( IOException e )
        {
            future.setException( e );
        }
        return future;
    }
    
    public IoServiceConfig getDefaultConfig()
    {
        return defaultConfig;
    }
}