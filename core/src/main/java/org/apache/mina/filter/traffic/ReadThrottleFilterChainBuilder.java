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
package org.apache.mina.filter.traffic;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.executor.AbstractExecutorFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.util.CopyOnWriteMap;

/**
 * An {@link IoFilterChainBuilder} that configures an {IoFilterChain} or
 * {@link DefaultIoFilterChainBuilder} to control incoming traffic to
 * prevent a unwanted {@link OutOfMemoryError} under heavy load.
 * <p>
 * The filters that this builder inserts will automatically disable reads
 * on an {@link IoSession} once the data batched for that session in the
 * {@link ExecutorFilter} reaches a defined threshold. It accomplishes this
 * by adding one filter before the {@link Executor} and the other after the
 * {@link ExecutorFilter}.
 * <p>
 * The size of the received data is calculated by {@link MessageSizeEstimator}.
 * If you are using a transport whose envelope is not an {@link IoBuffer},
 * you could write your own {@link MessageSizeEstimator} for better traffic
 * calculation.  However, the {@link DefaultMessageSizeEstimator} will suffice
 * in most cases.
 * <p>
 * It is recommended to use this builder at the end of your filter chain
 * construction because it is possible to subvert the behavior of the added
 * filters by adding filters immediately after the {@link ExecutorFilter}
 * after using this builder, consequently leading to a unexpected behavior.
 *
 * <h3>Usage</h3>
 * <pre><code>
 * DefaultFilterChainBuilder chain = ...
 * ReadThrottleFilterChainBuilder builder = new ReadThrottleFilterChainBuilder();
 * filter.buildFilerChain( chain );
 * </code></pre>
 *
 * or
 *
 * <pre><code>
 * IoFilterChain chain = ...
 * ReadThrottleFilterChainBuilder builder = new ReadThrottleFilterChainBuilder();
 * filter.buildFilerChain( chain );
 * </code></pre>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * TODO Provide per-service limitation
 * FIXME May not be added more than once
 */
public class ReadThrottleFilterChainBuilder implements IoFilterChainBuilder {
    
    private static final AtomicInteger globalBufferSize = new AtomicInteger();
    private static final Map<IoService, AtomicInteger> serviceBufferSizes =
        new CopyOnWriteMap<IoService, AtomicInteger>();
    
    private static final AttributeKey STATE =
        new AttributeKey(ReadThrottleFilterChainBuilder.class, "state");

    /**
     * Returns the current amount of data in the buffer of the {@link ExecuorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder.
     */
    public static int getGlobalBufferSize() {
        return globalBufferSize.get();
    }
    
    public static int getServiceBufferSize(IoService service) {
        AtomicInteger answer = serviceBufferSizes.get(service);
        if (answer == null) {
            return 0;
        } else {
            return answer.get();
        }
    }
    
    /**
     * Returns the current amount of data in the buffer of the {@link ExecutorFilter}
     * for the specified {@link IoSession}.
     */
    public static int getSessionBufferSize(IoSession session) {
        State state = (State) session.getAttribute(STATE);
        if (state == null) {
            return 0;
        }
        
        synchronized (state) {
            return state.sessionBufferSize;
        }
    }
    
    private final IoFilter enterFilter = new EnterFilter();
    private final IoFilter exitFilter = new ExitFilter();
    
    private final MessageSizeEstimator messageSizeEstimator;
    private volatile int maxSessionBufferSize;
    private volatile int maxServiceBufferSize;
    private volatile int maxGlobalBufferSize;
    
    /**
     * Creates a new instance with 64KB <tt>maxSessionBufferSize</tt>,
     * 128MB <tt>maxGlobalBufferSize</tt> and a new {@link DefaultMessageSizeEstimator}.
     */
    public ReadThrottleFilterChainBuilder() {
        // 64KB, 128MB
        this(65536, 1048576 * 128);
    }
    
    /**
     * Creates a new instance with the specified <tt>maxSessionBufferSize</tt>,
     * <tt>maxGlobalBufferSize</tt> and a new {@link DefaultMessageSizeEstimator}.
     * 
     */
    public ReadThrottleFilterChainBuilder(int maxSessionBufferSize, int maxGlobalBufferSize) {
        this(maxSessionBufferSize, maxGlobalBufferSize, null);
    }

