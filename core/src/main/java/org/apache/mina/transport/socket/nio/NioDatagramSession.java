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
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.DefaultTransportMetadata;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfig;
import org.apache.mina.transport.socket.DatagramSession;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class NioDatagramSession extends NioSession implements DatagramSession {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "datagram", true, false,
                    InetSocketAddress.class,
                    DatagramSessionConfig.class, ByteBuffer.class);

    private final IoService service;

    private final DatagramSessionConfig config = new SessionConfigImpl();

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);

    private final DatagramChannel ch;

    private final IoHandler handler;

    private final InetSocketAddress localAddress;

    private final InetSocketAddress remoteAddress;
    
    private final IoProcessor processor;

    private SelectionKey key;

    /**
     * Creates a new acceptor-side session instance.
     */
    NioDatagramSession(IoService service,
                        DatagramChannel ch, IoProcessor processor,
                        SocketAddress remoteAddress) {
        this.service = service;
        this.ch = ch;
        this.handler = service.getHandler();
        this.processor = processor;
        this.remoteAddress = (InetSocketAddress) remoteAddress;
        this.localAddress = (InetSocketAddress) ch.socket()
                .getLocalSocketAddress();

        this.config.setAll(service.getSessionConfig());
    }

    /**
     * Creates a new connector-side session instance.
     */
    NioDatagramSession(IoService service,
                        DatagramChannel ch, IoProcessor processor) {
        this(service, ch, processor, ch.socket().getRemoteSocketAddress());
    }

    public IoService getService() {
        return service;
    }

    @Override
    protected IoProcessor getProcessor() {
        return processor;
    }

    public DatagramSessionConfig getConfig() {
        return config;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    DatagramChannel getChannel() {
        return ch;
    }

    @Override
    SelectionKey getSelectionKey() {
        return key;
    }

    @Override
    void setSelectionKey(SelectionKey key) {
        this.key = key;
    }

    public IoHandler getHandler() {
        return handler;
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

    private class SessionConfigImpl extends AbstractDatagramSessionConfig {

        public int getReceiveBufferSize() {
            try {
                return ch.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReceiveBufferSize(int receiveBufferSize) {
            if (DefaultDatagramSessionConfig.isSetReceiveBufferSizeAvailable()) {
                try {
                    ch.socket().setReceiveBufferSize(receiveBufferSize);
                    // Re-retrieve the effective receive buffer size.
                    receiveBufferSize = ch.socket().getReceiveBufferSize();
                } catch (SocketException e) {
                    throw new RuntimeIoException(e);
                }
            }
        }

        public boolean isBroadcast() {
            try {
                return ch.socket().getBroadcast();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setBroadcast(boolean broadcast) {
            try {
                ch.socket().setBroadcast(broadcast);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSendBufferSize() {
            try {
                return ch.socket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSendBufferSize(int sendBufferSize) {
            if (DefaultDatagramSessionConfig.isSetSendBufferSizeAvailable()) {
                try {
                    ch.socket().setSendBufferSize(sendBufferSize);
                } catch (SocketException e) {
                    throw new RuntimeIoException(e);
                }
            }
        }

        public boolean isReuseAddress() {
            try {
                return ch.socket().getReuseAddress();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReuseAddress(boolean reuseAddress) {
            try {
                ch.socket().setReuseAddress(reuseAddress);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getTrafficClass() {
            if (DefaultDatagramSessionConfig.isGetTrafficClassAvailable()) {
                try {
                    return ch.socket().getTrafficClass();
                } catch (SocketException e) {
                    throw new RuntimeIoException(e);
                }
            } else {
                return 0;
            }
        }

        public void setTrafficClass(int trafficClass) {
            if (DefaultDatagramSessionConfig.isSetTrafficClassAvailable()) {
                try {
                    ch.socket().setTrafficClass(trafficClass);
                } catch (SocketException e) {
                    throw new RuntimeIoException(e);
                }
            }
        }
    }
}