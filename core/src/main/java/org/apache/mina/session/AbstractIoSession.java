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

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    private final Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>(4);

    /** unique identifier generator */
    private static final AtomicLong NEXT_ID = new AtomicLong(0);

    protected final Object stateMonitor = new Object();

    protected volatile SessionState state;

    /** is this session registered for being polled for write ready events */
    private AtomicBoolean registeredForWrite = new AtomicBoolean();

    /** the queue of pending writes for the session, to be dequeued by the {@link SelectorProcessor} */
    private Queue<WriteRequest> writeQueue = new DefaultWriteQueue();

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
        synchronized (stateMonitor) {
            this.state = SessionState.CREATED;
        }
    }

    public SessionState getState() {
        return state;
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
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T setAttribute(String name, T value) {
        return (T) attributes.put(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAttribute(Object name) {
        return attributes.containsKey(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object removeAttribute(Object name) {
        return attributes.remove(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Object> getAttributeNames() {
        return attributes.keySet();
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
        if (state == SessionState.CLOSED || state == SessionState.CLOSING) {
            LOG.error("writing to closed or closing session, the message is discarded");
            return;
        }

        // process the queue
        getFilterChain().processMessageWriting(this, message, future);
    }

    /**
     * {@inheritDoc}
     */
    public WriteRequest enqueueWriteRequest(Object message) {
        DefaultWriteRequest request = new DefaultWriteRequest(message);
        writeQueue.add(request);

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
    public Queue<WriteRequest> getWriteQueue() {
        return writeQueue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoFilterController getFilterChain() {
        return filterProcessor;
    }
}