    /**
     * Creates a new instance with the specified <tt>maxSessionBufferSize</tt>,
     * <tt>maxGlobalBufferSize</tt> and {@link MessageSizeEstimator}.
     * 
     * @param maxSessionBufferSize the maximum amount of data in the buffer of
     *                           the {@link ExecutorFilter} per {@link IoSession}.
     *                           Specify {@code 0} or a smaller value to disable.
     * @param maxGlobalBufferSize the maximum amount of data in the buffer of
     *                            the {@link ExecutorFilter} for all {@link IoSession}
     *                            whose {@link IoFilterChain} has been configured by
     *                            this builder.
     *                            Specify {@code 0} or a smaller value to disable.
     * @param messageSizeEstimator the message size estimator. If {@code null},
     *                             a new {@link DefaultMessageSizeEstimator} is created.
     */
    public ReadThrottleFilterChainBuilder(
            int maxSessionBufferSize, int maxGlobalBufferSize, MessageSizeEstimator messageSizeEstimator) {
        if (messageSizeEstimator == null) {
            messageSizeEstimator = new DefaultMessageSizeEstimator();
        }
        this.messageSizeEstimator = messageSizeEstimator;
        setMaxSessionBufferSize(maxSessionBufferSize);
        setMaxGlobalBufferSize(maxGlobalBufferSize);
    }

    /**
     * Returns the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * per {@link IoSession}.  {@code 0} means 'disabled'.
     */
    public int getMaxSessionBufferSize() {
        return maxSessionBufferSize;
    }
    
    public int getMaxServiceBufferSize() {
        return maxServiceBufferSize;
    }
    
    /**
     * Returns the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder. {@code 0} means 'disabled'.
     */
    public int getMaxGlobalBufferSize() {
        return maxGlobalBufferSize;
    }
    
    /**
     * Sets the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * per {@link IoSession}.  Specify {@code 0} or a smaller value to disable.
     */
    public void setMaxSessionBufferSize(int maxSessionBufferSize) {
        if (maxSessionBufferSize < 0) {
            maxSessionBufferSize = 0;
        }
        this.maxSessionBufferSize = maxSessionBufferSize;
    }

    public void setMaxServiceBufferSize(int maxServiceBufferSize) {
        if (maxServiceBufferSize < 0) {
            maxServiceBufferSize = 0;
        }
        this.maxServiceBufferSize = maxServiceBufferSize;
    }

    /**
     * Sets the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder. Specify {@code 0} or a smaller value to disable.
     */
    public void setMaxGlobalBufferSize(int maxGlobalBufferSize) {
        if (maxGlobalBufferSize < 0) {
            maxGlobalBufferSize = 0;
        }
        this.maxGlobalBufferSize = maxGlobalBufferSize;
    }
    
    /**
     * Returns the size estimator currently in use.
     */
    public MessageSizeEstimator getMessageSizeEstimator() {
        return messageSizeEstimator;
    }
    
    /**
     * Attaches the two inflow traffic controlling filters before and after the
     * specified <tt>filterEntry</tt>.  <tt>filterEntry</tt> doesn't necessarily
     * need to be an {@link ExecutorFilter}, just in case you implemented your
     * own threading filter.
     */
    public void buildFilterChain(IoFilterChain.Entry filterEntry) {
        if (filterEntry == null) {
            throw new NullPointerException("filterEntry");
        }
        
        filterEntry.addBefore(filterEntry.getName() + ".readThrottle", enterFilter);
        filterEntry.addAfter(filterEntry.getName() + ".readThrottleExit", exitFilter);
    }

    /**
     * Attaches the two inflow traffic controlling filters before and after the
     * {@link ExecutorFilter} of the specified <tt>chain</tt>.  If there are more
     * than one {@link ExecutorFilter}s in the chain, the first one is chosen.
     */
    public void buildFilterChain(IoFilterChain chain) {
        if (chain == null) {
            throw new NullPointerException("chain");
        }
        
        IoFilterChain.Entry entry = chain.getEntry(AbstractExecutorFilter.class);
        if (entry == null) {
            throw new IllegalStateException(
                    "The specified chain does not contain an " + ExecutorFilter.class.getName());
        }
        buildFilterChain(entry);
    }
    
