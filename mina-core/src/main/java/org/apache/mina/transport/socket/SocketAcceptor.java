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
import java.net.ServerSocket;
import java.util.Set;

import org.apache.mina.core.service.IoAcceptor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface SocketAcceptor extends IoAcceptor {
    /**
     * @return the local InetSocketAddress which is bound currently.  If more than one
     * address are bound, only one of them will be returned, but it's not
     * necessarily the firstly bound address.
     * This method overrides the {@link IoAcceptor#getLocalAddress()} method.
     */
    @Override
    InetSocketAddress getLocalAddress();

    /**
     * @return a {@link Set} of the local InetSocketAddress which are bound currently.
     * This method overrides the {@link IoAcceptor#getDefaultLocalAddress()} method.
     */
    @Override
    InetSocketAddress getDefaultLocalAddress();

    /**
     * Sets the default local InetSocketAddress to bind when no argument is specified in
     * {@link #bind()} method. Please note that the default will not be used
     * if any local InetSocketAddress is specified.
     * This method overrides the {@link IoAcceptor#setDefaultLocalAddress(java.net.SocketAddress)} method.
     * 
     * @param localAddress The local address
     */
    void setDefaultLocalAddress(InetSocketAddress localAddress);

    /**
     * @see ServerSocket#getReuseAddress()
     * 
     * @return <tt>true</tt> if the <tt>SO_REUSEADDR</tt> is enabled
     */
    boolean isReuseAddress();

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     * 
     * @param reuseAddress tells if the <tt>SO_REUSEADDR</tt> is to be enabled
     */
    void setReuseAddress(boolean reuseAddress);

    /**
     * @return the size of the backlog.
     */
    int getBacklog();

    /**
     * Sets the size of the backlog.  This can only be done when this
     * class is not bound
     * 
     * @param backlog The backlog's size
     */
    void setBacklog(int backlog);

    /**
     * @return the default configuration of the new SocketSessions created by 
     * this acceptor service.
     */
    @Override
    SocketSessionConfig getSessionConfig();
}
