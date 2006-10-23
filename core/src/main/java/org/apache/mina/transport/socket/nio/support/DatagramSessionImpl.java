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
package org.apache.mina.transport.socket.nio.support;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.mina.common.BroadcastIoSession;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.util.Queue;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class DatagramSessionImpl extends BaseIoSession implements BroadcastIoSession
{
    private final IoService service;
    private final DatagramSessionConfig config = new SessionConfigImpl();
    private final DatagramService managerDelegate;
    private final DatagramFilterChain filterChain = new DatagramFilterChain( this );
    private final DatagramChannel ch;
    private final Queue writeRequestQueue = new Queue();
    private final IoHandler handler;
    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private SelectionKey key;
    private int readBufferSize;

    /**
     * Creates a new acceptor instance.
     */
    DatagramSessionImpl( IoAcceptor service,
                         DatagramService managerDelegate,
                         DatagramChannel ch, IoHandler defaultHandler,
                         SocketAddress remoteAddress )
    {
        this.service = service;
        this.managerDelegate = managerDelegate;
        this.ch = ch;
        this.handler = defaultHandler;
        this.remoteAddress = remoteAddress;

        // We didn't set the localAddress by calling getLocalSocketAddress() to avoid
        // the case that getLocalSocketAddress() returns IPv6 address while
        // serviceAddress represents the same address in IPv4.
        this.localAddress = service.getLocalAddress();

        applySettings();
    }

    /**
     * Creates a new connector instance.
     */
    DatagramSessionImpl( IoConnector service,
                         DatagramService managerDelegate,
                         DatagramChannel ch, IoHandler defaultHandler )
    {
        this.service = service;
        this.managerDelegate = managerDelegate;
        this.ch = ch;
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();

        applySettings();
    }

    private void applySettings() {
        // Apply the initial session settings
        IoSessionConfig sessionConfig = getService().getSessionConfig();
        if( sessionConfig instanceof DatagramSessionConfig )
        {
            DatagramSessionConfig cfg = ( DatagramSessionConfig ) sessionConfig;
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
        return service;
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
        managerDelegate.getSessionRecycler().remove( this );
        filterChain.fireFilterClose( this );
    }

    Queue getWriteRequestQueue()
    {
        return writeRequestQueue;
    }
    
    public WriteFuture write( Object message, SocketAddress destination )
    {
        if( !this.config.isBroadcast() )
        {
            throw new IllegalStateException( "Non-broadcast session" );
        }
        
        return super.write( message, destination );
    }

    protected void write0( WriteRequest writeRequest )
    {
        filterChain.fireFilterWrite( this, writeRequest );
    }

    public int getScheduledWriteMessages()
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

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }

    protected void updateTrafficMask()
    {
        managerDelegate.updateTrafficMask( this );
    }

    int getReadBufferSize()
    {
        return readBufferSize;
    }

    private class SessionConfigImpl extends DatagramSessionConfigImpl implements DatagramSessionConfig
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
            if( DatagramSessionConfigImpl.isSetReceiveBufferSizeAvailable() )
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
            if( DatagramSessionConfigImpl.isSetSendBufferSizeAvailable() )
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
            if( DatagramSessionConfigImpl.isGetTrafficClassAvailable() )
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
            else
            {
                return 0;
            }
        }

        public void setTrafficClass( int trafficClass )
        {
            if( DatagramSessionConfigImpl.isSetTrafficClassAvailable() )
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
}