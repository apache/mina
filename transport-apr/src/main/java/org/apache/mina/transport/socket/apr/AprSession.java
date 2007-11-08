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
package org.apache.mina.transport.socket.apr;

import java.net.InetSocketAddress;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.DefaultTransportMetadata;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportMetadata;
import org.apache.tomcat.jni.Socket;

/**
 * {@link IoSession} for the {@link AprConnector}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class AprSession extends AbstractIoSession {
    private long socket;

    private final IoService service;
    private final AprSessionConfig config = new APRSessionConfigImpl();
    private final IoProcessor<AprSession> processor;

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);
    private final IoHandler handler;

    private byte[] readBuffer = new byte[1024]; //FIXME : fixed rcvd buffer, need to change that to a config value

    private final InetSocketAddress remoteAddress;
    private final InetSocketAddress localAddress;

    static final TransportMetadata METADATA = new DefaultTransportMetadata(
            "Apache Portable Runtime socket", false, true,
            InetSocketAddress.class, AprSessionConfig.class, IoBuffer.class);

    private boolean isOpRead=false;
    private boolean isOpWrite=false;
    
    /**
     * Creates a new instance.
     */
    AprSession(IoService service, IoProcessor<AprSession> processor, long socket,
            InetSocketAddress remoteAddress, InetSocketAddress localAddress) {
        this.service = service;
        this.processor = processor;
        this.handler = service.getHandler();
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        this.socket = socket;
    }

    long getAPRSocket() {
        return socket;
    }

    @Override
    protected IoProcessor<AprSession> getProcessor() {
        return processor;
    }

    public AprSessionConfig getConfig() {
        return config;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    byte[] getReadBuffer() {
        return readBuffer;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public IoFilterChain getFilterChain() {

        return filterChain;
    }

    public IoHandler getHandler() {
        return handler;
    }

    
    public IoService getService() {
        return service;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }
    
    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }

    boolean isOpRead() {
        return isOpRead;
    }

    void setOpRead(boolean isOpRead) {
        this.isOpRead = isOpRead;
    }

    boolean isOpWrite() {
        return isOpWrite;
    }

    void setOpWrite(boolean isOpWrite) {
        this.isOpWrite = isOpWrite;
    }

    private class APRSessionConfigImpl extends AbstractAprSessionConfig
            implements AprSessionConfig {

        public boolean isKeepAlive() {
            try {
                return Socket.optGet(getAPRSocket(), Socket.APR_SO_KEEPALIVE) == 1;
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        public void setKeepAlive(boolean on) {
            Socket.optSet(getAPRSocket(), Socket.APR_SO_KEEPALIVE, on ? 1 : 0);
        }

        public boolean isOobInline() {
            return Socket.atmark(getAPRSocket());
        }

        public void setOobInline(boolean on) {
            // TODO : where the f***k it's in APR ?
            throw new UnsupportedOperationException("Not implemented");
        }

        public boolean isReuseAddress() {
            try {
                return Socket.optGet(getAPRSocket(), Socket.APR_SO_REUSEADDR) == 1;
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        public void setReuseAddress(boolean on) {
            Socket.optSet(getAPRSocket(), Socket.APR_SO_REUSEADDR, on ? 1 : 0);
        }

        public int getSoLinger() {
            try {
                return Socket.optGet(getAPRSocket(), Socket.APR_SO_LINGER);
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        public void setSoLinger(int linger) {
            // TODO : it's me or APR isn't able to disable linger ?
            Socket.optSet(getAPRSocket(), Socket.APR_SO_LINGER, linger);
        }

        public boolean isTcpNoDelay() {
            try {
                return Socket.optGet(getAPRSocket(), Socket.APR_TCP_NODELAY) == 1;
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        public void setTcpNoDelay(boolean on) {
            Socket.optSet(getAPRSocket(), Socket.APR_TCP_NODELAY, on ? 1 : 0);
        }

        public int getTrafficClass() {
            // TODO : find how to do that with APR
            throw new UnsupportedOperationException("Not implemented");
        }

        public void setTrafficClass(int tc) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public int getSendBufferSize() {
            try {
                return Socket.optGet(getAPRSocket(), Socket.APR_SO_SNDBUF);
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        public void setSendBufferSize(int size) {
            Socket.optSet(getAPRSocket(), Socket.APR_SO_SNDBUF, size);
        }

        public int getReceiveBufferSize() {
            try {
                return Socket.optGet(getAPRSocket(), Socket.APR_SO_RCVBUF);
            } catch (Exception e) {
                throw new RuntimeException("APR Exception", e);
            }
        }

        public void setReceiveBufferSize(int size) {
            Socket.optSet(getAPRSocket(), Socket.APR_SO_RCVBUF, size);
        }

    }
}