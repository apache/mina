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
import java.nio.channels.SocketChannel;

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
    private final SocketChannel ch;

    SocketSessionConfig( SocketChannel ch )
    {
        this.ch = ch;
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
}