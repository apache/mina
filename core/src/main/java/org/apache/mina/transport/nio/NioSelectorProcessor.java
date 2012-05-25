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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLException;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.RuntimeIoException;
import org.apache.mina.service.AbstractIoService;
import org.apache.mina.service.SelectorProcessor;
import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A {@link SelectorProcessor} for processing NIO based {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioSelectorProcessor implements SelectorProcessor<NioTcpServer, NioUdpServer> {

    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(NioSelectorProcessor.class);

    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     */
    private static final long SELECT_TIMEOUT = 1000L;

    private SelectorStrategy<NioSelectorProcessor> strategy;

    /** Read buffer for all the incoming bytes (default to 64Kb) */
    private final ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024);

    /** the thread polling and processing the I/O events */
    private SelectorWorker worker = null;

    /** helper for detecting idleing sessions */
    private final IdleChecker idleChecker = new IndexedIdleChecker();

    /** A queue containing the servers to bind to this selector */
    private final Queue<NioTcpServer> tcpServersToadd = new ConcurrentLinkedQueue<NioTcpServer>();

    /** A queue containing the servers to bind to this selector */
    private final Queue<NioUdpServer> udpServersToadd = new ConcurrentLinkedQueue<NioUdpServer>();

    /** server to remove of the selector */
    private final Queue<NioTcpServer> tcpServersToRemove = new ConcurrentLinkedQueue<NioTcpServer>();

    /** server to remove of the selector */
    private final Queue<NioUdpServer> udpServersToRemove = new ConcurrentLinkedQueue<NioUdpServer>();

    /**
     * new session freshly accepted, placed here for being added to the selector
     */
    private final Queue<NioTcpSession> sessionsToConnect = new ConcurrentLinkedQueue<NioTcpSession>();

    /** session to be removed of the selector */
    private final Queue<NioTcpSession> sessionsToClose = new ConcurrentLinkedQueue<NioTcpSession>();

    /** A queue used to store the sessions to be flushed */
    private final Queue<NioTcpSession> flushingSessions = new ConcurrentLinkedQueue<NioTcpSession>();

    private Selector selector;

    // Lock for Selector worker, using default. can look into fairness later.
    // We need to think about a lock less mechanism here.
    private final Lock workerLock = new ReentrantLock();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setStrategy(SelectorStrategy<?> strategy) {
        this.strategy = (SelectorStrategy<NioSelectorProcessor>) strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServer(NioTcpServer server) {
        tcpServersToadd.add(server);
        this.wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addServer(NioUdpServer server) {
        udpServersToadd.add(server);
        this.wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeServer(NioTcpServer server) {
        tcpServersToRemove.add(server);
        this.wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeServer(NioUdpServer server) {
        udpServersToRemove.add(server);
        this.wakeupWorker();
    }

    /**
     * Wake the I/O worker thread and if none exists, create a new one FIXME :
     * too much locking there ?
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
    public void createSession(final IoService service, final Object clientSocket) throws SSLException {
        LOGGER.debug("create session");
        final SocketChannel socketChannel = (SocketChannel) clientSocket;
        final TcpSessionConfig config = (TcpSessionConfig) service.getSessionConfig();
        final NioTcpSession session = new NioTcpSession(service, socketChannel,
                this.strategy.getSelectorForNewSession(this), idleChecker);

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

    public IdleChecker getIdleChecker() {
        return idleChecker;
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
     * Add the session to list of session to close and remove
     * @param session
     */
    public void addSessionToClose(NioTcpSession session) {
        sessionsToClose.add(session);
    }

    public void cancelKeyForWritting(NioTcpSession session) {

        // a key registered for read ? (because we can have a
        // Selector for reads and another for the writes
        SelectionKey readKey = this.sessionReadKey.get(session);

        if (readKey != null) {
            LOGGER.debug("registering key for only reading");
            SelectionKey mykey;
            try {
                mykey = session.getSocketChannel().register(selector, SelectionKey.OP_READ, session);
                this.sessionReadKey.put(session, mykey);
            } catch (ClosedChannelException e) {
                LOGGER.error("already closed session", e);
            }
        } else {
            LOGGER.debug("cancel key for writing");
            session.getSocketChannel().keyFor(NioSelectorProcessor.this.selector).cancel();
        }
    }

    // map for finding read keys associated with a given session
    private Map<NioTcpSession, SelectionKey> sessionReadKey;

    /**
     * The worker processing incoming session creation, session destruction
     * requests, session write and reads. It will also bind new servers.
     */
    private class SelectorWorker extends Thread {

        public SelectorWorker() {
            sessionReadKey = new HashMap<NioTcpSession, SelectionKey>();
        }

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
                        if (NioSelectorProcessor.this.tcpServersToRemove.size() > 0
                                || NioSelectorProcessor.this.udpServersToRemove.size() > 0) {
                            this.processServerRemove();
                        }

                        // pop new server sockets for accepting
                        if (NioSelectorProcessor.this.tcpServersToadd.size() > 0
                                || NioSelectorProcessor.this.udpServersToadd.size() > 0) {
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

                            // Loop on each SelectionKey and process any valid
                            // action
                            while (selectedKeys.hasNext()) {
                                SelectionKey key = selectedKeys.next();
                                selectedKeys.remove();

                                if (!key.isValid()) {
                                    continue;
                                }

                                NioSelectorProcessor.this.selector.selectedKeys().remove(key);

                                if (key.isAcceptable()) {
                                    ((SelectorEventListener) key.attachment()).acceptReady(NioSelectorProcessor.this);
                                }

                                if (key.isReadable()) {
                                    ((SelectorEventListener) key.attachment()).readReady(NioSelectorProcessor.this,
                                            readBuffer);
                                }

                                if (key.isWritable()) {
                                    ((SelectorEventListener) key.attachment()).writeReady(NioSelectorProcessor.this);
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
         * Handles the servers addition
         */
        private void processServerAdd() throws IOException {

            NioTcpServer tcpServer;
            while ((tcpServer = tcpServersToadd.poll()) != null) {
                // register for accept
                SelectionKey key = tcpServer.getServerSocketChannel().register(NioSelectorProcessor.this.selector,
                        SelectionKey.OP_ACCEPT);
                key.attach(tcpServer);
                tcpServer.setAcceptKey(key);
                LOGGER.debug("registered for accept : {}", tcpServer);
            }

            NioUdpServer udpServer;
            while ((udpServer = udpServersToadd.poll()) != null) {
                // register for read
                SelectionKey key = udpServer.getDatagramChannel().register(NioSelectorProcessor.this.selector,
                        SelectionKey.OP_READ);
                key.attach(udpServer);
                udpServer.setReadKey(key);
                LOGGER.debug("registered for accept : {}", udpServer);
            }
        }

        /**
         * Handles the servers removal
         */
        private void processServerRemove() {
            NioTcpServer tcpServer;
            while ((tcpServer = tcpServersToRemove.poll()) != null) {
                // find the server key and cancel it
                SelectionKey key = tcpServer.getAcceptKey();
                key.cancel();
                tcpServer.setAcceptKey(null);
                key.attach(null);
            }
            NioUdpServer udpServer;
            while ((udpServer = udpServersToRemove.poll()) != null) {
                // find the server key and cancel it
                SelectionKey key = udpServer.getReadKey();
                key.cancel();
                udpServer.setReadKey(null);
                key.attach(null);
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

                sessionReadKey.put(session, key);

                // Switch to CONNECTED, only if the session is not secured, as
                // the SSL Handshake
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

                SelectionKey key = sessionReadKey.remove(session);
                key.cancel();

                // closing underlying socket
                session.getSocketChannel().close();
                // fire the event
                session.processSessionClosed();
                ((AbstractIoService) session.getService()).fireSessionDestroyed(session);
            }
        }

        /**
         * Flushes the sessions
         */
        private void processFlushSessions() throws IOException {
            NioTcpSession session = NioSelectorProcessor.this.flushingSessions.poll();
            // a key registered for read ? (because we can have a
            // Selector for reads and another for the writes
            SelectionKey readKey = sessionReadKey.get(session);

            if (readKey != null) {
                // register for read/write
                SelectionKey key = session.getSocketChannel().register(NioSelectorProcessor.this.selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE, session);

                sessionReadKey.put(session, key);
            } else {
                session.getSocketChannel().register(NioSelectorProcessor.this.selector, SelectionKey.OP_WRITE, session);
            }
        }
    }
}
