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
package org.apache.mina.transport.udp;

import java.net.SocketAddress;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.service.client.AbstractIoClient;
import org.apache.mina.service.executor.IoHandlerExecutor;

/**
 * Base class for UDP based Clients
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractUdpClient extends AbstractIoClient {
    /**
     * Create an new AbsractUdpClient instance
     */
    protected AbstractUdpClient(IoHandlerExecutor ioHandlerExecutor) {
        super(ioHandlerExecutor);
        this.config = new DefaultUdpSessionConfig();
    }

    /**
     * Connects to the specified remote address binding to the specified local address.
     * 
     * @param remoteAddress Remote {@link SocketAddress} to connect
     * @param localAddress Local {@link SocketAddress} to use while initiating connection to remote
     *        {@link SocketAddress}
     * @return the {@link IoFuture} instance which is completed when the connection attempt initiated by this call
     *         succeeds or fails.
     */
    public abstract IoFuture<IoSession> connect(SocketAddress remoteAddress, SocketAddress localAddress);

    /**
     * {@inheritDoc}
     */
    @Override
    public UdpSessionConfig getSessionConfig() {
        return (UdpSessionConfig) config;
    }
}
