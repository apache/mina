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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoService;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.transport.socket.nio.SocketSession;
import org.apache.mina.util.Queue;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class SocketSessionImpl extends BaseIoSession implements SocketSession
{
    private static final int DEFAULT_READ_BUFFER_SIZE = 1024;

    private final IoService manager;
    private final SocketIoProcessor ioProcessor;
    private final SocketFilterChain filterChain;
    private final SocketChannel ch;
    private final Queue writeRequestQueue;
    private final IoHandler handler;
    private final SocketAddress remoteAddress;
    private final SocketAddress localAddress;
    private final Set managedSessions;    
    private SelectionKey key;
    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;

    /**
     * Creates a new instance.
     */
    public SocketSessionImpl(
            IoService manager, Set managedSessions,
            SocketChannel ch, IoHandler defaultHandler )
    {
        this.manager = manager;
        this.managedSessions = managedSessions;
        this.ioProcessor = SocketIoProcessor.getInstance();
        this.filterChain = new SocketFilterChain( this );
        this.ch = ch;
        this.writeRequestQueue = new Queue();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
    }
    
    public IoService getService()
    {
        return manager;
    }
    
    SocketIoProcessor getIoProcessor()
    {
        return ioProcessor;
    }
    
    public IoFilterChain getFilterChain()
    {
        return filterChain;
    }

    SocketChannel getChannel()
    {
        return ch;
    }

    Set getManagedSessions()
    {
        return managedSessions;
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

    public int getScheduledWriteRequests()
    {
        synchronized( writeRequestQueue )
        {
            return writeRequestQueue.size();
        }
    }

    protected void write0( WriteRequest writeRequest )
    {
        filterChain.filterWrite( this, writeRequest );
    }

    public TransportType getTransportType()
    {
        return TransportType.SOCKET;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }

    public boolean getKeepAlive() throws SocketException
    {
        return ch.socket().getKeepAlive();
    }

    public void setKeepAlive( boolean on ) throws SocketException
    {
        ch.socket().setKeepAlive( on );
    }

    public boolean getOOBInline() throws SocketException
    {
        return ch.socket().getOOBInline();
    }

    public void setOOBInline( boolean on ) throws SocketException
    {
        ch.socket().setOOBInline( on );
    }

    public boolean getReuseAddress() throws SocketException
    {
        return ch.socket().getReuseAddress();
    }

    public void setReuseAddress( boolean on ) throws SocketException
    {
        ch.socket().setReuseAddress( on );
    }

    public int getSoLinger() throws SocketException
    {
        return ch.socket().getSoLinger();
    }

    public void setSoLinger( boolean on, int linger ) throws SocketException
    {
        ch.socket().setSoLinger( on, linger );
    }

    public boolean getTcpNoDelay() throws SocketException
    {
        return ch.socket().getTcpNoDelay();
    }

    public void setTcpNoDelay( boolean on ) throws SocketException
    {
        ch.socket().setTcpNoDelay( on );
    }

    public int getTrafficClass() throws SocketException
    {
        return ch.socket().getTrafficClass();
    }

    public void setTrafficClass( int tc ) throws SocketException
    {
        ch.socket().setTrafficClass( tc );
    }

    public int getSendBufferSize() throws SocketException
    {
        return ch.socket().getSendBufferSize();
    }

    public void setSendBufferSize( int size ) throws SocketException
    {
        ch.socket().setSendBufferSize( size );
    }

    public int getReceiveBufferSize() throws SocketException
    {
        return ch.socket().getReceiveBufferSize();
    }

    public void setReceiveBufferSize( int size ) throws SocketException
    {
        ch.socket().setReceiveBufferSize( size );
    }
    
    public int getSessionReceiveBufferSize()
    {
        return readBufferSize;
    }
    
    public void setSessionReceiveBufferSize( int size )
    {
        if( size <= 0 )
        {
            throw new IllegalArgumentException( "Invalid session receive buffer size: " + size );
        }
        
        this.readBufferSize = size;
    }

    protected void updateTrafficMask()
    {
        this.ioProcessor.updateTrafficMask( this );
    }
}
