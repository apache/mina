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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLException;

import org.apache.mina.api.IoClient;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.DefaultWriteFuture;
import org.apache.mina.session.DefaultWriteQueue;
import org.apache.mina.session.DefaultWriteRequest;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.util.AbstractIoFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common ancestor for NIO based {@link IoSession} implementation.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractNioSession extends AbstractIoSession {
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNioSession.class);

    // A speedup for logs
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** the NIO channel for this session */
    protected final SelectableChannel channel;

    /** is this session registered for being polled for write ready events */
    private final AtomicBoolean registeredForWrite = new AtomicBoolean();

    /** the queue of pending writes for the session, to be dequeued by the {@link SelectorLoop} */
    private final Queue<WriteRequest> writeQueue = new DefaultWriteQueue();

    public AbstractNioSession(IoService service, SelectableChannel channel, IdleChecker idleChecker) {
        super(service, idleChecker);
        this.channel = channel;
    }

    /**
     * Writes the message immediately. If we can't write all the message, we will get back the number of written bytes.
     * 
     * @param message the message to write
     * @return the number of written bytes
     */
    protected abstract int writeDirect(Object message);

    /**
     * Copy the HeapBuffer into a DirectBuffer, if needed.
     * 
     * @param writeRequest The request containing the HeapBuffer
     * @param createNew A flag to force the creation of a DirectBuffer
     * @return A DirectBuffer
     */
    protected abstract ByteBuffer convertToDirectBuffer(WriteRequest writeRequest, boolean createNew);

    // ------------------------------------------------------------------------
    // Close session management
    // ------------------------------------------------------------------------

    /** we pre-allocate a close future for lock-less {@link #close(boolean)} */
    private final IoFuture<Void> closeFuture = new AbstractIoFuture<Void>() {

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean cancelOwner(final boolean mayInterruptIfRunning) {
            // we don't cancel close
            return false;
        }
    };

    @Override
    public void processSessionOpen() {
        super.processSessionOpen();
        try {
            if (isSecured() && getService() instanceof IoClient) {
                getAttribute(SSL_HELPER).beginHandshake();
            }
        } catch (IOException e) {
            processException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<Void> close(final boolean immediately) {
        switch (state) {
        case CREATED:
            LOG.error("Session {} not opened", this);
            throw new IllegalStateException("cannot close an not opened session");
        case CONNECTED:
            state = SessionState.CLOSING;
            if (immediately) {
                channelClose();
                processSessionClosed();
            } else {
                if (isSecured()) {
                    SslHelper sslHelper = getAttribute(SSL_HELPER, null);
                    if (sslHelper != null) {
                        try {
                            sslHelper.close();
                        } catch (IOException e) {
                            processException(e);
                        }
                    }
                }
                // flush this session the flushing code will close the session
                flushWriteQueue();
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
            throw new IllegalStateException("not implemented session state : " + state);
        }

        return closeFuture;
    }

    /**
     * Close the inner socket channel
     */
    protected abstract void channelClose();

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteRequest enqueueWriteRequest(WriteRequest writeRequest) {
        if (IS_DEBUG) {
            LOG.debug("enqueueWriteRequest {}", writeRequest);
        }

        if (isSecured()) {
            // SSL/TLS : we have to encrypt the message
            SslHelper sslHelper = getAttribute(SSL_HELPER, null);

            if (sslHelper == null) {
                throw new IllegalStateException();
            }

            if (!writeRequest.isSecureInternal()) {
                writeRequest = sslHelper.processWrite(this, writeRequest.getMessage(), writeQueue);
            }
        }

        if (writeRequest != null) {
            ByteBuffer message = (ByteBuffer) writeRequest.getMessage();

            if (writeQueue.isEmpty()) {
                // Transfer the buffer in a DirectByteBuffer if it's a HeapByteBuffer and if it's too big
                message = convertToDirectBuffer(writeRequest, false);

                // We don't have anything in the writeQueue, let's try to write the
                // data in the channel immediately if we can
                int written = writeDirect(writeRequest.getMessage());

                if (IS_DEBUG) {
                    LOG.debug("wrote {} bytes to {}", written, this);
                }

                if (written > 0) {
                    incrementWrittenBytes(written);
                }

                // Update the idle status for this session
                idleChecker.sessionWritten(this, System.currentTimeMillis());
                int remaining = message.remaining();

                if ((written < 0) || (remaining > 0)) {
                    // Create a DirectBuffer unconditionally
                    convertToDirectBuffer(writeRequest, true);

                    // We have to push the request on the writeQueue
                    writeQueue.add(writeRequest);

                    // If it wasn't, we register this session as interested to write.
                    // It's done in atomic fashion for avoiding two concurrent registering.
                    if (!registeredForWrite.getAndSet(true)) {
                        flushWriteQueue();
                    }
                } else {
                    // The message has been fully written : update the stats, and signal the handler
                    // generate the message sent event
                    // complete the future if we have one (we should...)
                    final DefaultWriteFuture future = (DefaultWriteFuture) writeRequest.getFuture();

                    if (future != null) {
                        future.complete();
                    }

                    final Object highLevel = ((DefaultWriteRequest) writeRequest).getOriginalMessage();

                    if ((highLevel != null) && writeRequest.isConfirmRequested()) {
                        processMessageSent(highLevel);
                    }
                }
            } else {
                // Transfer the buffer in a DirectByteBuffer if it's a HeapByteBuffer
                message = convertToDirectBuffer(writeRequest, true);

                // We have to push the request on the writeQueue
                writeQueue.add(writeRequest);
                if (!registeredForWrite.getAndSet(true)) {
                    flushWriteQueue();
                }
            }
        }

        return writeRequest;
    }

    public abstract void flushWriteQueue();

    public void setNotRegisteredForWrite() {
        registeredForWrite.set(false);
    }

    protected boolean isRegisteredForWrite() {
        return registeredForWrite.get();
    }

    /**
     * Get the {@link Queue} of this session. The write queue contains the pending writes.
     * 
     * @return the write queue of this session
     */
    public Queue<WriteRequest> getWriteQueue() {
        return writeQueue;
    }

    /**
     * Process a write operation. This will be executed only because the session has something to write into the
     * channel.
     */
    public void processWrite(SelectorLoop selectorLoop) {
        try {
            if (IS_DEBUG) {
                LOG.debug("ready for write");
                LOG.debug("writable session : {}", this);
            }

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
                ByteBuffer buf = (ByteBuffer) writeRequest.getMessage();

                // Note that if the connection is secured, the buffer
                // already contains encrypted data.

                // Try to write the data, and get back the number of bytes
                // actually written
                int written = ((SocketChannel) channel).write(buf);

                if (IS_DEBUG) {
                    LOG.debug("wrote {} bytes to {}", written, this);
                }

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

                    if ((highLevel != null) && writeRequest.isConfirmRequested()) {
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
                        if (IS_DEBUG) {
                            LOG.debug("closing session {} have empty write queue, so we close it", this);
                        }

                        // we was flushing writes, now we to the close
                        channelClose();
                        processSessionClosed();
                    } else {
                        // no more write event needed
                        selectorLoop.modifyRegistration(false, !isReadSuspended(), false, (SelectorListener) this,
                                channel, false);

                        // Reset the flag in IoSession too
                        setNotRegisteredForWrite();
                    }
                }
                // if the queue is not empty, that means we have some more data to write : 
                // the channel OP_WRITE interest remains as it was.
            }
        } catch (final IOException e) {
            LOG.error("Exception while writing : ", e);
            processException(e);
        }
    }
}
