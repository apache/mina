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

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.TrafficMask;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.util.ReferenceCountingFilter;

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
 *   <li>{@link #onPreAdd(IoFilterChain, String, IoFilter)} is invoked to notify
 *       that the filter will be added to the chain.</li>
 *   <li>The filter is added to the chain, and all events and I/O requests
 *       pass through the filter from now.</li>
 *   <li>{@link #onPostAdd(IoFilterChain, String, IoFilter)} is invoked to notify
 *       that the filter is added to the chain.</li>
 *   <li>The filter is removed from the chain if {@link #onPostAdd(IoFilterChain, String, IoFilter)}
 *       threw an exception.  {@link #destroy()} is also invoked by
 *       {@link ReferenceCountingFilter} if the filter is the last filter which
 *       was added to {@link IoFilterChain}s.</li>
 * </ol>
 * <p>
 * When you remove an {@link IoFilter} from an {@link IoFilterChain}:
 * <ol>
 *   <li>{@link #onPreRemove(IoFilterChain, String, IoFilter)} is invoked to
 *       notify that the filter will be removed from the chain.</li>
 *   <li>The filter is removed from the chain, and any events and I/O requests
 *       don't pass through the filter from now.</li>
 *   <li>{@link #onPostRemove(IoFilterChain, String, IoFilter)} is invoked to
 *       notify that the filter is removed from the chain.</li>
 *   <li>{@link #destroy()} is invoked by {@link ReferenceCountingFilter} if
 *       the removed filter was the last one.</li>
 * </ol>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 591770 $, $Date: 2007-11-04 13:22:44 +0100 (Sun, 04 Nov 2007) $
 *
 * @see IoFilterAdapter
 */
public interface IoFilter {
    /**
     * Invoked by {@link ReferenceCountingFilter} when this filter
     * is added to a {@link IoFilterChain} at the first time, so you can
     * initialize shared resources.  Please note that this method is never
     * called if you don't wrap a filter with {@link ReferenceCountingFilter}.
     */
    void init() throws Exception;

    /**
     * Invoked by {@link ReferenceCountingFilter} when this filter
     * is not used by any {@link IoFilterChain} anymore, so you can destroy
     * shared resources.  Please note that this method is never called if
     * you don't wrap a filter with {@link ReferenceCountingFilter}.
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
     * @param filter the next {@link IoFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPreAdd(IoSession session, int index, String name, IoFilter filter)
            throws Exception;

    /**
     * Invoked after this filter is added to the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is added to more than one parents.  This method is not
     * invoked before {@link #init()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param filter the next {@link IoFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPostAdd(IoSession session, int index, String name, IoFilter filter)
            throws Exception;

    /**
     * Invoked before this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param filter the {@link IoFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPreRemove(IoSession session, int index, String name, IoFilter filter)
            throws Exception;

    /**
     * Invoked after this filter is removed from the specified <tt>parent</tt>.
     * Please note that this method can be invoked more than once if
     * this filter is removed from more than one parents.
     * This method is always invoked before {@link #destroy()} is invoked.
     *
     * @param parent the parent who called this method
     * @param name the name assigned to this filter
     * @param filter the {@link IoFilter} for this filter.  You can reuse
     *                   this object until this filter is removed from the chain.
     */
    void onPostRemove(IoSession session, int index, String name, IoFilter filter)
            throws Exception;

    /**
     * Filters {@link IoHandler#sessionCreated(IoSession)} event.
     */
    void sessionCreated(int index, IoSession session)
            throws Exception;

    /**
     * Filters {@link IoHandler#sessionOpened(IoSession)} event.
     */
    void sessionOpened(int index, IoSession session)
            throws Exception;

    /**
     * Filters {@link IoHandler#sessionClosed(IoSession)} event.
     */
    void sessionClosed(int index, IoSession session)
            throws Exception;

    /**
     * Filters {@link IoHandler#sessionIdle(IoSession,IdleStatus)}
     * event.
     */
    void sessionIdle(int index, IoSession session, IdleStatus status)
            throws Exception;

    /**
     * Filters {@link IoHandler#exceptionCaught(IoSession,Throwable)}
     * event.
     */
    void exceptionCaught(int index, IoSession session,
            Throwable cause) throws Exception;

    /**
     * Filters {@link IoHandler#messageReceived(IoSession,Object)}
     * event.
     */
    void messageReceived(int index, IoSession session,
            Object message) throws Exception;

    /**
     * Filters {@link IoHandler#messageSent(IoSession,Object)}
     * event.
     */
    void messageSent(int index, IoSession session,
            WriteRequest writeRequest) throws Exception;

    /**
     * Filters {@link IoSession#close()} method invocation.
     */
    void filterClose(int index, IoSession session) throws Exception;

    /**
     * Filters {@link IoSession#write(Object)} method invocation.
     */
    void filterWrite(int index, IoSession session,
            WriteRequest writeRequest) throws Exception;
    
    /**
     * Filters {@link IoSession#setTrafficMask(TrafficMask)} method invocation.
     */
    void filterSetTrafficMask(
    		int index, IoSession session, TrafficMask trafficMask) throws Exception;
    
    /**
     * @return The filter's name
     */
    String getName();
}
