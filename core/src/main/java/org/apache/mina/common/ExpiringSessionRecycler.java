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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.util.ExpirationListener;
import org.apache.mina.util.ExpiringMap;

/**
 * An {@link IoSessionRecycler} with sessions that time out on inactivity.
 * 
 * TODO Document me.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ExpiringSessionRecycler implements IoSessionRecycler {
    private ExpiringMap<Object, IoSession> sessionMap;

    private ExpiringMap.Expirer mapExpirer;

    public ExpiringSessionRecycler() {
        this(ExpiringMap.DEFAULT_TIME_TO_LIVE);
    }

    public ExpiringSessionRecycler(int timeToLive) {
        this(timeToLive, ExpiringMap.DEFAULT_EXPIRATION_INTERVAL);
    }

    public ExpiringSessionRecycler(int timeToLive, int expirationInterval) {
        sessionMap = new ExpiringMap<Object, IoSession>(timeToLive,
                expirationInterval);
        mapExpirer = sessionMap.getExpirer();
        sessionMap.addExpirationListener(new DefaultExpirationListener());
    }

    public void put(IoSession session) {
        mapExpirer.startExpiringIfNotStarted();

        Object key = generateKey(session);

        if (!sessionMap.containsKey(key)) {
            sessionMap.put(key, session);
        }
    }

    public IoSession recycle(SocketAddress localAddress,
            SocketAddress remoteAddress) {
        return sessionMap.get(generateKey(localAddress, remoteAddress));
    }

    public void remove(IoSession session) {
        sessionMap.remove(generateKey(session));
    }

    public void stopExpiring() {
        mapExpirer.stopExpiring();
    }

    public int getExpirationInterval() {
        return sessionMap.getExpirationInterval();
    }

    public int getTimeToLive() {
        return sessionMap.getTimeToLive();
    }

    public void setExpirationInterval(int expirationInterval) {
        sessionMap.setExpirationInterval(expirationInterval);
    }

    public void setTimeToLive(int timeToLive) {
        sessionMap.setTimeToLive(timeToLive);
    }

    private Object generateKey(IoSession session) {
        return generateKey(session.getLocalAddress(), session
                .getRemoteAddress());
    }

    private Object generateKey(SocketAddress localAddress,
            SocketAddress remoteAddress) {
        List<SocketAddress> key = new ArrayList<SocketAddress>(2);
        key.add(remoteAddress);
        key.add(localAddress);
        return key;
    }

    private class DefaultExpirationListener implements
            ExpirationListener<IoSession> {
        public void expired(IoSession expiredSession) {
            expiredSession.close();
        }
    }
}
