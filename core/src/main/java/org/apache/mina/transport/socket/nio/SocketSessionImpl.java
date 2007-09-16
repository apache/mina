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
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultTransportMetadata;
import org.apache.mina.common.FileRegion;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.common.WriteRequest;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class SocketSessionImpl extends AbstractIoSession implements SocketSession {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "socket", false, true,
                    InetSocketAddress.class,
                    SocketSessionConfig.class,
                    ByteBuffer.class, FileRegion.class);

    private final IoService service;

    private final SocketSessionConfig config = new SessionConfigImpl();

    private final SocketIoProcessor ioProcessor;

    private final SocketFilterChain filterChain;

    private final SocketChannel ch;

    private final IoHandler handler;

    private SelectionKey key;

    private int readBufferSize = 1024;

    SocketSessionImpl(IoService service, SocketIoProcessor ioProcessor, SocketChannel ch) {
        this.service = service;
        this.ioProcessor = ioProcessor;
        this.filterChain = new SocketFilterChain(this);
        this.ch = ch;
        this.handler = service.getHandler();
        this.config.setAll(service.getSessionConfig());
    }

    public IoService getService() {
        return service;
    }

    public SocketSessionConfig getConfig() {
        return config;
    }

    SocketIoProcessor getIoProcessor() {
        return ioProcessor;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    SocketChannel getChannel() {
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
        filterChain.fireFilterClose(this);
    }

    @Override
    protected void write0(WriteRequest writeRequest) {
        filterChain.fireFilterWrite(this, writeRequest);
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) ch.socket().getRemoteSocketAddress();
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) ch.socket().getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }

    @Override
    protected void updateTrafficMask() {
        this.ioProcessor.updateTrafficMask(this);
    }

    int getReadBufferSize() {
        return readBufferSize;
    }

    void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    void queueWriteRequest(WriteRequest writeRequest) {
        if (writeRequest.getMessage() instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) writeRequest.getMessage();
            // SocketIoProcessor.doFlush() will reset it after write is finished
            // because the buffer will be passed with messageSent event.
            buffer.mark();
            int remaining = buffer.remaining();
            if (remaining == 0) {
                increaseScheduledWriteMessages();            
            } else {
                increaseScheduledWriteBytes(buffer.remaining());
            }
        }

        getWriteRequestQueue().add(writeRequest);

        if (getTrafficMask().isWritable()) {
            getIoProcessor().flush(this);
        }
    }

    private class SessionConfigImpl extends AbstractSocketSessionConfig implements SocketSessionConfig {
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
            if (DefaultSocketSessionConfig.isGetTrafficClassAvailable()) {
                try {
                    return ch.socket().getTrafficClass();
                } catch (SocketException e) {
                    // Throw an exception only when setTrafficClass is also available.
                    if (DefaultSocketSessionConfig.isSetTrafficClassAvailable()) {
                        throw new RuntimeIOException(e);
                    }
                }
            }

            return 0;
        }

        public void setTrafficClass(int tc) {
            if (DefaultSocketSessionConfig.isSetTrafficClassAvailable()) {
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
            if (DefaultSocketSessionConfig.isSetSendBufferSizeAvailable()) {
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
            if (DefaultSocketSessionConfig.isSetReceiveBufferSizeAvailable()) {
                try {
                    ch.socket().setReceiveBufferSize(size);
                } catch (SocketException e) {
                    throw new RuntimeIOException(e);
                }
            }
        }
    }
}
