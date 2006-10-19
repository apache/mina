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

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Accepts incoming connection, communicates with clients, and fires events to
 * {@link IoHandler}s.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/echoserver/Main.html">EchoServer</a>
 * example. 
 * <p>
 * You should bind to the desired socket address to accept incoming
 * connections, and then events for incoming connections will be sent to
 * the specified default {@link IoHandler}.
 * <p>
 * Threads accept incoming connections start automatically when
 * {@link #bind(SocketAddress, IoHandler)} is invoked, and stop when all
 * addresses are unbound.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoAcceptor extends IoService
{
    /**
     * Returns the local address to bind.
     */
    SocketAddress getLocalAddress();
    
    /**
     * Sets the local address to bind.
     * 
     * @throws IllegalStateException if this service is already running.
     */
    void setLocalAddress( SocketAddress localAddress );
    
    /**
     * Returns <tt>true</tt> if and only if all clients are disconnected
     * when this acceptor unbinds the related local address.
     */
    boolean isDisconnectOnUnbind();
    
    /**
     * Sets whether all clients are disconnected when this acceptor unbinds the
     * related local address.  The default value is <tt>true</tt>.
     */
    void setDisconnectOnUnbind( boolean disconnectOnUnbind );

    /**
     * Bind to the configured local address and start to accept incoming connections.
     * 
     * @throws IOException if failed to bind
     */
    void bind() throws IOException;
    
    /**
     * Unbind from the configured local address and stop to accept incoming connections.
     * All managed connections will be closed if <tt>disconnectOnUnbind</tt> property is set.
     * This method does nothing if not bound yet.
     */
    void unbind();
    
    /**
     * Returns <tt>true</tt> if and if only this service is bound to the local address.
     */
    boolean isBound();

    /**
     * (Optional) Returns an {@link IoSession} that is bound to the current
     * local address and the specified <tt>remoteAddress</tt> which reuses
     * the local address that is already bound by this service.
     * <p>
     * This operation is optional.  Please throw {@link UnsupportedOperationException}
     * if the transport type doesn't support this operation.  This operation is
     * usually implemented for connectionless transport types.
     * 
     * @throws UnsupportedOperationException if this operation is not supported
     * @throws IllegalStateException if this service is not running.
     */
    IoSession newSession( SocketAddress remoteAddress );
}