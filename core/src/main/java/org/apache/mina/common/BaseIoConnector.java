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


/**
 * A base implementation of {@link IoConnector}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoConnector extends BaseIoService implements
        IoConnector {
    private int connectTimeout = 60; // 1 minute

    protected BaseIoConnector(IoSessionConfig sessionConfig) {
        super(sessionConfig);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeout * 1000L;
    }

    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout: "
                    + connectTimeout);
        }
        this.connectTimeout = connectTimeout;
    }

    public final ConnectFuture connect(SocketAddress remoteAddress) {
        return connect(remoteAddress, null);
    }

    public final ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        if (!getTransportType().getAddressType().isAssignableFrom(
                remoteAddress.getClass())) {
            throw new IllegalArgumentException("remoteAddress type: "
                    + remoteAddress.getClass() + " (expected: "
                    + getTransportType().getAddressType() + ")");
        }

        if (localAddress != null
                && !getTransportType().getAddressType().isAssignableFrom(
                        localAddress.getClass())) {
            throw new IllegalArgumentException("localAddress type: "
                    + localAddress.getClass() + " (expected: "
                    + getTransportType().getAddressType() + ")");
        }

        if (getHandler() == null) {
            throw new IllegalStateException("handler is not set.");
        }

        return doConnect(remoteAddress, localAddress);
    }

    /**
     * Implement this method to perform the actual connect operation.
     * 
     * @param localAddress <tt>null</tt> if no local address is specified
     */
    protected abstract ConnectFuture doConnect(SocketAddress remoteAddress,
            SocketAddress localAddress);
}
