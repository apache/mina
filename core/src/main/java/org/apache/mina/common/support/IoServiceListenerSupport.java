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
package org.apache.mina.common.support;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.util.IdentityHashSet;

/**
 * A helper which provides addition and removal of {@link IoServiceListener}s and firing
 * events.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupport {
    /**
     * A list of {@link IoServiceListener}s.
     */
    private final List<IoServiceListener> listeners = new CopyOnWriteArrayList<IoServiceListener>();

    /**
     * Tracks managed <tt>serviceAddress</tt>es.
     */
    private final Set<SocketAddress> managedServiceAddresses = new CopyOnWriteArraySet<SocketAddress>();

    /**
     * Tracks managed sesssions with <tt>serviceAddress</tt> as a key.
     */
    private final ConcurrentMap<SocketAddress, Set<IoSession>> managedSessions = new ConcurrentHashMap<SocketAddress, Set<IoSession>>();

    /**
     * Creates a new instance.
     */
    public IoServiceListenerSupport() {
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

    public Set<SocketAddress> getManagedServiceAddresses() {
        return Collections.unmodifiableSet(managedServiceAddresses);
    }

    public boolean isManaged(SocketAddress serviceAddress) {
        return managedServiceAddresses.contains(serviceAddress);
    }

    public Set<IoSession> getManagedSessions(SocketAddress serviceAddress) {
        Set<IoSession> sessions = managedSessions.get(serviceAddress);

        if (null == sessions) {
            return Collections.emptySet();
        }

        synchronized (sessions) {
            return new IdentityHashSet<IoSession>(sessions);
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceActivated(IoService, SocketAddress, IoHandler, IoServiceConfig)}
     * for all registered listeners.
     */
    public void fireServiceActivated(IoService service,
            SocketAddress serviceAddress, IoHandler handler,
            IoServiceConfig config) {
        if (!managedServiceAddresses.add(serviceAddress)) {
            return;
        }

        for (IoServiceListener listener : listeners) {
            try {
                listener.serviceActivated(service, serviceAddress, handler, config);
            } catch (Throwable e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceDeactivated(IoService, SocketAddress, IoHandler, IoServiceConfig)}
     * for all registered listeners.
     */
    public synchronized void fireServiceDeactivated(IoService service,
            SocketAddress serviceAddress, IoHandler handler,
            IoServiceConfig config) {
        if (!managedServiceAddresses.remove(serviceAddress)) {
            return;
        }

        try {
            for (IoServiceListener listener : listeners) {
                try {
                    listener.serviceDeactivated(service, serviceAddress, handler,
                            config);
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        } finally {
            disconnectSessions(serviceAddress, config);
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionCreated(IoSession)} for all registered listeners.
     */
    public void fireSessionCreated(IoSession session) {
        SocketAddress serviceAddress = session.getServiceAddress();

        boolean firstSession;
        Set<IoSession> s = new IdentityHashSet<IoSession>();
        synchronized (managedSessions) {
            // Get the session set.
            Set<IoSession> sessions = managedSessions.putIfAbsent(serviceAddress,
                    Collections.synchronizedSet(s));
    
            if (null == sessions) {
                sessions = s;
                firstSession = true;
            } else {
                firstSession = false;
            }
    
            // If already registered, ignore.
            if (!sessions.add(session)) {
                return;
            }
        }

        // If the first connector session, fire a virtual service activation event.
        if (session.getService() instanceof IoConnector && firstSession) {
            fireServiceActivated(session.getService(), session
                    .getServiceAddress(), session.getHandler(), session
                    .getServiceConfig());
        }

        // Fire session events.
        session.getFilterChain().fireSessionCreated(session);
        session.getFilterChain().fireSessionOpened(session);

        // Fire listener events.
        for (IoServiceListener listener : listeners) {
            try {
                listener.sessionCreated(session);
            } catch (Throwable e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionDestroyed(IoSession)} for all registered listeners.
     */
    public void fireSessionDestroyed(IoSession session) {
        SocketAddress serviceAddress = session.getServiceAddress();

        boolean lastSession = false;
        synchronized (managedSessions) {
            // Get the session set.
            Set<IoSession> sessions = managedSessions.get(serviceAddress);
            // Ignore if unknown.
            if (sessions == null) {
                return;
            }
    
            sessions.remove(session);
    
            // Try to remove the remaining empty session set after removal.
            if (sessions.isEmpty()) {
                lastSession = managedSessions.remove(serviceAddress, sessions);
            }
        }

        // Fire session events.
        session.getFilterChain().fireSessionClosed(session);

        // Fire listener events.
        try {
            for (IoServiceListener listener : listeners) {
                try {
                    listener.sessionDestroyed(session);
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        } finally {
            // Fire a virtual service deactivation event for the last session of the connector.
            //TODO double-check that this is *STILL* the last session. May not be the case
            if (session.getService() instanceof IoConnector && lastSession) {
                fireServiceDeactivated(session.getService(), session
                        .getServiceAddress(), session.getHandler(), session
                        .getServiceConfig());
            }
        }
    }

    private void disconnectSessions(SocketAddress serviceAddress,
            IoServiceConfig config) {
        if (!(config instanceof IoAcceptorConfig)) {
            return;
        }

        if (!((IoAcceptorConfig) config).isDisconnectOnUnbind()) {
            return;
        }

        Set<IoSession> sessions = getManagedSessions(serviceAddress);

        if (sessions.isEmpty()) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(sessions.size());

        for (IoSession session : sessions) {
            session.close().addListener(new IoFutureListener() {
                public void operationComplete(IoFuture future) {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeIOException(e);
        }
    }
}
