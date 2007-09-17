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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Executor;

import org.apache.mina.common.AbstractIoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultConnectFuture;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceListenerSupport;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramConnector extends AbstractIoConnector {
    private static volatile int nextId = 0;

    private final int id = nextId++;
    private final IoService parent;
    private final int processorCount;
    private final NIOProcessor[] ioProcessors;

    private int processorDistributor = 0;

    /**
     * Creates a new instance.
     */
    public DatagramConnector() {
        this(new NewThreadExecutor());
    }

    /**
     * Creates a new instance.
     */
    public DatagramConnector(Executor executor) {
        this(Runtime.getRuntime().availableProcessors() + 1, executor);
    }

    /**
     * Creates a new instance.
     */
    public DatagramConnector(int processorCount, Executor executor) {
        this(null, null, processorCount, executor);
    }
    
    DatagramConnector(
            IoService parent, String threadNamePrefix, int processorCount, Executor executor) {
        super(new DefaultDatagramSessionConfig());
        
        // DotagramAcceptor can use DatagramConnector as a child.
        if (parent == null) {
            parent = this;
        }
        if (threadNamePrefix == null) {
            threadNamePrefix = "DatagramConnector-" + id;
        }
        this.parent = parent;
        
        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        this.processorCount = processorCount;
        ioProcessors = new NIOProcessor[processorCount];

        // create an array of SocketIoProcessors that will be used for
        // handling sessions.
        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new NIOProcessor(
                    threadNamePrefix + '.' + i, executor);
        }
    }

    private NIOProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    public TransportMetadata getTransportMetadata() {
        return DatagramSessionImpl.METADATA;
    }

    @Override
    protected IoServiceListenerSupport getListeners() {
        if (parent == this) {
            return super.getListeners();
        } else {
            return ((DatagramAcceptor) parent).getListeners();
        }
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        if (parent == this) {
            return (DatagramSessionConfig) super.getSessionConfig();
        } else {
            return (DatagramSessionConfig) parent.getSessionConfig();
        }
    }

    @Override
    public DefaultIoFilterChainBuilder getFilterChain() {
        if (parent == this) {
            return super.getFilterChain();
        } else {
            return parent.getFilterChain();
        }
    }

    @Override
    public IoFilterChainBuilder getFilterChainBuilder() {
        if (parent == this) {
            return super.getFilterChainBuilder();
        } else {
            return parent.getFilterChainBuilder();
        }
    }

    @Override
    public void setFilterChainBuilder(IoFilterChainBuilder builder) {
        if (parent == this) {
            super.setFilterChainBuilder(builder);
        } else {
            parent.setFilterChainBuilder(builder);
        }
    }

    @Override
    public IoHandler getHandler() {
        if (parent == this) {
            return super.getHandler();
        } else {
            return parent.getHandler();
        }
    }

    @Override
    public void setHandler(IoHandler handler) {
        if (parent == this) {
            super.setHandler(handler);
        } else {
            parent.setHandler(handler);
        }
    }

    @Override
    protected ConnectFuture doConnect(SocketAddress remoteAddress,
                                      SocketAddress localAddress) {
        DatagramChannel ch = null;
        boolean initialized = false;
        IoSession session = null;
        try {
            ch = DatagramChannel.open();
            ch.socket().setReuseAddress(getSessionConfig().isReuseAddress());
            ch.socket().setReuseAddress(true);
            ch.socket().setBroadcast(getSessionConfig().isBroadcast());
            
            if (localAddress != null) {
                ch.socket().bind(localAddress);
            }
            ch.connect(remoteAddress);

            NIOProcessor processor = nextProcessor();
            session = new DatagramSessionImpl(parent, ch, processor);
            ConnectFuture future = new DefaultConnectFuture();
            // DefaultIoFilterChain will notify the connect future.
            session.setAttribute(DefaultIoFilterChain.CONNECT_FUTURE, future);

            processor.add(session);
            initialized = true;
            return future;
        } catch (Exception e) {
            return DefaultConnectFuture.newFailedFuture(e);
        } finally {
            if (!initialized && ch != null) {
                try {
                    ch.disconnect();
                    ch.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }
}
