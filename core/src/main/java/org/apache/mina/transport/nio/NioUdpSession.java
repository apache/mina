/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.mina.transport.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.udp.UdpSessionConfig;
import org.apache.mina.util.AbstractIoFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UDP session based on NIO
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpSession extends AbstractIoSession implements SelectorListener {

    private static final Logger LOG = LoggerFactory.getLogger(NioUdpSession.class);

    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    /** the socket configuration */
    private final UdpSessionConfig configuration;

    /** we pre-allocate a close future for lock-less {@link #close(boolean)} */
    private final IoFuture<Void> closeFuture = new AbstractIoFuture<Void>() {

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean cancelOwner(boolean mayInterruptIfRunning) {
            // we don't cancel close
            return false;
        }
    };

    /**
     * @param service
     * @param writeProcessor
     * @param idleChecker
     */
    /* No qualifier*/NioUdpSession(IoService service, IdleChecker idleChecker, DatagramChannel datagramChannel,
            SocketAddress localAddress, SocketAddress remoteAddress) {
        super(service, datagramChannel, idleChecker);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.config = service.getSessionConfig();
        this.configuration = (UdpSessionConfig) this.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void channelClose() {
        // No inner socket to close for UDP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushWriteQueue() {
        // TODO flush queue
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<Void> close(boolean immediately) {
        switch (state) {
        case CREATED:
            LOG.error("Session {} not opened", this);
            throw new IllegalStateException("cannot close an not opened session");
        case CONNECTED:
        case CLOSING:
            if (immediately) {
                state = SessionState.CLOSED;
            } else {
                // we wait for the write queue to be depleted
                state = SessionState.CLOSING;
            }
            break;
        case CLOSED:
            LOG.warn("Already closed session {}", this);
            break;
        default:
            throw new IllegalStateException("not implemented session state : " + state);
        }
        return closeFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendRead() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendWrite() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resumeRead() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resumeWrite() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadSuspended() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteSuspended() {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UdpSessionConfig getConfig() {
        return configuration;
    }

    /**
     * Called when the session received a datagram.
     * 
     * @param readBuffer the received datagram
     */
    void receivedDatagram(ByteBuffer readBuffer) {
        processMessageReceived(readBuffer);
        idleChecker.sessionRead(this, System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int writeDirect(Object message) {
        try {
            // Check that we can write into the channel
            if (!isRegisteredForWrite()) {
                // We don't have pending writes
                return ((DatagramChannel) channel).write((ByteBuffer) message);
            } else {
                return -1;
            }
        } catch (final IOException e) {
            LOG.error("Exception while reading : ", e);
            processException(e);

            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ByteBuffer convertToDirectBuffer(WriteRequest writeRequest, boolean createNew) {
        return (ByteBuffer) writeRequest.getMessage();
    }

    void setSelectionKey(SelectionKey key) {
        //this.selectionKey = key;
    }

    /**
     * Set this session status as connected. To be called by the processor selecting/polling this session.
     */
    void setConnected() {
        if (!isCreated()) {
            throw new RuntimeException("Trying to open a non created session");
        }

        state = SessionState.CONNECTED;

        /*if (connectFuture != null) {
            connectFuture.complete(this);
            connectFuture = null; // free some memory
        }*/

        processSessionOpen();
    }

    @Override
    public void ready(boolean accept, boolean connect, boolean read, ByteBuffer readBuffer, boolean write) {
    }
}
