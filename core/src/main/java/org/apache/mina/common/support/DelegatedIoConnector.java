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
package org.apache.mina.common.support;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;

/**
 * A delegated {@link IoConnector} that wraps the other {@link IoConnector}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DelegatedIoConnector implements IoConnector {
    protected IoConnector delegate;

    /**
     * Creates a new instance.
     */
    protected DelegatedIoConnector() {
    }

    /**
     * Sets the delegate.  This method should be invoked before any operation
     * is requested.
     */
    protected void init(IoConnector delegate) {
        this.delegate = delegate;
    }

    public ConnectFuture connect(SocketAddress address, IoHandler handler) {
        return delegate.connect(address, handler);
    }

    public ConnectFuture connect(SocketAddress address, IoHandler handler,
            IoServiceConfig config) {
        return delegate.connect(address, handler, config);
    }

    public ConnectFuture connect(SocketAddress address,
            SocketAddress localAddress, IoHandler handler) {
        return delegate.connect(address, localAddress, handler);
    }

    public ConnectFuture connect(SocketAddress address,
            SocketAddress localAddress, IoHandler handler,
            IoServiceConfig config) {
        return delegate.connect(address, localAddress, handler, config);
    }

    public boolean isManaged(SocketAddress serviceAddress) {
        return delegate.isManaged(serviceAddress);
    }

    public Set<SocketAddress> getManagedServiceAddresses() {
        return delegate.getManagedServiceAddresses();
    }

    public Set<IoSession> getManagedSessions(SocketAddress serviceAddress) {
        return delegate.getManagedSessions(serviceAddress);
    }

    public IoServiceConfig getDefaultConfig() {
        return delegate.getDefaultConfig();
    }

    public IoFilterChainBuilder getFilterChainBuilder() {
        return delegate.getFilterChainBuilder();
    }

    public void setFilterChainBuilder(IoFilterChainBuilder builder) {
        delegate.setFilterChainBuilder(builder);
    }

    public DefaultIoFilterChainBuilder getFilterChain() {
        return delegate.getFilterChain();
    }

    public void addListener(IoServiceListener listener) {
        delegate.addListener(listener);
    }

    public void removeListener(IoServiceListener listener) {
        delegate.removeListener(listener);
    }
}
