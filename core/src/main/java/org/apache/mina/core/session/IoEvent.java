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
    private final IoEventType type;

    private final IoSession session;

    private final Object parameter;

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

    public IoEventType getType() {
        return type;
    }

    public IoSession getSession() {
        return session;
    }

    public Object getParameter() {
        return parameter;
    }
    
    public void run() {
        fire();
    }

    public void fire() {
        switch (getType()) {
        case MESSAGE_RECEIVED:
            getSession().getFilterChain().fireMessageReceived(getParameter());
            break;
        case MESSAGE_SENT:
            getSession().getFilterChain().fireMessageSent((WriteRequest) getParameter());
            break;
        case WRITE:
            getSession().getFilterChain().fireFilterWrite((WriteRequest) getParameter());
            break;
        case CLOSE:
            getSession().getFilterChain().fireFilterClose();
            break;
        case EXCEPTION_CAUGHT:
            getSession().getFilterChain().fireExceptionCaught((Throwable) getParameter());
            break;
        case SESSION_IDLE:
            getSession().getFilterChain().fireSessionIdle((IdleStatus) getParameter());
            break;
        case SESSION_OPENED:
            getSession().getFilterChain().fireSessionOpened();
            break;
        case SESSION_CREATED:
            getSession().getFilterChain().fireSessionCreated();
            break;
        case SESSION_CLOSED:
            getSession().getFilterChain().fireSessionClosed();
            break;
        default:
            throw new IllegalArgumentException("Unknown event type: " + getType());
        }
    }

    @Override
    public String toString() {
        if (getParameter() == null) {
            return "[" + getSession() + "] " + getType().name();
        }
        
        return "[" + getSession() + "] " + getType().name() + ": "
                    + getParameter();
    }
}
