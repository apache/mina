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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.util.BasicSessionConfig;

/**
 * A {@link SessionConfig} for socket transport (TCP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class SocketSessionConfig extends BasicSessionConfig
{
    private final SocketSession session;

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
    
    public void getSessionReceiveBufferSize()
    {
        session.getReadBuffer().capacity();
    }
    
    public void setSessionReceiveBufferSize( int size )
    {
        session.setReadBuffer( ByteBuffer.allocate( size ).limit( 0 ) );
    }
}