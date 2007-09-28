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
package org.apache.mina.common;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * A dummy {@link IoSession} for unit-testing or non-network-use of
 * the classes that depends on {@link IoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DummySession extends AbstractIoSession {

    private static final TransportMetadata TRANSPORT_METADATA =
            new DefaultTransportMetadata(
                    "dummy", false, false,
                    SocketAddress.class, IoSessionConfig.class, Object.class);

    private static final SocketAddress ANONYMOUS_ADDRESS = new SocketAddress() {
        private static final long serialVersionUID = -496112902353454179L;

        @Override
        public String toString() {
            return "?";
        }
    };

    private volatile IoService service;

    private volatile IoProcessor processor;

    private volatile IoSessionConfig config = new AbstractIoSessionConfig() {
        @Override
        protected void doSetAll(IoSessionConfig config) {
        }
    };

    private volatile IoFilterChain filterChain = new DefaultIoFilterChain(this);

    private volatile IoHandler handler = new IoHandlerAdapter();
    private volatile SocketAddress localAddress = ANONYMOUS_ADDRESS;
    private volatile SocketAddress remoteAddress = ANONYMOUS_ADDRESS;
    private volatile TransportMetadata transportMetadata = TRANSPORT_METADATA;

    /**
     * Creates a new instance.
     */
    public DummySession() {
        // Initialize dummy service.
        IoAcceptor acceptor = new AbstractIoAcceptor(
                new AbstractIoSessionConfig() {
                    @Override
                    protected void doSetAll(IoSessionConfig config) {
                    }
                }) {

            @Override
            protected void doBind() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void doUnbind() {
                throw new UnsupportedOperationException();
            }

            public IoSession newSession(SocketAddress remoteAddress) {
                throw new UnsupportedOperationException();
            }

            public TransportMetadata getTransportMetadata() {
                return TRANSPORT_METADATA;
            }
        };

        // Set meaningless default values.
        acceptor.setHandler(new IoHandlerAdapter());
        acceptor.setLocalAddress(ANONYMOUS_ADDRESS);

        this.service = acceptor;

        this.processor = new IoProcessor() {
            public void add(IoSession session) {
            }

            public void flush(IoSession session) {
                getFilterChain().fireMessageSent(
                        ((DummySession) session).getWriteRequestQueue().poll());
            }

            public void remove(IoSession session) {
            }

            public void updateTrafficMask(IoSession session) {
            }
        };
    }

    public IoSessionConfig getConfig() {
        return config;
    }

    /**
     * Sets the configuration of this session.
     */
    public void setConfig(IoSessionConfig config) {
        if (config == null) {
            throw new NullPointerException("config");
        }

        this.config = config;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    /**
     * Sets the filter chain that affects this session.
     */
    public void setFilterChain(IoFilterChain filterChain) {
        if (filterChain == null) {
            throw new NullPointerException("filterChain");
        }

        this.filterChain = filterChain;
    }

    public IoHandler getHandler() {
        return handler;
    }

    /**
     * Sets the {@link IoHandler} which handles this session.
     */
    public void setHandler(IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }

        this.handler = handler;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Sets the socket address of local machine which is associated with
     * this session.
     */
    public void setLocalAddress(SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }

        this.localAddress = localAddress;
    }

    /**
     * Sets the socket address of remote peer.
     */
    public void setRemoteAddress(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        this.remoteAddress = remoteAddress;
    }

    @Override
    public long getScheduledWriteBytes() {
        return 0;
    }

    @Override
    public int getScheduledWriteMessages() {
        return 0;
    }

    public IoService getService() {
        return service;
    }

    /**
     * Sets the {@link IoService} which provides I/O service to this session.
     */
    public void setService(IoService service) {
        if (service == null) {
            throw new NullPointerException("service");
        }

        this.service = service;
    }

    @Override
    public IoProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(IoProcessor processor) {
        if (processor == null) {
            throw new NullPointerException("processor");
        }

        this.processor = processor;
    }

    public TransportMetadata getTransportMetadata() {
        return transportMetadata;
    }

    /**
     * Sets the {@link TransportMetadata} that this session runs on.
     */
    public void setTransportMetadata(TransportMetadata transportMetadata) {
        if (transportMetadata == null) {
            throw new NullPointerException("transportMetadata");
        }

        this.transportMetadata = transportMetadata;
    }
}
