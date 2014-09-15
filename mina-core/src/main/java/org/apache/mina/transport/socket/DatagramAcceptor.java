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
package org.apache.mina.transport.socket;

import java.net.InetSocketAddress;
import java.util.Set;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionRecycler;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface DatagramAcceptor extends IoAcceptor {
    /**
     * Returns the local InetSocketAddress which is bound currently.  If more than one
     * address are bound, only one of them will be returned, but it's not
     * necessarily the firstly bound address.
     * This method overrides the {@link IoAcceptor#getLocalAddress()} method.
     */
    InetSocketAddress getLocalAddress();

    /**
     * Returns a {@link Set} of the local InetSocketAddress which are bound currently.
     * This method overrides the {@link IoAcceptor#getDefaultLocalAddress()} method.
     */
    InetSocketAddress getDefaultLocalAddress();

    /**
     * Sets the default local InetSocketAddress to bind when no argument is specified in
     * {@link #bind()} method. Please note that the default will not be used
     * if any local InetSocketAddress is specified.
     * This method overrides the {@link IoAcceptor#setDefaultLocalAddress(java.net.SocketAddress)} method.
     */
    void setDefaultLocalAddress(InetSocketAddress localAddress);

    /**
     * Returns the {@link IoSessionRecycler} for this service.
     */
    IoSessionRecycler getSessionRecycler();

    /**
     * Sets the {@link IoSessionRecycler} for this service.
     *
     * @param sessionRecycler <tt>null</tt> to use the default recycler
     */
    void setSessionRecycler(IoSessionRecycler sessionRecycler);

    /**
     * Returns the default Datagram configuration of the new {@link IoSession}s
     * created by this service.
     */
    DatagramSessionConfig getSessionConfig();
}
