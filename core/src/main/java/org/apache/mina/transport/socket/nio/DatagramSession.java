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
import java.net.SocketException;

import org.apache.mina.common.IoSession;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface DatagramSession extends IoSession
{
    /**
     * @see DatagramSocket#getReceiveBufferSize()
     */
    int getReceiveBufferSize() throws SocketException;
    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     */
    void setReceiveBufferSize( int receiveBufferSize ) throws SocketException;
    /**
     * @see DatagramSocket#getReuseAddress()
     */
    boolean getReuseAddress() throws SocketException;
    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     */
    void setReuseAddress( boolean reuseAddress ) throws SocketException;
    /**
     * @see DatagramSocket#getBroadcast()
     */
    boolean getBroadcast() throws SocketException;
    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    void setBroadcast( boolean broadcast ) throws SocketException;
    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    int getSendBufferSize() throws SocketException;
    /**
     * @see DatagramSocket#setSendBufferSize(int)
     */
    void setSendBufferSize( int sendBufferSize ) throws SocketException;
    /**
     * @see DatagramSocket#getTrafficClass()
     */
    int getTrafficClass() throws SocketException;
    /**
     * @see DatagramSocket#setTrafficClass(int)
     */
    void setTrafficClass( int trafficClass ) throws SocketException;
}