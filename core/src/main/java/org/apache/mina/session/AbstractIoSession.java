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

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.DefaultIoFilterController;
import org.apache.mina.filterchain.IoFilterController;
import org.apache.mina.service.SelectorProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link IoSession} shared with all the different transports.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoSession implements IoSession {
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIoSession.class);

    /** The session's unique identifier */
    private final long id;

    /** The session's creation time */
    private final long creationTime;

    /** The service this session is associated with */
    private final IoService service;

    /** The {@link SelectorProcessor} used for handling this session writing */
    protected SelectorProcessor writeProcessor;

    /** The number of bytes read since this session has been created */
    private volatile long readBytes;

    /** The number of bytes written since this session has been created */
    private volatile long writtenBytes;

    /** Last time something was read for this session */
    private volatile long lastReadTime;

    /** Last time something was written for this session */
    private volatile long lastWriteTime;

    /** attributes map */
    private final AttributeContainer attributes = new DefaultAttributeContainer();

    /** unique identifier generator */
    private static final AtomicLong NEXT_ID = new AtomicLong(0);

    protected final Object stateMonitor = new Object();

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

    /** is this session registered for being polled for write ready events */
    private AtomicBoolean registeredForWrite = new AtomicBoolean();

    /** the queue of pending writes for the session, to be dequeued by the {@link SelectorProcessor} */
    private Queue<WriteRequest> writeQueue = new DefaultWriteQueue();

    /** A lock to protect the access to the write queue */
    private final ReadWriteLock writeQueueLock = new ReentrantReadWriteLock();

    /** A Read lock on the reentrant writeQueue lock */
    private final Lock writeQueueReadLock = writeQueueLock.readLock();

    /** A Write lock on the reentrant writeQueue lock */
    private final Lock writeQueueWriteLock = writeQueueLock.writeLock();

    /** The controller for the {@link IoFilter} chain of this session */
    private IoFilterController filterProcessor;

    /**
     * Create an {@link org.apache.mina.api.IoSession} with a unique identifier (
     * {@link org.apache.mina.api.IoSession#getId()}) and an associated {@link IoService}
     * 
     * @param service the service this session is associated with
     * @param writeProcessor the processor in charge of processing this session write queue
     */
    public AbstractIoSession(IoService service, SelectorProcessor writeProcessor) {
        // generated a unique id
        id = NEXT_ID.getAndIncrement();
        creationTime = System.currentTimeMillis();
        this.service = service;
        this.writeProcessor = writeProcessor;
        this.filterProcessor = new DefaultIoFilterController(service.getFilters());

        LOG.debug("Created new session with id : {}", id);

        this.state = SessionState.CREATED;
    }

    //------------------------------------------------------------------------
    // Session State management
    //------------------------------------------------------------------------
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
    public void changeState(SessionState to) throws IllegalStateException {
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

    //------------------------------------------------------------------------
    // SSL/TLS session state management
    //------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return secured;
    }

    /**
     * {@inheritDoc}
     */
    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    /**
     * {@inheritDoc}
     */
    public void initSecure(SSLContext sslContext) throws SSLException {
        SslHelper sslHelper = new SslHelper(this, sslContext);
        sslHelper.init();

        attributes.setAttribute(SSL_HELPER, sslHelper);
        setSecured(true);
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
     * @exception IllegalArgumentException
     * <ul>
     *   <li>
     *     if <code>key==null</code>
     *   </li>
     *   <li>
     *     if <code>value</code> is not <code>null</code> and not
     *     an instance of type that is specified in by the given
     *     <code>key</code> (see {@link AttributeKey#getType()})
     *   </li>
     *  </ul>
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
     * @exception IllegalArgumentException
     *                if <code>key==null</code>
     */
    @Override
    public <T> T removeAttribute(AttributeKey<T> key) {
        return attributes.removeAttribute(key);
    }

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
        LOG.debug("writing message {} to session {}", message, this);

        if ((state == SessionState.CLOSED) || (state == SessionState.CLOSING)) {
            LOG.error("writing to closed or closing session, the message is discarded");
            return;
        }

        // process the queue
        IoFilterController chain = getFilterChain();
        chain.processMessageWriting(this, message, future);
    }

    /**
     * {@inheritDoc}
     */
    public WriteRequest enqueueWriteRequest(Object message) {
        WriteRequest request = null;

        try {
            // Lock the queue while the message is written into it
            writeQueueReadLock.lock();

            if (isConnectedSecured()) {
                // SSL/TLS : we have to encrypt the message
                SslHelper sslHelper = getAttribute(SSL_HELPER, null);

                if (sslHelper == null) {
                    throw new IllegalStateException();
                }

                request = sslHelper.processWrite(this, message, writeQueue);
            } else {
                // Plain message
                request = new DefaultWriteRequest(message);

                writeQueue.add(request);
            }
        } finally {
            writeQueueReadLock.unlock();
        }

        // If it wasn't, we register this session as interested to write.
        // It's done in atomic fashion for avoiding two concurrent registering.
        if (!registeredForWrite.getAndSet(true)) {
            writeProcessor.flush(this);
        }

        return request;
    }

    public void setNotRegisteredForWrite() {
        registeredForWrite.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Queue<WriteRequest> acquireWriteQueue() {
        writeQueueWriteLock.lock();
        return writeQueue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseWriteQueue() {
        writeQueueWriteLock.unlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFilterController getFilterChain() {
        return filterProcessor;
    }
}