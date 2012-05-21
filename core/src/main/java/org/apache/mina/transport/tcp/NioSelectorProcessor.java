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

package org.apache.mina.transport.tcp;

import static org.apache.mina.api.IoSession.SSL_HELPER;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLException;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoServer;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.RuntimeIoException;
import org.apache.mina.service.AbstractIoService;
import org.apache.mina.service.SelectorProcessor;
import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.SslHelper;
import org.apache.mina.session.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A {@link SelectorProcessor} for processing NIO based {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioSelectorProcessor implements SelectorProcessor {
    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(NioSelectorProcessor.class);

    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     */
    private static final long SELECT_TIMEOUT = 1000L;

    private final SelectorStrategy strategy;

    private final Map<SocketAddress, ServerSocketChannel> serverSocketChannels = new ConcurrentHashMap<SocketAddress, ServerSocketChannel>();

    /** Read buffer for all the incoming bytes (default to 64Kb) */
    private final ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024);

    /** the thread polling and processing the I/O events */
    private SelectorWorker worker = null;

    /** helper for detecting idleing sessions */
    private final IdleChecker idleChecker = new IndexedIdleChecker();

    /** A queue containing the servers to bind to this selector */
    private final Queue<Object[]> serversToAdd = new ConcurrentLinkedQueue<Object[]>();

    /** server to remove of the selector */
    private final Queue<ServerSocketChannel> serversToRemove = new ConcurrentLinkedQueue<ServerSocketChannel>();

    /** new session freshly accepted, placed here for being added to the selector */
    private final Queue<NioTcpSession> sessionsToConnect = new ConcurrentLinkedQueue<NioTcpSession>();

    /** session to be removed of the selector */
    private final Queue<NioTcpSession> sessionsToClose = new ConcurrentLinkedQueue<NioTcpSession>();

    /** A queue used to store the sessions to be flushed */
    private final Queue<NioTcpSession> flushingSessions = new ConcurrentLinkedQueue<NioTcpSession>();

    private Selector selector;

    // Lock for Selector worker, using default. can look into fairness later.
    // We need to think about a lock less mechanism here.
    private final Lock workerLock = new ReentrantLock();

    public NioSelectorProcessor(final String name, final SelectorStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Add a bound server channel for starting accepting new client connections.
     * 
     * @param serverChannel
     */
    private void add(final ServerSocketChannel serverChannel, final IoServer server) {
        LOGGER.debug("adding a server channel {} for server {}", serverChannel, server);
        this.serversToAdd.add(new Object[] { serverChannel, server });
        this.wakeupWorker();
    }

    /**
     * Wake the I/O worker thread and if none exists, create a new one
     * FIXME : too much locking there ?
     */
    private void wakeupWorker() {
        this.workerLock.lock();
        try {
            if (this.worker == null) {
                this.worker = new SelectorWorker();
                this.worker.start();
            }
        } finally {
            this.workerLock.unlock();
        }

        if (this.selector != null) {
            this.selector.wakeup();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindAndAcceptAddress(final IoServer server, final SocketAddress address) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        // FIXME : should be "genericified"
        if (server instanceof AbstractTcpServer) {
            serverSocketChannel.socket().setReuseAddress(((AbstractTcpServer) server).isReuseAddress());
        }
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.configureBlocking(false);
        this.serverSocketChannels.put(address, serverSocketChannel);
        this.add(serverSocketChannel, server);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind(final SocketAddress address) throws IOException {
        ServerSocketChannel channel = this.serverSocketChannels.get(address);
        channel.socket().close();
        channel.close();
        if (this.serverSocketChannels.remove(address) == null) {
            LOGGER.warn("The server channel for address {} was already unbound", address);
        }
        LOGGER.debug("Removing a server channel {}", channel);
        this.serversToRemove.add(channel);
        this.wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSession(final IoService service, final Object clientSocket) throws SSLException {
        LOGGER.debug("create session");
        final SocketChannel socketChannel = (SocketChannel) clientSocket;
        final TcpSessionConfig config = (TcpSessionConfig) service.getSessionConfig();
        final NioTcpSession session = new NioTcpSession(service, socketChannel,
                this.strategy.getSelectorForNewSession(this));

        try {
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            LOGGER.error("Unexpected exception, while configuring socket as non blocking", e);
            throw new RuntimeIoException("cannot configure socket as non-blocking", e);
        }
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

        Integer receiveBufferSize = config.getReceiveBufferSize();

        if (receiveBufferSize != null) {
            session.getConfig().setReceiveBufferSize(receiveBufferSize);
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

        // event session created
        session.processSessionCreated();

        // add the session to the queue for being added to the selector
        this.sessionsToConnect.add(session);
        this.wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush(final AbstractIoSession session) {
        LOGGER.debug("scheduling session {} for writing", session);
        // add the session to the list of session to be registered for writing
        this.flushingSessions.add((NioTcpSession) session);
        // wake the selector for unlocking the I/O thread
        this.wakeupWorker();
    }

    /**
     * The worker processing incoming session creation, session destruction requests, session write and reads.
     * It will also bind new servers.
     */
    private class SelectorWorker extends Thread {
        // map for finding the keys associated with a given server
        private final Map<ServerSocketChannel, SelectionKey> serverKey = new HashMap<ServerSocketChannel, SelectionKey>();

        // map for finding read keys associated with a given session
        private final Map<NioTcpSession, SelectionKey> sessionReadKey = new HashMap<NioTcpSession, SelectionKey>();

        @Override
        public void run() {
            try {
                if (NioSelectorProcessor.this.selector == null) {
                    LOGGER.debug("opening a new selector");

                    try {
                        NioSelectorProcessor.this.selector = Selector.open();
                    } catch (IOException e) {
                        LOGGER.error("IOException while opening a new Selector", e);
                    }
                }

                for (;;) {
                    try {
                        // pop server sockets for removing
                        if (NioSelectorProcessor.this.serversToRemove.size() > 0) {
                            this.processServerRemove();
                        }

                        // pop new server sockets for accepting
                        if (NioSelectorProcessor.this.serversToAdd.size() > 0) {
                            this.processServerAdd();
                        }

                        // pop new session for starting read/write
                        if (NioSelectorProcessor.this.sessionsToConnect.size() > 0) {
                            this.processConnectSessions();
                        }

                        // pop session for close, if any
                        if (NioSelectorProcessor.this.sessionsToClose.size() > 0) {
                            this.processCloseSessions();
                        }

                        LOGGER.debug("selecting...");
                        int readyCount = NioSelectorProcessor.this.selector.select(SELECT_TIMEOUT);
                        LOGGER.debug("... done selecting : {}", readyCount);

                        if (readyCount > 0) {
                            // process selected keys
                            Iterator<SelectionKey> selectedKeys = NioSelectorProcessor.this.selector.selectedKeys()
                                    .iterator();

                            // Loop on each SelectionKey and process any valid action
                            while (selectedKeys.hasNext()) {
                                SelectionKey key = selectedKeys.next();
                                selectedKeys.remove();

                                if (!key.isValid()) {
                                    continue;
                                }

                                NioSelectorProcessor.this.selector.selectedKeys().remove(key);

                                if (key.isAcceptable()) {
                                    this.processAccept(key);
                                }

                                if (key.isReadable()) {
                                    this.processRead(key);
                                }

                                if (key.isWritable()) {
                                    this.processWrite(key);
                                }

                            }
                        }

                        // registering session with data in the write queue for
                        // writing
                        while (!NioSelectorProcessor.this.flushingSessions.isEmpty()) {
                            this.processFlushSessions();
                        }
                    } catch (IOException e) {
                        LOGGER.error("IOException while selecting selector", e);
                    }

                    // stop the worker if needed
                    NioSelectorProcessor.this.workerLock.lock();

                    try {
                        if (NioSelectorProcessor.this.selector.keys().isEmpty()) {
                            NioSelectorProcessor.this.worker = null;
                            break;
                        }
                    } finally {
                        NioSelectorProcessor.this.workerLock.unlock();
                    }

                    // check for idle events
                    NioSelectorProcessor.this.idleChecker.processIdleSession(System.currentTimeMillis());
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected exception : ", e);
            }
        }

        /**
         * Handles the servers removal
         */
        private void processServerRemove() {
            while (!NioSelectorProcessor.this.serversToRemove.isEmpty()) {
                ServerSocketChannel channel = NioSelectorProcessor.this.serversToRemove.poll();
                SelectionKey key = this.serverKey.remove(channel);

                if (key == null) {
                    LOGGER.error("The server socket was already removed of the selector");
                } else {
                    LOGGER.debug("Removing the server from this selector : {}", key);
                    key.cancel();
                }
            }
        }

        /**
         * Handles the servers addition
         */
        private void processServerAdd() throws IOException {
            while (!NioSelectorProcessor.this.serversToAdd.isEmpty()) {
                Object[] tmp = NioSelectorProcessor.this.serversToAdd.poll();
                ServerSocketChannel channel = (ServerSocketChannel) tmp[0];
                SelectionKey key = channel.register(NioSelectorProcessor.this.selector, SelectionKey.OP_ACCEPT);
                key.attach(tmp);
                LOGGER.debug("Accepted the server on this selector : {}", key);
            }
        }

        /**
         * Handles all the sessions that must be connected
         */
        private void processConnectSessions() throws IOException {
            while (!NioSelectorProcessor.this.sessionsToConnect.isEmpty()) {
                NioTcpSession session = NioSelectorProcessor.this.sessionsToConnect.poll();
                SelectionKey key = session.getSocketChannel().register(NioSelectorProcessor.this.selector,
                        SelectionKey.OP_READ);
                key.attach(session);
                this.sessionReadKey.put(session, key);

                // Switch to CONNECTED, only if the session is not secured, as the SSL Handshake
                // will occur later.
                if (!session.isSecured()) {
                    session.setConnected();

                    // fire the event
                    ((AbstractIoService) session.getService()).fireSessionCreated(session);
                    session.processSessionOpened();
                    long time = System.currentTimeMillis();
                    NioSelectorProcessor.this.idleChecker.sessionRead(session, time);
                    NioSelectorProcessor.this.idleChecker.sessionWritten(session, time);
                }
            }
        }

        /**
         * Handles all the sessions that must be closed
         */
        private void processCloseSessions() throws IOException {
            while (!NioSelectorProcessor.this.sessionsToClose.isEmpty()) {
                NioTcpSession session = NioSelectorProcessor.this.sessionsToClose.poll();

                SelectionKey key = this.sessionReadKey.remove(session);
                key.cancel();

                // closing underlying socket
                session.getSocketChannel().close();
                // fire the event
                session.processSessionClosed();
                ((AbstractIoService) session.getService()).fireSessionDestroyed(session);
            }
        }

        /**
         * Processes the Accept action for the given SelectionKey
         */
        private void processAccept(final SelectionKey key) throws IOException {
            LOGGER.debug("acceptable new client {}", key);
            ServerSocketChannel serverSocket = (ServerSocketChannel) ((Object[]) key.attachment())[0];
            IoServer server = (IoServer) (((Object[]) key.attachment())[1]);
            // accepted connection
            SocketChannel newClientChannel = serverSocket.accept();
            LOGGER.debug("client accepted");
            // and give it's to the strategy
            NioSelectorProcessor.this.strategy.getSelectorForNewSession(NioSelectorProcessor.this).createSession(
                    server, newClientChannel);
        }

        /**
         * Processes the Read action for the given SelectionKey
         */
        private void processRead(final SelectionKey key) throws IOException {
            LOGGER.debug("readable client {}", key);
            NioTcpSession session = (NioTcpSession) key.attachment();
            SocketChannel channel = session.getSocketChannel();
            NioSelectorProcessor.this.readBuffer.clear();
            int readCount = channel.read(NioSelectorProcessor.this.readBuffer);

            LOGGER.debug("read {} bytes", readCount);

            if (readCount < 0) {
                // session closed by the remote peer
                LOGGER.debug("session closed by the remote peer");
                NioSelectorProcessor.this.sessionsToClose.add(session);
            } else {
                // we have read some data
                // limit at the current position & rewind buffer back to start & push to the chain
                NioSelectorProcessor.this.readBuffer.flip();

                if (session.isSecured()) {
                    // We are reading data over a SSL/TLS encrypted connection. Redirect
                    // the processing to the SslHelper class.
                    SslHelper sslHelper = session.getAttribute(SSL_HELPER, null);

                    if (sslHelper == null) {
                        throw new IllegalStateException();
                    }

                    sslHelper.processRead(session, NioSelectorProcessor.this.readBuffer);
                } else {
                    // Plain message, not encrypted : go directly to the chain
                    session.processMessageReceived(NioSelectorProcessor.this.readBuffer);
                }

                NioSelectorProcessor.this.idleChecker.sessionRead(session, System.currentTimeMillis());
            }
        }

        /**
         * Processes the Write action for the given SelectionKey
         */
        private void processWrite(final SelectionKey key) throws IOException {
            NioTcpSession session = (NioTcpSession) key.attachment();

            LOGGER.debug("writable session : {}", session);

            session.setNotRegisteredForWrite();

            // write from the session write queue
            boolean isEmpty = false;

            try {
                Queue<WriteRequest> queue = session.acquireWriteQueue();

                do {
                    // get a write request from the queue
                    WriteRequest wreq = queue.peek();

                    if (wreq == null) {
                        break;
                    }

                    ByteBuffer buf = (ByteBuffer) wreq.getMessage();

                    // Note that if the connection is secured, the buffer already
                    // contains encrypted data.
                    int wrote = session.getSocketChannel().write(buf);
                    session.incrementWrittenBytes(wrote);
                    LOGGER.debug("wrote {} bytes to {}", wrote, session);

                    NioSelectorProcessor.this.idleChecker.sessionWritten(session, System.currentTimeMillis());

                    if (buf.remaining() == 0) {
                        // completed write request, let's remove it
                        queue.remove();
                        // complete the future
                        DefaultWriteFuture future = (DefaultWriteFuture) wreq.getFuture();

                        if (future != null) {
                            future.complete();
                        }
                    } else {
                        // output socket buffer is full, we need
                        // to give up until next selection for
                        // writing
                        break;
                    }
                } while (!queue.isEmpty());

                isEmpty = queue.isEmpty();
            } finally {
                session.releaseWriteQueue();
            }

            // if the session is no more interested in writing, we need
            // to stop listening for OP_WRITE events
            if (isEmpty) {
                if (session.isClosing()) {
                    LOGGER.debug("closing session {} have empty write queue, so we close it", session);
                    // we was flushing writes, now we to the close
                    session.getSocketChannel().close();
                } else {
                    // a key registered for read ? (because we can have a
                    // Selector for reads and another for the writes
                    SelectionKey readKey = this.sessionReadKey.get(session);

                    if (readKey != null) {
                        LOGGER.debug("registering key for only reading");
                        SelectionKey mykey = session.getSocketChannel().register(NioSelectorProcessor.this.selector,
                                SelectionKey.OP_READ, session);
                        this.sessionReadKey.put(session, mykey);
                    } else {
                        LOGGER.debug("cancel key for writing");
                        session.getSocketChannel().keyFor(NioSelectorProcessor.this.selector).cancel();
                    }
                }
            }
        }

        /**
         * Flushes the sessions
         */
        private void processFlushSessions() throws IOException {
            NioTcpSession session = NioSelectorProcessor.this.flushingSessions.poll();
            // a key registered for read ? (because we can have a
            // Selector for reads and another for the writes
            SelectionKey readKey = this.sessionReadKey.get(session);

            if (readKey != null) {
                // register for read/write
                SelectionKey key = session.getSocketChannel().register(NioSelectorProcessor.this.selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE, session);

                this.sessionReadKey.put(session, key);
            } else {
                session.getSocketChannel().register(NioSelectorProcessor.this.selector, SelectionKey.OP_WRITE, session);
            }
        }
    }
}
