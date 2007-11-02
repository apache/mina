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

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.executor.AbstractExecutorFilter;
import org.apache.mina.filter.executor.ExecutorFilter;

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
 */
public class ReadThrottleFilterChainBuilder implements IoFilterChainBuilder {
    private final AttributeKey STATE = new AttributeKey(getClass(), "state");

    private final IoFilter enterFilter = new EnterFilter();
    private final IoFilter exitFilter = new ExitFilter();
    private final AtomicInteger globalBufferSize = new AtomicInteger();
    
    private final MessageSizeEstimator messageSizeEstimator;
    private volatile int localMaxBufferSize;
    private volatile int globalMaxBufferSize;
    
    /**
     * Creates a new instance with 64KB <tt>localMaxBufferSize</tt>,
     * 128MB <tt>globalMaxBufferSize</tt> and a new {@link DefaultMessageSizeEstimator}.
     */
    public ReadThrottleFilterChainBuilder() {
        // localMax = 64KB, globalMax = 128MB
        this(65536, 1048576 * 128);
    }
    
    /**
     * Creates a new instance with the specified <tt>localMaxBufferSize</tt>,
     * <tt>globalMaxBufferSize</tt> and a new {@link DefaultMessageSizeEstimator}.
     * 
     */
    public ReadThrottleFilterChainBuilder(int localMaxBufferSize, int globalMaxBufferSize) {
        this(localMaxBufferSize, globalMaxBufferSize, null);
    }

    /**
     * Creates a new instance with the specified <tt>localMaxBufferSize</tt>,
     * <tt>globalMaxBufferSize</tt> and {@link MessageSizeEstimator}.
     * 
     * @param localMaxBufferSize the maximum amount of data in the buffer of
     *                           the {@link ExecutorFilter} per {@link IoSession}.
     *                           Specify {@code 0} or a smaller value to disable.
     * @param globalMaxBufferSize the maximum amount of data in the buffer of
     *                            the {@link ExecutorFilter} for all {@link IoSession}
     *                            whose {@link IoFilterChain} has been configured by
     *                            this builder.
     *                            Specify {@code 0} or a smaller value to disable.
     * @param messageSizeEstimator the message size estimator. If {@code null},
     *                             a new {@link DefaultMessageSizeEstimator} is created.
     */
    public ReadThrottleFilterChainBuilder(
            int localMaxBufferSize, int globalMaxBufferSize, MessageSizeEstimator messageSizeEstimator) {
        if (messageSizeEstimator == null) {
            messageSizeEstimator = new DefaultMessageSizeEstimator();
        }
        this.messageSizeEstimator = messageSizeEstimator;
        setLocalMaxBufferSize(localMaxBufferSize);
        setGlobalMaxBufferSize(globalMaxBufferSize);
    }

    /**
     * Returns the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * per {@link IoSession}.  {@code 0} means 'disabled'.
     */
    public int getLocalMaxBufferSize() {
        return localMaxBufferSize;
    }
    
    /**
     * Returns the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder. {@code 0} means 'disabled'.
     */
    public int getGlobalMaxBufferSize() {
        return globalMaxBufferSize;
    }
    
    /**
     * Returns the current amount of data in the buffer of the {@link ExecuorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder.
     */
    public int getGlobalBufferSize() {
        return globalBufferSize.get();
    }
    
    /**
     * Returns the current amount of data in the buffer of the {@link ExecutorFilter}
     * for the specified {@link IoSession}.
     */
    public int getLocalBufferSize(IoSession session) {
        State state = (State) session.getAttribute(STATE);
        if (state == null) {
            return 0;
        }
        
        synchronized (state) {
            return state.bufferSize;
        }
    }
    
    /**
     * Sets the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * per {@link IoSession}.  Specify {@code 0} or a smaller value to disable.
     */
    public void setLocalMaxBufferSize(int localMaxBufferSize) {
        if (localMaxBufferSize < 0) {
            localMaxBufferSize = 0;
        }
        this.localMaxBufferSize = localMaxBufferSize;
    }

    /**
     * Sets the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder. Specify {@code 0} or a smaller value to disable.
     */
    public void setGlobalMaxBufferSize(int globalMaxBufferSize) {
        if (globalMaxBufferSize < 0) {
            globalMaxBufferSize = 0;
        }
        this.globalMaxBufferSize = globalMaxBufferSize;
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
    
    private void acquire(IoSession session, int size) {
        State state = getState(session);
        
        int globalBufferSize = this.globalBufferSize.addAndGet(size);
        
        int globalMaxBufferSize = this.globalMaxBufferSize;
        int localMaxBufferSize = this.localMaxBufferSize;
        
        synchronized (state) {
            int bufferSize = (state.bufferSize += size);
            if ((localMaxBufferSize != 0 && bufferSize >= localMaxBufferSize) ||
                (globalMaxBufferSize != 0 && globalBufferSize >= globalMaxBufferSize)) {
                session.suspendRead();
                state.suspendedRead = true;
            }
        }
    }

    private void release(IoSession session, int size) {
        State state = getState(session);

        int globalBufferSize = this.globalBufferSize.addAndGet(-size);
        if (globalBufferSize < 0) {
            this.globalBufferSize.set(0);
            throw new IllegalStateException("globalBufferSize < 0");
        }
        
        int globalMaxBufferSize = this.globalMaxBufferSize;
        int localMaxBufferSize = this.localMaxBufferSize;

        synchronized (state) {
            int bufferSize = (state.bufferSize -= size);
            if (bufferSize < 0) {
                state.bufferSize = 0;
                throw new IllegalStateException("bufferSize < 0");
            }
    
            if (state.suspendedRead &&
                (localMaxBufferSize == 0 || bufferSize < localMaxBufferSize) &&
                (globalMaxBufferSize == 0 || globalBufferSize < globalMaxBufferSize)) {
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
    
    private class EnterFilter extends IoFilterAdapter implements ReadThrottleFilter {
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
            acquire(session, messageSizeEstimator.estimateSize(message));
            nextFilter.messageReceived(session, message);
        }

        public int getGlobalBufferSize() {
            return ReadThrottleFilterChainBuilder.this.getGlobalBufferSize();
        }

        public int getGlobalMaxBufferSize() {
            return ReadThrottleFilterChainBuilder.this.getGlobalMaxBufferSize();
        }

        public int getLocalBufferSize(IoSession session) {
            return ReadThrottleFilterChainBuilder.this.getLocalBufferSize(session);
        }

        public int getLocalMaxBufferSize() {
            return ReadThrottleFilterChainBuilder.this.getLocalMaxBufferSize();
        }

        public MessageSizeEstimator getMessageSizeEstimator() {
            return ReadThrottleFilterChainBuilder.this.getMessageSizeEstimator();
        }

        public void setGlobalMaxBufferSize(int globalMaxBufferSize) {
            ReadThrottleFilterChainBuilder.this.setGlobalMaxBufferSize(globalMaxBufferSize);
        }

        public void setLocalMaxBufferSize(int localMaxBufferSize) {
            ReadThrottleFilterChainBuilder.this.setLocalMaxBufferSize(localMaxBufferSize);
        }
        
        @Override
        public String toString() {
            return String.valueOf(getGlobalBufferSize()) + '/' + getGlobalMaxBufferSize();
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
            release(session, messageSizeEstimator.estimateSize(message));
            nextFilter.messageReceived(session, message);
        }
        
        @Override
        public String toString() {
            return String.valueOf(getGlobalBufferSize()) + '/' + getGlobalMaxBufferSize();
        }
    }
    
    private static class State {
        private int bufferSize;
        private boolean suspendedRead;
    }
}
