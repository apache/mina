/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.common;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;

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
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoAcceptor extends IoSessionManager
{
    /**
     * Binds to the specified <code>address</code> and handles incoming
     * connections with the specified <code>handler</code>.
     * 
     * @throws IOException if failed to bind
     */
    void bind( SocketAddress address, IoHandler handler ) throws IOException;

    /**
     * Binds to the specified <code>address</code> and handles incoming
     * connections with the specified <code>handler</code>.
     *
     * @param filterChainBuilder
     *            an {@link IoFilterChainBuilder} that will modify the
     *            {@link IoFilterChain} of a newly created {@link IoSession}
     * @throws IOException if failed to bind
     */
    void bind( SocketAddress address, IoHandler handler, IoFilterChainBuilder filterChainBuilder ) throws IOException;

    /**
     * Unbinds from the specified <code>address</code> and disconnects all clients
     * connected there.
     */
    void unbind( SocketAddress address );
    
    /**
     * Returns all sessions currently connected to the specified local address.
     * 
     * @param address the local address to return all sessions for. Must have
     *        been bound previously.
     * @return the sessions.
     * @throws IllegalArgumentException if the specified <tt>address</tt> has 
     *         not been bound.
     * @throws UnsupportedOperationException if this operation isn't supported
     *         for the particular transport type implemented by this 
     *         {@link IoAcceptor}.
     */
    Collection getManagedSessions( SocketAddress address );
    
    /**
     * (Optional) Returns an {@link IoSession} that is bound to the specified
     * <tt>localAddress</tt> and <tt>remoteAddress</tt> which reuses
     * the <tt>localAddress</tt> that is already bound by {@link IoAcceptor}
     * via {@link #bind(SocketAddress, IoHandler)}.
     * <p>
     * This operation is optional.  Please throw {@link UnsupportedOperationException}
     * if the transport type doesn't support this operation.  This operation is
     * usually implemented for connectionless transport types.
     * 
     * @throws UnsupportedOperationException if this operation is not supported
     * @throws IllegalArgumentException if the specified <tt>localAddress</tt> is
     *                                  not bound yet. (see {@link #bind(SocketAddress, IoHandler)})
     */
    IoSession newSession( SocketAddress remoteAddress, SocketAddress localAddress );
    
    /**
     * Returns <tt>true</tt> if and only if all clients are disconnected
     * when this acceptor unbinds the related local address.
     */
    boolean isDisconnectClientsOnUnbind();
    
    /**
     * Sets whether all clients are disconnected when this acceptor unbinds the
     * related local address.  The default value is <tt>true</tt>.
     */
    void setDisconnectClientsOnUnbind( boolean disconnectClientsOnUnbind );
}