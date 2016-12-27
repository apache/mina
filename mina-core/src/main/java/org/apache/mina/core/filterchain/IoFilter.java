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

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * A filter which intercepts {@link IoHandler} events like Servlet
 * filters.  Filters can be used for these purposes:
 * <ul>
 *   <li>Event logging,</li>
 *   <li>Performance measurement,</li>
 *   <li>Authorization,</li>
 *   <li>Overload control,</li>
 *   <li>Message transformation (e.g. encryption and decryption, ...),</li>
 *   <li>and many more.</li>
 * </ul>
 * <p>
 * <strong>Please NEVER implement your filters to wrap
 * {@link IoSession}s.</strong> Users can cache the reference to the
 * session, which might malfunction if any filters are added or removed later.
 *
 * <h3>The Life Cycle</h3>
 * {@link IoFilter}s are activated only when they are inside {@link IoFilterChain}.
 * <p>
 * When you add an {@link IoFilter} to an {@link IoFilterChain}:
 * <ol>
 *   <li>{@link #init()} is invoked by {@link ReferenceCountingFilter} if
 *       the filter is added at the first time.</li>
 *   <li>{@link #onPreAdd(IoFilterChain, String, NextFilter)} is invoked to notify
 *       that the filter will be added to the chain.</li>
 *   <li>The filter is added to the chain, and all events and I/O requests
 *       pass through the filter from now.</li>
 *   <li>{@link #onPostAdd(IoFilterChain, String, NextFilter)} is invoked to notify
 *       that the filter is added to the chain.</li>
 *   <li>The filter is removed from the chain if {@link #onPostAdd(IoFilterChain, String, org.apache.mina.core.filterchain.IoFilter.NextFilter)}
 *       threw an exception.  {@link #destroy()} is also invoked by
 *       {@link ReferenceCountingFilter} if the filter is the last filter which
 *       was added to {@link IoFilterChain}s.</li>
 * </ol>
 * <p>
 * When you remove an {@link IoFilter} from an {@link IoFilterChain}:
 * <ol>
 *   <li>{@link #onPreRemove(IoFilterChain, String, NextFilter)} is invoked to
 *       notify that the filter will be removed from the chain.</li>
 *   <li>The filter is removed from the chain, and any events and I/O requests
 *       don't pass through the filter from now.</li>
 *   <li>{@link #onPostRemove(IoFilterChain, String, NextFilter)} is invoked to
 *       notify that the filter is removed from the chain.</li>
 *   <li>{@link #destroy()} is invoked by {@link ReferenceCountingFilter} if
 *       the removed filter was the last one.</li>
 * </ol>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see IoFilterAdapter
 */
public interface IoFilter {
    /**
     * Invoked by {@link ReferenceCountingFilter} when this filter
     * is added to a {@link IoFilterChain} at the first time, so you can
     * initialize shared resources.  Please note that this method is never
     * called if you don't wrap a filter with {@link ReferenceCountingFilter}.
     * 
     * @throws Exception If an error occurred while processing the event
     */
    void init() throws Exception;

    /**
     * Invoked by {@link ReferenceCountingFilter} when this filter
     * is not used by any {@link IoFilterChain} anymore, so you can destroy
     * shared resources.  Please note that this method is never called if
     * you don't wrap a filter with {@link ReferenceCountingFilter}.
     * 
     * @throws Exception If an error occurred while processing the event
     */
    void destroy() throws Exception;

    /**
     * Invoked before this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.  This method is not
     * invoked before {@link #init()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked after this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.  This method is not
     * invoked before {@link #init()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked before this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Invoked after this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param nextFilter the {@link NextFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     * @throws Exception If an error occurred while processing the event
     */
    void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception;

    /**
     * Filters {@link IoHandler#sessionCreated(IoSession)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionOpened(IoSession)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionClosed(IoSession)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#sessionIdle(IoSession,IdleStatus)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param status The {@link IdleStatus} type
     * @throws Exception If an error occurred while processing the event
     */
    void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception;

    /**
     * Filters {@link IoHandler#exceptionCaught(IoSession,Throwable)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param cause The exception that cause this event to be received
     * @throws Exception If an error occurred while processing the event
     */
    void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception;

    /**
     * Filters {@link IoHandler#inputClosed(IoSession)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @throws Exception If an error occurred while processing the event
     */
    void inputClosed(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoHandler#messageReceived(IoSession,Object)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param message The received message
     * @throws Exception If an error occurred while processing the event
     */
    void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception;

    /**
     * Filters {@link IoHandler#messageSent(IoSession,Object)} event.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has received this event
     * @param writeRequest The {@link WriteRequest} that contains the sent message
     * @throws Exception If an error occurred while processing the event
     */
    void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception;

    /**
     * Filters {@link IoSession#closeNow()} or a {@link IoSession#closeOnFlush()} method invocations.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session
     *            The {@link IoSession} which has to process this method
     *            invocation
     * @throws Exception If an error occurred while processing the event
     */
    void filterClose(NextFilter nextFilter, IoSession session) throws Exception;

    /**
     * Filters {@link IoSession#write(Object)} method invocation.
     * 
     * @param nextFilter
     *            the {@link NextFilter} for this filter. You can reuse this
     *            object until this filter is removed from the chain.
     * @param session The {@link IoSession} which has to process this invocation
     * @param writeRequest The {@link WriteRequest} to process
     * @throws Exception If an error occurred while processing the event
     */
    void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception;

    /**
     * Represents the next {@link IoFilter} in {@link IoFilterChain}.
     */
    interface NextFilter {
        /**
         * Forwards <tt>sessionCreated</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
        void sessionCreated(IoSession session);

        /**
         * Forwards <tt>sessionOpened</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
        void sessionOpened(IoSession session);

        /**
         * Forwards <tt>sessionClosed</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
        void sessionClosed(IoSession session);

        /**
         * Forwards <tt>sessionIdle</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param status The {@link IdleStatus} type
         */
        void sessionIdle(IoSession session, IdleStatus status);

        /**
         * Forwards <tt>exceptionCaught</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param cause The exception that cause this event to be received
         */
        void exceptionCaught(IoSession session, Throwable cause);

        /**
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
        void inputClosed(IoSession session);

        /**
         * Forwards <tt>messageReceived</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param message The received message
         */
        void messageReceived(IoSession session, Object message);

        /**
         * Forwards <tt>messageSent</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param writeRequest The {@link WriteRequest} to process
         */
        void messageSent(IoSession session, WriteRequest writeRequest);

        /**
         * Forwards <tt>filterWrite</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         * @param writeRequest The {@link WriteRequest} to process
         */
        void filterWrite(IoSession session, WriteRequest writeRequest);

        /**
         * Forwards <tt>filterClose</tt> event to next filter.
         * 
         * @param session The {@link IoSession} which has to process this invocation
         */
        void filterClose(IoSession session);

    }
}