    /**
     * Attaches the two inflow traffic controlling filters before and after the
     * {@link ExecutorFilter} of the specified <tt>chain</tt>.  If there are more
     * than one {@link ExecutorFilter}s in the chain, the first one is chosen.
     */
    public void buildFilterChain(DefaultIoFilterChainBuilder chain) {
        if (chain == null) {
            throw new NullPointerException("chain");
        }
        
        IoFilterChain.Entry entry = chain.getEntry(AbstractExecutorFilter.class);
        if (entry == null) {
            throw new IllegalStateException(
                    "The specified chain does not contain an " + ExecutorFilter.class.getName());
        }
        buildFilterChain(chain.getEntry(AbstractExecutorFilter.class));
    }
    
    private void enter(IoSession session, int size) {
        State state = getState(session);
        
        int globalBufferSize = ReadThrottleFilterChainBuilder.globalBufferSize.addAndGet(size);
        
        int maxGlobalBufferSize = this.maxGlobalBufferSize;
        int maxSessionBufferSize = this.maxSessionBufferSize;
        
        synchronized (state) {
            int bufferSize = (state.sessionBufferSize += size);
            if ((maxSessionBufferSize != 0 && bufferSize >= maxSessionBufferSize) ||
                (maxGlobalBufferSize != 0 && globalBufferSize >= maxGlobalBufferSize)) {
                session.suspendRead();
                state.suspendedRead = true;
            }
        }
    }

    private void exit(IoSession session, int size) {
        State state = getState(session);

        int globalBufferSize = ReadThrottleFilterChainBuilder.globalBufferSize.addAndGet(-size);
        if (globalBufferSize < 0) {
            ReadThrottleFilterChainBuilder.globalBufferSize.set(0);
            throw new IllegalStateException("globalBufferSize < 0");
        }
        
        int maxGlobalBufferSize = this.maxGlobalBufferSize;
        int maxSessionBufferSize = this.maxSessionBufferSize;

        synchronized (state) {
            int bufferSize = (state.sessionBufferSize -= size);
            if (bufferSize < 0) {
                state.sessionBufferSize = 0;
                throw new IllegalStateException("sessionBufferSize < 0");
            }
    
            if (state.suspendedRead &&
                (maxSessionBufferSize == 0 || bufferSize < maxSessionBufferSize) &&
                (maxGlobalBufferSize == 0 || globalBufferSize < maxGlobalBufferSize)) {
                session.resumeRead();
                state.suspendedRead = false;
            }
        }
    }

    private State getState(IoSession session) {
        State state = (State) session.getAttribute(STATE);
        if (state == null) {
            state = new State();
            State oldState = (State) session.setAttributeIfAbsent(STATE, state);
            if (oldState != null) {
                state = oldState;
            }
        }
        return state;
    }
    
    private class EnterFilter extends IoFilterAdapter {
        @Override
        public void onPreRemove(
                IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
            // Remove the exit filter together.
            try {
                parent.remove(exitFilter);
            } catch (Exception e) {
                // Ignore.
            }
        }
        
        @Override
        public void onPostRemove(
                IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
            parent.getSession().removeAttribute(STATE);
        }

        @Override
        public void messageReceived(
                NextFilter nextFilter, IoSession session, Object message) throws Exception {
            enter(session, messageSizeEstimator.estimateSize(message));
            nextFilter.messageReceived(session, message);
        }
    }

    private class ExitFilter extends IoFilterAdapter {
        @Override
        public void onPostRemove(
                IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
            // Remove the enter filter together.
            try {
                parent.remove(enterFilter);
            } catch (Exception e) {
                // Ignore.
            }
        }

        @Override
        public void messageReceived(
                NextFilter nextFilter, IoSession session, Object message) throws Exception {
            exit(session, messageSizeEstimator.estimateSize(message));
            nextFilter.messageReceived(session, message);
        }
        
        @Override
        public String toString() {
            return String.valueOf(getGlobalBufferSize()) + '/' + getMaxGlobalBufferSize();
        }
    }
    
    private static class State {
        private int sessionBufferSize;
        private boolean suspendedRead;
    }
}
