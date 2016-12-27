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
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * A container of {@link IoFilter}s that forwards {@link IoHandler} events
 * to the consisting filters and terminal {@link IoHandler} sequentially.
 * Every {@link IoSession} has its own {@link IoFilterChain} (1-to-1 relationship).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoFilterChain {
    /**
     * @return the parent {@link IoSession} of this chain.
     */
    IoSession getSession();

    /**
     * Returns the {@link Entry} with the specified <tt>name</tt> in this chain.
     * 
     * @param name The filter's name we are looking for
     * @return <tt>null</tt> if there's no such name in this chain
     */
    Entry getEntry(String name);

    /**
     * Returns the {@link Entry} with the specified <tt>filter</tt> in this chain.
     * 
     * @param filter  The Filter we are looking for
     * @return <tt>null</tt> if there's no such filter in this chain
     */
    Entry getEntry(IoFilter filter);

    /**
     * Returns the {@link Entry} with the specified <tt>filterType</tt>
     * in this chain. If there's more than one filter with the specified
     * type, the first match will be chosen.
     * 
     * @param filterType The filter class we are looking for
     * @return <tt>null</tt> if there's no such name in this chain
     */
    Entry getEntry(Class<? extends IoFilter> filterType);

    /**
     * Returns the {@link IoFilter} with the specified <tt>name</tt> in this chain.
     * 
     * @param name the filter's name
     * @return <tt>null</tt> if there's no such name in this chain
     */
    IoFilter get(String name);

    /**
     * Returns the {@link IoFilter} with the specified <tt>filterType</tt>
     * in this chain. If there's more than one filter with the specified
     * type, the first match will be chosen.
     * 
     * @param filterType The filter class
     * @return <tt>null</tt> if there's no such name in this chain
     */
    IoFilter get(Class<? extends IoFilter> filterType);

    /**
     * Returns the {@link NextFilter} of the {@link IoFilter} with the
     * specified <tt>name</tt> in this chain.
     * 
     * @param name The filter's name we want the next filter
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(String name);

    /**
     * Returns the {@link NextFilter} of the specified {@link IoFilter}
     * in this chain.
     * 
     * @param filter The filter for which we want the next filter
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(IoFilter filter);

    /**
     * Returns the {@link NextFilter} of the specified <tt>filterType</tt>
     * in this chain.  If there's more than one filter with the specified
     * type, the first match will be chosen.
     * 
     * @param filterType The Filter class for which we want the next filter
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter(Class<? extends IoFilter> filterType);

    /**
     * @return The list of all {@link Entry}s this chain contains.
     */
    List<Entry> getAll();

    /**
     * @return The reversed list of all {@link Entry}s this chain contains.
     */
    List<Entry> getAllReversed();

    /**
     * @param name The filter's name we are looking for
     * 
     * @return <tt>true</tt> if this chain contains an {@link IoFilter} with the
     * specified <tt>name</tt>.
     */
    boolean contains(String name);

    /**
     * @param filter The filter we are looking for
     * 
     * @return <tt>true</tt> if this chain contains the specified <tt>filter</tt>.
     */
    boolean contains(IoFilter filter);

    /**
     * @param  filterType The filter's class we are looking for
     * 
     * @return <tt>true</tt> if this chain contains an {@link IoFilter} of the
     * specified <tt>filterType</tt>.
     */
    boolean contains(Class<? extends IoFilter> filterType);

    /**
     * Adds the specified filter with the specified name at the beginning of this chain.
     * 
     * @param name The filter's name
     * @param filter The filter to add
     */
    void addFirst(String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name at the end of this chain.
     * 
     * @param name The filter's name
     * @param filter The filter to add
     */
    void addLast(String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name just before the filter whose name is
     * <code>baseName</code> in this chain.
     * 
     * @param baseName The targeted Filter's name
     * @param name The filter's name
     * @param filter The filter to add
     */
    void addBefore(String baseName, String name, IoFilter filter);

    /**
     * Adds the specified filter with the specified name just after the filter whose name is
     * <code>baseName</code> in this chain.
     * 
     * @param baseName The targeted Filter's name
     * @param name The filter's name
     * @param filter The filter to add
     */
    void addAfter(String baseName, String name, IoFilter filter);

    /**
     * Replace the filter with the specified name with the specified new
     * filter.
     *
     * @param name The name of the filter we want to replace
     * @param newFilter The new filter
     * @return the old filter
     */
    IoFilter replace(String name, IoFilter newFilter);

    /**
     * Replace the filter with the specified name with the specified new
     * filter.
     *
     * @param oldFilter The filter we want to replace
     * @param newFilter The new filter
     */
    void replace(IoFilter oldFilter, IoFilter newFilter);

    /**
     * Replace the filter of the specified type with the specified new
     * filter.  If there's more than one filter with the specified type,
     * the first match will be replaced.
     *
     * @param oldFilterType The filter class we want to replace
     * @param newFilter The new filter
     * @return The replaced IoFilter
     */
    IoFilter replace(Class<? extends IoFilter> oldFilterType, IoFilter newFilter);

    /**
     * Removes the filter with the specified name from this chain.
     * 
     * @param name The name of the filter to remove
     * @return The removed filter
     */
    IoFilter remove(String name);

    /**
     * Replace the filter with the specified name with the specified new filter.
     * 
     * @param filter The filter to remove
     */
    void remove(IoFilter filter);

    /**
     * Replace the filter of the specified type with the specified new filter.
     * If there's more than one filter with the specified type, the first match
     * will be replaced.
     * 
     * @param filterType The filter class to remove
     * @return The removed filter
     */
    IoFilter remove(Class<? extends IoFilter> filterType);

    /**
     * Removes all filters added to this chain.
     * 
     * @throws Exception If we weren't able to clear the filters
     */
    void clear() throws Exception;

    /**
     * Fires a {@link IoHandler#sessionCreated(IoSession)} event. Most users don't need to
     * call this method at all. Please use this method only when you implement a new transport
     * or fire a virtual event.
     */
    void fireSessionCreated();

    /**
     * Fires a {@link IoHandler#sessionOpened(IoSession)} event. Most users don't need to call
     * this method at all. Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    void fireSessionOpened();

    /**
     * Fires a {@link IoHandler#sessionClosed(IoSession)} event. Most users don't need to call
     * this method at all. Please use this method only when you implement a new transport or
     * fire a virtual event.
     */
    void fireSessionClosed();

    /**
     * Fires a {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event. Most users don't
     * need to call this method at all. Please use this method only when you implement a new
     * transport or fire a virtual event.
     * 
     * @param status The current status to propagate
     */
    void fireSessionIdle(IdleStatus status);

    /**
     * Fires a {@link IoHandler#messageReceived(IoSession, Object)} event. Most
     * users don't need to call this method at all. Please use this method only
     * when you implement a new transport or fire a virtual event.
     * 
     * @param message The received message
     */
    void fireMessageReceived(Object message);

    /**
     * Fires a {@link IoHandler#messageSent(IoSession, Object)} event. Most
     * users don't need to call this method at all. Please use this method only
     * when you implement a new transport or fire a virtual event.
     * 
     * @param request The sent request
     */
    void fireMessageSent(WriteRequest request);

    /**
     * Fires a {@link IoHandler#exceptionCaught(IoSession, Throwable)} event. Most users don't
     * need to call this method at all. Please use this method only when you implement a new
     * transport or fire a virtual event.
     * 
     * @param cause The exception cause
     */
    void fireExceptionCaught(Throwable cause);

    /**
     * Fires a {@link IoHandler#inputClosed(IoSession)} event. Most users don't
     * need to call this method at all. Please use this method only when you
     * implement a new transport or fire a virtual event.
     */
    void fireInputClosed();

    /**
     * Fires a {@link IoSession#write(Object)} event. Most users don't need to
     * call this method at all. Please use this method only when you implement a
     * new transport or fire a virtual event.
     * 
     * @param writeRequest The message to write
     */
    void fireFilterWrite(WriteRequest writeRequest);

    /**
     * Fires a {@link IoSession#closeNow()} or a {@link IoSession#closeOnFlush()} event. Most users don't need to call this method at
     * all. Please use this method only when you implement a new transport or fire a virtual
     * event.
     */
    void fireFilterClose();

    /**
     * Represents a name-filter pair that an {@link IoFilterChain} contains.
     *
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     */
    interface Entry {
        /**
         * @return the name of the filter.
         */
        String getName();

        /**
         * @return the filter.
         */
        IoFilter getFilter();

        /**
         * @return The {@link NextFilter} of the filter.
         */
        NextFilter getNextFilter();

        /**
         * Adds the specified filter with the specified name just before this entry.
         * 
         * @param name The Filter's name
         * @param filter The added Filter 
         */
        void addBefore(String name, IoFilter filter);

        /**
         * Adds the specified filter with the specified name just after this entry.
         * 
         * @param name The Filter's name
         * @param filter The added Filter 
         */
        void addAfter(String name, IoFilter filter);

        /**
         * Replace the filter of this entry with the specified new filter.
         * 
         * @param newFilter The new filter that will be put in the chain 
         */
        void replace(IoFilter newFilter);

        /**
         * Removes this entry from the chain it belongs to.
         */
        void remove();
    }
}
