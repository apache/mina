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
package org.apache.mina.core.service;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;

/**
 * A base implementation of {@link IoConnector}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoConnector 
        extends AbstractIoService implements IoConnector {
    /**
     * The minimum timeout value that is supported (in milliseconds).
     */
    private long connectTimeoutCheckInterval = 50L;
    private long connectTimeoutInMillis = 60 * 1000L; // 1 minute by default
    private SocketAddress defaultRemoteAddress;

    /**
     * Constructor for {@link AbstractIoConnector}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     *
     * {@see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)}
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling execution of I/O
     *            events. Can be <code>null</code>.
     */
    protected AbstractIoConnector(IoSessionConfig sessionConfig, Executor executor) {
        super(sessionConfig, executor);
    }

    /**
     * Returns the minimum connection timeout value for this connector
     * 
     * @return
     *  The minimum time that this connector can have for a connection
     *  timeout in milliseconds.
     */
    public long getConnectTimeoutCheckInterval() {
        return connectTimeoutCheckInterval;
    }

    public void setConnectTimeoutCheckInterval(long minimumConnectTimeout) {
        if( getConnectTimeoutMillis() < minimumConnectTimeout ){
            this.connectTimeoutInMillis = minimumConnectTimeout;
        }
        
        this.connectTimeoutCheckInterval = minimumConnectTimeout;
    }

    /**
     * @deprecated
     *  Take a look at <tt>getConnectTimeoutMillis()</tt>
     */
    public final int getConnectTimeout() {
        return (int)connectTimeoutInMillis/1000;
    }

    /**
     * {@inheritDoc}
     */
    public final long getConnectTimeoutMillis() {
        return connectTimeoutInMillis;
    }

    /**
     * @deprecated
     *  Take a look at <tt>setConnectTimeoutMillis(long)</tt>
     */
    public final void setConnectTimeout(int connectTimeout) {
        
        setConnectTimeoutMillis( connectTimeout * 1000L );
    }
    
    /**
     * Sets the connect timeout value in milliseconds.
     * 
     */
    public final void setConnectTimeoutMillis(long connectTimeoutInMillis) {
        if (connectTimeoutInMillis <= connectTimeoutCheckInterval) {
            this.connectTimeoutCheckInterval = connectTimeoutInMillis;
        }
        this.connectTimeoutInMillis = connectTimeoutInMillis;
    }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getDefaultRemoteAddress() {
        return defaultRemoteAddress;
    }

    /**
     * {@inheritDoc}
     */
    public final void setDefaultRemoteAddress(SocketAddress defaultRemoteAddress) {
        if (defaultRemoteAddress == null) {
            throw new IllegalArgumentException("defaultRemoteAddress");
        }
        
        if (!getTransportMetadata().getAddressType().isAssignableFrom(
                defaultRemoteAddress.getClass())) {
            throw new IllegalArgumentException("defaultRemoteAddress type: "
                    + defaultRemoteAddress.getClass() + " (expected: "
                    + getTransportMetadata().getAddressType() + ")");
        }
        this.defaultRemoteAddress = defaultRemoteAddress;
    }
    
    /**
     * {@inheritDoc}
     */
    public final ConnectFuture connect() {
        SocketAddress defaultRemoteAddress = getDefaultRemoteAddress();
        if (defaultRemoteAddress == null) {
            throw new IllegalStateException("defaultRemoteAddress is not set.");
        }
        
        return connect(defaultRemoteAddress, null, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public ConnectFuture connect(IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        SocketAddress defaultRemoteAddress = getDefaultRemoteAddress();
        if (defaultRemoteAddress == null) {
            throw new IllegalStateException("defaultRemoteAddress is not set.");
        }
        
        return connect(defaultRemoteAddress, null, sessionInitializer);
    }

    /**
     * {@inheritDoc}
     */
    public final ConnectFuture connect(SocketAddress remoteAddress) {
        return connect(remoteAddress, null, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public ConnectFuture connect(SocketAddress remoteAddress,
            IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        return connect(remoteAddress, null, sessionInitializer);
    }
    
    /**
     * {@inheritDoc}
     */
    public ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        return connect(remoteAddress, localAddress, null);
    }

    /**
     * {@inheritDoc}
     */
    public final ConnectFuture connect(SocketAddress remoteAddress,
            SocketAddress localAddress, IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
        if (isDisposing()) {
            throw new IllegalStateException("The connector has been disposed.");
        }

        if (remoteAddress == null) {
            throw new IllegalArgumentException("remoteAddress");
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
                        // Empty handler
                    }

                    public void messageReceived(IoSession session,
                            Object message) throws Exception {
                        // Empty handler
                    }

                    public void messageSent(IoSession session, Object message)
                            throws Exception {
                        // Empty handler
                    }

                    public void sessionClosed(IoSession session)
                            throws Exception {
                        // Empty handler
                    }

                    public void sessionCreated(IoSession session)
                            throws Exception {
                        // Empty handler
                    }

                    public void sessionIdle(IoSession session, IdleStatus status)
                            throws Exception {
                        // Empty handler
                    }

                    public void sessionOpened(IoSession session)
                            throws Exception {
                        // Empty handler
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
                    session.close(true);
                }
            }
        });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        TransportMetadata m = getTransportMetadata();
        return '(' + m.getProviderName() + ' ' + m.getName() + " connector: " + 
               "managedSessionCount: " + getManagedSessionCount() + ')'; 
    }
}
