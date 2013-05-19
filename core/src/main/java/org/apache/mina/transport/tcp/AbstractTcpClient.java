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
package org.apache.mina.transport.tcp;

import org.apache.mina.service.client.AbstractIoClient;
import org.apache.mina.service.executor.IoHandlerExecutor;

/**
 * Base class for TCP based Clients
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractTcpClient extends AbstractIoClient {

    /** the connection timeout in milliseconds, after that delay the connection to remote server should fail. */
    private int connectTimeoutInMillis = 10000;

    /**
     * Create an new AbsractTcpClient instance
     */
    protected AbstractTcpClient(IoHandlerExecutor ioHandlerExecutor) {
        super(ioHandlerExecutor);
        config = new DefaultTcpSessionConfig();
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
    public void setSessionConfig(final TcpSessionConfig config) {
        this.config = config;
    }

    /**
     * Returns the connect timeout in milliseconds. The default value is 10 seconds.
     * 
     * @return the connect timeout in milliseconds
     */
    public int getConnectTimeoutMillis() {
        return connectTimeoutInMillis;
    }

    /**
     * Sets the connect timeout in milliseconds. The default value is 1 minute.
     * 
     * @param connectTimeoutInMillis Connection timeout in ms
     */
    public void setConnectTimeoutMillis(int connectTimeoutInMillis) {
        this.connectTimeoutInMillis = connectTimeoutInMillis;
    }
}
