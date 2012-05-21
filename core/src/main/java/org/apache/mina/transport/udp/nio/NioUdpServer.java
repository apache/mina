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
package org.apache.mina.transport.udp.nio;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.transport.udp.AbstractUdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a UDP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpServer extends AbstractUdpServer {
	
	static final Logger LOG = LoggerFactory.getLogger(NioUdpServer.class);

    // list of bound addresses
    private final Set<SocketAddress> addresses = Collections.synchronizedSet(new HashSet<SocketAddress>());

    // the strategy for dispatching servers and client to selector threads.
    private final SelectorStrategy strategy;

    /**
     * Create a new instance of NioUdpServer
     */
    public NioUdpServer(final SelectorStrategy strategy) {
        super();
        this.strategy = strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoSessionConfig getSessionConfig() {
        // TODO Auto-generated method stub
        return null;
    }
}
