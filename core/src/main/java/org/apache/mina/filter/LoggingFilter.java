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
package org.apache.mina.filter;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;
import org.slf4j.Logger;

/**
 * Logs all MINA protocol events to {@link Logger}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 * @see SessionLog
 */
public class LoggingFilter extends IoFilterAdapter {
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = SessionLog.PREFIX;

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = SessionLog.LOGGER;

    /**
     * Creates a new instance.
     */
    public LoggingFilter() {
    }

    public void sessionCreated(NextFilter nextFilter, IoSession session) {
        SessionLog.info(session, "CREATED");
        nextFilter.sessionCreated(session);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        SessionLog.info(session, "OPENED");
        nextFilter.sessionOpened(session);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        SessionLog.info(session, "CLOSED");
        nextFilter.sessionClosed(session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) {
        if (SessionLog.isInfoEnabled(session)) {
            SessionLog.info(session, "IDLE: " + status);
        }
        nextFilter.sessionIdle(session, status);
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) {
        if (SessionLog.isWarnEnabled(session)) {
            SessionLog.warn(session, "EXCEPTION:", cause);
        }
        nextFilter.exceptionCaught(session, cause);
    }

    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) {
        if (SessionLog.isInfoEnabled(session)) {
            SessionLog.info(session, "RECEIVED: " + message);
        }
        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) {
        if (SessionLog.isInfoEnabled(session)) {
            SessionLog.info(session, "SENT: " + message);
        }
        nextFilter.messageSent(session, message);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (SessionLog.isInfoEnabled(session)) {
            SessionLog.info(session, "WRITE: " + writeRequest);
        }
        nextFilter.filterWrite(session, writeRequest);
    }

    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        SessionLog.info(session, "CLOSE");
        nextFilter.filterClose(session);
    }
}
