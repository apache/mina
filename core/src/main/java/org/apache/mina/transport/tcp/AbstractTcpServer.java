/*
 *  Licensed to the Apache Software Foundation () under one
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
package org.apache.mina.transport.tcp;

import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.server.AbstractIoServer;

/**
 * Base class for TCP based Servers
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractTcpServer extends AbstractIoServer {

    /**
     * Create an new AbsractTcpServer instance
     * 
     * @param eventExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O one).
     *        Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    protected AbstractTcpServer(IoHandlerExecutor eventExecutor) {
        super(new DefaultTcpSessionConfig(), eventExecutor);
    }

    /**
     * Create an new AbsractTcpServer instance, with a specific configuration
     * 
     * @param sessionConfig The configuration to use for this server
     * @param eventExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O one).
     *        Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    protected AbstractTcpServer(TcpSessionConfig config, IoHandlerExecutor eventExecutor) {
        super(config, eventExecutor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TcpSessionConfig getSessionConfig() {
        return (TcpSessionConfig) config;
    }

    /**
     * Set the default configuration for created TCP sessions
     * 
     * @param config
     */
    public void setSessionConfig(TcpSessionConfig config) {
        this.config = config;
    }
}