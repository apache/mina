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
package org.apache.mina.transport.vmpipe.support;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.IdleStatus;

/**
 * Dectects idle sessions and fires <tt>sessionIdle</tt> events to them. 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeIdleStatusChecker {
    private static final VmPipeIdleStatusChecker INSTANCE = new VmPipeIdleStatusChecker();

    public static VmPipeIdleStatusChecker getInstance() {
        return INSTANCE;
    }

    private final Set<VmPipeSessionImpl> sessions = new HashSet<VmPipeSessionImpl>();

    private final Worker worker = new Worker();

    private VmPipeIdleStatusChecker() {
        worker.start();
    }

    public void addSession(VmPipeSessionImpl session) {
        synchronized (sessions) {
            sessions.add(session);
        }
    }

    private class Worker extends Thread {
        private Worker() {
            super("VmPipeIdleStatusChecker");
            setDaemon(true);
        }

        public void run() {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                long currentTime = System.currentTimeMillis();

                synchronized (sessions) {
                    Iterator<VmPipeSessionImpl> it = sessions.iterator();
                    while (it.hasNext()) {
                        VmPipeSessionImpl session = it.next();
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

    private void notifyIdleSession(VmPipeSessionImpl session, long currentTime) {
        notifyIdleSession0(session, currentTime, session
                .getIdleTimeInMillis(IdleStatus.BOTH_IDLE),
                IdleStatus.BOTH_IDLE, Math.max(session.getLastIoTime(), session
                        .getLastIdleTime(IdleStatus.BOTH_IDLE)));
        notifyIdleSession0(session, currentTime, session
                .getIdleTimeInMillis(IdleStatus.READER_IDLE),
                IdleStatus.READER_IDLE, Math.max(session.getLastReadTime(),
                        session.getLastIdleTime(IdleStatus.READER_IDLE)));
        notifyIdleSession0(session, currentTime, session
                .getIdleTimeInMillis(IdleStatus.WRITER_IDLE),
                IdleStatus.WRITER_IDLE, Math.max(session.getLastWriteTime(),
                        session.getLastIdleTime(IdleStatus.WRITER_IDLE)));
    }

    private void notifyIdleSession0(VmPipeSessionImpl session,
            long currentTime, long idleTime, IdleStatus status, long lastIoTime) {
        if (idleTime > 0 && lastIoTime != 0
                && (currentTime - lastIoTime) >= idleTime) {
            session.increaseIdleCount(status);
            session.getFilterChain().fireSessionIdle(session, status);
        }
    }

}