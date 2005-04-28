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
package org.apache.mina.io.socket;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.mina.common.BaseSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilterChain;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.IoSessionFilterChain;
import org.apache.mina.io.IoSessionManagerFilterChain;
import org.apache.mina.util.Queue;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class SocketSession extends BaseSession implements IoSession
{
    private final IoSessionManagerFilterChain managerFilterChain;
    
    private final IoSessionFilterChain filterChain;

    private final SocketChannel ch;

    private final SocketSessionConfig config;

    private final Queue writeBufferQueue;

    private final Queue writeMarkerQueue;

    private final IoHandler handler;

    private final SocketAddress remoteAddress;

    private final SocketAddress localAddress;

    private SelectionKey key;
    
    private boolean disposed;

    /**
     * Creates a new instance.
     */
    SocketSession( IoSessionManagerFilterChain managerFilterChain,
                   SocketChannel ch, IoHandler defaultHandler )
    {
        this.managerFilterChain = managerFilterChain;
        this.filterChain = new IoSessionFilterChain( managerFilterChain );
        this.ch = ch;
        this.config = new SocketSessionConfig( this );
        this.writeBufferQueue = new Queue();
        this.writeMarkerQueue = new Queue();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
    }
    
    IoSessionManagerFilterChain getManagerFilterChain()
    {
        return managerFilterChain;
    }
    
    public IoHandlerFilterChain getFilterChain()
    {
        return filterChain;
    }

    SocketChannel getChannel()
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
    
    synchronized void notifyClose()
    {
        if( !disposed )
        {
            disposed = true;
            notify();
        }
    }
    

    public synchronized void close( boolean wait )
    {
        if( disposed )
        {
            return;
        }

        SocketIoProcessor.getInstance().removeSession( this );
        if( wait )
        {
            while( disposed )
            {
                try
                {
                    wait();
                }
                catch( InterruptedException e )
                {
                }
            }
        }
    }

    Queue getWriteBufferQueue()
    {
        return writeBufferQueue;
    }

    Queue getWriteMarkerQueue()
    {
        return writeMarkerQueue;
    }

    public void write( ByteBuffer buf, Object marker )
    {
        filterChain.filterWrite( this, buf, marker );
    }

    public TransportType getTransportType()
    {
        return TransportType.SOCKET;
    }

    public boolean isConnected()
    {
        return ch.isConnected();
    }

    public SessionConfig getConfig()
    {
        return config;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }
}
