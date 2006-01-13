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
package org.apache.mina.transport.socket.nio;

import java.net.SocketException;

import org.apache.mina.common.IoSession;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface SocketSession extends IoSession
{
    boolean getKeepAlive() throws SocketException;
    void setKeepAlive( boolean on ) throws SocketException;
    boolean getOOBInline() throws SocketException;
    void setOOBInline( boolean on ) throws SocketException;
    boolean getReuseAddress() throws SocketException;
    void setReuseAddress( boolean on ) throws SocketException;
    int getSoLinger() throws SocketException;
    void setSoLinger( boolean on, int linger ) throws SocketException;
    boolean getTcpNoDelay() throws SocketException;
    void setTcpNoDelay( boolean on ) throws SocketException;
    int getTrafficClass() throws SocketException;
    void setTrafficClass( int tc ) throws SocketException;
    int getSendBufferSize() throws SocketException;
    void setSendBufferSize( int size ) throws SocketException;
    int getReceiveBufferSize() throws SocketException;
    void setReceiveBufferSize( int size ) throws SocketException;
    int getSessionReceiveBufferSize();
    void setSessionReceiveBufferSize( int size );
}
