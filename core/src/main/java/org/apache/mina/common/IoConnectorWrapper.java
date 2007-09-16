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

import java.net.SocketAddress;
import java.util.Set;


/**
 * An {@link IoConnector} that wraps the other {@link IoConnector}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoConnectorWrapper implements IoConnector {
    protected IoConnector connector;

    /**
     * Creates a new instance.
     */
    protected IoConnectorWrapper() {
    }

    /**
     * Sets the connector to be wrapped.  This method should be invoked before any operation
     * is requested.
     */
    protected void init(IoConnector connector) {
        this.connector = connector;
    }

    public IoFilterChainBuilder getFilterChainBuilder() {
        return connector.getFilterChainBuilder();
    }

    public void setFilterChainBuilder(IoFilterChainBuilder builder) {
        connector.setFilterChainBuilder(builder);
    }

    public DefaultIoFilterChainBuilder getFilterChain() {
        return connector.getFilterChain();
    }

    public void addListener(IoServiceListener listener) {
        connector.addListener(listener);
    }

    public void removeListener(IoServiceListener listener) {
        connector.removeListener(listener);
    }

    public ConnectFuture connect(SocketAddress remoteAddress) {
        return connector.connect(remoteAddress);
    }

    public ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        return connector.connect(remoteAddress, localAddress);
    }

    public int getConnectTimeout() {
        return connector.getConnectTimeout();
    }

    public long getConnectTimeoutMillis() {
        return connector.getConnectTimeoutMillis();
    }

    public void setConnectTimeout(int connectTimeout) {
        connector.setConnectTimeout(connectTimeout);
    }

    public IoHandler getHandler() {
        return connector.getHandler();
    }

    public Set<IoSession> getManagedSessions() {
        return connector.getManagedSessions();
    }

    public IoSessionConfig getSessionConfig() {
        return connector.getSessionConfig();
    }

    public void setHandler(IoHandler handler) {
        connector.setHandler(handler);
    }

    public TransportMetadata getTransportMetadata() {
        return connector.getTransportMetadata();
    }
}
