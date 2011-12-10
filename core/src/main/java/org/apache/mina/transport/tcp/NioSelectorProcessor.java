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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.apache.mina.api.IoServer;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.IoSession.SessionState;
import org.apache.mina.api.RuntimeIoException;
import org.apache.mina.service.AbstractIoService;
import org.apache.mina.service.SelectorProcessor;
import org.apache.mina.service.SelectorStrategy;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.SslHelper;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.tcp.nio.NioTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A {@link SelectorProcessor} for processing NIO based {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class NioSelectorProcessor implements SelectorProcessor {
    /**
     * A timeout used for the select, as we need to get out to deal with idle
     * sessions
     */
    private static final long SELECT_TIMEOUT = 1000L;

    private SelectorStrategy strategy;

    private static final Logger LOGGER = LoggerFactory.getLogger(NioSelectorProcessor.class);

    private Map<SocketAddress, ServerSocketChannel> serverSocketChannels = new ConcurrentHashMap<SocketAddress, ServerSocketChannel>();

    /** Read buffer for all the incoming bytes */
    private ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024);

    /** Application buffer for all the outgoing messages */
    private ByteBuffer appBuffer = ByteBuffer.allocate(16 * 1024);
    
    /** An empty buffer used during the handshake phase */
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    // the thread polling and processing the I/O events
    private SelectorWorker worker = null;

    /**
     * new binded server to add to the selector {ServerSocketChannel, IoServer}
     * jvermillard : FIXME the typing is ugly !!!
     */
    private final Queue<Object[]> serversToAdd = new ConcurrentLinkedQueue<Object[]>();

    /** server to remove of the selector */
    private final Queue<ServerSocketChannel> serversToRemove = new ConcurrentLinkedQueue<ServerSocketChannel>();

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
    private Lock workerLock = new ReentrantLock();

    public NioSelectorProcessor(String name, SelectorStrategy strategy) {
        this.strategy = strategy;

        // FIXME : configurable parameter
        readBuffer = ByteBuffer.allocate(1024);
    }

    /**
     * Add a bound server channel for starting accepting new client connections.
     * 
     * @param serverChannel
     */
    private void add(ServerSocketChannel serverChannel, IoServer server) {
        LOGGER.debug("adding a server channel {} for server {}", serverChannel, server);
        serversToAdd.add(new Object[] { serverChannel, server });
        wakeupWorker();
    }

    /**
     * Wake the I/O worker thread and if none exists, create a new one
     * FIXME : too much locking there ?
     */
    private void wakeupWorker() {
        workerLock.lock();
        try {
            if (worker == null) {
                worker = new SelectorWorker();
                worker.start();
            }
        } finally {
            workerLock.unlock();
        }

        if (selector != null) {
            selector.wakeup();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindAndAcceptAddress(IoServer server, SocketAddress address) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        // FIXME : should be "genericified"
        if (server instanceof AbstractTcpServer) {
            serverSocketChannel.socket().setReuseAddress(((AbstractTcpServer) server).isReuseAddress());
        }
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannels.put(address, serverSocketChannel);
        add(serverSocketChannel, server);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind(SocketAddress address) throws IOException {
        ServerSocketChannel channel = serverSocketChannels.get(address);
        channel.socket().close();
        channel.close();
        if (serverSocketChannels.remove(address) == null) {
            LOGGER.warn("The server channel for address {} was already unbound", address);
        }
        LOGGER.debug("Removing a server channel {}", channel);
        serversToRemove.add(channel);
        wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSession(IoService service, Object clientSocket) throws SSLException {
        LOGGER.debug("create session");
        final SocketChannel socketChannel = (SocketChannel) clientSocket;
        final SocketSessionConfig defaultConfig = (SocketSessionConfig) service.getSessionConfig();
        final NioTcpSession session = new NioTcpSession((NioTcpServer) service, socketChannel,
                strategy.getSelectorForNewSession(this));

        try {
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            LOGGER.error("Unexpected exception, while configuring socket as non blocking", e);
            throw new RuntimeIoException("cannot configure socket as non-blocking", e);
        }

        // apply the default service socket configuration
        Boolean keepAlive = defaultConfig.isKeepAlive();
        
        if (keepAlive != null) {
            session.getConfig().setKeepAlive(keepAlive);
        }

        Boolean oobInline = defaultConfig.isOobInline();
        
        if (oobInline != null) {
            session.getConfig().setOobInline(oobInline);
        }

        Boolean reuseAddress = defaultConfig.isReuseAddress();
        
        if (reuseAddress != null) {
            session.getConfig().setReuseAddress(reuseAddress);
        }

        Boolean tcpNoDelay = defaultConfig.isTcpNoDelay();
        
        if (tcpNoDelay != null) {
            session.getConfig().setTcpNoDelay(tcpNoDelay);
        }

        Integer receiveBufferSize = defaultConfig.getReceiveBufferSize();
        
        if (receiveBufferSize != null) {
            session.getConfig().setReceiveBufferSize(receiveBufferSize);
        }

        Integer sendBufferSize = defaultConfig.getSendBufferSize();
        
        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        }

        Integer trafficClass = defaultConfig.getTrafficClass();
        
        if (trafficClass != null) {
            session.getConfig().setTrafficClass(trafficClass);
        }

        Integer soLinger = defaultConfig.getSoLinger();
        
        if (soLinger != null) {
            session.getConfig().setSoLinger(soLinger);
        }
        
        // Set the secured flag if the service is to be used over SSL/TLS
        if (service.isSecured()) {
            session.initSecure( service.getSslContext() );
        }

        // event session created
        session.getFilterChain().processSessionCreated(session);

        // add the session to the queue for being added to the selector
        sessionsToConnect.add(session);
        wakeupWorker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush(IoSession session) {
        LOGGER.debug("scheduling session {} for writing", session);
        // add the session to the list of session to be registered for writing
        flushingSessions.add((NioTcpSession) session);
        // wake the selector for unlocking the I/O thread
        wakeupWorker();
    }

    /**
     * The worker processing incoming session creation, session destruction requests, session write and reads.
     * It will also bind new servers.
     */
    private class SelectorWorker extends Thread {
        // map for finding the keys associated with a given server
        private Map<ServerSocketChannel, SelectionKey> serverKey = new HashMap<ServerSocketChannel, SelectionKey>();

        // map for finding read keys associated with a given session
        private Map<NioTcpSession, SelectionKey> sessionReadKey = new HashMap<NioTcpSession, SelectionKey>();
        
        private boolean handshaking = false;

        @Override
        public void run() {
            if (selector == null) {
                LOGGER.debug("opening a new selector");

                try {
                    selector = Selector.open();
                } catch (IOException e) {
                    LOGGER.error("IOException while opening a new Selector", e);
                }
            }

            for (;;) {
                try {
                    // pop server sockets for removing
                    if (serversToRemove.size() > 0) {
                        while (!serversToRemove.isEmpty()) {
                            ServerSocketChannel channel = serversToRemove.poll();
                            SelectionKey key = serverKey.remove(channel);

                            if (key == null) {
                                LOGGER.error("The server socket was already removed of the selector");
                            } else {
                                key.cancel();
                            }
                        }
                    }

                    // pop new server sockets for accepting
                    if (serversToAdd.size() > 0) {
                        while (!serversToAdd.isEmpty()) {
                            Object[] tmp = serversToAdd.poll();
                            ServerSocketChannel channel = (ServerSocketChannel) tmp[0];
                            SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
                            key.attach(tmp);
                        }
                    }

                    // pop new session for starting read/write
                    if (sessionsToConnect.size() > 0) {
                        processConnectSessions();
                    }

                    // pop session for close, if any
                    if (sessionsToClose.size() > 0) {
                        processCloseSessions();
                    }

                    LOGGER.debug("selecting...");
                    int readyCount = selector.select(SELECT_TIMEOUT);
                    LOGGER.debug("... done selecting : {}", readyCount);

                    if (readyCount > 0) {
                        // process selected keys
                        Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                        // Loop on each SelectionKey and process any valid action
                        while (selectedKeys.hasNext()) {
                            SelectionKey key = selectedKeys.next();
                            selectedKeys.remove();

                            if (!key.isValid()) {
                                continue;
                            }

                            selector.selectedKeys().remove(key);

                            if (key.isReadable()) {
                                processRead(key);
                            }

                            if (key.isWritable()) {
                                processWrite(key);
                            }

                            if (key.isAcceptable()) {
                                processAccept(key);
                            }
                        }
                    }

                    // registering session with data in the write queue for
                    // writing
                    while (!flushingSessions.isEmpty()) {
                        processFushSessions();
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException while selecting selector", e);
                }

                // stop the worker if needed
                workerLock.lock();
                try {
                    if (selector.keys().isEmpty()) {
                        worker = null;
                        break;
                    }
                } finally {
                    workerLock.unlock();
                }
            }
        }
        
        /**
         * Handles all the sessions that must be connected
         */
        private void processConnectSessions() throws IOException {
            while (!sessionsToConnect.isEmpty()) {
                NioTcpSession session = sessionsToConnect.poll();
                SelectionKey key = session.getSocketChannel().register(selector, SelectionKey.OP_READ);
                key.attach(session);
                sessionReadKey.put(session, key);

                // Switch to CONNECTED, only if the session is not secured, as the SSL Handshake
                // will occur later.
                if (!session.isSecured()) {
                    session.setConnected();
                    
                    // fire the event
                    ((AbstractIoService) session.getService()).fireSessionCreated(session);
                    session.getFilterChain().processSessionOpened(session);
                }
            }
        }
        
        /**
         * Handles all the sessions that must be closed
         */
        private void processCloseSessions() throws IOException {
            while (!sessionsToClose.isEmpty()) {
                NioTcpSession session = sessionsToClose.poll();

                SelectionKey key = sessionReadKey.remove(session);
                key.cancel();

                // closing underlying socket
                session.getSocketChannel().close();
                // fire the event
                session.getFilterChain().processSessionClosed(session);
                ((AbstractIoService) session.getService()).fireSessionDestroyed(session);
            }
        }
        
        /**
         * Processes the Accept action for the given SelectionKey
         */
        private void processAccept(SelectionKey key) throws IOException {
            LOGGER.debug("acceptable new client {}", key);
            ServerSocketChannel serverSocket = (ServerSocketChannel) ((Object[]) key.attachment())[0];
            IoServer server = (IoServer) (((Object[]) key.attachment())[1]);
            // accepted connection
            SocketChannel newClientChannel = serverSocket.accept();
            LOGGER.debug("client accepted");
            // and give it's to the strategy
            strategy.getSelectorForNewSession(NioSelectorProcessor.this).createSession(server,
                    newClientChannel);
        }
        
        /**
         * Processes the Read action for the given SelectionKey
         */
        private void processRead(SelectionKey key) throws IOException{
            LOGGER.debug("readable client {}", key);
            NioTcpSession session = (NioTcpSession) key.attachment();
            SocketChannel channel = session.getSocketChannel();
            readBuffer.clear();
            int readCount = channel.read(readBuffer);

            LOGGER.debug("read {} bytes", readCount);

            if (readCount < 0) {
                // session closed by the remote peer
                LOGGER.debug("session closed by the remote peer");
                sessionsToClose.add(session);
            } else {
                // we have read some data
                // limit at the current position & rewind buffer back to start & push to the chain
                readBuffer.flip();
                
                if (session.isSecured() && !session.isConnectedSecured()) {
                    // Process the SSL handshake now
                    processHandShake(session, readBuffer);
                } else {
                    session.getFilterChain().processMessageReceived(session, readBuffer);
                }
            }
        }
        
        private boolean processHandShake(IoSession session, ByteBuffer inBuffer) throws SSLException {
            SslHelper sslHelper = session.getAttribute( IoSession.SSL_HELPER );
            
            if (sslHelper == null) {
                throw new IllegalStateException();
            }
            
            SSLEngine engine = sslHelper.getEngine();
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            boolean processingData = true;
            
            // Start the Handshake if we aren't already processing a HandShake
            if (!handshaking) {
                engine.beginHandshake();
                handshaking = true;
            }
            
            hsStatus = engine.getHandshakeStatus();

            // If the SSLEngine has not be started, then the status will be NOT_HANDSHAKING
            while ((hsStatus != HandshakeStatus.FINISHED) &&
                   (hsStatus != HandshakeStatus.NOT_HANDSHAKING ) &&
                   processingData) {
                switch (hsStatus) {
                    case NEED_TASK :
                        hsStatus = sslHelper.processTasks(engine);
                        
                        break;
                        
                    case NEED_WRAP :
                        if ( LOGGER.isDebugEnabled()) {
                            LOGGER.debug("{} processing the NEED_WRAP state", session);
                        }
                        
                        int capacity = engine.getSession().getPacketBufferSize();
                        ByteBuffer outBuffer = ByteBuffer.allocate(capacity);
                        SSLEngineResult result = null;

                        while (true) {
                            result = engine.wrap(EMPTY_BUFFER, outBuffer);
                            
                            if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                                // TODO : increase the AppBuffer size
                            } else {
                                break;
                            }
                        }
        
                        outBuffer.flip();
                        session.write(outBuffer);
                        hsStatus = result.getHandshakeStatus();

                        // We continue to loop while we don't expect messages to unwrap,
                        // otherwise, we have to exit the loop.
                        processingData = (hsStatus != HandshakeStatus.NEED_UNWRAP);

                        break;
                        
                    case NEED_UNWRAP :
                        Status status = sslHelper.processUnwrap(engine, inBuffer, EMPTY_BUFFER);

                        if ( status == Status.BUFFER_UNDERFLOW) {
                            // Read more data
                            processingData = false;
                        } else {
                            hsStatus = engine.getHandshakeStatus();
                        }
                        
                        break;
                }
            }
            
            if (hsStatus == HandshakeStatus.FINISHED) {
                if ( LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} processing the FINISHED state", session);
                }
                
                session.changeState(SessionState.SECURED);
                handshaking = false;

                return true;
            }
            
            return false;
        }
        
        /**
         * Processes the Write action for the given SelectionKey
         */
        private void processWrite(SelectionKey key) throws IOException {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("writable session : {}", key.attachment());
            }
            
            NioTcpSession session = (NioTcpSession) key.attachment();
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

                    int wrote = session.getSocketChannel().write(buf);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("wrote {} bytes to {}", wrote, session);
                    }

                    if (buf.remaining() == 0) {
                        // completed write request, let's remove
                        // it
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
                // a key registered for read ? (because we can have a
                // Selector for reads and another for the writes
                SelectionKey readKey = sessionReadKey.get(session);
                
                if (readKey != null) {
                    LOGGER.debug("registering key for only reading");
                    SelectionKey mykey = session.getSocketChannel().register(selector,
                            SelectionKey.OP_READ, session);
                    sessionReadKey.put(session, mykey);
                } else {
                    LOGGER.debug("cancel key for writing");
                    session.getSocketChannel().keyFor(selector).cancel();
                }
            }
        }
        
        /**
         * Flushes the sessions
         */
        private void processFushSessions() throws IOException {
            NioTcpSession session = flushingSessions.poll();
            // a key registered for read ? (because we can have a
            // Selector for reads and another for the writes
            SelectionKey readKey = sessionReadKey.get(session);
            
            if (readKey != null) {
                // register for read/write
                SelectionKey key = session.getSocketChannel().register(selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE, session);

                sessionReadKey.put(session, key);
            } else {
                session.getSocketChannel().register(selector, SelectionKey.OP_WRITE, session);
            }
        }
    }
}
