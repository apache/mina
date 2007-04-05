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
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.transport.vmpipe.support.VmPipe;
import org.apache.mina.transport.vmpipe.support.VmPipeFilterChain;
import org.apache.mina.transport.vmpipe.support.VmPipeIdleStatusChecker;
import org.apache.mina.transport.vmpipe.support.VmPipeSessionImpl;

/**
 * Connects to {@link IoHandler}s which is bound on the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
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

    public TransportType getTransportType() {
        return TransportType.VM_PIPE;
    }
    
    @Override
    public VmPipeSessionConfig getSessionConfig() {
        return (VmPipeSessionConfig) super.getSessionConfig();
    }

    // This method is overriden to work around a problem with
    // bean property access mechanism.

    @Override
    public void setSessionConfig(IoSessionConfig sessionConfig) {
        super.setSessionConfig(sessionConfig);
    }
    
    @Override
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
                    nextAnonymousAddress(),  // Assign the local address dynamically,
                    getHandler(),
                    entry );
        
        // and reclaim the address when the connection is closed.
        localSession.getCloseFuture().addListener(ANON_ADDRESS_RECLAIMER);
        
        // initialize connector session
        try
        {
            IoFilterChain filterChain = localSession.getFilterChain();
            this.getFilterChainBuilder().buildFilterChain( filterChain );

            // The following sentences don't throw any exceptions.
            localSession.setAttribute( AbstractIoFilterChain.CONNECT_FUTURE, future );
            getListeners().fireSessionCreated( localSession );
            VmPipeIdleStatusChecker.getInstance().addSession( localSession );
        }
        catch( Throwable t )
        {
            future.setException( t );
            return future;
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
        
        ( ( VmPipeFilterChain ) localSession.getFilterChain() ).start();
        ( ( VmPipeFilterChain ) remoteSession.getFilterChain() ).start();

        return future;
    }
    
    private static final Set<VmPipeAddress> TAKEN_ANON_ADDRESSES =
        new HashSet<VmPipeAddress>();
    private static int nextAnonymousAddress = -1;
    
    private static final IoFutureListener ANON_ADDRESS_RECLAIMER = 
        new AnonymousAddressReclaimer();

    private static VmPipeAddress nextAnonymousAddress() {
        synchronized (TAKEN_ANON_ADDRESSES) {
            for (;;) {
                VmPipeAddress answer = new VmPipeAddress(nextAnonymousAddress --);
                if (!TAKEN_ANON_ADDRESSES.contains(answer)) {
                    TAKEN_ANON_ADDRESSES.add(answer);
                    return answer;
                }
            }
        }
    }
    
    private static class AnonymousAddressReclaimer implements IoFutureListener {
        public void operationComplete(IoFuture future) {
            synchronized (TAKEN_ANON_ADDRESSES) {
                TAKEN_ANON_ADDRESSES.remove(future.getSession().getLocalAddress());
            }
        }
    }
}