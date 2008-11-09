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

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.write.WriteRequest;

/**
 * An I/O event or an I/O request that MINA provides.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 592965 $, $Date: 2007-11-08 01:15:00 +0100 (Thu, 08 Nov 2007) $
 */
public class IoEvent implements Runnable {
    private final IoEventType type;

    private final IoSession session;

    private final Object parameter;

    public IoEvent(IoEventType type, IoSession session, Object parameter) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (session == null) {
            throw new NullPointerException("session");
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
    	IoFilter nextFilter;

    	try {
            switch (type) {
            case MESSAGE_RECEIVED:
            	nextFilter = session. getFilterInHead();
            	nextFilter.messageReceived(0, session, getParameter());
                break;
            case MESSAGE_SENT:
            	nextFilter = session. getFilterInHead();
            	nextFilter.messageSent(0, session, (WriteRequest) getParameter());
                break;
            case WRITE:
            	nextFilter = session. getFilterOutHead();
            	nextFilter.filterWrite(0, session, (WriteRequest) getParameter());
                break;
            case SET_TRAFFIC_MASK:
            	nextFilter = session. getFilterInHead();
            	nextFilter.filterSetTrafficMask(0, session, (TrafficMask) getParameter());
                break;
            case CLOSE:
            	nextFilter = session. getFilterInHead();
            	nextFilter.filterClose(0, session);
                break;
            case EXCEPTION_CAUGHT:
            	nextFilter = session. getFilterInHead();
            	nextFilter.exceptionCaught(0, session, (Throwable) getParameter());
                break;
            case SESSION_IDLE:
            	nextFilter = session. getFilterInHead();
            	nextFilter.sessionIdle(0, session, (IdleStatus) getParameter());
                break;
            case SESSION_OPENED:
            	nextFilter = session. getFilterInHead();
            	nextFilter.sessionOpened(0, session);
                break;
            case SESSION_CREATED:
            	nextFilter = session. getFilterInHead();
            	nextFilter.sessionCreated(0, session);
                break;
            case SESSION_CLOSED:
            	nextFilter = session. getFilterInHead();
            	nextFilter.sessionClosed(0, session);
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + getType());
            }
    	} catch (Exception e) {
    		// TODO : handle the exception
    	}
    }

    @Override
    public String toString() {
        if (getParameter() == null) {
            return "[" + getSession() + "] " + getType().name();
        } else {
            return "[" + getSession() + "] " + getType().name() + ": "
                    + getParameter();
        }
    }
}
