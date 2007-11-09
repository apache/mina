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
package org.apache.mina.filter.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterEvent;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * A filter that forwards I/O events to {@link Executor} to enforce a certain
 * thread model while allowing the events per session to be processed
 * simultaneously. You can apply various thread model by inserting this filter
 * to a {@link IoFilterChain}.
 * 
 * <h2>Life Cycle Management</h2>
 * 
 * Please note that this filter doesn't manage the life cycle of the {@link Executor}.
 * If you created this filter using {@link #ExecutorFilter(Executor)} or similar
 * constructor that accepts an {@link Executor} that you've instantiated, you have
 * full control and responsibility of managing its life cycle (e.g. calling
 * {@link ExecutorService#shutdown()}.
 * <p> 
 * If you created this filter using convenience constructors like
 * {@link #ExecutorFilter(int)}, then you can shut down the executor by calling
 * {@link #destroy()} explicitly.
 * 
 * <h2>Event Ordering</h2>
 * 
 * All convenience constructors of this filter creates a new
 * {@link OrderedThreadPoolExecutor} instance.  Therefore, the order of event is
 * maintained like the following:
 * <ul>
 * <li>All event handler methods are called exclusively.
 *     (e.g. messageReceived and messageSent can't be invoked at the same time.)</li>
 * <li>The event order is never mixed up.
 *     (e.g. messageReceived is always invoked before sessionClosed or messageSent.)</li>
 * </ul>
 * However, if you specified other {@link Executor} instance in the constructor,
 * the order of events are not maintained at all.  This means more than one event
 * handler methods can be invoked at the same time with mixed order.  For example,
 * let's assume that messageReceived, messageSent, and sessionClosed events are
 * fired.
 * <ul>
 * <li>All event handler methods can be called simultaneously.
 *     (e.g. messageReceived and messageSent can be invoked at the same time.)</li>
 * <li>The event order can be mixed up.
 *     (e.g. sessionClosed or messageSent can be invoked before messageReceived
 *           is invoked.)</li>
 * </ul>
 * If you need to maintain the order of events per session, please specify an
 * {@link OrderedThreadPoolExecutor} instance or use the convenience constructors.
 * 
 * <h2>Selective Filtering</h2>
 * 
 * By default, all event types but <tt>sessionCreated</tt>, <tt>filterWrite</tt>,
 * <tt>filterClose</tt> and <tt>filterSetTrafficMask</tt> are submitted to the
 * underlying executor, which is most common setting.
 * <p>
 * If you want to submit only a certain set of event types, you can specify them
 * in the constructor.  For example, you could configure a thread pool for
 * write operation for the maximum performance:
 * <pre><code>
 * IoService service = ...;
 * DefaultIoFilterChainBuilder chain = service.getFilterChain();
 * 
 * chain.addLast("codec", new ProtocolCodecFilter(...));
 * // Use one thread pool for most events.
 * chain.addLast("executor1", new ExecutorFilter());
 * // and another dedicated thread pool for 'filterWrite' events.
 * chain.addLast("executor2", new ExecutorFilter(IoEventType.WRITE));
 * </code></pre>
 * 
 * <h2>Preventing {@link OutOfMemoryError}</h2>
 * 
 * Please refer to {@link IoEventQueueThrottle}, which is specified as
 * a parameter of the convenience constructors.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see OrderedThreadPoolExecutor
 * @see UnorderedThreadPoolExecutor
 */
public class ExecutorFilter extends IoFilterAdapter {

    private final EnumSet<IoEventType> eventTypes;
    private final Executor executor;
    private final boolean createdExecutor;

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter() {
        this(16, (IoEventType[]) null);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(int maximumPoolSize) {
        this(0, maximumPoolSize, (IoEventType[]) null);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(int corePoolSize, int maximumPoolSize) {
        this(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS, (IoEventType[]) null);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, (IoEventType[]) null);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            IoEventQueueHandler queueHandler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory(), queueHandler, (IoEventType[]) null);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, null, (IoEventType[]) null);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventQueueHandler queueHandler) {
        this(new OrderedThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, queueHandler), true, (IoEventType[]) null);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(IoEventType... eventTypes) {
        this(16, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(int maximumPoolSize, IoEventType... eventTypes) {
        this(0, maximumPoolSize, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(int corePoolSize, int maximumPoolSize, IoEventType... eventTypes) {
        this(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, IoEventType... eventTypes) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory(), eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            IoEventQueueHandler queueHandler, IoEventType... eventTypes) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory(), queueHandler, eventTypes);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventType... eventTypes) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, null, eventTypes);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventQueueHandler queueHandler, IoEventType... eventTypes) {
        this(new OrderedThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, queueHandler), true, eventTypes);
    }
    
    /**
     * Creates a new instance with the specified {@link Executor}.
     */
    public ExecutorFilter(Executor executor) {
        this(executor, false, (IoEventType[]) null);
    }

    /**
     * Creates a new instance with the specified {@link Executor}.
     */
    public ExecutorFilter(Executor executor, IoEventType... eventTypes) {
        this(executor, false, eventTypes);
    }

    private ExecutorFilter(Executor executor, boolean createdExecutor, IoEventType... eventTypes) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        if (eventTypes == null || eventTypes.length == 0) {
            eventTypes = new IoEventType[] { IoEventType.EXCEPTION_CAUGHT,
                    IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT,
                    IoEventType.SESSION_CLOSED, IoEventType.SESSION_IDLE,
                    IoEventType.SESSION_OPENED, };
        }

        for (IoEventType t : eventTypes) {
            if (t == IoEventType.SESSION_CREATED) {
                throw new IllegalArgumentException(IoEventType.SESSION_CREATED
                        + " is not allowed.");
            }
        }

        this.executor = executor;
        this.createdExecutor = createdExecutor;

        Collection<IoEventType> eventTypeCollection = new ArrayList<IoEventType>(
                eventTypes.length);
        Collections.addAll(eventTypeCollection, eventTypes);
        this.eventTypes = EnumSet.copyOf(eventTypeCollection);
    }
    
    /**
     * Shuts down the underlying executor if this filter is creates via
     * a convenience constrcutor.
     */
    @Override
    public void destroy() {
        if (createdExecutor) {
            ((ExecutorService) executor).shutdown();
        }
    }

    /**
     * Returns the underlying {@link Executor} instance this filter uses.
     */
    public final Executor getExecutor() {
        return executor;
    }

    /**
     * Fires the specified event through the underlying executor.
     */
    protected void fireEvent(IoFilterEvent event) {
        getExecutor().execute(event);
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }

    @Override
    public final void sessionCreated(NextFilter nextFilter, IoSession session) {
        nextFilter.sessionCreated(session);
    }

    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) {
        if (eventTypes.contains(IoEventType.SESSION_OPENED)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.SESSION_OPENED,
                    session, null));
        } else {
            nextFilter.sessionOpened(session);
        }
    }

    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session) {
        if (eventTypes.contains(IoEventType.SESSION_CLOSED)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.SESSION_CLOSED,
                    session, null));
        } else {
            nextFilter.sessionClosed(session);
        }
    }

    @Override
    public final void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) {
        if (eventTypes.contains(IoEventType.SESSION_IDLE)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.SESSION_IDLE,
                    session, status));
        } else {
            nextFilter.sessionIdle(session, status);
        }
    }

    @Override
    public final void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) {
        if (eventTypes.contains(IoEventType.EXCEPTION_CAUGHT)) {
            fireEvent(new IoFilterEvent(nextFilter,
                    IoEventType.EXCEPTION_CAUGHT, session, cause));
        } else {
            nextFilter.exceptionCaught(session, cause);
        }
    }

    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) {
        if (eventTypes.contains(IoEventType.MESSAGE_RECEIVED)) {
            fireEvent(new IoFilterEvent(nextFilter,
                    IoEventType.MESSAGE_RECEIVED, session, message));
        } else {
            nextFilter.messageReceived(session, message);
        }
    }

    @Override
    public final void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (eventTypes.contains(IoEventType.MESSAGE_SENT)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.MESSAGE_SENT,
                    session, writeRequest));
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    @Override
    public final void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (eventTypes.contains(IoEventType.WRITE)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.WRITE, session,
                    writeRequest));
        } else {
            nextFilter.filterWrite(session, writeRequest);
        }
    }

    @Override
    public final void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (eventTypes.contains(IoEventType.CLOSE)) {
            fireEvent(new IoFilterEvent(nextFilter, IoEventType.CLOSE, session,
                    null));
        } else {
            nextFilter.filterClose(session);
        }
    }
}
