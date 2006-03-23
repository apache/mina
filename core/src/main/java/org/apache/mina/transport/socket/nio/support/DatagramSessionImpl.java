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
package org.apache.mina.transport.socket.nio.support;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.util.Queue;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class DatagramSessionImpl extends BaseIoSession
{
    private final IoService wrapperManager;
    private final DatagramSessionConfig config = new DatagramSessionConfigImpl();
    private final DatagramService managerDelegate;
    private final DatagramFilterChain filterChain;
    private final DatagramChannel ch;
    private final Queue writeRequestQueue;
    private final IoHandler handler;
    private final SocketAddress localAddress;
    private final SocketAddress serviceAddress;
    private SocketAddress remoteAddress;
    private SelectionKey key;
    private int readBufferSize;

    /**
     * Creates a new instance.
     */
    DatagramSessionImpl( IoService wrapperManager,
                         DatagramService managerDelegate,
                         IoSessionConfig config,
                         DatagramChannel ch, IoHandler defaultHandler,
                         SocketAddress serviceAddress )
    {
        this.wrapperManager = wrapperManager;
        this.managerDelegate = managerDelegate;
        this.filterChain = new DatagramFilterChain( this );
        this.ch = ch;
        this.writeRequestQueue = new Queue();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
        this.serviceAddress = serviceAddress;
        
        // Apply the initial session settings
        if( config instanceof DatagramSessionConfig )
        {
            DatagramSessionConfig cfg = ( DatagramSessionConfig ) config;
            this.config.setBroadcast( cfg.isBroadcast() );
            this.config.setReceiveBufferSize( cfg.getReceiveBufferSize() );
            this.readBufferSize = cfg.getReceiveBufferSize();
            this.config.setReuseAddress( cfg.isReuseAddress() );
            this.config.setSendBufferSize( cfg.getSendBufferSize() );

            if( this.config.getTrafficClass() != cfg.getTrafficClass() )
            {
                this.config.setTrafficClass( cfg.getTrafficClass() );
            }
        }
    }
    
    public IoService getService()
    {
        return wrapperManager;
    }
    
    public IoSessionConfig getConfig()
    {
        return config;
    }
    
    DatagramService getManagerDelegate()
    {
        return managerDelegate;
    }

    public IoFilterChain getFilterChain()
    {
        return filterChain;
    }

    DatagramChannel getChannel()
    {
        return ch;
    }

    SelectionKey getSelectionKey()
    {
        return key;
    }

    void setSelectionKey( SelectionKey key )
    {
        this.key = key;
    }

    public IoHandler getHandler()
    {
        return handler;
    }
    
    protected void close0()
    {
        filterChain.filterClose( this );
    }

    Queue getWriteRequestQueue()
    {
        return writeRequestQueue;
    }

    protected void write0( WriteRequest writeRequest )
    {
        filterChain.filterWrite( this, writeRequest );
    }

    public int getScheduledWriteRequests()
    {
        synchronized( writeRequestQueue )
        {
            return writeRequestQueue.size();
        }
    }
    
    public int getScheduledWriteBytes()
    {
        synchronized( writeRequestQueue )
        {
            return writeRequestQueue.byteSize();
        }
    }
    
    public TransportType getTransportType()
    {
        return TransportType.DATAGRAM;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    void setRemoteAddress( SocketAddress remoteAddress )
    {
        this.remoteAddress = remoteAddress;
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
        managerDelegate.updateTrafficMask( this );
    }
    
    int getReadBufferSize()
    {
        return readBufferSize;
    }
    
    private class DatagramSessionConfigImpl extends BaseIoSessionConfig implements DatagramSessionConfig
    {
        public int getReceiveBufferSize()
        {
            try
            {
                return ch.socket().getReceiveBufferSize();
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public void setReceiveBufferSize( int receiveBufferSize )
        {
            try
            {
                ch.socket().setReceiveBufferSize( receiveBufferSize );
                DatagramSessionImpl.this.readBufferSize = receiveBufferSize;
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public boolean isBroadcast()
        {
            try
            {
                return ch.socket().getBroadcast();
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public void setBroadcast( boolean broadcast )
        {
            try
            {
                ch.socket().setBroadcast( broadcast );
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public int getSendBufferSize()
        {
            try
            {
                return ch.socket().getSendBufferSize();
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public void setSendBufferSize( int sendBufferSize )
        {
            try
            {
                ch.socket().setSendBufferSize( sendBufferSize );
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public boolean isReuseAddress()
        {
            try
            {
                return ch.socket().getReuseAddress();
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public void setReuseAddress( boolean reuseAddress )
        {
            try
            {
                ch.socket().setReuseAddress( reuseAddress );
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public int getTrafficClass()
        {
            try
            {
                return ch.socket().getTrafficClass();
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }

        public void setTrafficClass( int trafficClass )
        {
            try
            {
                ch.socket().setTrafficClass( trafficClass );
            }
            catch( SocketException e )
            {
                throw new RuntimeIOException( e );
            }
        }
    }
}