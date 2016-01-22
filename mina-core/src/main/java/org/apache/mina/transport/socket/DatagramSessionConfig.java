/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.transport.socket;

import java.net.DatagramSocket;
import java.net.PortUnreachableException;

import org.apache.mina.core.session.IoSessionConfig;

/**
 * An {@link IoSessionConfig} for datagram transport type.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface DatagramSessionConfig extends IoSessionConfig {
    /**
     * @see DatagramSocket#getBroadcast()
     * 
     * @return <tt>true</tt> if SO_BROADCAST is enabled.
     */
    boolean isBroadcast();

    /**
     * @see DatagramSocket#setBroadcast(boolean)
     * 
     * @param broadcast Tells if SO_BROACAST is enabled or not 
     */
    void setBroadcast(boolean broadcast);

    /**
     * @see DatagramSocket#getReuseAddress()
     * 
     * @return <tt>true</tt> if SO_REUSEADDR is enabled.
     */
    boolean isReuseAddress();

    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     * 
     * @param reuseAddress Tells if SO_REUSEADDR is enabled or disabled
     */
    void setReuseAddress(boolean reuseAddress);

    /**
     * @see DatagramSocket#getReceiveBufferSize()
     * 
     * @return the size of the receive buffer
     */
    int getReceiveBufferSize();

    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     * 
     * @param receiveBufferSize The size of the receive buffer
     */
    void setReceiveBufferSize(int receiveBufferSize);

    /**
     * @see DatagramSocket#getSendBufferSize()
     * 
     * @return the size of the send buffer
     */
    int getSendBufferSize();

    /**
     * @see DatagramSocket#setSendBufferSize(int)
     * 
     * @param sendBufferSize The size of the send buffer
     */
    void setSendBufferSize(int sendBufferSize);

    /**
     * @see DatagramSocket#getTrafficClass()
     * 
     * @return the traffic class
     */
    int getTrafficClass();

    /**
     * @see DatagramSocket#setTrafficClass(int)
     * 
     * @param trafficClass The traffic class to set, one of IPTOS_LOWCOST (0x02)
     * IPTOS_RELIABILITY (0x04), IPTOS_THROUGHPUT (0x08) or IPTOS_LOWDELAY (0x10)
     */
    void setTrafficClass(int trafficClass);

    /**
     * If method returns true, it means session should be closed when a
     * {@link PortUnreachableException} occurs.
     * 
     * @return Tells if we should close if the port is unreachable
     */
    boolean isCloseOnPortUnreachable();

    /**
     * Sets if the session should be closed if an {@link PortUnreachableException} 
     * occurs.
     * 
     * @param closeOnPortUnreachable <tt>true</tt> if we should close if the port is unreachable
     */
    void setCloseOnPortUnreachable(boolean closeOnPortUnreachable);
}
