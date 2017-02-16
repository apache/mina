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

import java.net.SocketAddress;

import org.apache.mina.util.ExpirationListener;
import org.apache.mina.util.ExpiringMap;

/**
 * An {@link IoSessionRecycler} with sessions that time out on inactivity.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class ExpiringSessionRecycler implements IoSessionRecycler {
    /** A map used to store the session */
    private ExpiringMap<SocketAddress, IoSession> sessionMap;

    /** A map used to keep a track of the expiration */ 
    private ExpiringMap<SocketAddress, IoSession>.Expirer mapExpirer;

    /**
     * Create a new ExpiringSessionRecycler instance
     */
    public ExpiringSessionRecycler() {
        this(ExpiringMap.DEFAULT_TIME_TO_LIVE);
    }

    /**
     * Create a new ExpiringSessionRecycler instance
     * 
     * @param timeToLive The delay after which the session is going to be recycled
     */
    public ExpiringSessionRecycler(int timeToLive) {
        this(timeToLive, ExpiringMap.DEFAULT_EXPIRATION_INTERVAL);
    }

    /**
     * Create a new ExpiringSessionRecycler instance
     * 
     * @param timeToLive The delay after which the session is going to be recycled
     * @param expirationInterval The delay after which the expiration occurs
     */
    public ExpiringSessionRecycler(int timeToLive, int expirationInterval) {
        sessionMap = new ExpiringMap<>(timeToLive, expirationInterval);
        mapExpirer = sessionMap.getExpirer();
        sessionMap.addExpirationListener(new DefaultExpirationListener());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(IoSession session) {
        mapExpirer.startExpiringIfNotStarted();

        SocketAddress key = session.getRemoteAddress();

        if (!sessionMap.containsKey(key)) {
            sessionMap.put(key, session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoSession recycle(SocketAddress remoteAddress) {
        return sessionMap.get(remoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(IoSession session) {
        sessionMap.remove(session.getRemoteAddress());
    }

    /**
     * Stop the thread from monitoring the map
     */
    public void stopExpiring() {
        mapExpirer.stopExpiring();
    }

    /**
     * @return The session expiration time in second
     */
    public int getExpirationInterval() {
        return sessionMap.getExpirationInterval();
    }

    /**
     * @return The session time-to-live in second
     */
    public int getTimeToLive() {
        return sessionMap.getTimeToLive();
    }

    /**
     * Set the interval in which a session will live in the map before it is removed.
     * 
     * @param expirationInterval The session expiration time in seconds
     */
    public void setExpirationInterval(int expirationInterval) {
        sessionMap.setExpirationInterval(expirationInterval);
    }

    /**
     * Update the value for the time-to-live
     *
     * @param timeToLive The time-to-live (seconds)
     */
    public void setTimeToLive(int timeToLive) {
        sessionMap.setTimeToLive(timeToLive);
    }

    private class DefaultExpirationListener implements ExpirationListener<IoSession> {
        @Override
        public void expired(IoSession expiredSession) {
            expiredSession.closeNow();
        }
    }
}
