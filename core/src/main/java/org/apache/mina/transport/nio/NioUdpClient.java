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
import java.nio.channels.DatagramChannel;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.MinaRuntimeException;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.ConnectFuture;
import org.apache.mina.transport.udp.AbstractUdpClient;
import org.apache.mina.transport.udp.UdpSessionConfig;
import org.apache.mina.util.Assert;

/**
 * This class implements a UDP NIO based client.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpClient extends AbstractUdpClient {

    /** the SelectorLoop for handling read/write session events */
    // This is final, so that we know if it's not initialized
    private final SelectorLoopPool readWriteSelectorPool;

    private final IndexedIdleChecker idleChecker = new IndexedIdleChecker();

    /**
     * Create a new instance of NioUdpClient
     */
    public NioUdpClient() {
        this(null);
    }

    /**
     * Create a new instance of NioUdpClient
     */
    public NioUdpClient(IoHandlerExecutor ioHandlerExecutor) {
        super(ioHandlerExecutor);
        readWriteSelectorPool = new FixedSelectorLoopPool("Client", 2);
        idleChecker.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress) {
        Assert.assertNotNull(remoteAddress, "remoteAddress");

        DatagramChannel ch;
        try {
            ch = DatagramChannel.open();
        } catch (IOException e) {
            throw new MinaRuntimeException("can't create a new socket, out of file descriptors ?", e);
        }
        try {
            ch.configureBlocking(false);
        } catch (IOException e) {
            throw new MinaRuntimeException("can't configure socket as non-blocking", e);
        }

        UdpSessionConfig config = getSessionConfig();

        NioSelectorLoop loop = (NioSelectorLoop) readWriteSelectorPool.getSelectorLoop();

        NioUdpSession session = new NioUdpSession(this, idleChecker, ch, null, remoteAddress, loop);

        session.setConnected();

        // apply idle configuration
        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE,
                config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        // Manage the Idle status
        idleChecker.sessionRead(session, System.currentTimeMillis());
        idleChecker.sessionWritten(session, System.currentTimeMillis());

        // apply the default service socket configuration

        Boolean reuseAddress = config.isReuseAddress();

        if (reuseAddress != null) {
            session.getConfig().setReuseAddress(reuseAddress);
        }

        Integer readBufferSize = config.getReadBufferSize();

        if (readBufferSize != null) {
            session.getConfig().setReadBufferSize(readBufferSize);
        } else {
            int rcvBufferSize;
            try {
                rcvBufferSize = ch.socket().getReceiveBufferSize();
                session.getConfig().setReadBufferSize(rcvBufferSize);

            } catch (SocketException e) {
                throw new MinaRuntimeException("can't configure socket receive buffer size", e);
            }
        }

        Integer sendBufferSize = config.getSendBufferSize();

        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        } else {
            int sndBufferSize;
            try {
                sndBufferSize = ch.socket().getSendBufferSize();
                session.getConfig().setSendBufferSize(sndBufferSize);
            } catch (SocketException e) {
                throw new MinaRuntimeException("can't configure socket send buffe size", e);
            }
        }

        Integer trafficClass = config.getTrafficClass();

        if (trafficClass != null) {
            session.getConfig().setTrafficClass(trafficClass);
        }

        loop.register(false, false, true, false, session, ch, null);

        ConnectFuture cf = new ConnectFuture();
        cf.complete(session);

        return cf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<IoSession> connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new IllegalStateException("not supported for UDP");
    }
}
