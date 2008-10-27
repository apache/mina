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

import java.util.Iterator;
import java.util.Set;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteTimeoutException;
import org.apache.mina.util.ConcurrentHashSet;

/**
 * Detects idle sessions and fires <tt>sessionIdle</tt> events to them.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 525369 $, $Date: 2007-04-04 05:05:11 +0200 (mer., 04 avr. 2007) $
 */
public class IdleStatusChecker {
    private final Set<AbstractIoSession> sessions =
        new ConcurrentHashSet<AbstractIoSession>();
    private final Set<AbstractIoService> services =
        new ConcurrentHashSet<AbstractIoService>();

    private final NotifyingTask notifyingTask = new NotifyingTask();
    private final IoFutureListener<IoFuture> sessionCloseListener =
        new SessionCloseListener();

    public IdleStatusChecker() {}

    public void addSession(AbstractIoSession session) {
        sessions.add(session);
        CloseFuture closeFuture = session.getCloseFuture();
        closeFuture.addListener(sessionCloseListener);
    }

    public void addService(AbstractIoService service) {
        services.add(service);
    }

    public void removeSession(AbstractIoSession session) {
        sessions.remove(session);
    }

    public void removeService(AbstractIoService service) {
        services.remove(service);
    }

    public NotifyingTask getNotifyingTask() {
        return notifyingTask;
    }

    public class NotifyingTask implements Runnable {
        private volatile boolean cancelled;
        private volatile Thread thread;

        public void run() {
            thread = Thread.currentThread();
            try {
                while (!cancelled) {
                    // Check idleness with fixed delay (1 second).
                    long currentTime = System.currentTimeMillis();
                    notifyServices(currentTime);
                    notifySessions(currentTime);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // will exit the loop if interrupted from interrupt()
                    }
                }
            } finally {
                thread = null;
            }
        }

        public void cancel() {
            cancelled = true;
            Thread thread = this.thread;
            if (thread != null) {
                thread.interrupt();
            }
        }

        private void notifyServices(long currentTime) {
            Iterator<AbstractIoService> it = services.iterator();
            while (it.hasNext()) {
                AbstractIoService service = it.next();
                if (service.isActive()) {
                    notifyIdleness(service, currentTime, false);
                }
            }
        }

        private void notifySessions(long currentTime) {
            Iterator<AbstractIoSession> it = sessions.iterator();
            while (it.hasNext()) {
                AbstractIoSession session = it.next();
                if (session.isConnected()) {
                    notifyIdleSession(session, currentTime);
                }
            }
        }
    }

    private class SessionCloseListener implements IoFutureListener<IoFuture> {
        public void operationComplete(IoFuture future) {
            removeSession((AbstractIoSession) future.getSession());
        }
    }

    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event to any applicable
     * sessions in the specified collection.
     *
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleness(Iterator<? extends IoSession> sessions, long currentTime) {
        IoSession s = null;
        while (sessions.hasNext()) {
            s = sessions.next();
            notifyIdleSession(s, currentTime);
        }
    }

    public static void notifyIdleness(IoService service, long currentTime) {
        notifyIdleness(service, currentTime, true);
    }

    private static void notifyIdleness(IoService service, long currentTime, boolean includeSessions) {
        if (!(service instanceof AbstractIoService)) {
            return;
        }

        ((AbstractIoService) service).notifyIdleness(currentTime);

        if (includeSessions) {
            notifyIdleness(service.getManagedSessions().values().iterator(), currentTime);
        }
    }

    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event if applicable for the
     * specified {@code session}.
     *
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleSession(IoSession session, long currentTime) {
        notifyIdleSession0(
                session, currentTime,
                session.getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(
                    session.getLastIoTime(),
                    session.getLastIdleTime(IdleStatus.BOTH_IDLE)));

        notifyIdleSession0(
                session, currentTime,
                session.getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE, Math.max(
                    session.getLastReadTime(),
                    session.getLastIdleTime(IdleStatus.READER_IDLE)));

        notifyIdleSession0(
                session, currentTime,
                session.getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE, Math.max(
                    session.getLastWriteTime(),
                    session.getLastIdleTime(IdleStatus.WRITER_IDLE)));

        notifyWriteTimeout(session, currentTime);
        updateThroughput(session, currentTime);
    }

    private static void notifyIdleSession0(
            IoSession session, long currentTime,
            long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && currentTime - lastIoTime >= idleTime) {
            session.getFilterChain().fireSessionIdle(status);
        }
    }

    private static void notifyWriteTimeout(
            IoSession session, long currentTime) {

        long writeTimeout = session.getConfig().getWriteTimeoutInMillis();
        if (writeTimeout > 0 &&
                currentTime - session.getLastWriteTime() >= writeTimeout &&
                !session.getWriteRequestQueue().isEmpty(session)) {
            WriteRequest request = session.getCurrentWriteRequest();
            if (request != null) {
                session.setCurrentWriteRequest(null);
                WriteTimeoutException cause = new WriteTimeoutException(request);
                request.getFuture().setException(cause);
                session.getFilterChain().fireExceptionCaught(cause);
                // WriteException is an IOException, so we close the session.
                session.close();
            }
        }
    }

    private static void updateThroughput(
            IoSession session, long currentTime) {
        session.updateThroughput(currentTime, false);
    }
}