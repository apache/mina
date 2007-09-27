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
package org.apache.mina.common;

import java.util.Iterator;
import java.util.Set;

import org.apache.mina.util.IdentityHashSet;

/**
 * Dectects idle sessions and fires <tt>sessionIdle</tt> events to them.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 525369 $, $Date: 2007-04-04 05:05:11 +0200 (mer., 04 avr. 2007) $
 */
public class IdleStatusChecker {
    private static final IdleStatusChecker INSTANCE = new IdleStatusChecker();

    public static IdleStatusChecker getInstance() {
        return INSTANCE;
    }

    private final Set<AbstractIoSession> sessions = new IdentityHashSet<AbstractIoSession>();

    private final Worker worker = new Worker();

    private IdleStatusChecker() {
        worker.start();
    }

    public void addSession(AbstractIoSession session) {
        synchronized (sessions) {
            sessions.add(session);
        }
    }

    private class Worker extends Thread {
        private Worker() {
            super("IdleStatusChecker");
            setDaemon(true);
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                long currentTime = System.currentTimeMillis();

                synchronized (sessions) {
                    Iterator<AbstractIoSession> it = sessions.iterator();
                    while (it.hasNext()) {
                        AbstractIoSession session = it.next();
                        if (!session.isConnected()) {
                            it.remove();
                        } else {
                            notifyIdleSession(session, currentTime);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event to any applicable
     * sessions in the specified collection.
     *   
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleSessions(Iterator<? extends IoSession> sessions, long currentTime) {
        while (sessions.hasNext()) {
            IoSession s = sessions.next();
            notifyIdleSession(s, currentTime);
        }
    }

    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event if applicable for the
     * specified {@code session}.
     * 
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleSession(IoSession session, long currentTime) {
        notifyIdleSession0(session, currentTime, session
                .getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(session.getLastIoTime(), session
                        .getLastIdleTime(IdleStatus.BOTH_IDLE)));
        notifyIdleSession0(session, currentTime, session
                .getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE, Math.max(session.getLastReadTime(),
                        session.getLastIdleTime(IdleStatus.READER_IDLE)));
        notifyIdleSession0(session, currentTime, session
                .getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE, Math.max(session.getLastWriteTime(),
                        session.getLastIdleTime(IdleStatus.WRITER_IDLE)));
        notifyWriteTimeout(session, currentTime, session
                .getConfig().getWriteTimeoutInMillis(), session.getLastWriteTime());
    }

    private static void notifyIdleSession0(IoSession session, long currentTime,
            long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && currentTime - lastIoTime >= idleTime) {
            if (session instanceof AbstractIoSession) {
                ((AbstractIoSession) session).increaseIdleCount(status);
            }
            session.getFilterChain().fireSessionIdle(status);
        }
    }

    private static void notifyWriteTimeout(IoSession session,
            long currentTime, long writeTimeout, long lastIoTime) {
        if (session instanceof AbstractIoSession &&
                writeTimeout > 0 && currentTime - lastIoTime >= writeTimeout &&
                !((AbstractIoSession) session).getWriteRequestQueue().isEmpty()) {
            session.getFilterChain().fireExceptionCaught(new WriteTimeoutException());
        }
    }
}