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
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandler;
import org.apache.mina.transport.AbstractTrafficControlTest;

/**
 * Tests suspending and resuming reads and writes for the
 * {@link org.apache.mina.common.TransportType#DATAGRAM} transport type. 
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Id$
 */
public class DatagramTrafficControlTest extends AbstractTrafficControlTest {

    public DatagramTrafficControlTest() {
        super(new DatagramAcceptor());
    }

    protected ConnectFuture connect(int port, IoHandler handler)
            throws Exception {
        IoConnector connector = new DatagramConnector();
        SocketAddress addr = new InetSocketAddress("localhost", port);
        return connector.connect(addr, handler);
    }

    protected SocketAddress createServerSocketAddress(int port) {
        return new InetSocketAddress(port);
    }

}
