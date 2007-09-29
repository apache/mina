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
package org.apache.mina.filter.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterEvent;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * A base abstract class for generic filters that forward I/O events to
 * {@link Executor} to enforce a certain thread model.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractExecutorFilter extends IoFilterAdapter {
    private final EnumSet<IoEventType> eventTypes;

    private final Executor executor;

    /**
     * Creates a new instance with the default thread pool implementation
     * (<tt>new ThreadPoolExecutor(1, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue() )</tt>).
     */
    protected AbstractExecutorFilter(IoEventType... eventTypes) {
        this(new ThreadPoolExecutor(1, 16, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()), eventTypes);
    }

    /**
     * Creates a new instance with the specified <tt>executor</tt>.
     */
    protected AbstractExecutorFilter(Executor executor,
            IoEventType... eventTypes) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        if (eventTypes == null || eventTypes.length == 0) {
            eventTypes = new IoEventType[] { IoEventType.EXCEPTION_CAUGHT,
                    IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT,
                    IoEventType.SESSION_CLOSED, IoEventType.SESSION_IDLE,
                    IoEventType.SESSION_OPENED, };
        }

        for (IoEventType t : eventTypes) {
            if (t == IoEventType.SESSION_CREATED) {
                throw new IllegalArgumentException(IoEventType.SESSION_CREATED
                        + " is not allowed.");
            }
        }

        this.executor = executor;

        Collection<IoEventType> eventTypeCollection = new ArrayList<IoEventType>(
                eventTypes.length);
        Collections.addAll(eventTypeCollection, eventTypes);
        this.eventTypes = EnumSet.copyOf(eventTypeCollection);
    }

    /**
     * Returns the underlying {@link Executor} instance this filter uses.
     */
    public final Executor getExecutor() {
        return executor;
    }

    protected abstract void fireEvent(IoFilterEvent event);

    @Override
    public final void sessionCreated(NextFilter nextFilter, IoSession session) {
        nextFilter.sessionCreated(session);
    }

    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) {
        if (eventTypes.contains(IoEventType.SESSION_OPENED)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.SESSION_OPENED,
                    session, null));
        } else {
            nextFilter.sessionOpened(session);
        }
    }

    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session) {
        if (eventTypes.contains(IoEventType.SESSION_CLOSED)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.SESSION_CLOSED,
                    session, null));
        } else {
            nextFilter.sessionClosed(session);
        }
    }

    @Override
    public final void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) {
        if (eventTypes.contains(IoEventType.SESSION_IDLE)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.SESSION_IDLE,
                    session, status));
        } else {
            nextFilter.sessionIdle(session, status);
        }
    }

    @Override
    public final void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) {
        if (eventTypes.contains(IoEventType.EXCEPTION_CAUGHT)) {
            fireEvent(new IoFilterEvent(nextFilter,
                    IoEventType.EXCEPTION_CAUGHT, session, cause));
        } else {
            nextFilter.exceptionCaught(session, cause);
        }
    }

    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) {
        if (eventTypes.contains(IoEventType.MESSAGE_RECEIVED)) {
            fireEvent(new IoFilterEvent(nextFilter,
                    IoEventType.MESSAGE_RECEIVED, session, message));
        } else {
            nextFilter.messageReceived(session, message);
        }
    }

    @Override
    public final void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (eventTypes.contains(IoEventType.MESSAGE_SENT)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.MESSAGE_SENT,
                    session, writeRequest));
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    @Override
    public final void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (eventTypes.contains(IoEventType.WRITE)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.WRITE, session,
                    writeRequest));
        } else {
            nextFilter.filterWrite(session, writeRequest);
        }
    }

    @Override
    public final void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (eventTypes.contains(IoEventType.CLOSE)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.CLOSE, session,
                    null));
        } else {
            nextFilter.filterClose(session);
        }
    }
}
