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
package org.apache.mina.transport.vmpipe.support;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.util.Queue;

/**
 * A {@link IoSession} for in-VM transport (VM_PIPE).
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeSessionImpl extends BaseIoSession
{
    private static final IoSessionConfig CONFIG = new BaseIoSessionConfig() {};
    
    private final IoService manager;
    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private final SocketAddress serviceAddress;
    private final IoHandler handler;
    private final VmPipeFilterChain filterChain;
    private final Set managedSessions;
    final VmPipeSessionImpl remoteSession;
    final Object lock;
    final Queue pendingDataQueue;

    /**
     * Constructor for client-side session.
     */
    public VmPipeSessionImpl( IoService manager, Object lock, SocketAddress localAddress,
                   IoHandler handler, IoFilterChainBuilder filterChainBuilder, ThreadModel threadModel,
                   VmPipe remoteEntry ) throws IOException
    {
        this.manager = manager;
        this.lock = lock;
        this.localAddress = localAddress;
        this.remoteAddress = this.serviceAddress = remoteEntry.getAddress();
        this.handler = handler;
        this.filterChain = new VmPipeFilterChain( this );
        this.pendingDataQueue = new Queue();

        this.managedSessions = remoteEntry.getManagedClientSessions();
        
        remoteSession = new VmPipeSessionImpl( manager, this, remoteEntry );
        
        // initialize remote session
        try
        {
            remoteEntry.getAcceptor().getFilterChainBuilder().buildFilterChain( remoteSession.getFilterChain() );
            remoteEntry.getConfig().getFilterChainBuilder().buildFilterChain( remoteSession.getFilterChain() );
            remoteEntry.getConfig().getThreadModel().buildFilterChain( remoteSession.getFilterChain() );
            ( ( VmPipeFilterChain ) remoteSession.getFilterChain() ).sessionCreated( remoteSession );
        }
        catch( Throwable t )
        {
            ExceptionMonitor.getInstance().exceptionCaught( t );
            IOException e = new IOException( "Failed to initialize remote session." );
            e.initCause( t );
            throw e;
        }
        
        // initialize client session
        try
        {
            manager.getFilterChainBuilder().buildFilterChain( filterChain );
            filterChainBuilder.buildFilterChain( filterChain );
            threadModel.buildFilterChain( filterChain );
            handler.sessionCreated( this );
        }
        catch( Throwable t )
        {
            throw ( IOException ) new IOException( "Failed to create a session." ).initCause( t );
        }

        VmPipeIdleStatusChecker.getInstance().addSession( remoteSession );
        VmPipeIdleStatusChecker.getInstance().addSession( this );
        
        remoteSession.managedSessions.add( remoteSession );
        this.managedSessions.add( this );
        
        ( ( VmPipeFilterChain ) remoteSession.getFilterChain() ).sessionOpened( remoteSession );
        filterChain.sessionOpened( this );
    }

    /**
     * Constructor for server-side session.
     */
    private VmPipeSessionImpl( IoService manager, VmPipeSessionImpl remoteSession, VmPipe entry )
    {
        this.manager = manager;
        this.lock = remoteSession.lock;
        this.localAddress = this.serviceAddress = remoteSession.remoteAddress;
        this.remoteAddress = remoteSession.localAddress;
        this.handler = entry.getHandler();
        this.filterChain = new VmPipeFilterChain( this );
        this.remoteSession = remoteSession;
        this.pendingDataQueue = new Queue();
        this.managedSessions = entry.getManagedServerSessions();
    }
    
    Set getManagedSessions()
    {
        return managedSessions;
    }

    public IoService getService()
    {
        return manager;
    }
    
    public IoSessionConfig getConfig()
    {
        return CONFIG;
    }

    public IoFilterChain getFilterChain()
    {
        return filterChain;
    }

    public IoHandler getHandler()
    {
        return handler;
    }

    protected void close0()
    {
        filterChain.filterClose( this );
    }
    
    protected void write0( WriteRequest writeRequest )
    {
        this.filterChain.filterWrite( this, writeRequest );
    }

    public int getScheduledWriteRequests()
    {
        return 0;
    }

    public int getScheduledWriteBytes()
    {
        return 0;
    }
    
    public TransportType getTransportType()
    {
        return TransportType.VM_PIPE;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }
    
    public SocketAddress getServiceAddress()
    {
        return serviceAddress;
    }

    protected void updateTrafficMask()
    {
        if( getTrafficMask().isReadable() || getTrafficMask().isWritable())
        {
            Object[] data = null;
            synchronized( pendingDataQueue )
            {
                data = pendingDataQueue.toArray();
                pendingDataQueue.clear();
            }
            
            for( int i = 0; i < data.length; i++ )
            {
                if( data[ i ] instanceof WriteRequest )
                {
                    WriteRequest wr = ( WriteRequest ) data[ i ];
                    filterChain.doWrite( this, wr );
                }
                else
                {
                    filterChain.messageReceived( this, data[ i ] );
                }
            }
        }
    }
}
