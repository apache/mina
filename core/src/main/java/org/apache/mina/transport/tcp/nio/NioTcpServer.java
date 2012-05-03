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
package org.apache.mina.transport.tcp.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.transport.tcp.AbstractTcpServer;
import org.apache.mina.transport.tcp.DefaultSocketSessionConfig;
import org.apache.mina.transport.tcp.NioSelectorProcessor;
import org.apache.mina.transport.tcp.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a TCP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpServer extends AbstractTcpServer {
    static final Logger LOG = LoggerFactory.getLogger(NioTcpServer.class);

    // list of bound addresses
    private final Set<SocketAddress> addresses = Collections.synchronizedSet(new HashSet<SocketAddress>());

    // the strategy for dispatching servers and client to selector threads.
    private final SelectorStrategy strategy;

    private SocketSessionConfig config;

    private boolean reuseAddress = false;

    public NioTcpServer(final SelectorStrategy strategy) {
        super();
        this.strategy = strategy;
        this.config = new DefaultSocketSessionConfig();
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return this.config;
    }

    public void setSessionConfig(final SocketSessionConfig config) {
        this.config = config;
    }

    @Override
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    @Override
    public boolean isReuseAddress() {
        return this.reuseAddress;
    }

    @Override
    public void bind(final SocketAddress... localAddress) throws IOException {
        if (localAddress == null) {
            // We should at least have one address to bind on
            throw new IllegalStateException("LocalAdress cannot be null");
        }

        for (SocketAddress address : localAddress) {
            // check if the address is already bound
            synchronized (this) {
                if (this.addresses.contains(address)) {
                    throw new IOException("address " + address + " already bound");
                }

                LOG.debug("binding address {}", address);

                this.addresses.add(address);
                NioSelectorProcessor processor = (NioSelectorProcessor) this.strategy.getSelectorForBindNewAddress();
                processor.bindAndAcceptAddress(this, address);
                if (this.addresses.size() == 1) {
                    // it's the first address bound, let's fire the event
                    this.fireServiceActivated();
                }
            }
        }
    }

    @Override
    public Set<SocketAddress> getLocalAddresses() {
        return this.addresses;
    }

    @Override
    public void unbind(final SocketAddress... localAddresses) throws IOException {
        for (SocketAddress socketAddress : localAddresses) {
            LOG.debug("unbinding {}", socketAddress);
            synchronized (this) {
                this.strategy.unbind(socketAddress);
                this.addresses.remove(socketAddress);
                if (this.addresses.isEmpty()) {
                    this.fireServiceInactivated();
                }
            }
        }
    }

    @Override
    public void unbindAll() throws IOException {
        for (SocketAddress socketAddress : this.addresses) {
            this.unbind(socketAddress);
        }
    }

}