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

import javax.net.ssl.SSLException;

import org.apache.mina.api.IoClient;
import org.apache.mina.api.IoSession;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.server.AbstractIoServer;

/**
 * Base implementation for all the UDP servers.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractUdpServer extends AbstractIoServer implements IoClient {
    /**
     * Create an new AbsractUdpServer instance
     * 
     * @param eventExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O one).
     *        Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    protected AbstractUdpServer(IoHandlerExecutor ioHandlerExecutor) {
        super(new DefaultUdpSessionConfig(), ioHandlerExecutor);
    }

    /**
     * Create an new AbsractUdpServer instance
     * 
     * @param sessionConfig The configuration to use for this server
     * @param eventExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O one).
     *        Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    protected AbstractUdpServer(UdpSessionConfig config, IoHandlerExecutor ioHandlerExecutor) {
        super(config, ioHandlerExecutor);
        this.config = new DefaultUdpSessionConfig();
    }

    /**
     * {@inheritDoc}
     */
    public void initSecured(IoSession session) throws SSLException {
        throw new IllegalStateException("SSL is not supported for UDP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UdpSessionConfig getSessionConfig() {
        return (UdpSessionConfig) config;
    }

    /**
     * {@inheritDoc}
     */
    public void setSessionConfig(UdpSessionConfig config) {
        this.config = config;
    }
}
