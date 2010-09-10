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
import org.apache.mina.core.service.IoService;
import org.apache.mina.util.ConcurrentHashSet;

/**
 * Detects idle sessions and fires <tt>sessionIdle</tt> events to them.
 * To be used for service unable to trigger idle events alone, like VmPipe
 * or SerialTransport. Polling base transport are advised to trigger idle 
 * events alone, using the poll/select timeout. 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IdleStatusChecker {
    
    // the list of session to check
    private final Set<AbstractIoSession> sessions =
        new ConcurrentHashSet<AbstractIoSession>();

    /* create a task you can execute in the transport code,
     * if the transport is like NIO or APR you don't need to call it,
     * you just need to call the needed static sessions on select()/poll() 
     * timeout.
     */ 
    private final NotifyingTask notifyingTask = new NotifyingTask();
    
    private final IoFutureListener<IoFuture> sessionCloseListener =
        new SessionCloseListener();

    public IdleStatusChecker() {
        // Do nothing
    }

    /**
     * Add the session for being checked for idle. 
     * @param session the session to check
     */
    public void addSession(AbstractIoSession session) {
        sessions.add(session);
        CloseFuture closeFuture = session.getCloseFuture();
        
        // isn't service reponsability to remove the session nicely ?
        closeFuture.addListener(sessionCloseListener);
    }

    /**
     * remove a session from the list of session being checked.
     * @param session
     */
    private void removeSession(AbstractIoSession session) {
        sessions.remove(session);
    }

    /**
     * get a runnable task able to be scheduled in the {@link IoService} executor.
     * @return
     */
    public NotifyingTask getNotifyingTask() {
        return notifyingTask;
    }

    /**
     * The class to place in the transport executor for checking the sessions idle 
     */
    public class NotifyingTask implements Runnable {
        private volatile boolean cancelled;
        private volatile Thread thread;
        
        // we forbid instantiation of this class outside
        /** No qualifier */ NotifyingTask() {
            // Do nothing
        }

        public void run() {
            thread = Thread.currentThread();
            try {
                while (!cancelled) {
                    // Check idleness with fixed delay (1 second).
                    long currentTime = System.currentTimeMillis();

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

        /**
         * stop execution of the task
         */
        public void cancel() {
            cancelled = true;
            Thread thread = this.thread;
            if (thread != null) {
                thread.interrupt();
            }
        }

        private void notifySessions(long currentTime) {
            Iterator<AbstractIoSession> it = sessions.iterator();
            while (it.hasNext()) {
                AbstractIoSession session = it.next();
                if (session.isConnected()) {
                    AbstractIoSession.notifyIdleSession(session, currentTime);
                }
            }
        }
    }

    private class SessionCloseListener implements IoFutureListener<IoFuture> {
        /**
         * Default constructor
         */
        public SessionCloseListener() {
            super();
        }
        
        public void operationComplete(IoFuture future) {
            removeSession((AbstractIoSession) future.getSession());
        }
    }
}