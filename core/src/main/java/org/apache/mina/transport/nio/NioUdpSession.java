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
import java.nio.channels.SocketChannel;
import java.util.Queue;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.DefaultWriteRequest;
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
                // First, connect if we aren't already connected
                if (!((DatagramChannel) channel).isConnected()) {
                    ((DatagramChannel) channel).connect(remoteAddress);
                }

                // And try to write the data. We will either write them all,
                // or none
                return ((DatagramChannel) channel).write((ByteBuffer) message);
            } else {
                System.out.println("Cannot write");
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
        ByteBuffer message = (ByteBuffer) writeRequest.getMessage();

        if (!message.isDirect()) {
            int remaining = message.remaining();

            ByteBuffer directBuffer = ByteBuffer.allocateDirect(remaining);
            directBuffer.put(message);
            directBuffer.flip();
            writeRequest.setMessage(directBuffer);

            return directBuffer;
        }

        return message;
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

    /**
     * Process a write operation. This will be executed only because the session has something to write into the
     * channel.
     */
    public void processWrite(SelectorLoop selectorLoop) {
        try {
            LOG.debug("ready for write");
            LOG.debug("writable session : {}", this);

            Queue<WriteRequest> writeQueue = getWriteQueue();

            do {
                // get a write request from the queue. We left it in the queue,
                // just in case we can't write all of the message content into
                // the channel : we will have to retrieve the message later
                final WriteRequest writeRequest = writeQueue.peek();

                if (writeRequest == null) {
                    // Nothing to write : we are done
                    break;
                }

                // The message is necessarily a ByteBuffer at this point
                final ByteBuffer buf = (ByteBuffer) writeRequest.getMessage();

                // Note that if the connection is secured, the buffer
                // already contains encrypted data.

                // Try to write the data, and get back the number of bytes
                // actually written
                final int written = ((SocketChannel) channel).write(buf);
                LOG.debug("wrote {} bytes to {}", written, this);

                if (written > 0) {
                    incrementWrittenBytes(written);
                }

                // Update the idle status for this session
                idleChecker.sessionWritten(this, System.currentTimeMillis());

                // Ok, we may not have written everything. Check that.
                if (buf.remaining() == 0) {
                    // completed write request, let's remove it (we use poll() instead
                    // of remove(), because remove() may throw an exception if the
                    // queue is empty.
                    writeQueue.poll();

                    // complete the future if we have one (we should...)
                    final DefaultWriteFuture future = (DefaultWriteFuture) writeRequest.getFuture();

                    if (future != null) {
                        future.complete();
                    }

                    // generate the message sent event
                    final Object highLevel = ((DefaultWriteRequest) writeRequest).getOriginalMessage();

                    if (highLevel != null) {
                        processMessageSent(highLevel);
                    }
                } else {
                    // output socket buffer is full, we need
                    // to give up until next selection for
                    // writing.
                    break;
                }
            } while (!writeQueue.isEmpty());

            // We may have exited from the loop for some other reason
            // that an empty queue
            // if the session is no more interested in writing, we need
            // to stop listening for OP_WRITE events
            //
            // IMPORTANT : this section is synchronized so that the OP_WRITE flag
            // can be set safely by both the selector thread and the writer thread.
            synchronized (writeQueue) {
                if (writeQueue.isEmpty()) {
                    if (isClosing()) {
                        LOG.debug("closing session {} have empty write queue, so we close it", this);
                        // we was flushing writes, now we to the close
                        channelClose();
                    } else {
                        // no more write event needed
                        selectorLoop.modifyRegistration(false, !isReadSuspended(), false, this, channel, false);

                        // Reset the flag in IoSession too
                        setNotRegisteredForWrite();
                    }
                } else {
                    // We have some more data to write : the channel OP_WRITE interest remains
                    // as it was.
                }
            }
        } catch (final IOException e) {
            LOG.error("Exception while writing : ", e);
            processException(e);
        }
    }
}
