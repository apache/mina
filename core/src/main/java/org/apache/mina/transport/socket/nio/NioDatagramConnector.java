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
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.transport.socket.DatagramConnector;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;
import org.apache.mina.util.NewThreadExecutor;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class NioDatagramConnector extends AbstractIoConnector implements DatagramConnector{

    private static volatile int nextId = 0;

    private final int id = nextId++;
    private final String threadName = "DatagramConnector-" + id;
    private final int processorCount;
    private final NioProcessor[] ioProcessors;

    private int processorDistributor = 0;

    /**
     * Creates a new instance.
     */
    public NioDatagramConnector() {
        this(new NewThreadExecutor());
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramConnector(Executor executor) {
        this(Runtime.getRuntime().availableProcessors() + 1, executor);
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramConnector(int processorCount, Executor executor) {
        super(new DefaultDatagramSessionConfig());

        if (processorCount < 1) {
            throw new IllegalArgumentException(
                    "Must have at least one processor");
        }

        this.processorCount = processorCount;
        ioProcessors = new NioProcessor[processorCount];

        // create an array of SocketIoProcessors that will be used for
        // handling sessions.
        for (int i = 0; i < processorCount; i++) {
            ioProcessors[i] = new NioProcessor(
                    threadName + '.' + i, executor);
        }
    }

    private NioProcessor nextProcessor() {
        if (this.processorDistributor == Integer.MAX_VALUE) {
            this.processorDistributor = Integer.MAX_VALUE % this.processorCount;
        }

        return ioProcessors[processorDistributor++ % processorCount];
    }

    public TransportMetadata getTransportMetadata() {
        return NioDatagramSession.METADATA;
    }

    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
    }

    @Override
    protected ConnectFuture doConnect(SocketAddress remoteAddress,
                                      SocketAddress localAddress) {
        DatagramChannel ch = null;
        boolean initialized = false;
        try {
            ch = DatagramChannel.open();
            ch.socket().setReuseAddress(getSessionConfig().isReuseAddress());
            ch.socket().setReuseAddress(true);
            ch.socket().setBroadcast(getSessionConfig().isBroadcast());

            if (localAddress != null) {
                ch.socket().bind(localAddress);
            }
            ch.connect(remoteAddress);

            NioProcessor processor = nextProcessor();
            final NioSession session = new NioDatagramSession(this, ch, processor);
            ConnectFuture future = new DefaultConnectFuture();
            finishSessionInitialization(session, future);
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
