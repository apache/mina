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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

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
 * {@link #bind()} is invoked, and stop when {@link #unbind()} is invoked.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoAcceptor extends IoService {
    /**
     * Returns the local address which is bound currently.  If more than one
     * address are bound, only one of them will be returned, but it's not
     * necessarily the firstly bound address.
     * 
     * @return The bound LocalAddress
     */
    SocketAddress getLocalAddress();

    /**
     * Returns a {@link Set} of the local addresses which are bound currently.
     * 
     * @return The Set of bound LocalAddresses
     */
    Set<SocketAddress> getLocalAddresses();

    /**
     * Returns the default local address to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.  If more than one address are
     * set, only one of them will be returned, but it's not necessarily the
     * firstly specified address in {@link #setDefaultLocalAddresses(List)}.
     * 
     * @return The default bound LocalAddress
     */
    SocketAddress getDefaultLocalAddress();

    /**
     * Returns a {@link List} of the default local addresses to bind when no
     * argument is specified in {@link #bind()} method.  Please note that the
     * default will not be used if any local address is specified.
     * 
     * @return The list of default bound LocalAddresses
     */
    List<SocketAddress> getDefaultLocalAddresses();

    /**
     * Sets the default local address to bind when no argument is specified in
     * {@link #bind()} method.  Please note that the default will not be used
     * if any local address is specified.
     * 
     * @param localAddress The local addresses to bind the acceptor on
     */
    void setDefaultLocalAddress(SocketAddress localAddress);

    /**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     * @param firstLocalAddress The first local address to bind the acceptor on
     * @param otherLocalAddresses The other local addresses to bind the acceptor on
     */
    void setDefaultLocalAddresses(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses);

    /**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     * 
     * @param localAddresses The local addresses to bind the acceptor on
     */
    void setDefaultLocalAddresses(Iterable<? extends SocketAddress> localAddresses);

    /**
     * Sets the default local addresses to bind when no argument is specified
     * in {@link #bind()} method.  Please note that the default will not be
     * used if any local address is specified.
     * 
     * @param localAddresses The local addresses to bind the acceptor on
     */
    void setDefaultLocalAddresses(List<? extends SocketAddress> localAddresses);

    /**
     * Returns <tt>true</tt> if and only if all clients are closed when this
     * acceptor unbinds from all the related local address (i.e. when the
     * service is deactivated).
     * 
     * @return <tt>true</tt> if the service sets the closeOnDeactivation flag
     */
    boolean isCloseOnDeactivation();

    /**
     * Sets whether all client sessions are closed when this acceptor unbinds
     * from all the related local addresses (i.e. when the service is
     * deactivated).  The default value is <tt>true</tt>.
     * 
     * @param closeOnDeactivation <tt>true</tt> if we should close on deactivation
     */
    void setCloseOnDeactivation(boolean closeOnDeactivation);

    /**
     * Binds to the default local address(es) and start to accept incoming
     * connections.
     *
     * @throws IOException if failed to bind
     */
    void bind() throws IOException;

    /**
     * Binds to the specified local address and start to accept incoming
     * connections.
     *
     * @param localAddress The SocketAddress to bind to
     * 
     * @throws IOException if failed to bind
     */
    void bind(SocketAddress localAddress) throws IOException;

    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections. If no address is given, bind on the default local address.
     * 
     * @param firstLocalAddress The first address to bind to
     * @param addresses The SocketAddresses to bind to
     * 
     * @throws IOException if failed to bind
     */
    void bind(SocketAddress firstLocalAddress, SocketAddress... addresses) throws IOException;

    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections. If no address is given, bind on the default local address.
     * 
     * @param addresses The SocketAddresses to bind to
     *
     * @throws IOException if failed to bind
     */
    void bind(SocketAddress... addresses) throws IOException;

    /**
     * Binds to the specified local addresses and start to accept incoming
     * connections.
     *
     * @param localAddresses The local address we will be bound to
     * @throws IOException if failed to bind
     */
    void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException;

    /**
     * Unbinds from all local addresses that this service is bound to and stops
     * to accept incoming connections.  All managed connections will be closed
     * if {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property
     * is <tt>true</tt>.  This method returns silently if no local address is
     * bound yet.
     */
    void unbind();

    /**
     * Unbinds from the specified local address and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * address is not bound yet.
     * 
     * @param localAddress The local address we will be unbound from
     */
    void unbind(SocketAddress localAddress);

    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * addresses are not bound yet.
     * 
     * @param firstLocalAddress The first local address to be unbound from
     * @param otherLocalAddresses The other local address to be unbound from
     */
    void unbind(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses);

    /**
     * Unbinds from the specified local addresses and stop to accept incoming
     * connections.  All managed connections will be closed if
     * {@link #setCloseOnDeactivation(boolean) disconnectOnUnbind} property is
     * <tt>true</tt>.  This method returns silently if the default local
     * addresses are not bound yet.
     * 
     * @param localAddresses The local address we will be unbound from
     */
    void unbind(Iterable<? extends SocketAddress> localAddresses);

    /**
     * (Optional) Returns an {@link IoSession} that is bound to the specified
     * <tt>localAddress</tt> and the specified <tt>remoteAddress</tt> which
     * reuses the local address that is already bound by this service.
     * <p>
     * This operation is optional.  Please throw {@link UnsupportedOperationException}
     * if the transport type doesn't support this operation.  This operation is
     * usually implemented for connectionless transport types.
     *
     * @param remoteAddress The remote address bound to the service
     * @param localAddress The local address the session will be bound to
     * @throws UnsupportedOperationException if this operation is not supported
     * @throws IllegalStateException if this service is not running.
     * @throws IllegalArgumentException if this service is not bound to the
     *                                  specified <tt>localAddress</tt>.
     * @return The session bound to the the given localAddress and remote address
     */
    IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress);
}