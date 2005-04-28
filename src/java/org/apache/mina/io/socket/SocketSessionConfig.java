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

import java.net.SocketException;

import org.apache.mina.common.BaseSessionConfig;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolSession;

/**
 * A {@link SessionConfig} for socket transport (TCP/IP).
 * You can downcast {@link SessionConfig} instance returned by
 * {@link IoSession#getConfig()} or {@link ProtocolSession#getConfig()}
 * if you've created datagram session using {@link SocketAcceptor} or 
 * {@link SocketConnector}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class SocketSessionConfig extends BaseSessionConfig
{
    private static final int DEFAULT_READ_BUFFER_SIZE = 1024;

    private final SocketSession session;
    
    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;

    SocketSessionConfig( SocketSession session )
    {
        this.session = session;
    }

    public boolean getKeepAlive() throws SocketException
    {
        return session.getChannel().socket().getKeepAlive();
    }

    public void setKeepAlive( boolean on ) throws SocketException
    {
        session.getChannel().socket().setKeepAlive( on );
    }

    public boolean getOOBInline() throws SocketException
    {
        return session.getChannel().socket().getOOBInline();
    }

    public void setOOBInline( boolean on ) throws SocketException
    {
        session.getChannel().socket().setOOBInline( on );
    }

    public boolean getReuseAddress() throws SocketException
    {
        return session.getChannel().socket().getReuseAddress();
    }

    public void setReuseAddress( boolean on ) throws SocketException
    {
        session.getChannel().socket().setReuseAddress( on );
    }

    public int getSoLinger() throws SocketException
    {
        return session.getChannel().socket().getSoLinger();
    }

    public void setSoLinger( boolean on, int linger ) throws SocketException
    {
        session.getChannel().socket().setSoLinger( on, linger );
    }

    public boolean getTcpNoDelay() throws SocketException
    {
        return session.getChannel().socket().getTcpNoDelay();
    }

    public void setTcpNoDelay( boolean on ) throws SocketException
    {
        session.getChannel().socket().setTcpNoDelay( on );
    }

    public int getTrafficClass() throws SocketException
    {
        return session.getChannel().socket().getTrafficClass();
    }

    public void setTrafficClass( int tc ) throws SocketException
    {
        session.getChannel().socket().setTrafficClass( tc );
    }

    public int getSendBufferSize() throws SocketException
    {
        return session.getChannel().socket().getSendBufferSize();
    }

    public void setSendBufferSize( int size ) throws SocketException
    {
        session.getChannel().socket().setSendBufferSize( size );
    }

    public int getReceiveBufferSize() throws SocketException
    {
        return session.getChannel().socket().getReceiveBufferSize();
    }

    public void setReceiveBufferSize( int size ) throws SocketException
    {
        session.getChannel().socket().setReceiveBufferSize( size );
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
}