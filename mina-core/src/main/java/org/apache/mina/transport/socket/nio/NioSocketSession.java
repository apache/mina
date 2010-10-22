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
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.AbstractSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class NioSocketSession extends NioSession {
    static final TransportMetadata METADATA = new DefaultTransportMetadata("nio", "socket", false, true,
            InetSocketAddress.class, SocketSessionConfig.class, IoBuffer.class, FileRegion.class);

    private Socket getSocket() {
        return ((SocketChannel) channel).socket();
    }

    /**
     * 
     * Creates a new instance of NioSocketSession.
     *
     * @param service the associated IoService 
     * @param processor the associated IoProcessor
     * @param ch the used channel
     */
    public NioSocketSession(IoService service, IoProcessor<NioSession> processor, SocketChannel channel) {
        super(processor, service, channel);
        config = new SessionConfigImpl();
        this.config.setAll(service.getSessionConfig());
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    /**
     * {@inheritDoc}
     */
    public SocketSessionConfig getConfig() {
        return (SocketSessionConfig) config;
    }

    @Override
    SocketChannel getChannel() {
        return (SocketChannel) channel;
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }

        Socket socket = getSocket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }

        Socket socket = getSocket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }

    private class SessionConfigImpl extends AbstractSocketSessionConfig {
        public boolean isKeepAlive() {
            try {
                return getSocket().getKeepAlive();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setKeepAlive(boolean on) {
            try {
                getSocket().setKeepAlive(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isOobInline() {
            try {
                return getSocket().getOOBInline();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setOobInline(boolean on) {
            try {
                getSocket().setOOBInline(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isReuseAddress() {
            try {
                return getSocket().getReuseAddress();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReuseAddress(boolean on) {
            try {
                getSocket().setReuseAddress(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSoLinger() {
            try {
                return getSocket().getSoLinger();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSoLinger(int linger) {
            try {
                if (linger < 0) {
                    getSocket().setSoLinger(false, 0);
                } else {
                    getSocket().setSoLinger(true, linger);
                }
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public boolean isTcpNoDelay() {
            if (!isConnected()) {
                return false;
            }

            try {
                return getSocket().getTcpNoDelay();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setTcpNoDelay(boolean on) {
            try {
                getSocket().setTcpNoDelay(on);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getTrafficClass() {
            try {
                return getSocket().getTrafficClass();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setTrafficClass(int tc) {
            try {
                getSocket().setTrafficClass(tc);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getSendBufferSize() {
            try {
                return getSocket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setSendBufferSize(int size) {
            try {
                getSocket().setSendBufferSize(size);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public int getReceiveBufferSize() {
            try {
                return getSocket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }

        public void setReceiveBufferSize(int size) {
            try {
                getSocket().setReceiveBufferSize(size);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }
    }
}
