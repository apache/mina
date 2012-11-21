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
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.tcp.AbstractTcpServer;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a TCP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpServer extends AbstractTcpServer implements SelectorListener {
    /** A logger for this class */
    static final Logger LOG = LoggerFactory.getLogger(NioTcpServer.class);

    /** the bound local address */
    private SocketAddress address = null;

    private final SelectorLoop acceptSelectorLoop;

    private final SelectorLoopPool readWriteSelectorPool;

    // the key used for selecting accept event
    private SelectionKey acceptKey = null;

    // the server socket for accepting clients
    private ServerSocketChannel serverChannel = null;

    private IdleChecker idleChecker;

    /**
     * Create a TCP server with new selector pool of default size.
     */
    public NioTcpServer() {
        this(new NioSelectorLoop("accept", 0),
                new FixedSelectorLoopPool(Runtime.getRuntime().availableProcessors() + 1));
    }

    /**
     * Create a TCP server with provided selector loops pool. We will use one SelectorLoop get from
     * the pool to manage the OP_ACCEPT events. If the pool contains only one SelectorLoop, then
     * all the events will be managed by the same Selector.
     * 
     * @param acceptSelectorLoop the selector loop for handling accept events (connection of new session)
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     */
    public NioTcpServer(SelectorLoopPool selectorLoopPool) {
        super();
        this.acceptSelectorLoop = selectorLoopPool.getSelectorLoop();
        this.readWriteSelectorPool = selectorLoopPool;
    }

    /**
     * Create a TCP server with provided selector loops pool
     * 
     * @param acceptSelectorLoop the selector loop for handling accept events (connection of new session)
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     */
    public NioTcpServer(SelectorLoop acceptSelectorLoop, SelectorLoopPool readWriteSelectorLoop) {
        super();
        this.acceptSelectorLoop = acceptSelectorLoop;
        this.readWriteSelectorPool = readWriteSelectorLoop;
    }

    /**
     * Get the inner Server socket for accepting new client connections
     * 
     * @return
     */
    public ServerSocketChannel getServerSocketChannel() {
        return this.serverChannel;
    }

    public void setServerSocketChannel(final ServerSocketChannel serverChannel) {
        this.serverChannel = serverChannel;
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
    public synchronized void bind(final SocketAddress localAddress) throws IOException {
        if (localAddress == null) {
            // We should at least have one address to bind on
            throw new IllegalArgumentException("LocalAdress cannot be null");
        }

        // check if the address is already bound
        if (this.address != null) {
            throw new IOException("address " + address + " already bound");
        }

        LOG.info("binding address {}", localAddress);
        this.address = localAddress;

        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().setReuseAddress(isReuseAddress());
        serverChannel.socket().bind(address);
        serverChannel.configureBlocking(false);

        acceptSelectorLoop.register(true, false, false, this, serverChannel);

        idleChecker = new IndexedIdleChecker();
        idleChecker.start();

        // it's the first address bound, let's fire the event
        this.fireServiceActivated();
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
    public synchronized void unbind() throws IOException {
        LOG.info("unbinding {}", address);
        if (this.address == null) {
            throw new IllegalStateException("server not bound");
        }
        serverChannel.socket().close();
        serverChannel.close();
        acceptSelectorLoop.unregister(this, serverChannel);

        this.address = null;
        this.fireServiceInactivated();

        // will stop the acceptor processor if we are the last service
        idleChecker.destroy();
    }

    /**
     * @return the acceptKey
     */
    public SelectionKey getAcceptKey() {
        return acceptKey;
    }

    /**
     * @param acceptKey the acceptKey to set
     */
    public void setAcceptKey(final SelectionKey acceptKey) {
        this.acceptKey = acceptKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ready(final boolean accept, final boolean read, final ByteBuffer readBuffer, final boolean write) {
        if (accept) {
            LOG.debug("acceptable new client");

            // accepted connection
            try {
                LOG.debug("new client accepted");
                createSession(getServerSocketChannel().accept());

            } catch (final IOException e) {
                LOG.error("error while accepting new client", e);
            }
        }

        if (read || write) {
            throw new IllegalStateException("should not receive read or write events");
        }
    }

    private void createSession(final SocketChannel clientSocket) throws IOException {
        LOG.debug("create session");
        final SocketChannel socketChannel = clientSocket;
        final TcpSessionConfig config = getSessionConfig();
        final SelectorLoop readWriteSelectorLoop = readWriteSelectorPool.getSelectorLoop();
        final NioTcpSession session = new NioTcpSession(this, socketChannel, readWriteSelectorLoop, idleChecker);

        socketChannel.configureBlocking(false);

        // apply idle configuration
        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE,
                config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        // apply the default service socket configuration
        final Boolean keepAlive = config.isKeepAlive();

        if (keepAlive != null) {
            session.getConfig().setKeepAlive(keepAlive);
        }

        final Boolean oobInline = config.isOobInline();

        if (oobInline != null) {
            session.getConfig().setOobInline(oobInline);
        }

        final Boolean reuseAddress = config.isReuseAddress();

        if (reuseAddress != null) {
            session.getConfig().setReuseAddress(reuseAddress);
        }

        final Boolean tcpNoDelay = config.isTcpNoDelay();

        if (tcpNoDelay != null) {
            session.getConfig().setTcpNoDelay(tcpNoDelay);
        }

        final Integer receiveBufferSize = config.getReceiveBufferSize();

        if (receiveBufferSize != null) {
            session.getConfig().setReceiveBufferSize(receiveBufferSize);
        }

        final Integer sendBufferSize = config.getSendBufferSize();

        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        }

        final Integer trafficClass = config.getTrafficClass();

        if (trafficClass != null) {
            session.getConfig().setTrafficClass(trafficClass);
        }

        final Integer soLinger = config.getSoLinger();

        if (soLinger != null) {
            session.getConfig().setSoLinger(soLinger);
        }

        // Set the secured flag if the service is to be used over SSL/TLS
        if (config.isSecured()) {
            session.initSecure(config.getSslContext());
        }

        // add the session to the queue for being added to the selector
        readWriteSelectorLoop.register(session, false, true, false, session, socketChannel);

        session.processSessionOpened();
        session.setConnected();
        idleChecker.sessionRead(session, System.currentTimeMillis());
        idleChecker.sessionWritten(session, System.currentTimeMillis());
    }

}