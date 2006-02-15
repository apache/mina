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

import java.net.SocketAddress;
import java.util.Set;


/**
 * Base interface for all {@link IoAcceptor}s and {@link IoConnector}s
 * that provide I/O service and manage {@link IoSession}s.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoService
{
    /**
     * Returns all sessions with the specified remote or local address,
     * which are currently managed by this service.
     * {@link IoAcceptor} will assume the specified <tt>address</tt> is a local
     * address, and {@link IoConnector} will assume it's a remote address.
     * 
     * @param address the address to return all sessions for.
     * @return the sessions. An empty collection if there's no session.
     * @throws IllegalArgumentException if the specified <tt>address</tt> has 
     *         not been bound.
     * @throws UnsupportedOperationException if this operation isn't supported
     *         for the particular transport type implemented by this {@link IoService}.
     */
    Set getManagedSessions( SocketAddress address );

    /**
     * Returns the default configuration which is used when you didn't specify
     * any configuration.
     */
    IoServiceConfig getDefaultConfig();
    
    /**
     * Returns the global {@link IoFilterChainBuilder} which will modify the
     * {@link IoFilterChain} of all {@link IoSession}s which is managed
     * by this service.
     * The default value is an empty {@link DefaultIoFilterChainBuilder}.
     */
    IoFilterChainBuilder getFilterChainBuilder();
    
    /**
     * Sets the global {@link IoFilterChainBuilder} which will modify the
     * {@link IoFilterChain} of all {@link IoSession}s which is managed
     * by this service.
     * If you specify <tt>null</tt> this property will be set to
     * an empty {@link DefaultIoFilterChainBuilder}.
     */
    void setFilterChainBuilder( IoFilterChainBuilder builder );
    
    /**
     * A shortcut for <tt>( ( DefaultIoFilterChainBuilder ) </tt>{@link #getFilterChainBuilder()}<tt> )</tt>.
     * Please note that the returned object is not a <b>real</b> {@link IoFilterChain}
     * but a {@link DefaultIoFilterChainBuilder}.  Modifying the returned builder
     * won't affect the existing {@link IoSession}s at all, because
     * {@link IoFilterChainBuilder}s affect only newly created {@link IoSession}s.
     *
     * @throws IllegalStateException if the current {@link IoFilterChainBuilder} is
     *                               not a {@link DefaultIoFilterChainBuilder}
     */
    DefaultIoFilterChainBuilder getFilterChain();
}
