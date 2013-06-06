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
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.MinaRuntimeException;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.executor.OrderedHandlerExecutor;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.ConnectFuture;
import org.apache.mina.transport.tcp.AbstractTcpClient;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.apache.mina.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a TCP NIO based client.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpClient extends AbstractTcpClient {

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(NioTcpClient.class);

    /** the SelectorLoop for connecting the sessions */
    // This is final, so that we know if it's not initialized
    private final SelectorLoop connectSelectorLoop;

    /** the Selectorloop for handling read/write session events */
    // This is final, so that we know if it's not initialized
    private final SelectorLoopPool readWriteSelectorPool;

    /** for detecting idle session */
    private IdleChecker idleChecker;

    /**
     * Create a TCP client with new selector pool of default size and a {@link IoHandlerExecutor} of default type (
     * {@link OrderedHandlerExecutor})
     */
    public NioTcpClient() {
        // Default to 2 threads in the pool
        this(new NioSelectorLoop("connect", 0), new FixedSelectorLoopPool("Client", 2), null);
    }

    /**
     * Create a TCP client with provided selector loops pool. We will use one SelectorLoop get from the pool to manage
     * the OP_CONNECT events. If the pool contains only one SelectorLoop, then all the events will be managed by the
     * same Selector.
     * 
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioTcpClient(SelectorLoopPool selectorLoopPool, IoHandlerExecutor handlerExecutor) {
        super(handlerExecutor);
        connectSelectorLoop = selectorLoopPool.getSelectorLoop();
        readWriteSelectorPool = selectorLoopPool;
    }

    /**
     * Create a TCP client with provided selector loops pool
     * 
     * @param connectSelectorLoop the selector loop for handling connection events (connection of new session)
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioTcpClient(SelectorLoop connectSelectorLoop, SelectorLoopPool readWriteSelectorLoop,
            IoHandlerExecutor handlerExecutor) {
        super(handlerExecutor);
        this.connectSelectorLoop = connectSelectorLoop;
        this.readWriteSelectorPool = readWriteSelectorLoop;
        idleChecker = new IndexedIdleChecker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress) {
        Assert.assertNotNull(remoteAddress, "remoteAddress");

        SocketChannel clientSocket;
        try {
            clientSocket = SocketChannel.open();
        } catch (IOException e) {
            throw new MinaRuntimeException("can't create a new socket, out of file descriptors ?", e);
        }

        try {
            clientSocket.socket().setSoTimeout(getConnectTimeoutMillis());
        } catch (SocketException e) {
            throw new MinaRuntimeException("can't set socket timeout", e);
        }

        // non blocking
        try {
            clientSocket.configureBlocking(false);
        } catch (IOException e) {
            throw new MinaRuntimeException("can't configure socket as non-blocking", e);
        }

        // apply idle configuration
        // Has to be final, as it's used in a inner class...
        final NioTcpSession session = new NioTcpSession(this, clientSocket, readWriteSelectorPool.getSelectorLoop(),
                idleChecker);
        TcpSessionConfig config = getSessionConfig();

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
        } else {
            int rcvBufferSize;
            try {
                rcvBufferSize = clientSocket.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new MinaRuntimeException("can't configure socket receive buffer size", e);
            }
            session.getConfig().setReadBufferSize(rcvBufferSize);
        }

        Integer sendBufferSize = config.getSendBufferSize();

        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        } else {
            int sndBufferSize;
            try {
                sndBufferSize = clientSocket.socket().getSendBufferSize();
            } catch (SocketException e) {
                throw new MinaRuntimeException("can't configure socket send buffe size", e);
            }
            session.getConfig().setSendBufferSize(sndBufferSize);
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

        // connect to a running server. We get an immediate result if
        // the socket is blocking, and either true or false if it's non blocking
        boolean connected;
        try {
            connected = clientSocket.connect(remoteAddress);
        } catch (IOException e) {
            ConnectFuture future = new ConnectFuture();
            future.cannotConnect(e);
            return future;
        }

        ConnectFuture connectFuture = new ConnectFuture();
        session.setConnectFuture(connectFuture);

        if (!connected) {
            // async connection, let's the connection complete in background, the selector loop will detect when the
            // connection is successful
            connectSelectorLoop.register(false, true, false, false, session, clientSocket, new RegistrationCallback() {

                @Override
                public void done(SelectionKey selectionKey) {
                    session.setSelectionKey(selectionKey);
                }
            });
        } else {
            // already connected (probably a loopback connection, or a blocking socket)
            // register for read
            connectSelectorLoop.register(false, false, true, false, session, clientSocket, new RegistrationCallback() {

                @Override
                public void done(SelectionKey selectionKey) {
                    session.setSelectionKey(selectionKey);
                }
            });

            session.setConnected();
        }

        return connectFuture;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void disconnect() throws IOException {
        LOG.info("Disconnecting sessions");

        // Close all the existing sessions
        for (IoSession session : getManagedSessions().values()) {
            session.close(true);
        }

        fireServiceInactivated();

        // will stop the idle processor if we are the last service
        idleChecker.destroy();
    }
}