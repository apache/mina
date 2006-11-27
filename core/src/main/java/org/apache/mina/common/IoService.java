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

import java.util.Set;


/**
 * Base interface for all {@link IoAcceptor}s and {@link IoConnector}s
 * that provide I/O service and manage {@link IoSession}s.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoService
{
    /**
     * Adds an {@link IoServiceListener} that listens any events related with
     * this service.
     */
    void addListener( IoServiceListener listener );
    
    /**
     * Removed an existing {@link IoServiceListener} that listens any events
     * related with this service.
     */
    void removeListener( IoServiceListener listener );
    
    /**
     * Returns the handler which will handle all connections managed by this service.
     */
    IoHandler getHandler();
    
    /**
     * Sets the handler which will handle all connections managed by this service.
     */
    void setHandler( IoHandler handler );
    
    /**
     * Returns all sessions which are currently managed by this service.
     * {@link IoAcceptor} will assume the specified <tt>address</tt> is a local
     * address, and {@link IoConnector} will assume it's a remote address.
     * 
     * @return the sessions. An empty collection if there's no session.
     * @throws UnsupportedOperationException if this operation isn't supported
     *         for the particular transport type implemented by this {@link IoService}.
     */
    Set getManagedSessions();

    /**
     * Returns the default configuration of the new {@link IoSession}s
     * created by this service.
     */
    IoSessionConfig getSessionConfig();
    
    /**
     * Sets the default configuration of the new {@link IoSession}s
     * created by this service.
     * 
     * @param config the new default config.
     * @throws IllegalArgumentException if the type of the specified config doesn't
     *                match the {@link IoSessionConfig} implementation supported by
     *                this {@link IoService}.
     */
    void setSessionConfig( IoSessionConfig config );
    
    /**
     * Returns the {@link IoFilterChainBuilder} which will build the
     * {@link IoFilterChain} of all {@link IoSession}s which is created
     * by this service.
     * The default value is an empty {@link DefaultIoFilterChainBuilder}.
     */
    IoFilterChainBuilder getFilterChainBuilder();
    
    /**
     * Sets the {@link IoFilterChainBuilder} which will build the
     * {@link IoFilterChain} of all {@link IoSession}s which is created
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
    
    /**
     * Returns the default {@link ThreadModel} of the {@link IoService}.
     * The default value is an {@link ExecutorThreadModel} whose service name is
     * <tt>'AnonymousIoService'</tt> and which has 16 maximum active threads.
     * It is strongly recommended to set a new {@link ExecutorThreadModel} by calling
     * {@link ExecutorThreadModel#getInstance(String)}.
     */
    ThreadModel getThreadModel();
    
    /**
     * Sets the default {@link ThreadModel} of the {@link IoService}.
     * If you specify <tt>null</tt>, this property will be set to the
     * default value.
     * The default value is an {@link ExecutorThreadModel} whose service name is
     * <tt>'AnonymousIoService'</tt> with 16 threads.
     * It is strongly recommended to set a new {@link ExecutorThreadModel} by calling
     * {@link ExecutorThreadModel#getInstance(String)}.
     */
    void setThreadModel( ThreadModel threadModel );
}
