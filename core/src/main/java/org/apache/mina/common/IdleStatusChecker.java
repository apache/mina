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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.util.ConcurrentHashSet;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * Detects idle sessions and fires <tt>sessionIdle</tt> events to them.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 525369 $, $Date: 2007-04-04 05:05:11 +0200 (mer., 04 avr. 2007) $
 */
public class IdleStatusChecker {
    private static final IdleStatusChecker INSTANCE = new IdleStatusChecker();

    public static IdleStatusChecker getInstance() {
        return INSTANCE;
    }

    private final Set<AbstractIoSession> sessions =
        new ConcurrentHashSet<AbstractIoSession>();
    private final Set<AbstractIoService> services =
        new ConcurrentHashSet<AbstractIoService>();

    private final Object lock = new Object();
    private final Runnable notifyingTask = new NamePreservingRunnable(
            new NotifyingTask(), "IdleStatusChecker");
    private final IoFutureListener<IoFuture> sessionCloseListener =
        new SessionCloseListener();
    private volatile ScheduledExecutorService executor;

    private IdleStatusChecker() {}

    public void addSession(AbstractIoSession session) {
        synchronized (lock) {
            boolean start = false;
            if (sessions.isEmpty() && services.isEmpty()) {
                start = true;
            }
            if (!sessions.add(session)) {
                return;
            }
            if (start) {
                start();
            }
        }
        
        session.getCloseFuture().addListener(sessionCloseListener);
    }
    
    public void addService(AbstractIoService service) {
        synchronized (lock) {
            boolean start = false;
            if (sessions.isEmpty() && services.isEmpty()) {
                start = true;
            }
            if (!services.add(service)) {
                return;
            }
            if (start) {
                start();
            }
        }
    }

    public void removeSession(AbstractIoSession session) {
        synchronized (lock) {
            sessions.remove(session);
            if (sessions.isEmpty() && services.isEmpty()) {
                stop();
            }
        }
    }
    
    public void removeService(AbstractIoService service) {
        synchronized (lock) {
            services.remove(service);
            if (sessions.isEmpty() && services.isEmpty()) {
                stop();
            }
        }
    }

    private void start() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        this.executor = executor;
        executor.scheduleWithFixedDelay(
                notifyingTask, 1000, 1000, TimeUnit.MILLISECONDS);
    }
    
    private void stop() {
        ScheduledExecutorService executor = this.executor;
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        this.executor = null;
    }

    private class NotifyingTask implements Runnable {
        public void run() {
            long currentTime = System.currentTimeMillis();
            notifyServices(currentTime);
            notifySessions(currentTime);
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
            notifyIdleness(service.getManagedSessions().iterator(), currentTime);
        }
    }

    /**
     * Fires a {@link IoEventType#SESSION_IDLE} event if applicable for the
     * specified {@code session}.
     *
     * @param currentTime the current time (i.e. {@link System#currentTimeMillis()})
     */
    public static void notifyIdleSession(IoSession session, long currentTime) {
        if (session instanceof AbstractIoSession) {
            AbstractIoSession s = (AbstractIoSession) session;
            notifyIdleSession1(
                    s, currentTime,
                    s.getConfig().getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                    IdleStatus.BOTH_IDLE, Math.max(
                            s.getLastIoTime(),
                            s.getLastIdleTime(IdleStatus.BOTH_IDLE)));
            
            notifyIdleSession1(
                    s, currentTime,
                    s.getConfig().getIdleTimeInMillis(IdleStatus.READER_IDLE),
                    IdleStatus.READER_IDLE, Math.max(
                            s.getLastReadTime(),
                            s.getLastIdleTime(IdleStatus.READER_IDLE)));
            
            notifyIdleSession1(
                    s, currentTime,
                    s.getConfig().getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                    IdleStatus.WRITER_IDLE, Math.max(
                            s.getLastWriteTime(),
                            s.getLastIdleTime(IdleStatus.WRITER_IDLE)));
    
            notifyWriteTimeout(s, currentTime);
            updateThroughput(s, currentTime);
        } else {
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
        }
    }

    private static void notifyIdleSession0(
            IoSession session, long currentTime,
            long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && currentTime - lastIoTime >= idleTime) {
            session.getFilterChain().fireSessionIdle(status);
        }
    }

    private static void notifyIdleSession1(
            AbstractIoSession session, long currentTime,
            long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && currentTime - lastIoTime >= idleTime) {
            session.getFilterChain().fireSessionIdle(status);
        }
    }

    private static void notifyWriteTimeout(
            AbstractIoSession session, long currentTime) {

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
            AbstractIoSession session, long currentTime) {
        session.updateThroughput(currentTime, false);
    }
}