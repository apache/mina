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
package org.apache.mina.proxy.session;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;

/**
 * ProxyIoSessionInitializer.java - {@link IoSessionInitializer} wrapper class to inject the 
 * {@link ProxyIoSession} object that contains all the attributes of the target connection 
 * into the {@link IoSession}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class ProxyIoSessionInitializer<T extends ConnectFuture> implements
        IoSessionInitializer<T> {
    private final IoSessionInitializer<T> wrappedSessionInitializer;

    private final ProxyIoSession proxyIoSession;

    public ProxyIoSessionInitializer(
            final IoSessionInitializer<T> wrappedSessionInitializer,
            final ProxyIoSession proxyIoSession) {
        this.wrappedSessionInitializer = wrappedSessionInitializer;
        this.proxyIoSession = proxyIoSession;
    }

    public ProxyIoSession getProxySession() {
        return proxyIoSession;
    }

    public void initializeSession(final IoSession session, T future) {
        if (wrappedSessionInitializer != null) {
            wrappedSessionInitializer.initializeSession(session, future);
        }

        if (proxyIoSession != null) {
            proxyIoSession.setSession(session);
            session.setAttribute(ProxyIoSession.PROXY_SESSION, proxyIoSession);
        }
    }
}