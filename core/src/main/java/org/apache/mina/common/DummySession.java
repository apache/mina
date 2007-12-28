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

import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

/**
 * A dummy {@link IoSession} for unit-testing or non-network-use of
 * the classes that depends on {@link IoSession}.
 * 
 * <h2>Overriding I/O request methods</h2>
 * All I/O request methods (i.e. {@link #close()}, {@link #write(Object)} and
 * {@link #setTrafficMask(TrafficMask)}) are final and therefore cannot be
 * overridden, but you can always add your custom {@link IoFilter} to the
 * {@link IoFilterChain} to intercept any I/O events and requests.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DummySession extends AbstractIoSession {

    private static final TransportMetadata TRANSPORT_METADATA =
            new DefaultTransportMetadata(
                    "mina", "dummy", false, false,
                    SocketAddress.class, IoSessionConfig.class, Object.class);

    private static final SocketAddress ANONYMOUS_ADDRESS = new SocketAddress() {
        private static final long serialVersionUID = -496112902353454179L;

        @Override
        public String toString() {
            return "?";
        }
    };

    private volatile IoService service;

    private volatile IoSessionConfig config = new AbstractIoSessionConfig() {
        @Override
        protected void doSetAll(IoSessionConfig config) {
        }
    };

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);
    private final IoProcessor<IoSession> processor;

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
                    protected void doSetAll(IoSessionConfig config) {}
                }) {

            @Override
            protected Set<SocketAddress> bind0(List<? extends SocketAddress> localAddresses) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void unbind0(List<? extends SocketAddress> localAddresses) throws Exception {
                throw new UnsupportedOperationException();
            }

            public IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
                throw new UnsupportedOperationException();
            }

            public TransportMetadata getTransportMetadata() {
                return TRANSPORT_METADATA;
            }

            @Override
            protected IoFuture dispose0() throws Exception {
                return null;
            }
        };

        // Set meaningless default values.
        acceptor.setHandler(new IoHandlerAdapter());

        this.service = acceptor;

        this.processor = new IoProcessor<IoSession>() {
            public void add(IoSession session) {
            }

            public void flush(IoSession session) {
                getFilterChain().fireMessageSent(
                        ((DummySession) session).getWriteRequestQueue().poll(session));
            }

            public void remove(IoSession session) {
            }

            public void updateTrafficMask(IoSession session) {
            }

            public void dispose() {
            }

            public boolean isDisposed() {
                return false;
            }

            public boolean isDisposing() {
                return false;
            }
        };

        try {
            IoSessionDataStructureFactory factory = new DefaultIoSessionDataStructureFactory();
            setAttributeMap(factory.getAttributeMap(this));
            setWriteRequestQueue(factory.getWriteRequestQueue(this));
        } catch (Exception e) {
            throw new InternalError();
        }
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
    protected final IoProcessor<IoSession> getProcessor() {
        return processor;
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
    
    @Override
    public void setScheduledWriteBytes(long byteCount){
        super.setScheduledWriteBytes(byteCount);
    }

    @Override
    public void setScheduledWriteMessages(int messages) {
        super.setScheduledWriteMessages(messages);
    }

    /**
     * Update all statistical properties related with throughput.  By default
     * this method returns silently without updating the throughput properties
     * if they were calculated already within last 
     * {@link IoSessionConfig#getThroughputCalculationInterval() calculation interval}.
     * If, however, <tt>force</tt> is specified as <tt>true</tt>, this method
     * updates the throughput properties immediately.
     */
    public void updateThroughput(boolean force) {
        super.updateThroughput(System.currentTimeMillis(), force);
    }
}
