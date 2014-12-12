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
package org.apache.mina.session;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.SSLContext;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoServer;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.service.executor.CloseEvent;
import org.apache.mina.service.executor.HandshakeCompletedEvent;
import org.apache.mina.service.executor.HandshakeStartedEvent;
import org.apache.mina.service.executor.IdleEvent;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.executor.OpenEvent;
import org.apache.mina.service.executor.ReceiveEvent;
import org.apache.mina.service.executor.SecureClosedEvent;
import org.apache.mina.service.executor.SentEvent;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.transport.nio.SelectorLoop;
import org.apache.mina.transport.nio.SslHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link IoSession} shared with all the different transports.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoSession implements IoSession, ReadFilterChainController, WriteFilterChainController {
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIoSession.class);

    // A speedup for logs
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** unique identifier generator */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    /** The session's unique identifier */
    private final long id;

    /** The session's creation time */
    private final long creationTime;

    /** The service this session is associated with */
    private final IoService service;

    /** attributes map */
    private final AttributeContainer attributes = new DefaultAttributeContainer();

    /** the {@link IdleChecker} in charge of detecting idle event for this session */
    protected final IdleChecker idleChecker;

    /** The session config */
    protected IoSessionConfig config;

    // ------------------------------------------------------------------------
    // Basic statistics
    // ------------------------------------------------------------------------

    /** The number of bytes read since this session has been created */
    private volatile long readBytes;

    /** The number of bytes written since this session has been created */
    private volatile long writtenBytes;

    /** Last time something was read for this session */
    private volatile long lastReadTime;

    /** Last time something was written for this session */
    private volatile long lastWriteTime;

    // ------------------------------------------------------------------------
    // Session state
    // ------------------------------------------------------------------------

    /** The session's state : one of CREATED, CONNECTED, CLOSING, CLOSED, SECURING, CONNECTED_SECURED */
    protected volatile SessionState state;

    /** A lock to protect the access to the session's state */
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

    /** A Read lock on the reentrant session's state lock */
    private final Lock stateReadLock = stateLock.readLock();

    /** A Write lock on the reentrant session's state lock */
    private final Lock stateWriteLock = stateLock.writeLock();

    /** Tells if the session is secured or not */
    protected volatile boolean secured;

    // ------------------------------------------------------------------------
    // Filter chain
    // ------------------------------------------------------------------------

    /** The list of {@link IoFilter} implementing this chain. */
    private final IoFilter[] chain;

    /** the current position in the write chain for this thread */
    private int writeChainPosition;

    /** the current position in the read chain for this thread */
    private int readChainPosition;

    /**
     * Create an {@link org.apache.mina.api.IoSession} with a unique identifier (
     * {@link org.apache.mina.api.IoSession#getId()}) and an associated {@link IoService}
     * 
     * @param service the service this session is associated with
     * @param idleChecker the checker for idle session
     */
    public AbstractIoSession(IoService service, IdleChecker idleChecker) {
        // generated a unique id
        id = NEXT_ID.getAndIncrement();
        creationTime = System.currentTimeMillis();
        this.service = service;
        this.chain = service.getFilters();
        this.idleChecker = idleChecker;
        this.config = service.getSessionConfig();

        if (IS_DEBUG) {
            LOG.debug("Created new session with id : {}", id);
        }

        this.state = SessionState.CREATED;
        service.getManagedSessions().put(id, this);
    }

    // ------------------------------------------------------------------------
    // Session State management
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        try {
            stateReadLock.lock();

            return state == SessionState.CLOSED;
        } finally {
            stateReadLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosing() {
        try {
            stateReadLock.lock();

            return state == SessionState.CLOSING;
        } finally {
            stateReadLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        try {
            stateReadLock.lock();

            return state == SessionState.CONNECTED;
        } finally {
            stateReadLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCreated() {
        try {
            stateReadLock.lock();

            return state == SessionState.CREATED;
        } finally {
            stateReadLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecuring() {
        try {
            stateReadLock.lock();

            return state == SessionState.SECURING;
        } finally {
            stateReadLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnectedSecured() {
        try {
            stateReadLock.lock();

            return state == SessionState.SECURED;
        } finally {
            stateReadLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeState(SessionState to) {
        try {
            stateWriteLock.lock();

            switch (state) {
            case CREATED:
                switch (to) {
                case CONNECTED:
                case SECURING:
                case CLOSING:
                    state = to;
                    break;

                default:
                    throw new IllegalStateException("Cannot transit from " + state + " to " + to);
                }

                break;

            case CONNECTED:
                switch (to) {
                case SECURING:
                case CLOSING:
                    state = to;
                    break;

                default:
                    throw new IllegalStateException("Cannot transit from " + state + " to " + to);
                }

                break;

            case SECURING:
                switch (to) {
                case SECURED:
                case CLOSING:
                    state = to;
                    break;

                default:
                    throw new IllegalStateException("Cannot transit from " + state + " to " + to);
                }

                break;

            case SECURED:
                switch (to) {
                case CONNECTED:
                case SECURING:
                case CLOSING:
                    state = to;
                    break;

                default:
                    throw new IllegalStateException("Cannot transit from " + state + " to " + to);
                }

                break;
            case CLOSING:
                if (to != SessionState.CLOSED) {
                    throw new IllegalStateException("Cannot transit from " + state + " to " + to);
                }

                state = to;

                break;

            case CLOSED:
                throw new IllegalStateException("The session is already closed. cannot switch to " + to);
            }
        } finally {
            stateWriteLock.unlock();
        }
    }

    // ------------------------------------------------------------------------
    // SSL/TLS session state management
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        SslHelper helper = attributes.getAttribute(SSL_HELPER);
        return helper != null && helper.isActive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initSecure(SSLContext sslContext) {
        SslHelper sslHelper = new SslHelper(this, sslContext);
        sslHelper.init();
        attributes.setAttribute(SSL_HELPER, sslHelper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getReadBytes() {
        return readBytes;
    }

    /**
     * To be called by the internal plumber when some bytes are written on the socket
     * 
     * @param bytesCount number of extra bytes written
     */
    public void incrementWrittenBytes(int bytesCount) {
        writtenBytes += bytesCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getWrittenBytes() {
        return writtenBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getLastIoTime() {
        return Math.max(lastReadTime, lastWriteTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoService getService() {
        return service;
    }

    /**
     * {@inheritDoc}
     * 
     * @exception IllegalArgumentException if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    @Override
    public final <T> T getAttribute(AttributeKey<T> key, T defaultValue) {
        return attributes.getAttribute(key, defaultValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @exception IllegalArgumentException if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    @Override
    public final <T> T getAttribute(AttributeKey<T> key) {
        return attributes.getAttribute(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @exception IllegalArgumentException <ul>
     *            <li>
     *            if <code>key==null</code></li>
     *            <li>
     *            if <code>value</code> is not <code>null</code> and not an instance of type that is specified in by the
     *            given <code>key</code> (see {@link AttributeKey#getType()})</li>
     *            </ul>
     * 
     * @see #getAttribute(AttributeKey)
     */
    @Override
    public final <T> T setAttribute(AttributeKey<? extends T> key, T value) {
        return attributes.setAttribute(key, value);
    };

    /**
     * {@inheritDoc}
     * 
     * @see Collections#unmodifiableSet(Set)
     */
    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributes.getAttributeKeys();
    }

    /**
     * {@inheritDoc}
     * 
     * @exception IllegalArgumentException if <code>key==null</code>
     */
    @Override
    public <T> T removeAttribute(AttributeKey<T> key) {
        return attributes.removeAttribute(key);
    }

    // ----------------------------------------------------
    // Write management
    // ----------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Object message) {
        doWriteWithFuture(message, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFuture<Void> writeWithFuture(Object message) {
        IoFuture<Void> future = new DefaultWriteFuture();
        doWriteWithFuture(message, future);

        return future;
    }

    private void doWriteWithFuture(Object message, IoFuture<Void> future) {
        if (IS_DEBUG) {
            LOG.debug("writing message {} to session {}", message, this);
        }

        if ((state == SessionState.CLOSED) || (state == SessionState.CLOSING)) {
            LOG.error("writing to closed or closing session, the message is discarded");
            return;
        }

        WriteRequest writeRequest = new DefaultWriteRequest(message);

        // process the queue
        processMessageWriting(writeRequest, future);
    }

    // ------------------------------------------------------------------------
    // Event processing using the filter chain
    // ------------------------------------------------------------------------

    /** send a caught exception to the {@link IoHandler} (if any) */
    protected void processException(Exception t) {
        if (IS_DEBUG) {
            LOG.debug("caught session exception ", t);
        }

        IoHandler handler = getService().getIoHandler();

        if (handler != null) {
            handler.exceptionCaught(this, t);
        }
    }

    /**
     * process session open event using the filter chain. To be called by the session {@link SelectorLoop} .
     */
    public void processSessionOpen() {
        if (IS_DEBUG) {
            LOG.debug("processing session open event");
        }

        try {

            for (IoFilter filter : chain) {
                filter.sessionOpened(this);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    executor.execute(new OpenEvent(this));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.sessionOpened(this);
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    /**
     * process session closed event using the filter chain. To be called by the session {@link SelectorLoop} .
     */
    public void processSessionClosed() {
        if (IS_DEBUG) {
            LOG.debug("processing session closed event");
        }

        try {
            for (IoFilter filter : chain) {
                filter.sessionClosed(this);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();
                if (executor != null) {
                    // asynchronous event
                    executor.execute(new CloseEvent(this));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.sessionClosed(this);
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Exception while closing the session : ", e);
        }
        service.getManagedSessions().remove(id);
    }

    /**
     * process session idle event using the filter chain. To be called by the session {@link SelectorLoop} .
     */
    public void processSessionIdle(IdleStatus status) {
        if (IS_DEBUG) {
            LOG.debug("processing session idle {} event for session {}", status, this);
        }

        try {
            for (IoFilter filter : chain) {
                filter.sessionIdle(this, status);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    executor.execute(new IdleEvent(this, status));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.sessionIdle(this, status);
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    /** for knowing if the message buffer is the selector loop one */
    static ThreadLocal<ByteBuffer> tl = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return null;
        }
    };

    /**
     * process session message received event using the filter chain. To be called by the session {@link SelectorLoop} .
     * 
     * @param message the received message
     */
    public void processMessageReceived(ByteBuffer message) {
        if (IS_DEBUG) {
            LOG.debug("processing message '{}' received event for session {}", message, this);
        }

        tl.set(message);
        try {
            // save basic statistics
            readBytes += message.remaining();
            lastReadTime = System.currentTimeMillis();

            if (chain.length < 1) {
                if (IS_DEBUG) {
                    LOG.debug("Nothing to do, the chain is empty");
                }

                IoHandler handler = getService().getIoHandler();

                if (handler != null) {
                    IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                    if (executor != null) {
                        // asynchronous event
                        // copy the bytebuffer
                        if (IS_DEBUG) {
                            LOG.debug("copying bytebuffer before pushing to the executor");
                        }

                        ByteBuffer original = message;
                        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
                        // copy from the beginning
                        original.rewind();
                        clone.put(original);
                        original.rewind();
                        clone.flip();
                        executor.execute(new ReceiveEvent(this, clone));
                    } else {
                        // synchronous call (in the I/O loop)
                        handler.messageReceived(this, message);
                    }
                }

            } else {
                readChainPosition = 0;
                // we call the first filter, it's supposed to call the next ones using the filter chain controller
                chain[readChainPosition].messageReceived(this, message, this);
            }
        } catch (RuntimeException e) {
            processException(e);
        }

    }

    /**
     * process session message writing event using the filter chain. To be called by the session {@link SelectorLoop} .
     * 
     * @param message the wrote message, should be transformed into ByteBuffer at the end of the filter chain
     */
    public void processMessageWriting(WriteRequest writeRequest, IoFuture<Void> future) {
        if (IS_DEBUG) {
            LOG.debug("processing message '{}' writing event for session {}", writeRequest, this);
        }

        try {
            // lastWriteRequest = null;

            if (chain.length < 1) {
                enqueueWriteRequest(writeRequest);
            } else {
                writeChainPosition = chain.length - 1;
                // we call the first filter, it's supposed to call the next ones using the filter chain controller
                int position = writeChainPosition;
                IoFilter nextFilter = chain[position];
                nextFilter.messageWriting(this, writeRequest, this);
            }

            // put the future in the last write request
            if (future != null) {
                writeRequest.setFuture(future);
            }
        } catch (RuntimeException e) {
            processException(e);
        }

    }

    public void processMessageSent(Object highLevelMessage) {
        if (IS_DEBUG) {
            LOG.debug("processing message '{}' sent event for session {}", highLevelMessage, this);
        }

        try {
            int size = chain.length;

            for (int i = size - 1; i >= 0; i--) {
                chain[i].messageSent(this, highLevelMessage);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    executor.execute(new SentEvent(this, highLevelMessage));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.messageSent(this, highLevelMessage);
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }

    }

    public void processHandshakeStarted() {
        if (IS_DEBUG) {
            LOG.debug("processing handshake started event for session {}", this);
        }

        try {
            int size = chain.length;

            for (int i = size - 1; i >= 0; i--) {
                chain[i].handshakeStarted(this);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    executor.execute(new HandshakeStartedEvent(this));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.handshakeStarted(this);
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }

    }

    public void processHandshakeCompleted() {
        if (IS_DEBUG) {
            LOG.debug("processing handshake completed event for session {}", this);
        }

        try {
            int size = chain.length;

            for (int i = size - 1; i >= 0; i--) {
                chain[i].handshakeCompleted(this);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    executor.execute(new HandshakeCompletedEvent(this));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.handshakeCompleted(this);
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }

    }

    public void processSecureClosed() {
        if (IS_DEBUG) {
            LOG.debug("processing secure closed event for session {}", this);
        }

        try {
            int size = chain.length;

            for (int i = size - 1; i >= 0; i--) {
                chain[i].secureClosed(this);
            }

            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    executor.execute(new SecureClosedEvent(this));
                } else {
                    // synchronous call (in the I/O loop)
                    handler.secureClosed(this);
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    /**
     * process session message received event using the filter chain. To be called by the session {@link SelectorLoop} .
     * 
     * @param message the received message
     */
    @Override
    public void callWriteNextFilter(WriteRequest message) {
        if (IS_DEBUG) {
            LOG.debug("calling next filter for writing for message '{}' position : {}", message, writeChainPosition);
        }

        writeChainPosition--;

        if (writeChainPosition < 0 || chain.length == 0) {
            // end of chain processing
            enqueueWriteRequest(message);
        } else {
            chain[writeChainPosition].messageWriting(this, message, this);
        }

        writeChainPosition++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void callWriteMessageForRead(Object message) {
        if (IS_DEBUG) {
            LOG.debug("calling filter for reading for message '{}' position : {} from a write event", message,
                    readChainPosition);
        }

        writeChainPosition = readChainPosition;
        callWriteNextFilter(new DefaultWriteRequest(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void callReadNextFilter(Object message) {
        readChainPosition++;

        if (readChainPosition >= chain.length) {
            // end of chain processing
            IoHandler handler = getService().getIoHandler();

            if (handler != null) {
                IoHandlerExecutor executor = getService().getIoHandlerExecutor();

                if (executor != null) {
                    // asynchronous event
                    if (message == tl.get()) {
                        // copy the bytebuffer
                        if (IS_DEBUG) {
                            LOG.debug("copying bytebuffer before pushing to the executor");
                        }

                        ByteBuffer original = (ByteBuffer) message;
                        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
                        // copy from the beginning
                        original.rewind();
                        clone.put(original);
                        original.rewind();
                        clone.flip();
                        executor.execute(new ReceiveEvent(this, clone));
                    } else {
                        executor.execute(new ReceiveEvent(this, message));
                    }
                } else {
                    // synchronous call (in the I/O loop)
                    handler.messageReceived(this, message);
                }
            }
        } else {
            chain[readChainPosition].messageReceived(this, message, this);
        }

        readChainPosition--;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (isConnected() || isClosing()) {
            String remote = null;
            String local = null;

            try {
                remote = String.valueOf(getRemoteAddress());
            } catch (Exception e) {
                remote = "Cannot get the remote address informations: " + e.getMessage();
            }

            try {
                local = String.valueOf(getLocalAddress());
            } catch (Exception e) {
                local = "Cannot get the local address informations: " + e.getMessage();
            }

            if (getService() instanceof IoServer) {
                return "(" + getIdAsString() + ": " + getServiceName() + ", server, " + remote + " => " + local + ')';
            }

            return "(" + getIdAsString() + ": " + getServiceName() + ", client, " + local + " => " + remote + ')';
        }

        return "(" + getIdAsString() + ") Session disconnected ...";
    }

    private String getServiceName() {
        return getService().getClass().getCanonicalName();
    }

    /** create string for session id padded with 0 */
    private String getIdAsString() {
        String id = Long.toHexString(getId()).toUpperCase();

        // Somewhat inefficient, but it won't happen that often
        // because an ID is often a big integer.
        while (id.length() < 8) {
            id = '0' + id; // padding
        }
        id = "0x" + id;

        return id;
    }
}