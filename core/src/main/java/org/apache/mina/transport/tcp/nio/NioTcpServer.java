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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.transport.tcp.AbstractTcpServer;
import org.apache.mina.transport.tcp.DefaultTcpSessionConfig;
import org.apache.mina.transport.tcp.NioSelectorProcessor;
import org.apache.mina.transport.tcp.TcpSessionConfig;
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
    private final Map<SocketAddress /* bound address */,NioSelectorProcessor /* used processor */> addresses = new HashMap<SocketAddress, NioSelectorProcessor>();
    
    // the strategy for dispatching servers and client to selector threads.
    private final SelectorStrategy<NioSelectorProcessor> strategy;

    // the default session confinguration
    private TcpSessionConfig config;

    private boolean reuseAddress = false;

    public NioTcpServer(final SelectorStrategy<NioSelectorProcessor> strategy) {
        super();
        this.strategy = strategy;
        this.config = new DefaultTcpSessionConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TcpSessionConfig getSessionConfig() {
        return this.config;
    }

    public void setSessionConfig(final TcpSessionConfig config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReuseAddress() {
        return this.reuseAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void bind(final SocketAddress... localAddress) throws IOException {
        if (localAddress == null) {
            // We should at least have one address to bind on
            throw new IllegalArgumentException("LocalAdress cannot be null");
        }

        for (SocketAddress address : localAddress) {
            // check if the address is already bound
            synchronized (this) {
                if (this.addresses.containsKey(address)) {
                    throw new IOException("address " + address + " already bound");
                }

                LOG.info("binding address {}", address);
                NioSelectorProcessor processor = this.strategy.getSelectorForBindNewAddress();
                
                this.addresses.put(address,processor);
                
                processor.bindTcpServer(this, address);
                
                if (this.addresses.size() == 1) {
                    // it's the first address bound, let's fire the event
                    this.fireServiceActivated();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<SocketAddress> getLocalAddresses() {
        return new HashSet<SocketAddress>(addresses.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void unbind(final SocketAddress... localAddresses) throws IOException {
        for (SocketAddress socketAddress : localAddresses) {
            LOG.info("unbinding {}", socketAddress);
            addresses.get(socketAddress).unbind(socketAddress);
            this.addresses.remove(socketAddress);
            if (this.addresses.isEmpty()) {
                this.fireServiceInactivated();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void unbindAll() throws IOException {
    	LOG.info("unbinding all");
    	for(SocketAddress address : addresses.keySet()) {
			LOG.debug("unbinding {}", address);
    		addresses.remove(address).unbind(address);
    	}
    }

}