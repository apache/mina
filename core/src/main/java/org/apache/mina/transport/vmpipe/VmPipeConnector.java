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
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.transport.vmpipe.support.VmPipe;
import org.apache.mina.transport.vmpipe.support.VmPipeIdleStatusChecker;
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

    /**
     * Creates a new instance.
     */
    public VmPipeConnector()
    {
        super( new DefaultVmPipeSessionConfig() );
    }

    protected Class<? extends SocketAddress> getAddressType()
    {
        return VmPipeAddress.class;
    }

    @Override
    protected Class<? extends IoSessionConfig> getSessionConfigType()
    {
        return VmPipeSessionConfig.class;
    }

    protected ConnectFuture doConnect( SocketAddress remoteAddress, SocketAddress localAddress )
    {
        VmPipe entry = VmPipeAcceptor.boundHandlers.get( remoteAddress );
        if( entry == null )
        {
            return DefaultConnectFuture.newFailedFuture(
                    new IOException( "Endpoint unavailable: " + remoteAddress ) );
        }

        DefaultConnectFuture future = new DefaultConnectFuture();
        VmPipeSessionImpl localSession =
            new VmPipeSessionImpl(
                    this,
                    getListeners(),
                    new Object(), // lock
                    new AnonymousSocketAddress(),
                    getHandler(),
                    entry );
        
        // initialize connector session
        try
        {
            IoFilterChain filterChain = localSession.getFilterChain();
            this.getFilterChainBuilder().buildFilterChain( filterChain );

            // The following sentences don't throw any exceptions.
            localSession.setAttribute( AbstractIoFilterChain.CONNECT_FUTURE, future );
            getListeners().fireSessionCreated( localSession );
            VmPipeIdleStatusChecker.getInstance().addSession( localSession);
        }
        catch( Throwable t )
        {
            future.setException( t );
        }
        
        // initialize acceptor session
        VmPipeSessionImpl remoteSession = localSession.getRemoteSession();
        try
        {
            IoFilterChain filterChain = remoteSession.getFilterChain();
            entry.getAcceptor().getFilterChainBuilder().buildFilterChain( filterChain );
            
            // The following sentences don't throw any exceptions.
            entry.getListeners().fireSessionCreated( remoteSession );
            VmPipeIdleStatusChecker.getInstance().addSession( remoteSession );
        }
        catch( Throwable t )
        {
            ExceptionMonitor.getInstance().exceptionCaught( t );
            remoteSession.close();
        }

        return future;
    }
}