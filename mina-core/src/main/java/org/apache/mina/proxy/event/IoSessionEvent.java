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
package org.apache.mina.proxy.event;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IoSessionEvent.java - Wrapper Class for enqueued events.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class IoSessionEvent {
    private final static Logger logger = LoggerFactory
            .getLogger(IoSessionEvent.class);

    /**
     * The next filter in the chain.
     */
    private final NextFilter nextFilter;

    /**
     * The session.
     */
    private final IoSession session;

    /**
     * The event type.
     */
    private final IoSessionEventType type;

    /**
     * The idle status if type value is {@link IoSessionEventType#IDLE},
     * null otherwise.
     */
    private IdleStatus status;

    /**
     * Creates an instance of this class when event type differs from 
     * {@link IoSessionEventType#IDLE}.
     * 
     * @param nextFilter the next filter
     * @param session the session
     * @param type the event type
     */
    public IoSessionEvent(final NextFilter nextFilter, final IoSession session,
            final IoSessionEventType type) {
        this.nextFilter = nextFilter;
        this.session = session;
        this.type = type;
    }

    /**
     * Creates an instance of this class when event type is 
     * {@link IoSessionEventType#IDLE}.
     * 
     * @param nextFilter the next filter
     * @param session the session
     * @param status the idle status
     */
    public IoSessionEvent(final NextFilter nextFilter, final IoSession session,
            final IdleStatus status) {
        this(nextFilter, session, IoSessionEventType.IDLE);
        this.status = status;
    }
    
    /**
     * Delivers this event to the next filter.
     */
    public void deliverEvent() {
        logger.debug("Delivering event {}", this);
        deliverEvent(this.nextFilter, this.session, this.type, this.status);
    }

    /**
     * Static method which effectively delivers the specified event to the next filter
     * <code>nextFilter</code> on the <code>session</code>.
     * 
     * @param nextFilter the next filter
     * @param session the session on which the event occured
     * @param type the event type
     * @param status the idle status should only be non null only if the event type is 
     * {@link IoSessionEventType#IDLE} 
     */
    private static void deliverEvent(final NextFilter nextFilter,
            final IoSession session, final IoSessionEventType type,
            final IdleStatus status) {
        switch (type) {
        case CREATED:
            nextFilter.sessionCreated(session);
            break;
        case OPENED:
            nextFilter.sessionOpened(session);
            break;
        case IDLE:
            nextFilter.sessionIdle(session, status);
            break;
        case CLOSED:
            nextFilter.sessionClosed(session);
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(IoSessionEvent.class
                .getSimpleName());
        sb.append('@');
        sb.append(Integer.toHexString(hashCode()));
        sb.append(" - [ ").append(session);
        sb.append(", ").append(type);
        sb.append(']');
        return sb.toString();
    }

    /**
     * Returns the idle status of the event.
     * 
     * @return the idle status of the event
     */
    public IdleStatus getStatus() {
        return status;
    }

    /**
     * Returns the next filter to which the event should be sent.
     * 
     * @return the next filter
     */
    public NextFilter getNextFilter() {
        return nextFilter;
    }

    /**
     * Returns the session on which the event occured.
     * 
     * @return the session
     */
    public IoSession getSession() {
        return session;
    }

    /**
     * Returns the event type that occured.
     * 
     * @return the event type
     */
    public IoSessionEventType getType() {
        return type;
    }
}