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
    /**
     * The minimum timeout value that is supported (in milliseconds).
     * 
     * TODO: Make this configurable and automatically adjusted if the timeout
     *       a user specified is smaller than the current minimum connect timeout.
     *       Please refer to the mailing list archive about this issue:
     *       
     *           Message-ID: <1202880068.7504.38.camel@laptop>
     *              Subject: Re: connect timeout
     */
    private static final long MINIMUM_CONNECT_TIMEOUT = 50L;
    
    private long connectTimeoutInMillis = 60*1000L; // 1 minute by default
    private SocketAddress defaultRemoteAddress;

    protected AbstractIoConnector(IoSessionConfig sessionConfig) {
        super(sessionConfig);
    }

    /**
     * @deprecated
     */
    public final int getConnectTimeout() {
        return (int)connectTimeoutInMillis/1000;
    }

    public final long getConnectTimeoutMillis() {
        return connectTimeoutInMillis;
    }

    /**
     * @deprecated
     */
    public final void setConnectTimeout(int connectTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout: "
                    + connectTimeout);
        }
        this.connectTimeoutInMillis = connectTimeout*1000L;
    }
    
    /**
     * Sets the connect timeout value in milliseconds.
     * 
     * @throws IllegalArgumentException if the value is smaller than 
     * <tt>MINIMUM_CONNECT_TIMEOUT</tt>.
     */
    public final void setConnectTimeoutMillis(long connectTimeoutInMillis) {
        if (connectTimeoutInMillis <= MINIMUM_CONNECT_TIMEOUT) {
            throw new IllegalArgumentException("connectTimeoutInMillis: " + 
                    connectTimeoutInMillis);
        }
        this.connectTimeoutInMillis = connectTimeoutInMillis;
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
        
        return connect(defaultRemoteAddress, null, null);
    }
    
    public ConnectFuture connect(IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        SocketAddress defaultRemoteAddress = getDefaultRemoteAddress();
        if (defaultRemoteAddress == null) {
            throw new IllegalStateException("defaultRemoteAddress is not set.");
        }
        
        return connect(defaultRemoteAddress, null, sessionInitializer);
    }

    public final ConnectFuture connect(SocketAddress remoteAddress) {
        return connect(remoteAddress, null, null);
    }
    
    public ConnectFuture connect(SocketAddress remoteAddress,
            IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        return connect(remoteAddress, null, sessionInitializer);
    }
    
    public ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        return connect(remoteAddress, localAddress, null);
    }

    public final ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress, IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        if (isDisposing()) {
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

        return connect0(remoteAddress, localAddress, sessionInitializer);
    }

    /**
     * Implement this method to perform the actual connect operation.
     *
     * @param localAddress <tt>null</tt> if no local address is specified
     */
    protected abstract ConnectFuture connect0(SocketAddress remoteAddress,
            SocketAddress localAddress, IoSessionInitializer<? extends ConnectFuture> sessionInitializer);

    /**
     * Adds required internal attributes and {@link IoFutureListener}s
     * related with event notifications to the specified {@code session}
     * and {@code future}.  Do not call this method directly;
     * {@link #finishSessionInitialization(IoSession, IoFuture, IoSessionInitializer)}
     * will call this method instead.
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
