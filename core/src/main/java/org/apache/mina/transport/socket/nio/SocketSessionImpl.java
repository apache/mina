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

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.common.support.IoServiceListenerSupport;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class SocketSessionImpl extends BaseIoSession {
    private final IoService manager;

    private final IoServiceConfig serviceConfig;

    private final SocketSessionConfig config = new SessionConfigImpl();

    private final SocketIoProcessor ioProcessor;

    private final SocketFilterChain filterChain;

    private final SocketChannel ch;

    private final Queue<WriteRequest> writeRequestQueue;

    private final IoHandler handler;

    private final SocketAddress remoteAddress;

    private final SocketAddress localAddress;

    private final SocketAddress serviceAddress;

    private final IoServiceListenerSupport serviceListeners;

    private SelectionKey key;

    private int readBufferSize = 1024;
    private boolean deferDecreaseReadBufferSize = true;

    /**
     * Creates a new instance.
     */
    SocketSessionImpl(IoService manager, SocketIoProcessor ioProcessor,
            IoServiceListenerSupport listeners, IoServiceConfig serviceConfig,
            SocketChannel ch, IoHandler defaultHandler,
            SocketAddress serviceAddress) {
        this.manager = manager;
        this.serviceListeners = listeners;
        this.ioProcessor = ioProcessor;
        this.filterChain = new SocketFilterChain(this);
        this.ch = ch;
        this.writeRequestQueue = new ConcurrentLinkedQueue<WriteRequest>();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
        this.serviceAddress = serviceAddress;
        this.serviceConfig = serviceConfig;

        // Apply the initial session settings
        IoSessionConfig sessionConfig = serviceConfig.getSessionConfig();
        if (sessionConfig instanceof SocketSessionConfig) {
            SocketSessionConfig cfg = (SocketSessionConfig) sessionConfig;
            this.config.setKeepAlive(cfg.isKeepAlive());
            this.config.setOobInline(cfg.isOobInline());
            this.config.setReceiveBufferSize(cfg.getReceiveBufferSize());
            this.config.setReuseAddress(cfg.isReuseAddress());
            this.config.setSendBufferSize(cfg.getSendBufferSize());
            this.config.setSoLinger(cfg.getSoLinger());
            this.config.setTcpNoDelay(cfg.isTcpNoDelay());

            if (this.config.getTrafficClass() != cfg.getTrafficClass()) {
                this.config.setTrafficClass(cfg.getTrafficClass());
            }
        }
    }

    public IoService getService() {
        return manager;
    }

    public IoServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public IoSessionConfig getConfig() {
        return config;
    }

    SocketIoProcessor getIoProcessor() {
        return ioProcessor;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    SocketChannel getChannel() {
        return ch;
    }

    IoServiceListenerSupport getServiceListeners() {
        return serviceListeners;
    }

    SelectionKey getSelectionKey() {
        return key;
    }

    void setSelectionKey(SelectionKey key) {
        this.key = key;
    }

    public IoHandler getHandler() {
        return handler;
    }

    @Override
    protected void close0() {
        filterChain.fireFilterClose(this);
    }

    Queue<WriteRequest> getWriteRequestQueue() {
        return writeRequestQueue;
    }

    @Override
    protected void write0(WriteRequest writeRequest) {
        filterChain.fireFilterWrite(this, writeRequest);
    }

    public TransportType getTransportType() {
        return TransportType.SOCKET;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getServiceAddress() {
        return serviceAddress;
    }

    @Override
    protected void updateTrafficMask() {
        this.ioProcessor.updateTrafficMask(this);
    }

    int getReadBufferSize() {
        return readBufferSize;
    }
    
    void increaseReadBufferSize() {
        int newReadBufferSize = getReadBufferSize() << 1;
        if (newReadBufferSize <= ((SocketSessionConfig) getConfig()).getReceiveBufferSize() << 1) {
            // read buffer size shouldn't get bigger than
            // twice of the receive buffer size because of
            // read-write fairness.
            setReadBufferSize(newReadBufferSize);
        }
    }
    
    void decreaseReadBufferSize() {
        if (deferDecreaseReadBufferSize) {
            deferDecreaseReadBufferSize = false;
            return;
        }
        
        if (getReadBufferSize() > 64) {
            setReadBufferSize(getReadBufferSize() >>> 1);
        }
    }
    
    private void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        this.deferDecreaseReadBufferSize = true;
    }

    private class SessionConfigImpl extends BaseIoSessionConfig implements
            SocketSessionConfig {
        public boolean isKeepAlive() {
            try {
                return ch.socket().getKeepAlive();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setKeepAlive(boolean on) {
            try {
                ch.socket().setKeepAlive(on);
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public boolean isOobInline() {
            try {
                return ch.socket().getOOBInline();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setOobInline(boolean on) {
            try {
                ch.socket().setOOBInline(on);
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public boolean isReuseAddress() {
            try {
                return ch.socket().getReuseAddress();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setReuseAddress(boolean on) {
            try {
                ch.socket().setReuseAddress(on);
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public int getSoLinger() {
            try {
                return ch.socket().getSoLinger();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setSoLinger(int linger) {
            try {
                if (linger < 0) {
                    ch.socket().setSoLinger(false, 0);
                } else {
                    ch.socket().setSoLinger(true, linger);
                }
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public boolean isTcpNoDelay() {
            try {
                return ch.socket().getTcpNoDelay();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setTcpNoDelay(boolean on) {
            try {
                ch.socket().setTcpNoDelay(on);
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public int getTrafficClass() {
            if (SocketSessionConfigImpl.isGetTrafficClassAvailable()) {
                try {
                    return ch.socket().getTrafficClass();
                } catch (SocketException e) {
                    // Throw an exception only when setTrafficClass is also available.
                    if (SocketSessionConfigImpl.isSetTrafficClassAvailable()) {
                        throw new RuntimeIOException(e);
                    }
                }
            }

            return 0;
        }

        public void setTrafficClass(int tc) {
            if (SocketSessionConfigImpl.isSetTrafficClassAvailable()) {
                try {
                    ch.socket().setTrafficClass(tc);
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }

        public int getSendBufferSize() {
            try {
                return ch.socket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setSendBufferSize(int size) {
            if (SocketSessionConfigImpl.isSetSendBufferSizeAvailable()) {
                try {
                    ch.socket().setSendBufferSize(size);
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }

        public int getReceiveBufferSize() {
            try {
                return ch.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        public void setReceiveBufferSize(int size) {
            if (SocketSessionConfigImpl.isSetReceiveBufferSizeAvailable()) {
                try {
                    ch.socket().setReceiveBufferSize(size);
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }
    }
}
