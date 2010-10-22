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
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DatagramSessionConfig;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class NioDatagramSession extends NioSession {
    static final TransportMetadata METADATA = new DefaultTransportMetadata("nio", "datagram", true, false,
            InetSocketAddress.class, DatagramSessionConfig.class, IoBuffer.class);

    private final InetSocketAddress localAddress;

    private final InetSocketAddress remoteAddress;

    /**
     * Creates a new acceptor-side session instance.
     */
    NioDatagramSession(IoService service, DatagramChannel channel, IoProcessor<NioSession> processor,
            SocketAddress remoteAddress) {
        super(processor, service, channel);
        config = new NioDatagramSessionConfig(channel);
        config.setAll(service.getSessionConfig());
        this.remoteAddress = (InetSocketAddress) remoteAddress;
        this.localAddress = (InetSocketAddress) channel.socket().getLocalSocketAddress();
    }

    /**
     * Creates a new connector-side session instance.
     */
    NioDatagramSession(IoService service, DatagramChannel channel, IoProcessor<NioSession> processor) {
        this(service, channel, processor, channel.socket().getRemoteSocketAddress());
    }

    /**
     * {@inheritDoc}
     */
    public DatagramSessionConfig getConfig() {
        return (DatagramSessionConfig) config;
    }

    @Override
    DatagramChannel getChannel() {
        return (DatagramChannel) channel;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }
}