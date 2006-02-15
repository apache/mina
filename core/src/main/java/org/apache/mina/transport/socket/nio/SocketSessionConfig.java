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

import java.net.Socket;

import org.apache.mina.common.IoSessionConfig;

/**
 * An {@link IoSessionConfig} for socket transport type.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface SocketSessionConfig extends IoSessionConfig
{
    /**
     * @see Socket#getReuseAddress()
     */
    boolean isReuseAddress();
    
    /**
     * @see Socket#setReuseAddress(boolean)
     */
    void setReuseAddress( boolean reuseAddress );

    /**
     * @see Socket#getReceiveBufferSize()
     */
    int getReceiveBufferSize();

    /**
     * @see Socket#setReceiveBufferSize(int)
     */
    void setReceiveBufferSize( int receiveBufferSize );

    /**
     * @see Socket#getSendBufferSize()
     */
    int getSendBufferSize();

    /**
     * @see Socket#setSendBufferSize(int)
     */
    void setSendBufferSize( int sendBufferSize );

    /**
     * @see Socket#getTrafficClass()
     */
    int getTrafficClass();

    /**
     * @see Socket#setTrafficClass(int)
     */
    void setTrafficClass( int trafficClass );

    /**
     * @see Socket#getKeepAlive()
     */
    boolean isKeepAlive();

    /**
     * @see Socket#setKeepAlive(boolean)
     */
    void setKeepAlive( boolean keepAlive );

    /**
     * @see Socket#getOOBInline()
     */
    boolean isOobInline();

    /**
     * @see Socket#setOOBInline(boolean)
     */
    void setOobInline( boolean oobInline );

    /**
     * @see Socket#getSoLinger()
     */
    int getSoLinger();

    /**
     * @param soLinger Please specify a negative value to disable <tt>SO_LINGER</tt>.
     * 
     * @see Socket#setSoLinger(boolean, int)
     */
    void setSoLinger( int soLinger );

    /**
     * @see Socket#getTcpNoDelay()
     */
    boolean isTcpNoDelay();

    /**
     * @see Socket#setTcpNoDelay(boolean)
     */
    void setTcpNoDelay( boolean tcpNoDelay );
}
