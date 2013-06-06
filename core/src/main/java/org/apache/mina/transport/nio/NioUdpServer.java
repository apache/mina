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

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.MinaRuntimeException;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.executor.OrderedHandlerExecutor;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.udp.AbstractUdpServer;
import org.apache.mina.transport.udp.UdpSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a UDP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpServer extends AbstractUdpServer implements SelectorListener {

    static final Logger LOG = LoggerFactory.getLogger(NioUdpServer.class);

    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    // the bound local address
    private SocketAddress address = null;

    // used for detecting idle sessions
    private final IdleChecker idleChecker = new IndexedIdleChecker();

    // the inner channel for read/write UDP datagrams
    private DatagramChannel datagramChannel = null;

    // the key used for selecting read event
    private SelectionKey readKey = null;

    // list of all the sessions by remote socket address
    private final Map<SocketAddress /* remote socket address */, NioUdpSession> sessions = new ConcurrentHashMap<SocketAddress, NioUdpSession>();

    /** The selector loop used to incoming data */
    private final SelectorLoop readSelectorLoop;

    /**
     * Create an UDP server with a new selector pool of default size and a {@link IoHandlerExecutor} of default type (
     * {@link OrderedHandlerExecutor})
     */
    public NioUdpServer() {
        this(new NioSelectorLoop("accept", 0), null);
    }

    /**
     * Create an UDP server with a new selector pool of default size and a {@link IoHandlerExecutor} of default type (
     * {@link OrderedHandlerExecutor})
     * 
     * @param sessionConfig The configuration to use for this server
     */
    public NioUdpServer(UdpSessionConfig config) {
        this(config, new NioSelectorLoop("accept", 0), null);
    }

    /**
     * Create an UDP server with provided selector loops pool
     * 
     * @param acceptSelectorLoop the selector loop for handling accept events (connection of new session)
     * @param readWriteSelectorLoop the pool of selector loop for handling read/write events of connected sessions
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioUdpServer(SelectorLoop readSelectorLoop, IoHandlerExecutor handlerExecutor) {
        super(handlerExecutor);
        this.readSelectorLoop = readSelectorLoop;
    }

    /**
     * Create an UDP server with provided selector loops pool
     * 
     * @param sessionConfig The configuration to use for this server
     * @param acceptSelectorLoop the selector loop for handling accept events (connection of new session)
     * @param ioHandlerExecutor used for executing IoHandler event in another pool of thread (not in the low level I/O
     *        one). Use <code>null</code> if you don't want one. Be careful, the IoHandler processing will block the I/O
     *        operations.
     */
    public NioUdpServer(UdpSessionConfig config, SelectorLoop readSelectorLoop, IoHandlerExecutor handlerExecutor) {
        super(config, handlerExecutor);
        this.readSelectorLoop = readSelectorLoop;
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
    public SocketAddress getBoundAddress() {
        return address;
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
    public void bind(final SocketAddress localAddress) {
        if (localAddress == null) {
            // We should at least have one address to bind on
            throw new IllegalArgumentException("LocalAdress cannot be null");
        }

        // check if the address is already bound
        if (this.address != null) {
            throw new IllegalStateException("address " + address + " already bound");
        }
        address = localAddress;

        LOG.info("binding address {}", localAddress);

        try {
            datagramChannel = DatagramChannel.open();
            datagramChannel.socket().setReuseAddress(isReuseAddress());
            datagramChannel.socket().bind(address);
            datagramChannel.configureBlocking(false);
        } catch (IOException e) {
            throw new MinaRuntimeException("can't open the address " + address, e);
        }

        readSelectorLoop.register(false, false, true, false, this, datagramChannel, null);

        // it's the first address bound, let's fire the event
        this.fireServiceActivated();
    }

    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress) {
        throw new IllegalStateException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind() {
        LOG.info("unbinding {}", address);
        if (this.address == null) {
            throw new IllegalStateException("server not bound");
        }

        readSelectorLoop.unregister(this, datagramChannel);
        datagramChannel.socket().close();
        try {
            datagramChannel.close();
        } catch (IOException e) {
            throw new MinaRuntimeException("can't close the datagram socket", e);
        }

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
        // Process the reads first
        try {
            // System.err.println("remaining : " + readBuffer.remaining());
            final SocketAddress source = datagramChannel.receive(readBuffer);
            NioUdpSession session = null;

            // let's find the corresponding session
            if (source != null) {
                session = sessions.get(source);

                if (session == null) {
                    session = createSession(source, datagramChannel);
                }
                if (read) {
                    if (IS_DEBUG) {
                        LOG.debug("readable datagram for UDP service : {}", this);
                    }

                    readBuffer.flip();

                    if (IS_DEBUG) {
                        LOG.debug("read {} bytes form {}", readBuffer.remaining(), source);
                    }

                    session.receivedDatagram(readBuffer);

                }

                // Now, process the writes
                if (write) {
                    session.processWrite(readSelectorLoop);
                }
            } else {
                if (IS_DEBUG) {
                    LOG.debug("Do data to read");
                }
            }

        } catch (final IOException ex) {
            LOG.error("IOException while reading the socket", ex);
        }
    }

    private NioUdpSession createSession(SocketAddress remoteAddress, DatagramChannel datagramChannel)
            throws IOException {
        LOG.debug("create session");
        UdpSessionConfig config = getSessionConfig();
        SocketAddress localAddress = new InetSocketAddress(datagramChannel.socket().getLocalAddress(), datagramChannel
                .socket().getLocalPort());
        final NioUdpSession session = new NioUdpSession(this, idleChecker, datagramChannel, localAddress, remoteAddress);

        // apply idle configuration
        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE,
                config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        // apply the default service socket configuration

        Boolean reuseAddress = config.isReuseAddress();

        if (reuseAddress != null) {
            session.getConfig().setReuseAddress(reuseAddress);
        }

        Integer readBufferSize = config.getReadBufferSize();

        if (readBufferSize != null) {
            session.getConfig().setReadBufferSize(readBufferSize);
        }

        Integer sendBufferSize = config.getSendBufferSize();

        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        }

        Integer trafficClass = config.getTrafficClass();

        if (trafficClass != null) {
            session.getConfig().setTrafficClass(trafficClass);
        }

        // Manage the Idle status
        idleChecker.sessionRead(session, System.currentTimeMillis());
        idleChecker.sessionWritten(session, System.currentTimeMillis());

        sessions.put(remoteAddress, session);

        // Inform the handler that the session has been created
        session.setConnected();

        return session;
    }
}