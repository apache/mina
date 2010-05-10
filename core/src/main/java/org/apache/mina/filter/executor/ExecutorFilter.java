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

import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 * @see OrderedThreadPoolExecutor
 * @see UnorderedThreadPoolExecutor
 * @org.apache.xbean.XBean
 */
public class ExecutorFilter extends IoFilterAdapter {
    /** The list of handled events */
    private EnumSet<IoEventType> eventTypes;
    
    /** The associated executor */
    private Executor executor;
    
    /** A flag set if the executor can be managed */ 
    private boolean manageableExecutor;
    
    /** The default pool size */
    private static final int DEFAULT_MAX_POOL_SIZE = 16;
    
    /** The number of thread to create at startup */
    private static final int BASE_THREAD_NUMBER = 0;
    
    /** The default KeepAlive time, in seconds */
    private static final long DEFAULT_KEEPALIVE_TIME = 30;
    
    /** 
     * A set of flags used to tell if the Executor has been created 
     * in the constructor or passed as an argument. In the second case, 
     * the executor state can be managed.
     **/
    private static final boolean MANAGEABLE_EXECUTOR = true;
    private static final boolean NOT_MANAGEABLE_EXECUTOR = false;
    
    /** A list of default EventTypes to be handled by the executor */
    private static IoEventType[] DEFAULT_EVENT_SET = new IoEventType[] {
        IoEventType.EXCEPTION_CAUGHT,
        IoEventType.MESSAGE_RECEIVED, 
        IoEventType.MESSAGE_SENT,
        IoEventType.SESSION_CLOSED, 
        IoEventType.SESSION_IDLE,
        IoEventType.SESSION_OPENED
    };

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}, no thread in the pool, and a 
     * maximum of 16 threads in the pool. All the event will be handled 
     * by this default executor.
     */
    public ExecutorFilter() {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            BASE_THREAD_NUMBER,
            DEFAULT_MAX_POOL_SIZE,
            DEFAULT_KEEPALIVE_TIME,
            TimeUnit.SECONDS,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}, no thread in the pool, but 
     * a maximum of threads in the pool is given. All the event will be handled 
     * by this default executor.
     * 
     * @param maximumPoolSize The maximum pool size
     */
    public ExecutorFilter(int maximumPoolSize) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            BASE_THREAD_NUMBER,
            maximumPoolSize,
            DEFAULT_KEEPALIVE_TIME,
            TimeUnit.SECONDS,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}, a number of thread to start with, a  
     * maximum of threads the pool can contain. All the event will be handled 
     * by this default executor.
     *
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     */
    public ExecutorFilter(int corePoolSize, int maximumPoolSize) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            DEFAULT_KEEPALIVE_TIME,
            TimeUnit.SECONDS,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     */
    public ExecutorFilter(int corePoolSize, int maximumPoolSize, long keepAliveTime, 
            TimeUnit unit) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param queueHandler The queue used to store events
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            IoEventQueueHandler queueHandler) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            Executors.defaultThreadFactory(),
            queueHandler);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param threadFactory The factory used to create threads
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            threadFactory,
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param threadFactory The factory used to create threads
     * @param queueHandler The queue used to store events
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventQueueHandler queueHandler) {
        // Create a new default Executor
        Executor executor = new OrderedThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, queueHandler);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            BASE_THREAD_NUMBER,
            DEFAULT_MAX_POOL_SIZE,
            DEFAULT_KEEPALIVE_TIME,
            TimeUnit.SECONDS,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param maximumPoolSize The maximum pool size
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(int maximumPoolSize, IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            BASE_THREAD_NUMBER,
            maximumPoolSize,
            DEFAULT_KEEPALIVE_TIME,
            TimeUnit.SECONDS,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(int corePoolSize, int maximumPoolSize, IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            DEFAULT_KEEPALIVE_TIME,
            TimeUnit.SECONDS,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, 
            IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            Executors.defaultThreadFactory(),
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }
    
    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param queueHandler The queue used to store events
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            IoEventQueueHandler queueHandler, IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            Executors.defaultThreadFactory(),
            queueHandler);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param threadFactory The factory used to create threads
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = createDefaultExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            threadFactory,
            null);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }

    /**
     * (Convenience constructor) Creates a new instance with a new
     * {@link OrderedThreadPoolExecutor}.
     * 
     * @param corePoolSize The initial pool size
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param threadFactory The factory used to create threads
     * @param queueHandler The queue used to store events
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(
            int corePoolSize, int maximumPoolSize, 
            long keepAliveTime, TimeUnit unit,
            ThreadFactory threadFactory, IoEventQueueHandler queueHandler, 
            IoEventType... eventTypes) {
        // Create a new default Executor
        Executor executor = new OrderedThreadPoolExecutor(corePoolSize, maximumPoolSize, 
            keepAliveTime, unit, threadFactory, queueHandler);
        
        // Initialize the filter
        init(executor, MANAGEABLE_EXECUTOR, eventTypes);
    }
    
    /**
     * Creates a new instance with the specified {@link Executor}.
     * 
     * @param executor the user's managed Executor to use in this filter
     */
    public ExecutorFilter(Executor executor) {
        // Initialize the filter
        init(executor, NOT_MANAGEABLE_EXECUTOR);
    }

    /**
     * Creates a new instance with the specified {@link Executor}.
     * 
     * @param executor the user's managed Executor to use in this filter
     * @param eventTypes The event for which the executor will be used
     */
    public ExecutorFilter(Executor executor, IoEventType... eventTypes) {
        // Initialize the filter
        init(executor, NOT_MANAGEABLE_EXECUTOR, eventTypes);
    }
    
    /**
     * Create an OrderedThreadPool executor.
     *
     * @param corePoolSize The initial pool sizePoolSize
     * @param maximumPoolSize The maximum pool size
     * @param keepAliveTime Default duration for a thread
     * @param unit Time unit used for the keepAlive value
     * @param threadFactory The factory used to create threads
     * @param queueHandler The queue used to store events
     * @return An instance of the created Executor
     */
    private Executor createDefaultExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
        TimeUnit unit, ThreadFactory threadFactory, IoEventQueueHandler queueHandler) {
        // Create a new Executor
        Executor executor = new OrderedThreadPoolExecutor(corePoolSize, maximumPoolSize, 
            keepAliveTime, unit, threadFactory, queueHandler);
        
        return executor;
    }
    
    /**
     * Create an EnumSet from an array of EventTypes, and set the associated
     * eventTypes field.
     *
     * @param eventTypes The array of handled events
     */
    private void initEventTypes(IoEventType... eventTypes) {
        if ((eventTypes == null) || (eventTypes.length == 0)) {
            eventTypes = DEFAULT_EVENT_SET;
        }

        // Copy the list of handled events in the event set
        this.eventTypes = EnumSet.of(eventTypes[0], eventTypes);
        
        // Check that we don't have the SESSION_CREATED event in the set
        if (this.eventTypes.contains( IoEventType.SESSION_CREATED )) {
            this.eventTypes = null;
            throw new IllegalArgumentException(IoEventType.SESSION_CREATED
                + " is not allowed.");
        }
    }

    /**
     * Creates a new instance of ExecutorFilter. This private constructor is called by all
     * the public constructor.
     *
     * @param executor The underlying {@link Executor} in charge of managing the Thread pool.
     * @param manageableExecutor Tells if the Executor's Life Cycle can be managed or not
     * @param eventTypes The lit of event which are handled by the executor
     * @param
     */
    private void init(Executor executor, boolean manageableExecutor, IoEventType... eventTypes) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }

        initEventTypes(eventTypes);
        this.executor = executor;
        this.manageableExecutor = manageableExecutor;
    }
    
    /**
     * Shuts down the underlying executor if this filter hase been created via
     * a convenience constructor.
     */
    @Override
    public void destroy() {
        if (manageableExecutor) {
            ((ExecutorService) executor).shutdown();
        }
    }

    /**
     * Returns the underlying {@link Executor} instance this filter uses.
     * 
     * @return The underlying {@link Executor}
     */
    public final Executor getExecutor() {
        return executor;
    }

    /**
     * Fires the specified event through the underlying executor.
     * 
     * @param event The filtered event
     */
    protected void fireEvent(IoFilterEvent event) {
        executor.execute(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) {
        if (eventTypes.contains(IoEventType.SESSION_OPENED)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter, IoEventType.SESSION_OPENED,
                session, null); 
            fireEvent(event);
        } else {
            nextFilter.sessionOpened(session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session) {
        if (eventTypes.contains(IoEventType.SESSION_CLOSED)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter, IoEventType.SESSION_CLOSED,
                session, null); 
            fireEvent(event);
        } else {
            nextFilter.sessionClosed(session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) {
        if (eventTypes.contains(IoEventType.SESSION_IDLE)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter, IoEventType.SESSION_IDLE,
                session, status); 
            fireEvent(event);
        } else {
            nextFilter.sessionIdle(session, status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) {
        if (eventTypes.contains(IoEventType.EXCEPTION_CAUGHT)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter,
                IoEventType.EXCEPTION_CAUGHT, session, cause); 
            fireEvent(event);
        } else {
            nextFilter.exceptionCaught(session, cause);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) {
        if (eventTypes.contains(IoEventType.MESSAGE_RECEIVED)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter,
                IoEventType.MESSAGE_RECEIVED, session, message); 
            fireEvent(event);
        } else {
            nextFilter.messageReceived(session, message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (eventTypes.contains(IoEventType.MESSAGE_SENT)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter, IoEventType.MESSAGE_SENT,
                session, writeRequest); 
            fireEvent(event);
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        if (eventTypes.contains(IoEventType.WRITE)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter, IoEventType.WRITE, session,
                writeRequest); 
            fireEvent(event);
        } else {
            nextFilter.filterWrite(session, writeRequest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (eventTypes.contains(IoEventType.CLOSE)) {
            IoFilterEvent event = new IoFilterEvent(nextFilter, IoEventType.CLOSE, session,
                null); 
            fireEvent(event);
        } else {
            nextFilter.filterClose(session);
        }
    }
}
