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

import java.util.List;

import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilter.WriteRequest;

/**
 * A container of {@link IoFilter}s that forwards {@link IoHandler} events
 * to the consisting filters and terminal {@link IoHandler} sequentially.
 * Every {@link IoSession} has its own {@link IoFilterChain} (1-to-1 relationship). 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoFilterChain {
    /**
     * Returns the parent {@link IoSession} of this chain.
     * @return {@link IoSession}
     */
    IoSession getSession();

    /**
     * Returns the {@link Entry} with the specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    Entry getEntry(String name);

    /**
     * Returns the {@link IoFilter} with the specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    IoFilter get(String name);

    /**
     * Returns the {@link NextFilter} of the {@link IoFilter} with the
     * specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(String name);

    /**
     * Returns the list of all {@link Entry}s this chain contains.
     */
    List<Entry> getAll();

    /**
     * Returns the reversed list of all {@link Entry}s this chain contains.
     */
    List<Entry> getAllReversed();

    /**
     * Returns <tt>true</tt> if this chain contains an {@link IoFilter} with the
     * specified <tt>name</tt>.
     */
    boolean contains(String name);

    /**
     * Returns <tt>true</tt> if this chain contains the specified <tt>filter</tt>.
     */
    boolean contains(IoFilter filter);

    /**
     * Returns <tt>true</tt> if this chain contains an {@link IoFilter} of the
     * specified <tt>filterType</tt>.
     */
    boolean contains(Class<? extends IoFilter> filterType);

    /**
     * Adds the specified filter with the specified name at the beginning of this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addFirst(String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name at the end of this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addLast(String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name just before the filter whose name is
     * <code>baseName</code> in this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addBefore(String baseName, String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name just after the filter whose name is
     * <code>baseName</code> in this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addAfter(String baseName, String name, IoFilter filter);

    /**
     * Removes the filter with the specified name from this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostRemove(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#destroy()} throws an exception.
     */
    IoFilter remove(String name);

    /**
     * Removes all filters added to this chain.
     * @throws Exception if {@link IoFilter#onPostRemove(IoFilterChain, String, NextFilter)} thrown an exception.
     */
    void clear() throws Exception;

    /**
     * Fires a {@link IoHandler#sessionCreated(IoSession)} event.  Most users don't need to
     * call this method at all.  Please use this method only when you implement a new transport
     * or fire a virtual event.
     */
    public void fireSessionCreated(IoSession session);

    /**
     * Fires a {@link IoHandler#sessionOpened(IoSession)} event.  Most users don't need to call
     * this method at all.  Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    public void fireSessionOpened(IoSession session);

    /**
     * Fires a {@link IoHandler#sessionClosed(IoSession)} event.  Most users don't need to call
     * this method at all.  Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    public void fireSessionClosed(IoSession session);

    /**
     * Fires a {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event.  Most users don't
     * need to call this method at all.  Please use this method only when you implement a new
     * transport or fire a virtual event.
     */
    public void fireSessionIdle(IoSession session, IdleStatus status);

    /**
     * Fires a {@link #fireMessageReceived(IoSession, Object)} event.  Most users don't need to
     * call this method at all.  Please use this method only when you implement a new transport
     * or fire a virtual event.
     */
    public void fireMessageReceived(IoSession session, Object message);

    /**
     * Fires a {@link IoHandler#sessionOpened(IoSession)} event.  Most users don't need to call
     * this method at all.  Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    public void fireMessageSent(IoSession session, WriteRequest request);

    /**
     * Fires a {@link IoHandler#exceptionCaught(IoSession, Throwable)} event.  Most users don't
     * need to call this method at all.  Please use this method only when you implement a new
     * transport or fire a virtual event.
     */
    public void fireExceptionCaught(IoSession session, Throwable cause);

    /**
     * Fires a {@link IoSession#write(Object)} event.  Most users don't need to call this
     * method at all.  Please use this method only when you implement a new transport or fire a
     * virtual event.
     */
    public void fireFilterWrite(IoSession session, WriteRequest writeRequest);

    /**
     * Fires a {@link IoSession#close()} event.  Most users don't need to call this method at
     * all.  Please use this method only when you implement a new transport or fire a virtual
     * event.
     */
    public void fireFilterClose(IoSession session);

    /**
     * Represents a name-filter pair that an {@link IoFilterChain} contains.
     *
     * @author The Apache Directory Project (mina-dev@directory.apache.org)
     * @version $Rev$, $Date$
     */
    public interface Entry {
        /**
         * Returns the name of the filter.
         */
        String getName();

        /**
         * Returns the filter.
         */
        IoFilter getFilter();

        /**
         * Returns the {@link NextFilter} of the filter.
         * 
         * @throws IllegalStateException if the {@link NextFilter} is not available
         */
        NextFilter getNextFilter();
    }
}
