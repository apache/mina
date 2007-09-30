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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.util.ConcurrentHashSet;

/**
 * A helper which provides addition and removal of {@link IoServiceListener}s and firing
 * events.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupport {
    /**
     * The {@link IoService} that this instance manages.
     */
    private final IoService service;

    /**
     * A list of {@link IoServiceListener}s.
     */
    private final List<IoServiceListener> listeners = new CopyOnWriteArrayList<IoServiceListener>();

    /**
     * Tracks managed sessions.
     */
    private final Set<IoSession> managedSessions = new ConcurrentHashSet<IoSession>();

    /**
     * Read only version of {@link #managedSessions}.
     */
    private final Set<IoSession> readOnlyManagedSessions = Collections.unmodifiableSet(managedSessions);

    private final AtomicBoolean activated = new AtomicBoolean();

    /**
     * Creates a new instance.
     */
    public IoServiceListenerSupport(IoService service) {
        if (service == null) {
            throw new NullPointerException("service");
        }
        this.service = service;
    }

    /**
     * Adds a new listener.
     */
    public void add(IoServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes an existing listener.
     */
    public void remove(IoServiceListener listener) {
        listeners.remove(listener);
    }

    public Set<IoSession> getManagedSessions() {
        return readOnlyManagedSessions;
    }
    
    public boolean isActive() {
        return activated.get();
    }

    /**
     * Calls {@link IoServiceListener#serviceActivated(IoService)}
     * for all registered listeners.
     */
    public void fireServiceActivated() {
        if (!activated.compareAndSet(false, true)) {
            return;
        }
        
        if (service instanceof AbstractIoService) {
            ((AbstractIoService) service).setActivationTime(System.currentTimeMillis());
        }

        for (IoServiceListener l : listeners) {
            l.serviceActivated(service);
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceDeactivated(IoService)}
     * for all registered listeners.
     */
    public void fireServiceDeactivated() {
        if (!activated.compareAndSet(true, false)) {
            return;
        }
        
        try {
            for (IoServiceListener l : listeners) {
                l.serviceDeactivated(service);
            }
        } finally {
            disconnectSessions();
            if (service instanceof AbstractIoService) {
                ((AbstractIoService) service).setActivationTime(0);
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionCreated(IoSession)} for all registered listeners.
     */
    public void fireSessionCreated(IoSession session) {
        boolean firstSession = false;
        if (session.getService() instanceof IoConnector) {
            synchronized (managedSessions) {
                firstSession = managedSessions.isEmpty();
            }
        }
        
        // If already registered, ignore.
        if (!managedSessions.add(session)) {
            return;
        }

        // If the first connector session, fire a virtual service activation event.
        if (firstSession) {
            fireServiceActivated();
        }

        // Fire session events.
        session.getFilterChain().fireSessionCreated();
        session.getFilterChain().fireSessionOpened();

        // Fire listener events.
        for (IoServiceListener l : listeners) {
            l.sessionCreated(session);
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionDestroyed(IoSession)} for all registered listeners.
     */
    public void fireSessionDestroyed(IoSession session) {
        // Try to remove the remaining empty session set after removal.
        if (!managedSessions.remove(session)) {
            return;
        }

        // Fire session events.
        session.getFilterChain().fireSessionClosed();

        // Fire listener events.
        try {
            for (IoServiceListener l : listeners) {
                l.sessionDestroyed(session);
            }
        } finally {
            // Fire a virtual service deactivation event for the last session of the connector.
            if (session.getService() instanceof IoConnector) {
                boolean lastSession = false;
                synchronized (managedSessions) {
                    lastSession = managedSessions.isEmpty();
                }
                if (lastSession) {
                    fireServiceDeactivated();
                }
            }
        }
    }

    private void disconnectSessions() {
        if (!(service instanceof IoAcceptor)) {
            return;
        }

        if (!((IoAcceptor) service).isDisconnectOnUnbind()) {
            return;
        }

        Object lock = new Object();
        IoFutureListener listener = new LockNotifyingListener(lock);

        for (IoSession s : managedSessions) {
            s.close().addListener(listener);
        }

        try {
            synchronized (lock) {
                while (!managedSessions.isEmpty()) {
                    lock.wait(500);
                }
            }
        } catch (InterruptedException ie) {
            // Ignored
        }
    }

    private static class LockNotifyingListener implements IoFutureListener {
        private final Object lock;

        public LockNotifyingListener(Object lock) {
            this.lock = lock;
        }

        public void operationComplete(IoFuture future) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }
}
