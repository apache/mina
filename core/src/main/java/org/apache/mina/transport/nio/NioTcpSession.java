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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.DefaultWriteRequest;
import org.apache.mina.session.SslHelper;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.tcp.ProxyTcpSessionConfig;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.apache.mina.util.AbstractIoFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NIO based TCP session, should be used by {@link NioTcpServer} and {@link NioTcpClient}. A TCP session is a
 * connection between a our server/client and the remote end-point.
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

    /** the future representing this session connection operation (client only) */
    private ConnectFuture connectFuture;

    private SelectionKey selectionKey;

    NioTcpSession(final IoService service, final SocketChannel channel, final SelectorLoop selectorLoop,
            final IdleChecker idleChecker) {
        super(service, idleChecker);
        this.channel = channel;
        this.selectorLoop = selectorLoop;
        this.configuration = new ProxyTcpSessionConfig(channel.socket());
    }

    void setConnectFuture(ConnectFuture connectFuture) {
        this.connectFuture = connectFuture;
    }

    /**
     * Get the underlying {@link SocketChannel} of this session
     * 
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
        final Socket socket = channel.socket();

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

        final Socket socket = channel.socket();

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

        if (connectFuture != null) {
            connectFuture.complete(this);
            connectFuture = null; // free some memory
        }

        processSessionOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void channelClose() {
        try {
            selectorLoop.unregister(this, channel);
            channel.close();
        } catch (final IOException e) {
            LOG.error("Exception while closing the channel : ", e);
            processException(e);
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
     * Process a read operation : read the data from the channel and push them to the chain.
     * 
     * @param readBuffer The buffer that will contain the read data
     */
    private void processRead(final ByteBuffer readBuffer) {
        try {
            LOG.debug("readable session : {}", this);

            // First reset the buffer from what it contained before
            readBuffer.clear();

            // Read everything we can up to the buffer size
            final int readCount = channel.read(readBuffer);

            LOG.debug("read {} bytes", readCount);

            if (readCount < 0) {
                // session closed by the remote peer
                LOG.debug("session closed by the remote peer");
                close(true);
            } else if (readCount > 0) {
                // we have read some data
                // limit at the current position & rewind buffer back to start &
                // push to the chain
                readBuffer.flip();

                if (isSecured()) {
                    // We are reading data over a SSL/TLS encrypted connection.
                    // Redirect the processing to the SslHelper class.
                    final SslHelper sslHelper = getAttribute(SSL_HELPER, null);

                    if (sslHelper == null) {
                        throw new IllegalStateException();
                    }

                    sslHelper.processRead(this, readBuffer);
                } else {
                    // Plain message, not encrypted : go directly to the chain
                    processMessageReceived(readBuffer);
                }

                // Update the session idle status
                idleChecker.sessionRead(this, System.currentTimeMillis());
            }
        } catch (final IOException e) {
            LOG.error("Exception while reading : ", e);
            processException(e);
        }
    }

    /**
     * Process a write operation. This will be executed only because the session has something to write into the
     * channel.
     */
    private void processWrite() {
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
                final int written = channel.write(buf);
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
                    final Object highLevel = ((DefaultWriteRequest) writeRequest).getHighLevelMessage();

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
                        selectorLoop.modifyRegistration(false, !isReadSuspended(), false, this, channel);
                    }
                } else {
                    // We have some more data to write : the channel OP_WRITE interest remains
                    // as it was.
                }
            }
        } catch (final IOException e) {
            LOG.error("Exception while reading : ", e);
            processException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ready(final boolean accept, boolean connect, final boolean read, final ByteBuffer readBuffer,
            final boolean write) {
        LOG.debug("session {} ready for accept={}, connect={}, read={}, write={}", new Object[] { this, accept,
                connect, read, write });
        if (connect) {
            try {

                boolean isConnected = channel.finishConnect();

                if (!isConnected) {
                    LOG.error("unable to connect session {}", this);
                } else {
                    // cancel current registration for connection
                    selectionKey.cancel();
                    selectionKey = null;

                    // Register for reading
                    selectorLoop.register(false, false, true, false, this, channel, new RegistrationCallback() {

                        @Override
                        public void done(SelectionKey selectionKey) {
                            setConnected();
                        }
                    });
                }
            } catch (IOException e) {
                LOG.debug("Connection error, we cancel the future", e);
                if (connectFuture != null) {
                    connectFuture.error(e);
                }
            }
        }

        if (read) {
            processRead(readBuffer);
        }

        if (write) {
            processWrite();
        }
        if (accept) {
            throw new IllegalStateException("accept event should never occur on NioTcpSession");
        }
    }

    void setSelectionKey(SelectionKey key) {
        this.selectionKey = key;
    }

    static class ConnectFuture extends AbstractIoFuture<IoSession> {

        @Override
        protected boolean cancelOwner(boolean mayInterruptIfRunning) {
            return false;
        }

        /**
         * session connected
         */
        public void complete(IoSession session) {
            setResult(session);
        }

        /**
         * connection error
         */
        public void error(Exception e) {
            setException(e);
        }
    }
}
