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

import java.net.SocketAddress;

import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.common.support.IoServiceListenerSupport;
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
    
    private final IoService service;
    private final IoServiceConfig serviceConfig;
    private final IoServiceListenerSupport serviceListeners;
    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private final SocketAddress serviceAddress;
    private final IoHandler handler;
    private final VmPipeFilterChain filterChain;
    private final VmPipeSessionImpl remoteSession;
    final Object lock;
    final Queue pendingDataQueue;

    /**
     * Constructor for client-side session.
     */
    public VmPipeSessionImpl(
            IoService service, IoServiceConfig serviceConfig,
            IoServiceListenerSupport serviceListeners, Object lock, SocketAddress localAddress,
            IoHandler handler, VmPipe remoteEntry )
    {
        this.service = service;
        this.serviceConfig = serviceConfig;
        this.serviceListeners = serviceListeners;
        this.lock = lock;
        this.localAddress = localAddress;
        this.remoteAddress = this.serviceAddress = remoteEntry.getAddress();
        this.handler = handler;
        this.filterChain = new VmPipeFilterChain( this );
        this.pendingDataQueue = new Queue();

        remoteSession = new VmPipeSessionImpl( this, remoteEntry );
    }

    /**
     * Constructor for server-side session.
     */
    private VmPipeSessionImpl( VmPipeSessionImpl remoteSession, VmPipe entry )
    {
        this.service = entry.getAcceptor();
        this.serviceConfig = entry.getConfig();
        this.serviceListeners = entry.getListeners();
        this.lock = remoteSession.lock;
        this.localAddress = this.serviceAddress = remoteSession.remoteAddress;
        this.remoteAddress = remoteSession.localAddress;
        this.handler = entry.getHandler();
        this.filterChain = new VmPipeFilterChain( this );
        this.remoteSession = remoteSession;
        this.pendingDataQueue = new Queue();
    }
    
    public IoService getService()
    {
        return service;
    }
    
    IoServiceListenerSupport getServiceListeners()
    {
        return serviceListeners;
    }
    
    public IoServiceConfig getServiceConfig()
    {
        return serviceConfig;
    }
    
    public IoSessionConfig getConfig()
    {
        return CONFIG;
    }

    public IoFilterChain getFilterChain()
    {
        return filterChain;
    }
    
    public VmPipeSessionImpl getRemoteSession()
    {
        return remoteSession;
    }

    public IoHandler getHandler()
    {
        return handler;
    }

    protected void close0()
    {
        filterChain.fireFilterClose( this );
    }
    
    protected void write0( WriteRequest writeRequest )
    {
        this.filterChain.fireFilterWrite( this, writeRequest );
    }

    public int getScheduledWriteMessages()
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
            Object[] data;
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
                    filterChain.fireMessageReceived( this, data[ i ] );
                }
            }
        }
    }
}
