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
package org.apache.mina.core.session;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.write.WriteRequest;

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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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
            // Do nothing
        }
    };

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);
    private final IoProcessor<AbstractIoSession> processor;

    private volatile IoHandler handler = new IoHandlerAdapter();
    private volatile SocketAddress localAddress = ANONYMOUS_ADDRESS;
    private volatile SocketAddress remoteAddress = ANONYMOUS_ADDRESS;
    private volatile TransportMetadata transportMetadata = TRANSPORT_METADATA;

    /**
     * Creates a new instance.
     */
    public DummySession() {
        super(

        // Initialize dummy service.
            new AbstractIoAcceptor(
                new AbstractIoSessionConfig() {
                    @Override
                    protected void doSetAll(IoSessionConfig config) {
                        // Do nothing
                    }
                },
                new Executor() {
                    public void execute(Runnable command) {
                        // Do nothing
                    }
                }) {

            @Override
            protected Set<SocketAddress> bindInternal(List<? extends SocketAddress> localAddresses) throws Exception {
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
            protected void dispose0() throws Exception {
            }
            } );

        processor = new IoProcessor<AbstractIoSession>() {
            public void add(AbstractIoSession session) {
                // Do nothing
            }

            public void flush(AbstractIoSession session) {
                DummySession s = (DummySession) session;
                WriteRequest req = s.getWriteRequestQueue().poll(session);
                
                // Chek that the request is not null. If the session has been closed,
                // we may not have any pending requests.
                if (req != null) {
                    Object m = req.getMessage();
                    if (m instanceof FileRegion) {
                        FileRegion file = (FileRegion) m;
                        try {
                            file.getFileChannel().position(file.getPosition() + file.getRemainingBytes());
                            file.update(file.getRemainingBytes());
                        } catch (IOException e) {
                            s.getFilterChain().fireExceptionCaught(e);
                        }
                    }
                    getFilterChain().fireMessageSent(req);
                }
            }

            public void remove(AbstractIoSession session) {
                if (!session.getCloseFuture().isClosed()) {
                    session.getFilterChain().fireSessionClosed();
                }
            }

            public void updateTrafficControl(AbstractIoSession session) {
                // Do nothing
            }

            public void dispose() {
                // Do nothing
            }

            public boolean isDisposed() {
                return false;
            }

            public boolean isDisposing() {
                return false;
            }

        };

        this.service = super.getService();

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
            throw new IllegalArgumentException("config");
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
            throw new IllegalArgumentException("handler");
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
            throw new IllegalArgumentException("localAddress");
        }

        this.localAddress = localAddress;
    }

    /**
     * Sets the socket address of remote peer.
     */
    public void setRemoteAddress(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new IllegalArgumentException("remoteAddress");
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
            throw new IllegalArgumentException("service");
        }

        this.service = service;
    }

    @Override
    public final IoProcessor<AbstractIoSession> getProcessor() {
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
            throw new IllegalArgumentException("transportMetadata");
        }

        this.transportMetadata = transportMetadata;
    }

    @Override
    public void setScheduledWriteBytes(int byteCount){
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
