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
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoProcessor;
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

    private final IoProcessor processor;

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
        super(new DefaultDatagramSessionConfig());

        processor = new NIOProcessor("DatagramConnector-" + id, executor);
    }

    public TransportMetadata getTransportMetadata() {
        return DatagramSessionImpl.METADATA;
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
        IoSession session = null;
        try {
            ch = DatagramChannel.open();
            DatagramSessionConfig cfg = getSessionConfig();

            ch.socket().setReuseAddress(cfg.isReuseAddress());
            ch.socket().setBroadcast(cfg.isBroadcast());
            ch.socket().setReceiveBufferSize(cfg.getReceiveBufferSize());
            ch.socket().setSendBufferSize(cfg.getSendBufferSize());

            if (ch.socket().getTrafficClass() != cfg.getTrafficClass()) {
                ch.socket().setTrafficClass(cfg.getTrafficClass());
            }

            if (localAddress != null) {
                ch.socket().bind(localAddress);
            }
            ch.connect(remoteAddress);

            session = new DatagramSessionImpl(this, ch, getHandler());
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

    IoProcessor getProcessor() {
        return processor;
    }
}
