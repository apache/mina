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
import org.apache.mina.api.MinaRuntimeException;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.executor.OrderedHandlerExecutor;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.tcp.AbstractTcpServer;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.apache.mina.util.Assert;
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
     * Create a TCP server with new selector pool of default size and a {@link IoHandlerExecutor} of default type (
     * {@link OrderedHandlerExecutor})
     */
    public NioTcpServer() {
        this(new NioSelectorLoop("accept", 0), new FixedSelectorLoopPool("Server", Runtime.getRuntime()
                .availableProcessors() + 1), null);
    }

    /**
     * Create a TCP server with new selector pool of default size and a {@link IoHandlerExecutor} of default type (
     * {@link OrderedHandlerExecutor})
     * 
     * @param config The specific configuration to use
     */
    public NioTcpServer(TcpSessionConfig config) {
        this(config, new NioSelectorLoop("accept", 0), new FixedSelectorLoopPool("Server", Runtime.getRuntime()
                .availableProcessors() + 1), null);
    }

    /**
     * Create a TCP server with provided selector loops pool. We will use one SelectorLoop get from the pool to manage
     * the OP_ACCEPT events. If the pool contains only one SelectorLoop, then all the events will be managed by the same
     * Selector.
     * 
     * @param selectorLoopPool the selector loop pool for handling all I/O events (accept, read, write)
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioTcpServer(SelectorLoopPool selectorLoopPool, IoHandlerExecutor handlerExecutor) {
        this(selectorLoopPool.getSelectorLoop(), selectorLoopPool, handlerExecutor);
    }

    /**
     * Create a TCP server with provided selector loops pool. We will use one SelectorLoop get from the pool to manage
     * the OP_ACCEPT events. If the pool contains only one SelectorLoop, then all the events will be managed by the same
     * Selector.
     * 
     * @param config The specific configuration to use
     * @param selectorLoopPool the selector loop pool for handling all I/O events (accept, read, write)
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioTcpServer(TcpSessionConfig config, SelectorLoopPool selectorLoopPool, IoHandlerExecutor handlerExecutor) {
        this(config, selectorLoopPool.getSelectorLoop(), selectorLoopPool, handlerExecutor);
    }

    /**
     * Create a TCP server with provided selector loops pool
     * 
     * @param acceptSelectorLoop the selector loop for handling accept events (connection of new session)
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioTcpServer(SelectorLoop acceptSelectorLoop, SelectorLoopPool readWriteSelectorLoop,
            IoHandlerExecutor handlerExecutor) {
        super(handlerExecutor);
        this.acceptSelectorLoop = acceptSelectorLoop;
        this.readWriteSelectorPool = readWriteSelectorLoop;
    }

    /**
     * Create a TCP server with provided selector loops pool
     * 
     * @param config The specific configuration to use
     * @param acceptSelectorLoop the selector loop for handling accept events (connection of new session)
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioTcpServer(TcpSessionConfig config, SelectorLoop acceptSelectorLoop,
            SelectorLoopPool readWriteSelectorLoop, IoHandlerExecutor handlerExecutor) {
        super(config, handlerExecutor);
        this.acceptSelectorLoop = acceptSelectorLoop;
        this.readWriteSelectorPool = readWriteSelectorLoop;
    }

    /**
     * Get the inner Server socket for accepting new client connections
     * 
     * @return
     */
    public synchronized ServerSocketChannel getServerSocketChannel() {
        return serverChannel;
    }

    public synchronized void setServerSocketChannel(final ServerSocketChannel serverChannel) {
        this.serverChannel = serverChannel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind(final int port) {
        bind(new InetSocketAddress(port));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void bind(SocketAddress localAddress) {
        Assert.assertNotNull(localAddress, "localAddress");

        // check if the address is already bound
        if (address != null) {
            throw new IllegalStateException("address " + address + " already bound");
        }

        LOG.info("binding address {}", localAddress);
        address = localAddress;

        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().setReuseAddress(isReuseAddress());
            serverChannel.socket().bind(address);
            serverChannel.configureBlocking(false);
        } catch (IOException e) {
            throw new MinaRuntimeException("can't bind address" + address, e);
        }

        acceptSelectorLoop.register(true, false, false, false, this, serverChannel, null);

        idleChecker = new IndexedIdleChecker();
        idleChecker.start();

        // it's the first address bound, let's fire the event
        fireServiceActivated();
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
    public synchronized void unbind() {
        LOG.info("unbinding {}", address);
        if (this.address == null) {
            throw new IllegalStateException("server not bound");
        }
        try {
            serverChannel.socket().close();
            serverChannel.close();
        } catch (IOException e) {
            throw new MinaRuntimeException("can't unbind server", e);
        }

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
    public void ready(final boolean accept, boolean connect, final boolean read, final ByteBuffer readBuffer,
            final boolean write) {
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

    private synchronized void createSession(SocketChannel clientSocket) throws IOException {
        LOG.debug("create session");
        SocketChannel socketChannel = clientSocket;
        TcpSessionConfig config = getSessionConfig();
        SelectorLoop readWriteSelectorLoop = readWriteSelectorPool.getSelectorLoop();
        final NioTcpSession session = new NioTcpSession(this, socketChannel, readWriteSelectorLoop, idleChecker);

        socketChannel.configureBlocking(false);

        // apply idle configuration
        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE,
                config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        // apply the default service socket configuration
        Boolean keepAlive = config.isKeepAlive();

        if (keepAlive != null) {
            session.getConfig().setKeepAlive(keepAlive);
        }

        Boolean oobInline = config.isOobInline();

        if (oobInline != null) {
            session.getConfig().setOobInline(oobInline);
        }

        Boolean reuseAddress = config.isReuseAddress();

        if (reuseAddress != null) {
            session.getConfig().setReuseAddress(reuseAddress);
        }

        Boolean tcpNoDelay = config.isTcpNoDelay();

        if (tcpNoDelay != null) {
            session.getConfig().setTcpNoDelay(tcpNoDelay);
        }

        Integer receiveBufferSize = config.getReadBufferSize();

        if (receiveBufferSize != null) {
            session.getConfig().setReadBufferSize(receiveBufferSize);
        }

        Integer sendBufferSize = config.getSendBufferSize();

        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        }

        Integer trafficClass = config.getTrafficClass();

        if (trafficClass != null) {
            session.getConfig().setTrafficClass(trafficClass);
        }

        Integer soLinger = config.getSoLinger();

        if (soLinger != null) {
            session.getConfig().setSoLinger(soLinger);
        }

        // Set the secured flag if the service is to be used over SSL/TLS
        if (config.isSecured()) {
            session.initSecure(config.getSslContext());
        }

        // add the session to the queue for being added to the selector
        readWriteSelectorLoop.register(false, false, true, false, session, socketChannel, new RegistrationCallback() {

            @Override
            public void done(SelectionKey selectionKey) {
                session.setSelectionKey(selectionKey);
                session.setConnected();
            }
        });

        idleChecker.sessionRead(session, System.currentTimeMillis());
        idleChecker.sessionWritten(session, System.currentTimeMillis());
    }

}