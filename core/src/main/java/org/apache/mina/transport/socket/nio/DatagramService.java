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

import java.net.DatagramSocket;

import org.apache.mina.common.IoService;

/**
 * An {@link IoService} which provides some common properties related with
 * NIO UDP/IP sockets.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public interface DatagramService extends IoService
{
    /**
     * @see DatagramSocket#getBroadcast()
     */
    boolean getBroadcast();
    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    void setBroadcast( boolean broadcast );
    /**
     * @see DatagramSocket#getReuseAddress()
     */
    boolean getReuseAddress();
    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     */
    void setReuseAddress( boolean reuseAddress );
    /**
     * @see DatagramSocket#getReceiveBufferSize()
     */
    int getReceiveBufferSize();
    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     */
    void setReceiveBufferSize( int receiveBufferSize );
    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    int getSendBufferSize();
    /**
     * @see DatagramSocket#setSendBufferSize(int)
     */
    void setSendBufferSize( int sendBufferSize );
    /**
     * @see DatagramSocket#getTrafficClass()
     */
    int getTrafficClass();
    /**
     * @see DatagramSocket#setTrafficClass(int)
     */
    void setTrafficClass( int trafficClass );
}
