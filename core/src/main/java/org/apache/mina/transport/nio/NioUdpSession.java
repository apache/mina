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

import org.apache.mina.api.IoService;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.udp.UdpSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UDP session based on NIO
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpSession extends AbstractNioSession implements SelectorListener {

    private static final Logger LOG = LoggerFactory.getLogger(NioUdpSession.class);

    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    /**
     * The selector loop in charge of generating read/write events for this session Used only for UDP client session.
     */
    private SelectorLoop selectorLoop = null;

    /** the socket configuration */
    private final UdpSessionConfig configuration;

    /**
     * For server handled UDP sessions
     */
    /* No qualifier */NioUdpSession(IoService service, IdleChecker idleChecker, DatagramChannel datagramChannel,
            SocketAddress localAddress, SocketAddress remoteAddress) {
        super(service, datagramChannel, idleChecker);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.config = service.getSessionConfig();
        this.configuration = (UdpSessionConfig) this.config;
    }

    /**
     * For client handled UDP sessions
     */
    /* No qualifier */NioUdpSession(IoService service, IdleChecker idleChecker, DatagramChannel datagramChannel,
            SocketAddress localAddress, SocketAddress remoteAddress, NioSelectorLoop selectorLoop) {
        super(service, datagramChannel, idleChecker);
        this.selectorLoop = selectorLoop;
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
        LOG.debug("channelClose");
        // No inner socket to close for UDP server, but some for UDP client
        if (channel != null) {
            try {
                selectorLoop.unregister(this, channel);
                channel.close();
            } catch (final IOException e) {
                LOG.error("Exception while closing the channel : ", e);
                processException(e);
            }
        }
        processSessionClosed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushWriteQueue() {
        // register for write
        if (selectorLoop != null) {
            selectorLoop.modifyRegistration(false, !isReadSuspended(), true, this, channel, true);
        }
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
    /*
     * @Override public IoFuture<Void> close(boolean immediately) { switch (state) { case CREATED:
     * LOG.error("Session {} not opened", this); throw new IllegalStateException("cannot close an not opened session");
     * case CONNECTED: case CLOSING: if (immediately) { state = SessionState.CLOSED; } else { // we wait for the write
     * queue to be depleted state = SessionState.CLOSING; } break; case CLOSED: LOG.warn("Already closed session {}",
     * this); break; default: throw new IllegalStateException("not implemented session state : " + state); } return
     * closeFuture; }
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendRead() {
        // TODO
        throw new IllegalStateException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendWrite() {
        // TODO
        throw new IllegalStateException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resumeRead() {
        // TODO
        throw new IllegalStateException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resumeWrite() {
        // TODO
        throw new IllegalStateException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadSuspended() {
        // TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteSuspended() {
        // TODO
        return false;
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
                // First, connect if we aren't already connected
                if (!((DatagramChannel) channel).isConnected()) {
                    ((DatagramChannel) channel).connect(remoteAddress);
                }

                // And try to write the data. We will either write them all,
                // or none
                return ((DatagramChannel) channel).write((ByteBuffer) message);
            } else {
                LOG.debug("Cannot write");
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
        // Here, we don't create a new DirectBuffer. We let the underlying layer do the job for us
        return (ByteBuffer) writeRequest.getMessage();
    }

    /**
     * Set this session status as connected. To be called by the processor selecting/polling this session.
     */
    void setConnected() {
        if (!isCreated()) {
            throw new IllegalStateException("Trying to open a non created session");
        }

        state = SessionState.CONNECTED;
        processSessionOpen();
    }

    @Override
    public void ready(boolean accept, boolean connect, boolean read, ByteBuffer readBuffer, boolean write) {
        if (IS_DEBUG) {
            LOG.debug("session {} ready for accept={}, connect={}, read={}, write={}", new Object[] { this, accept,
                                    connect, read, write });
        }

        if (read) {
            if (IS_DEBUG) {
                LOG.debug("readable datagram for UDP service : {}", this);
            }

            // Read everything we can up to the buffer size
            try {
                readBuffer.clear();
                ((DatagramChannel) channel).receive(readBuffer);
                readBuffer.flip();

                int readbytes = readBuffer.remaining();

                if (IS_DEBUG) {
                    LOG.debug("read {} bytes", readbytes);
                }

                if (readbytes <= 0) {
                    // session closed by the remote peer
                    if (IS_DEBUG) {
                        LOG.debug("session closed by the remote peer");
                    }

                    close(true);
                } else {
                    receivedDatagram(readBuffer);
                }
            } catch (IOException e) {
                processException(e);
            }

        }

        if (write) {
            processWrite(selectorLoop);
        }
        if (accept) {
            throw new IllegalStateException("accept event should never occur on NioUdpSession");
        }

    }

}
