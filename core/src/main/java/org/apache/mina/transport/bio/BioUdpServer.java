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
package org.apache.mina.transport.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.MinaRuntimeException;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.ConnectFuture;
import org.apache.mina.transport.udp.AbstractUdpServer;
import org.apache.mina.transport.udp.UdpSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A java blocking I/O based UDP server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class BioUdpServer extends AbstractUdpServer {

    private static final Logger LOG = LoggerFactory.getLogger(BioUdpServer.class);

    private SocketAddress boundAddress = null;

    // the inner server socket/channel
    DatagramChannel channel;

    // thread in charge of reading the server socket
    private Worker worker;
    private boolean bound = false;

    // for detecting idle sessions
    private IdleChecker idleChecker = new IndexedIdleChecker();

    // list of all the sessions by remote socket address
    private final Map<SocketAddress /* remote socket address */, BioUdpSession> sessions = new ConcurrentHashMap<SocketAddress, BioUdpSession>();

    /**
     * Create an UDP server
     */
    public BioUdpServer() {
        super(null);
    }

    public BioUdpServer(IoHandlerExecutor executor) {
        super(executor);
    }

    public BioUdpServer(UdpSessionConfig config, IoHandlerExecutor executor) {
        super(config, executor);
    }

    public DatagramChannel getDatagramChannel() {
        return channel;
    }

    @Override
    public SocketAddress getBoundAddress() {
        return boundAddress;
    }

    @Override
    public void bind(SocketAddress localAddress) {
        LOG.info("binding to address {}", localAddress);
        if (bound) {
            throw new IllegalStateException("already bound");
        }
        boundAddress = localAddress;
        try {
            channel = DatagramChannel.open();
            channel.socket().bind(localAddress);
            Boolean reuseAddress = config.isReuseAddress();

            if (reuseAddress != null) {
                channel.socket().setReuseAddress(reuseAddress);
            }

            Integer readBufferSize = config.getReadBufferSize();

            if (readBufferSize != null) {
                channel.socket().setReceiveBufferSize(readBufferSize);
            }

            Integer sendBufferSize = config.getSendBufferSize();

            if (sendBufferSize != null) {
                channel.socket().setSendBufferSize(sendBufferSize);
            }

            Integer trafficClass = config.getTrafficClass();

            if (trafficClass != null) {
                channel.socket().setTrafficClass(trafficClass);
            }
        } catch (IOException e) {
            throw new MinaRuntimeException("Can't open and configure a new udp socket", e);
        }

        worker = new Worker();
        bound = true;
        worker.start();
        idleChecker.start();
    }

    @Override
    public void bind(int port) {
        bind(new InetSocketAddress(port));
    }

    @Override
    public void unbind() {
        if (!bound) {
            throw new IllegalStateException("not bound");
        }
        bound = false;
        try {
            try {
                channel.close();
            } catch (IOException e) {
                throw new MinaRuntimeException("can't unbind the udp channel", e);
            }
            boundAddress = null;
            idleChecker.destroy();
            worker.join();
        } catch (InterruptedException e) {
            LOG.error("exception", e);
        }

    }

    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress) {
        BioUdpSession session = new BioUdpSession(remoteAddress, BioUdpServer.this, idleChecker);
        sessions.put(remoteAddress, session);
        ConnectFuture cf = new ConnectFuture();
        cf.complete(session);
        return cf;
    }

    private class Worker extends Thread {

        public Worker() {
            super("BioUdpServerWorker");
        }

        @Override
        public void run() {
            /** 64k receive buffer */
            ByteBuffer rcvdBuffer = ByteBuffer.allocate(64 * 1024);
            while (bound) {
                // I/O !
                rcvdBuffer.clear();
                try {
                    SocketAddress from = channel.receive(rcvdBuffer);
                    BioUdpSession session = sessions.get(from);
                    if (session == null) {
                        // create the session
                        session = new BioUdpSession(from, BioUdpServer.this, idleChecker);
                        sessions.put(from, session);
                        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE,
                                config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
                        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE,
                                config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));
                        idleChecker.sessionWritten(session, System.currentTimeMillis());
                        session.setConnected();
                        
                        // fire open
                        session.processSessionOpen();

                    }
                    rcvdBuffer.flip();
                    session.processMessageReceived(rcvdBuffer);
                    // Update the session idle status
                    idleChecker.sessionRead(session, System.currentTimeMillis());
                } catch (AsynchronousCloseException aec) {
                    LOG.debug("closed service");
                    break;
                } catch (IOException e) {
                    LOG.error("Exception while reading", e);
                }
            }
        }
    }

    /** remove a closed session from the list on managed sessions */
    void destroy(BioUdpSession bioUdpSession) {
        sessions.remove(bioUdpSession.getRemoteAddress());
    }
}