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
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class IoSessionEvent {
    private final static Logger logger = LoggerFactory
            .getLogger(IoSessionEvent.class);

    private final NextFilter nextFilter;

    private final IoSession session;

    private final IoSessionEventType type;

    private IdleStatus status = null;

    public IoSessionEvent(final NextFilter nextFilter, final IoSession session,
            final IoSessionEventType type) {
        this.nextFilter = nextFilter;
        this.session = session;
        this.type = type;
    }

    public void deliverEvent() {
        logger.debug("Delivering event {}", this);
        deliverEvent(this.nextFilter, this.session, this.type, this.status);
    }

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

    public IdleStatus getStatus() {
        return status;
    }

    public void setStatus(IdleStatus status) {
        this.status = status;
    }

    public NextFilter getNextFilter() {
        return nextFilter;
    }

    public IoSession getSession() {
        return session;
    }

    public IoSessionEventType getType() {
        return type;
    }
}