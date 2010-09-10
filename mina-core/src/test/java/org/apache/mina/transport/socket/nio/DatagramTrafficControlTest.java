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

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.transport.AbstractTrafficControlTest;

/**
 * Tests suspending and resuming reads and writes for the datagram
 * transport type.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DatagramTrafficControlTest extends AbstractTrafficControlTest {

    public DatagramTrafficControlTest() {
        super(new NioDatagramAcceptor());
    }

    @Override
    protected ConnectFuture connect(int port, IoHandler handler)
            throws Exception {
        IoConnector connector = new NioDatagramConnector();
        connector.setHandler(handler);
        return connector.connect(new InetSocketAddress("localhost", port));
    }

    @Override
    protected SocketAddress createServerSocketAddress(int port) {
        return new InetSocketAddress(port);
    }

    @Override
    protected int getPort(SocketAddress address) {
        return ((InetSocketAddress) address).getPort();
    }
}
