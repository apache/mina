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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import org.apache.mina.api.IoService;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.SslHelper;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.tcp.ProxyTcpSessionConfig;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NIO based TCP session, should be used by {@link NioTcpServer} and {@link NioTcpClient}.
 * A TCP session is a connection between a our server/client and the remote end-point.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class NioTcpSession extends AbstractIoSession implements SelectorListener {

    private static final Logger LOG = LoggerFactory.getLogger(NioTcpSession.class);

    /** the NIO socket channel for this TCP session */
    private final SocketChannel channel;

    /** the selector loop in charge of generating read/write events for this session */
    private final SelectorLoop selectorLoop;

    /** the socket configuration */
    private final TcpSessionConfig configuration;

    NioTcpSession(IoService service, SocketChannel channel, SelectorLoop selectorLoop, IdleChecker idleChecker) {
        super(service, idleChecker);
        this.channel = channel;
        this.selectorLoop = selectorLoop;
        this.configuration = new ProxyTcpSessionConfig(channel.socket());
    }

    /**
     * Get the underlying {@link SocketChannel} of this session
     * @return the socket channel used by this session
     */
    SocketChannel getSocketChannel() {
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }
        Socket socket = channel.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }

        Socket socket = channel.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getLocalSocketAddress();
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
        return false;
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
    public TcpSessionConfig getConfig() {
        return configuration;
    }

    /**
     * Set this session status as connected. To be called by the processor selecting/polling this session.
     */
    void setConnected() {
        if (!isCreated()) {
            throw new RuntimeException("Trying to open a non created session");
        }

        state = SessionState.CONNECTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void channelClose() {
        try {
            selectorLoop.unregister(this, channel);
            selectorLoop.decrementServiceCount();
            channel.close();
        } catch (IOException e) {
            LOG.error("Exception while closing the channel : ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushWriteQueue() {
        // register for write
        selectorLoop.modifyRegistration(false, !isReadSuspended(), true, this, channel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ready(boolean accept, boolean read, ByteBuffer readBuffer, boolean write) {
        if (read) {
            try {

                LOG.debug("readable session : {}", this);
                readBuffer.clear();
                int readCount = channel.read(readBuffer);

                LOG.debug("read {} bytes", readCount);

                if (readCount < 0) {
                    // session closed by the remote peer
                    LOG.debug("session closed by the remote peer");
                    close(true);
                } else {
                    // we have read some data
                    // limit at the current position & rewind buffer back to start &
                    // push to the chain
                    readBuffer.flip();

                    if (isSecured()) {
                        // We are reading data over a SSL/TLS encrypted connection.
                        // Redirect
                        // the processing to the SslHelper class.
                        SslHelper sslHelper = getAttribute(SSL_HELPER, null);

                        if (sslHelper == null) {
                            throw new IllegalStateException();
                        }

                        sslHelper.processRead(this, readBuffer);
                    } else {
                        // Plain message, not encrypted : go directly to the chain
                        processMessageReceived(readBuffer);
                    }

                    idleChecker.sessionRead(this, System.currentTimeMillis());
                }
            } catch (IOException e) {
                LOG.error("Exception while reading : ", e);
            }

        }
        if (write) {
            try {
                LOG.debug("ready for write");
                LOG.debug("writable session : {}", this);

                setNotRegisteredForWrite();

                // write from the session write queue
                boolean isEmpty = false;

                try {
                    Queue<WriteRequest> queue = acquireWriteQueue();

                    do {
                        // get a write request from the queue
                        WriteRequest wreq = queue.peek();

                        if (wreq == null) {
                            break;
                        }

                        ByteBuffer buf = (ByteBuffer) wreq.getMessage();

                        // Note that if the connection is secured, the buffer
                        // already
                        // contains encrypted data.
                        int wrote = getSocketChannel().write(buf);
                        incrementWrittenBytes(wrote);
                        LOG.debug("wrote {} bytes to {}", wrote, this);

                        idleChecker.sessionWritten(this, System.currentTimeMillis());

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
                    this.releaseWriteQueue();
                }

                // if the session is no more interested in writing, we need
                // to stop listening for OP_WRITE events
                if (isEmpty) {
                    if (isClosing()) {
                        LOG.debug("closing session {} have empty write queue, so we close it", this);
                        // we was flushing writes, now we to the close
                        channelClose();
                    } else {
                        // no more write event needed
                        selectorLoop.modifyRegistration(false, !isReadSuspended(), false, this, channel);
                    }
                }
            } catch (IOException e) {
                LOG.error("Exception while reading : ", e);
            }
        }
        if (accept) {
            throw new IllegalStateException("accept event should never occur on NioTcpSession");
        }
    }
}
