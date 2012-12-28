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
package org.apache.mina.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.udp.AbstractUdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a UDP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpServer extends AbstractUdpServer implements SelectorListener {

    static final Logger LOG = LoggerFactory.getLogger(NioUdpServer.class);

    // the bound local address
    private SocketAddress address = null;

    // the processor used for read and write this server
    private final NioSelectorLoop selectorLoop;

    // used for detecting idle sessions
    private final IdleChecker idleChecker = new IndexedIdleChecker();

    // the inner channel for read/write UDP datagrams
    private DatagramChannel datagramChannel = null;

    // the key used for selecting read event
    private SelectionKey readKey = null;

    // list of all the sessions by remote socket address
    private final Map<SocketAddress /* remote socket address */, NioUdpSession> sessions = new ConcurrentHashMap<SocketAddress, NioUdpSession>();

    /**
     * Create a new instance of NioUdpServer
     */
    public NioUdpServer(NioSelectorLoop selectorLoop, IoHandlerExecutor ioHandlerExecutor) {
        super(ioHandlerExecutor);
        this.selectorLoop = selectorLoop;
    }

    /**
     * Get the inner datagram channel for read and write operations. To be called by the {@link NioSelectorProcessor}
     * 
     * @return the datagram channel bound to this {@link NioUdpServer}.
     */
    public DatagramChannel getDatagramChannel() {
        return datagramChannel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoSessionConfig getSessionConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getBoundAddress() {
        return address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind(final int port) throws IOException {
        bind(new InetSocketAddress(port));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind(final SocketAddress localAddress) throws IOException {
        if (localAddress == null) {
            // We should at least have one address to bind on
            throw new IllegalArgumentException("LocalAdress cannot be null");
        }

        // check if the address is already bound
        if (this.address != null) {
            throw new IOException("address " + address + " already bound");
        }
        address = localAddress;

        LOG.info("binding address {}", localAddress);

        datagramChannel = DatagramChannel.open();

        datagramChannel.socket().setReuseAddress(isReuseAddress());
        datagramChannel.socket().bind(address);
        datagramChannel.configureBlocking(false);

        selectorLoop.register(false, false, true, false, this, datagramChannel, null);

        // it's the first address bound, let's fire the event
        this.fireServiceActivated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind() throws IOException {
        LOG.info("unbinding {}", address);
        if (this.address == null) {
            throw new IllegalStateException("server not bound");
        }

        selectorLoop.unregister(this, datagramChannel);
        datagramChannel.socket().close();
        datagramChannel.close();

        this.address = null;
        this.fireServiceInactivated();
    }

    /**
     * @return the readKey
     */
    public SelectionKey getReadKey() {
        return readKey;
    }

    /**
     * @param readKey the readKey to set
     */
    public void setReadKey(final SelectionKey readKey) {
        this.readKey = readKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ready(final boolean accept, boolean connect, final boolean read, final ByteBuffer readBuffer,
            final boolean write) {
        if (read) {
            try {
                LOG.debug("readable datagram for UDP service : {}", this);
                readBuffer.clear();

                final SocketAddress source = datagramChannel.receive(readBuffer);
                readBuffer.flip();

                LOG.debug("read {} bytes form {}", readBuffer.remaining(), source);

                // let's find the corresponding session

                NioUdpSession session = sessions.get(source);
                if (session == null) {
                    session = new NioUdpSession(this, idleChecker, address, source);
                }

                session.receivedDatagram(readBuffer);
            } catch (final IOException ex) {
                LOG.error("IOException while reading the socket", ex);
            }
        }
        if (write) {
            // TODO : flush session
        }
    }

}