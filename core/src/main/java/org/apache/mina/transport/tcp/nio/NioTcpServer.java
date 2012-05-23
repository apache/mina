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
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.transport.tcp.AbstractTcpServer;
import org.apache.mina.transport.tcp.NioSelectorProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a TCP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpServer extends AbstractTcpServer {
    static final Logger LOG = LoggerFactory.getLogger(NioTcpServer.class);
    
    // the strategy for dispatching servers and client to selector threads.
    private final SelectorStrategy<NioSelectorProcessor> strategy;

    // the bound local address
    private SocketAddress address = null;
    
    private NioSelectorProcessor acceptProcessor = null;
    
    // the key used for selecting accept event
    private SelectionKey acceptKey = null;
    
    // the server socket for accepting clients
    private ServerSocketChannel serverChannel = null;  
    
    public NioTcpServer(final SelectorStrategy<NioSelectorProcessor> strategy) {
        super();
        this.strategy = strategy;
    }
    
    /**
     * Get the inner Server socket for accepting new client connections
     * @return
     */
    public ServerSocketChannel getServerSocketChannel() {
    	return this.serverChannel;
    }

    public void setServerSocketChannel(ServerSocketChannel serverChannel) {
    	this.serverChannel = serverChannel;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void bind(final SocketAddress localAddress) throws IOException {
        if (localAddress == null) {
            // We should at least have one address to bind on
            throw new IllegalArgumentException("LocalAdress cannot be null");
        }

        // check if the address is already bound
        if (this.address != null) {
            throw new IOException("address " + address + " already bound");
        }

        LOG.info("binding address {}", localAddress);
        this.address = localAddress;
        
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().setReuseAddress(isReuseAddress());
        serverChannel.socket().bind(address);
        serverChannel.configureBlocking(false);

        acceptProcessor = this.strategy.getSelectorForBindNewAddress();
            
        acceptProcessor.addServer(this);
                
        // it's the first address bound, let's fire the event
        this.fireServiceActivated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getBoundAddress() {
    	return address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void unbind() throws IOException {
    	LOG.info("unbinding {}", address);
    	if( this.address == null) { 
    		throw new IllegalStateException("server not bound");
        }
        serverChannel.socket().close();
        serverChannel.close();
    	acceptProcessor.removeServer(this);
        
        this.address = null;
        this.fireServiceInactivated();
    }

	/**
	 * @return the acceptKey
	 */
	public SelectionKey getAcceptKey() {
		return acceptKey;
	}

	/**
	 * @param acceptKey the acceptKey to set
	 */
	public void setAcceptKey(SelectionKey acceptKey) {
		this.acceptKey = acceptKey;
	}
    
}