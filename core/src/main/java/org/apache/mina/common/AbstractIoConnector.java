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
public abstract class AbstractIoConnector 
        extends AbstractIoService implements IoConnector {
    
    private int connectTimeout = 60; // 1 minute
    private SocketAddress defaultRemoteAddress;

    protected AbstractIoConnector(IoSessionConfig sessionConfig) {
        super(sessionConfig);
    }

    public final int getConnectTimeout() {
        return connectTimeout;
    }

    public final long getConnectTimeoutMillis() {
        return connectTimeout * 1000L;
    }

    public final void setConnectTimeout(int connectTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout: "
                    + connectTimeout);
        }
        this.connectTimeout = connectTimeout;
    }

    public SocketAddress getDefaultRemoteAddress() {
        return defaultRemoteAddress;
    }

    public final void setDefaultRemoteAddress(SocketAddress defaultRemoteAddress) {
        if (defaultRemoteAddress == null) {
            throw new NullPointerException("defaultRemoteAddress");
        }
        
        if (!getTransportMetadata().getAddressType().isAssignableFrom(
                defaultRemoteAddress.getClass())) {
            throw new IllegalArgumentException("defaultRemoteAddress type: "
                    + defaultRemoteAddress.getClass() + " (expected: "
                    + getTransportMetadata().getAddressType() + ")");
        }
        this.defaultRemoteAddress = defaultRemoteAddress;
    }
    
    public final ConnectFuture connect() {
        SocketAddress defaultRemoteAddress = getDefaultRemoteAddress();
        if (defaultRemoteAddress == null) {
            throw new IllegalStateException("defaultRemoteAddress is not set.");
        }
        
        return connect(defaultRemoteAddress, null);
    }

    public final ConnectFuture connect(SocketAddress remoteAddress) {
        return connect(remoteAddress, null);
    }

    public final ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        if (isDisposed()) {
            throw new IllegalStateException("Already disposed.");
        }

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        if (!getTransportMetadata().getAddressType().isAssignableFrom(
                remoteAddress.getClass())) {
            throw new IllegalArgumentException("remoteAddress type: "
                    + remoteAddress.getClass() + " (expected: "
                    + getTransportMetadata().getAddressType() + ")");
        }

        if (localAddress != null
                && !getTransportMetadata().getAddressType().isAssignableFrom(
                        localAddress.getClass())) {
            throw new IllegalArgumentException("localAddress type: "
                    + localAddress.getClass() + " (expected: "
                    + getTransportMetadata().getAddressType() + ")");
        }

        if (getHandler() == null) {
            if (getSessionConfig().isUseReadOperation()) {
                setHandler(new IoHandler() {
                    public void exceptionCaught(IoSession session,
                            Throwable cause) throws Exception {
                    }

                    public void messageReceived(IoSession session,
                            Object message) throws Exception {
                    }

                    public void messageSent(IoSession session, Object message)
                            throws Exception {
                    }

                    public void sessionClosed(IoSession session)
                            throws Exception {
                    }

                    public void sessionCreated(IoSession session)
                            throws Exception {
                    }

                    public void sessionIdle(IoSession session, IdleStatus status)
                            throws Exception {
                    }

                    public void sessionOpened(IoSession session)
                            throws Exception {
                    }
                });
            } else {
                throw new IllegalStateException("handler is not set.");
            }
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

    /**
     * Adds required internal attributes and {@link IoFutureListener}s
     * related with event notifications to the specified {@code session}
     * and {@code future}.  Do not call this method directly;
     * {@link #finishSessionInitialization(IoSession, IoFuture)} will call
     * this method instead.
     */
    @Override
    protected final void finishSessionInitialization0(
            final IoSession session, IoFuture future) {
        // In case that ConnectFuture.cancel() is invoked before
        // setSession() is invoked, add a listener that closes the
        // connection immediately on cancellation.
        future.addListener(new IoFutureListener<ConnectFuture>() {
            public void operationComplete(ConnectFuture future) {
                if (future.isCanceled()) {
                    session.close();
                }
            }
        });
    }
    
    @Override
    public String toString() {
        TransportMetadata m = getTransportMetadata();
        return '(' + m.getProviderName() + ' ' + m.getName() + " connector: " + 
               "managedSessionCount: " + getManagedSessionCount() + ')'; 
    }
}
