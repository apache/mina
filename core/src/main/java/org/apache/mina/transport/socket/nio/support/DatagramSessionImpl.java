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

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoService;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.transport.socket.nio.DatagramSession;
import org.apache.mina.util.Queue;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class DatagramSessionImpl extends BaseIoSession implements DatagramSession
{
    private final IoService wrapperManager;
    private final DatagramService managerDelegate;
    private final DatagramFilterChain filterChain;
    private final DatagramChannel ch;
    private final Queue writeRequestQueue;
    private final IoHandler handler;
    private final SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private SelectionKey key;

    /**
     * Creates a new instance.
     */
    DatagramSessionImpl( IoService wrapperManager,
                         DatagramService managerDelegate,
                         DatagramChannel ch, IoHandler defaultHandler )
    {
        this.wrapperManager = wrapperManager;
        this.managerDelegate = managerDelegate;
        this.filterChain = new DatagramFilterChain( this );
        this.ch = ch;
        this.writeRequestQueue = new Queue();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
    }
    
    public IoService getManager()
    {
        return wrapperManager;
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
    
    protected void close0( CloseFuture closeFuture )
    {
        filterChain.filterClose( this, closeFuture );
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

    public boolean getReuseAddress() throws SocketException
    {
        return ch.socket().getReuseAddress();
    }

    public void setReuseAddress( boolean on ) throws SocketException
    {
        ch.socket().setReuseAddress( on );
    }

    public int getTrafficClass() throws SocketException
    {
        return ch.socket().getTrafficClass();
    }

    public void setTrafficClass( int tc ) throws SocketException
    {
        ch.socket().setTrafficClass( tc );
    }

    protected void updateTrafficMask()
    {
        managerDelegate.updateTrafficMask( this );
    }

    public int getReceiveBufferSize() throws SocketException {
        return ch.socket().getReceiveBufferSize();
    }

    public void setReceiveBufferSize( int receiveBufferSize ) throws SocketException
    {
        ch.socket().setReceiveBufferSize( receiveBufferSize );
    }

    public boolean getBroadcast() throws SocketException
    {
        return ch.socket().getBroadcast();
    }

    public void setBroadcast( boolean broadcast ) throws SocketException
    {
        ch.socket().setBroadcast( broadcast );
    }

    public int getSendBufferSize() throws SocketException
    {
        return ch.socket().getSendBufferSize();
    }

    public void setSendBufferSize( int sendBufferSize ) throws SocketException
    {
        ch.socket().setSendBufferSize( sendBufferSize );
    }
}