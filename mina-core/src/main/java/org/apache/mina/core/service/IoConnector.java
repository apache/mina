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

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;

/**
 * Connects to endpoint, communicates with the server, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/netcat/Main.html">NetCat</a>
 * example.
 * <p>
 * You should connect to the desired socket address to start communication,
 * and then events for incoming connections will be sent to the specified
 * default {@link IoHandler}.
 * <p>
 * Threads connect to endpoint start automatically when
 * {@link #connect(SocketAddress)} is invoked, and stop when all
 * connection attempts are finished.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoConnector extends IoService {
    /**
     * @return the connect timeout in seconds.  The default value is 1 minute.
     * 
     * @deprecated
     */
    @Deprecated
    int getConnectTimeout();

    /**
     * @return the connect timeout in milliseconds.  The default value is 1 minute.
     */
    long getConnectTimeoutMillis();

    /**
     * Sets the connect timeout in seconds.  The default value is 1 minute.
     * 
     * @deprecated
     * @param connectTimeout The time out for the connection
     */
    @Deprecated
    void setConnectTimeout(int connectTimeout);

    /**
     * Sets the connect timeout in milliseconds.  The default value is 1 minute.
     * 
     * @param connectTimeoutInMillis The time out for the connection
     */
    void setConnectTimeoutMillis(long connectTimeoutInMillis);

    /**
     * @return the default remote address to connect to when no argument
     * is specified in {@link #connect()} method.
     */
    SocketAddress getDefaultRemoteAddress();

    /**
     * Sets the default remote address to connect to when no argument is
     * specified in {@link #connect()} method.
     * 
     * @param defaultRemoteAddress The default remote address
     */
    void setDefaultRemoteAddress(SocketAddress defaultRemoteAddress);

    /**
     * @return the default local address
     */
    SocketAddress getDefaultLocalAddress();

    /**
     * Sets the default local address
     * 
     * @param defaultLocalAddress The default local address
     */
    void setDefaultLocalAddress(SocketAddress defaultLocalAddress);

    /**
     * Connects to the {@link #setDefaultRemoteAddress(SocketAddress) default
     * remote address}.
     * 
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     * @throws IllegalStateException
     *             if no default remoted address is set.
     */
    ConnectFuture connect();

    /**
     * Connects to the {@link #setDefaultRemoteAddress(SocketAddress) default
     * remote address} and invokes the <code>ioSessionInitializer</code> when
     * the IoSession is created but before {@link IoHandler#sessionCreated(IoSession)}
     * is invoked.  There is <em>no</em> guarantee that the <code>ioSessionInitializer</code>
     * will be invoked before this method returns.
     * 
     * @param sessionInitializer  the callback to invoke when the {@link IoSession} object is created
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     * 
     * @throws IllegalStateException if no default remote address is set.
     */
    ConnectFuture connect(IoSessionInitializer<? extends ConnectFuture> sessionInitializer);

    /**
     * Connects to the specified remote address.
     * 
     * @param remoteAddress The remote address to connect to
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    ConnectFuture connect(SocketAddress remoteAddress);

    /**
     * Connects to the specified remote address and invokes
     * the <code>ioSessionInitializer</code> when the IoSession is created but before
     * {@link IoHandler#sessionCreated(IoSession)} is invoked.  There is <em>no</em>
     * guarantee that the <code>ioSessionInitializer</code> will be invoked before
     * this method returns.
     * 
     * @param remoteAddress  the remote address to connect to
     * @param sessionInitializer  the callback to invoke when the {@link IoSession} object is created
     * 
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    ConnectFuture connect(SocketAddress remoteAddress, IoSessionInitializer<? extends ConnectFuture> sessionInitializer);

    /**
     * Connects to the specified remote address binding to the specified local address.
     *
     * @param remoteAddress The remote address to connect
     * @param localAddress The local address to bind
     * 
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    ConnectFuture connect(SocketAddress remoteAddress, SocketAddress localAddress);

    /**
     * Connects to the specified remote address binding to the specified local
     * address and and invokes the <code>ioSessionInitializer</code> when the
     * IoSession is created but before {@link IoHandler#sessionCreated(IoSession)}
     * is invoked.  There is <em>no</em> guarantee that the <code>ioSessionInitializer</code>
     * will be invoked before this method returns.
     * 
     * @param remoteAddress  the remote address to connect to
     * @param localAddress  the local interface to bind to
     * @param sessionInitializer  the callback to invoke when the {@link IoSession} object is created
     *
     * @return the {@link ConnectFuture} instance which is completed when the
     *         connection attempt initiated by this call succeeds or fails.
     */
    ConnectFuture connect(SocketAddress remoteAddress, SocketAddress localAddress,
            IoSessionInitializer<? extends ConnectFuture> sessionInitializer);
}
