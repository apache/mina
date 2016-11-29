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
package org.apache.mina.core.session;

import org.apache.mina.core.write.WriteRequest;

/**
 * An I/O event or an I/O request that MINA provides.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoEvent implements Runnable {
    /** The IoEvent type */
    private final IoEventType type;

    /** The associated IoSession */
    private final IoSession session;

    /** The stored parameter */
    private final Object parameter;

    /**
     * Creates a new IoEvent
     * 
     * @param type The type of event to create
     * @param session The associated IoSession
     * @param parameter The parameter to add to the event
     */
    public IoEvent(IoEventType type, IoSession session, Object parameter) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        
        if (session == null) {
            throw new IllegalArgumentException("session");
        }
        
        this.type = type;
        this.session = session;
        this.parameter = parameter;
    }

    /**
     * @return The IoEvent type
     */
    public IoEventType getType() {
        return type;
    }

    /**
     * @return The associated IoSession
     */
    public IoSession getSession() {
        return session;
    }

    /**
     * @return The stored parameter
     */
    public Object getParameter() {
        return parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        fire();
    }

    /**
     * Fire an event
     */
    public void fire() {
        switch ( type ) {
            case MESSAGE_RECEIVED:
                session.getFilterChain().fireMessageReceived(getParameter());
                break;
                
            case MESSAGE_SENT:
                session.getFilterChain().fireMessageSent((WriteRequest) getParameter());
                break;
                
            case WRITE:
                session.getFilterChain().fireFilterWrite((WriteRequest) getParameter());
                break;
                
            case CLOSE:
                session.getFilterChain().fireFilterClose();
                break;
                
            case EXCEPTION_CAUGHT:
                session.getFilterChain().fireExceptionCaught((Throwable) getParameter());
                break;
                
            case SESSION_IDLE:
                session.getFilterChain().fireSessionIdle((IdleStatus) getParameter());
                break;
                
            case SESSION_OPENED:
                session.getFilterChain().fireSessionOpened();
                break;
                
            case SESSION_CREATED:
                session.getFilterChain().fireSessionCreated();
                break;
                
            case SESSION_CLOSED:
                session.getFilterChain().fireSessionClosed();
                break;
                
            default:
                throw new IllegalArgumentException("Unknown event type: " + getType());
        }
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append('[');
        sb.append(session);
        sb.append(']');
        sb.append(type.name());
        
        if (parameter != null) {
            sb.append(':');
            sb.append(parameter);
        }

        return sb.toString();
    }
}
