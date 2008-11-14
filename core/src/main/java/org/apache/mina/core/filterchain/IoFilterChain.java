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
package org.apache.mina.core.filterchain;

import java.util.List;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * A container of {@link IoFilter}s that forwards {@link IoHandler} events
 * to the consisting filters and terminal {@link IoHandler} sequentially.
 * Every {@link IoSession} has its own {@link IoFilterChain} (1-to-1 relationship).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 593474 $, $Date: 2007-11-09 11:14:12 +0100 (Fri, 09 Nov 2007) $
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
     * Returns the {@link Entry} with the specified <tt>filter</tt> in this chain.
     * @return <tt>null</tt> if there's no such filter in this chain
     */
    Entry getEntry(IoFilter filter);

    /**
     * Returns the {@link Entry} with the specified <tt>filterType</tt>
     * in this chain.  If there's more than one filter with the specified
     * type, the first match will be chosen.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    Entry getEntry(Class<? extends IoFilter> filterType);

    /**
     * Returns the {@link IoFilter} with the specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    IoFilter get(String name);

    /**
     * Returns the {@link IoFilter} with the specified <tt>filterType</tt>
     * in this chain. If there's more than one filter with the specified
     * type, the first match will be chosen.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    IoFilter get(Class<? extends IoFilter> filterType);

    /**
     * Returns the {@link NextFilter} of the {@link IoFilter} with the
     * specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(String name);

    /**
     * Returns the {@link NextFilter} of the specified {@link IoFilter}
     * in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(IoFilter filter);

    /**
     * Returns the {@link NextFilter} of the specified <tt>filterType</tt>
     * in this chain.  If there's more than one filter with the specified
     * type, the first match will be chosen.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(Class<? extends IoFilter> filterType);

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
     * Replace the filter with the specified name with the specified new
     * filter.
     *
     * @return the old filter
     * @throws IllegalArgumentException if there's no such filter
     */
    IoFilter replace(String name, IoFilter newFilter);

    /**
     * Replace the filter with the specified name with the specified new
     * filter.
     *
     * @throws IllegalArgumentException if there's no such filter
     */
    void replace(IoFilter oldFilter, IoFilter newFilter);

    /**
     * Replace the filter of the specified type with the specified new
     * filter.  If there's more than one filter with the specified type,
     * the first match will be replaced.
     *
     * @throws IllegalArgumentException if there's no such filter
     */
    IoFilter replace(Class<? extends IoFilter> oldFilterType, IoFilter newFilter);

    /**
     * Removes the filter with the specified name from this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostRemove(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#destroy()} throws an exception.
     */
    IoFilter remove(String name);

    /**
     * Replace the filter with the specified name with the specified new
     * filter.
     *
     * @throws IllegalArgumentException if there's no such filter
     */
    void remove(IoFilter filter);

    /**
     * Replace the filter of the specified type with the specified new
     * filter.  If there's more than one filter with the specified type,
     * the first match will be replaced.
     *
     * @throws IllegalArgumentException if there's no such filter
     */
    IoFilter remove(Class<? extends IoFilter> filterType);

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
    public void fireSessionCreated();

    /**
     * Fires a {@link IoHandler#sessionOpened(IoSession)} event.  Most users don't need to call
     * this method at all.  Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    public void fireSessionOpened();

    /**
     * Fires a {@link IoHandler#sessionClosed(IoSession)} event.  Most users don't need to call
     * this method at all.  Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    public void fireSessionClosed();

    /**
     * Fires a {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event.  Most users don't
     * need to call this method at all.  Please use this method only when you implement a new
     * transport or fire a virtual event.
     */
    public void fireSessionIdle(IdleStatus status);

    /**
     * Fires a {@link #fireMessageReceived(Object)} event.  Most users don't need to
     * call this method at all.  Please use this method only when you implement a new transport
     * or fire a virtual event.
     */
    public void fireMessageReceived(Object message);

    /**
     * Fires a {@link IoHandler#sessionOpened(IoSession)} event.  Most users don't need to call
     * this method at all.  Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    public void fireMessageSent(WriteRequest request);

    /**
     * Fires a {@link IoHandler#exceptionCaught(IoSession, Throwable)} event.  Most users don't
     * need to call this method at all.  Please use this method only when you implement a new
     * transport or fire a virtual event.
     */
    public void fireExceptionCaught(Throwable cause);

    /**
     * Fires a {@link IoSession#write(Object)} event.  Most users don't need to call this
     * method at all.  Please use this method only when you implement a new transport or fire a
     * virtual event.
     */
    public void fireFilterWrite(WriteRequest writeRequest);

    /**
     * Fires a {@link IoSession#close()} event.  Most users don't need to call this method at
     * all.  Please use this method only when you implement a new transport or fire a virtual
     * event.
     */
    public void fireFilterClose();

    /**
     * Represents a name-filter pair that an {@link IoFilterChain} contains.
     *
     * @author The Apache MINA Project (dev@mina.apache.org)
     * @version $Rev: 593474 $, $Date: 2007-11-09 11:14:12 +0100 (Fri, 09 Nov 2007) $
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
        
        /**
         * Adds the specified filter with the specified name just before this entry.
         * @throws IoFilterLifeCycleException
         *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
         *             {@link IoFilter#init()} throws an exception.
         */
        void addBefore(String name, IoFilter filter);

        /**
         * Adds the specified filter with the specified name just after this entry.
         * @throws IoFilterLifeCycleException
         *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
         *             {@link IoFilter#init()} throws an exception.
         */
        void addAfter(String name, IoFilter filter);

        /**
         * Replace the filter of this entry with the specified new filter.
         *
         * @throws IllegalArgumentException if there's no such filter
         */
        void replace(IoFilter newFilter);
        
        /**
         * Removes this entry from the chain it belongs to.
         */
        void remove();
    }
}
