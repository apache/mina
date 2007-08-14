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
import java.util.LinkedList;
import java.util.Queue;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.WriteRequest;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class DatagramSessionImpl extends AbstractIoSession implements DatagramSession {
    private final IoService service;

    private final DatagramSessionConfig config = new SessionConfigImpl();

    private final DatagramFilterChain filterChain = new DatagramFilterChain(
            this);

    private final DatagramChannel ch;

    private final Queue<WriteRequest> writeRequestQueue = new LinkedList<WriteRequest>();

    private final IoHandler handler;

    private final InetSocketAddress localAddress;

    private final InetSocketAddress remoteAddress;

    private SelectionKey key;

    private int readBufferSize;

    /**
     * Creates a new acceptor instance.
     */
    DatagramSessionImpl(
            DatagramAcceptor service,
            DatagramChannel ch, IoHandler defaultHandler,
            InetSocketAddress remoteAddress) {
        this.service = service;
        this.ch = ch;
        this.handler = defaultHandler;
        this.remoteAddress = remoteAddress;

        // We didn't set the localAddress by calling getLocalSocketAddress() to avoid
        // the case that getLocalSocketAddress() returns IPv6 address while
        // serviceAddress represents the same address in IPv4.
        this.localAddress = service.getLocalAddress();

        applySettings();
    }

    /**
     * Creates a new connector instance.
     */
    DatagramSessionImpl(DatagramConnector service,
            DatagramChannel ch, IoHandler defaultHandler) {
        this.service = service;
        this.ch = ch;
        this.handler = defaultHandler;
        this.remoteAddress = (InetSocketAddress) ch.socket()
                .getRemoteSocketAddress();
        this.localAddress = (InetSocketAddress) ch.socket()
                .getLocalSocketAddress();

        applySettings();
    }

    private void applySettings() {
        // Apply the initial session settings
        IoSessionConfig sessionConfig = getService().getSessionConfig();
        if (sessionConfig instanceof DatagramSessionConfig) {
            DatagramSessionConfig cfg = (DatagramSessionConfig) sessionConfig;
            this.config.setBroadcast(cfg.isBroadcast());
            this.config.setReceiveBufferSize(cfg.getReceiveBufferSize());
            this.config.setReuseAddress(cfg.isReuseAddress());
            this.config.setSendBufferSize(cfg.getSendBufferSize());

            if (this.config.getTrafficClass() != cfg.getTrafficClass()) {
                this.config.setTrafficClass(cfg.getTrafficClass());
            }
        }
    }

    public IoService getService() {
        return service;
    }

    public DatagramSessionConfig getConfig() {
        return config;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    DatagramChannel getChannel() {
        return ch;
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
        if (service instanceof IoAcceptor) {
            ((DatagramAcceptor) service).getSessionRecycler()
                    .remove(this);
        }
        filterChain.fireFilterClose(this);
    }

    Queue<WriteRequest> getWriteRequestQueue() {
        return writeRequestQueue;
    }

    @Override
    public WriteFuture write(Object message, SocketAddress destination) {
        if (!this.config.isBroadcast()) {
            throw new IllegalStateException("Non-broadcast session");
        }

        return super.write(message, destination);
    }

    @Override
    protected void write0(WriteRequest writeRequest) {
        filterChain.fireFilterWrite(this, writeRequest);
    }

    public int getScheduledWriteMessages() {
        int size = 0;
        synchronized (writeRequestQueue) {
            for (WriteRequest request : writeRequestQueue) {
                Object message = request.getMessage();
                if (message instanceof ByteBuffer) {
                    if (((ByteBuffer) message).hasRemaining()) {
                        size ++;
                    }
                } else {
                    size ++;
                }
            }
        }

        return size;
    }

    public int getScheduledWriteBytes() {
        int size = 0;
        synchronized (writeRequestQueue) {
            for (Object o : writeRequestQueue) {
                if (o instanceof ByteBuffer) {
                    size += ((ByteBuffer) o).remaining();
                }
            }
        }

        return size;
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

    @Override
    protected void updateTrafficMask() {
        if (service instanceof DatagramConnector) {
            ((DatagramConnector) service).updateTrafficMask(this);
        }
    }

    int getReadBufferSize() {
        return readBufferSize;
    }

    private class SessionConfigImpl extends DefaultDatagramSessionConfig
            implements DatagramSessionConfig {
        @Override
        public int getReceiveBufferSize() {
            try {
                return ch.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        @Override
        public void setReceiveBufferSize(int receiveBufferSize) {
            if (DefaultDatagramSessionConfig.isSetReceiveBufferSizeAvailable()) {
                try {
                    ch.socket().setReceiveBufferSize(receiveBufferSize);
                    // Re-retrieve the effective receive buffer size.
                    receiveBufferSize = ch.socket().getReceiveBufferSize();
                    DatagramSessionImpl.this.readBufferSize = receiveBufferSize;
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }

        @Override
        public boolean isBroadcast() {
            try {
                return ch.socket().getBroadcast();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        @Override
        public void setBroadcast(boolean broadcast) {
            try {
                ch.socket().setBroadcast(broadcast);
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        @Override
        public int getSendBufferSize() {
            try {
                return ch.socket().getSendBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        @Override
        public void setSendBufferSize(int sendBufferSize) {
            if (DefaultDatagramSessionConfig.isSetSendBufferSizeAvailable()) {
                try {
                    ch.socket().setSendBufferSize(sendBufferSize);
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }

        @Override
        public boolean isReuseAddress() {
            try {
                return ch.socket().getReuseAddress();
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        @Override
        public void setReuseAddress(boolean reuseAddress) {
            try {
                ch.socket().setReuseAddress(reuseAddress);
            } catch (SocketException e) {
                throw new RuntimeIOException(e);
            }
        }

        @Override
        public int getTrafficClass() {
            if (DefaultDatagramSessionConfig.isGetTrafficClassAvailable()) {
                try {
                    return ch.socket().getTrafficClass();
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            } else {
                return 0;
            }
        }

        @Override
        public void setTrafficClass(int trafficClass) {
            if (DefaultDatagramSessionConfig.isSetTrafficClassAvailable()) {
                try {
                    ch.socket().setTrafficClass(trafficClass);
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }
    }
}