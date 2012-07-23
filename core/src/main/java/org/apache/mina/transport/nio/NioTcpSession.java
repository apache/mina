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

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.RuntimeIoException;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.SslHelper;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.tcp.ProxyTcpSessionConfig;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.apache.mina.util.AbstractIoFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NIO based TCP session, should be used by {@link NioTcpServer} and {@link NioTcpClient}.
 * A TCP session is a connection between a our server/client and the remote end-point.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class NioTcpSession extends AbstractNioSession implements SelectorEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(NioTcpSession.class);

    /** the NIO socket channel for this TCP session */
    private final SocketChannel channel;

    /** the socket configuration */
    private final TcpSessionConfig configuration;

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

    NioTcpSession(IoService service, SocketChannel channel, NioSelectorProcessor writeProcessor, IdleChecker idleChecker) {
        super(service, writeProcessor, idleChecker);
        this.channel = channel;
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
    public IoFuture<Void> close(boolean immediately) {
        switch (state) {
        case CREATED:
            LOG.error("Session {} not opened", this);
            throw new RuntimeIoException("cannot close an not opened session");
        case CONNECTED:
            state = SessionState.CLOSING;
            if (immediately) {
                try {
                    channel.close();
                } catch (IOException e) {
                    throw new RuntimeIoException(e);
                }
            } else {
                // flush this session the flushing code will close the session
                writeProcessor.flush(this);
            }
            break;
        case CLOSING:
            // return the same future
            LOG.warn("Already closing session {}", this);
            break;
        case CLOSED:
            LOG.warn("Already closed session {}", this);
            break;
        default:
            throw new RuntimeIoException("not implemented session state : " + state);
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
    public void acceptReady(NioSelectorProcessor processor) {
        // should never happen
        throw new IllegalStateException("accept event should never occur on NioTcpSession");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readReady(NioSelectorProcessor processor, ByteBuffer readBuffer) throws IOException {
        LOG.debug("readable session : {}", this);
        readBuffer.clear();
        int readCount = channel.read(readBuffer);

        LOG.debug("read {} bytes", readCount);

        if (readCount < 0) {
            // session closed by the remote peer
            LOG.debug("session closed by the remote peer");
            processor.addSessionToClose(this);
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

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeReady(NioSelectorProcessor processor) throws IOException {
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
                getSocketChannel().close();
            } else {
                // no more write event needed
                processor.cancelKeyForWritting(this);
            }
        }
    }
}
