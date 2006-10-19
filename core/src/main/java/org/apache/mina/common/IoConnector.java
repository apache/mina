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
 * {@link #connect(SocketAddress, IoHandler)} is invoked, and stop when all
 * connection attempts are finished.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoConnector extends IoService
{
    /**
     * Returns the local address to bind.
     * @return <tt>null</tt> if no local address is specified.
     */
    SocketAddress getLocalAddress();
    
    /**
     * Sets the local address to bind.
     * @param localAddress <tt>null</tt> to specify no specific local address 
     */
    void setLocalAddress( SocketAddress localAddress );
    
    /**
     * Returns the address of the remote peer to connect to.  This property can't
     * be changed once after {@link #connect()} method is called.
     */
    SocketAddress getRemoteAddress();
    
    /**
     * Sets the address of the remote peer to connect to.  This property can't be
     * changed once after {@link #connect()} method is called.
     * 
     * @throws IllegalStateException if {@link #connect()} method is already called
     */
    void setRemoteAddress( SocketAddress remoteAddress );
    
    /**
     * Returns the connect timeout in seconds.  The default value is 1 minute.
     */
    int getConnectTimeout();

    /**
     * Returns the connect timeout in milliseconds.  The default value is 1 minute.
     */
    long getConnectTimeoutMillis();

    /**
     * Sets the connect timeout in seconds.  The default value is 1 minute.
     */
    void setConnectTimeout( int connectTimeout );
    
    /**
     * Connects to the configured remote address.
     * 
     * @return the {@link ConnectFuture} instance which is completed when the 
     *         connection attempt initiated by this call succeeds or fails.
     */
    ConnectFuture connect();
}