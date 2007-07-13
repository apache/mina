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
package org.apache.mina.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.common.support.DefaultTransportType;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeSessionConfig;

/**
 * Represents a network transport type and its related metadata.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface TransportType {

    /**
     * A pre-defined transport type for TCP/IP socket transport.
     */
    static TransportType SOCKET = new DefaultTransportType("socket", false,
            InetSocketAddress.class, ByteBuffer.class,
            SocketSessionConfig.class);

    /**
     * A pre-defined transport type for UDP/IP datagram transport.
     */
    static TransportType DATAGRAM = new DefaultTransportType("datagram", true,
            InetSocketAddress.class, ByteBuffer.class,
            DatagramSessionConfig.class);

    /**
     * A pre-defined transport type for in-VM pipe transport.
     */
    static TransportType VM_PIPE = new DefaultTransportType("vmpipe", false,
            VmPipeAddress.class, Object.class, VmPipeSessionConfig.class);

    /**
     * Returns the name of this transport type.
     */
    String getName();

    /**
     * Returns <code>true</code> if the session of this transport type is
     * connectionless.
     */
    boolean isConnectionless();

    /**
     * Returns the address type of this transport type.
     */
    Class<? extends SocketAddress> getAddressType();

    /**
     * Returns the type of the envelope message of this transport type.
     */
    Class<? extends Object> getEnvelopeType();

    /**
     * Returns the type of the {@link IoSessionConfig} of this transport type.
     */
    Class<? extends IoSessionConfig> getSessionConfigType();
}
