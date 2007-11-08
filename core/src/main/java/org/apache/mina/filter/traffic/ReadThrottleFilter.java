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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.common.TrafficMask;
import org.apache.mina.filter.executor.AbstractExecutorFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.util.CopyOnWriteMap;
import org.slf4j.Logger;

/**
 * An {@link IoFilter} that throttles incoming traffic to
 * prevent a unwanted {@link OutOfMemoryError} under heavy load.
 * <p>
 * This filter will automatically disable reads on an {@link IoSession} once
 * the amount of the read data batched for that session in the {@link ExecutorFilter}
 * reaches a defined threshold. It accomplishes this by adding one filter before the
 * {@link ExecutorFilter}.
 * <p>
 * The size of the received data is calculated by {@link MessageSizeEstimator}.
 * If you are using a transport whose envelope is not an {@link IoBuffer},
 * you could write your own {@link MessageSizeEstimator} for better traffic
 * calculation.  However, the {@link DefaultMessageSizeEstimator} will suffice
 * in most cases.
 * <p>
 * It is recommended to add this filter at the end of your filter chain
 * configuration because it is possible to subvert the behavior of the added
 * filters by adding a filter immediately before/after the {@link ExecutorFilter}
 * after inserting this builder, consequently leading to a unexpected behavior.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ReadThrottleFilter extends IoFilterAdapter {
    
    private static final AtomicInteger globalBufferSize = new AtomicInteger();
    private static final Map<IoService, AtomicInteger> serviceBufferSizes =
        new CopyOnWriteMap<IoService, AtomicInteger>();
    
    private static final Object globalResumeLock = new Object();
    private static long lastGlobalResumeTime = 0;
    
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
    
    private static int increaseServiceBufferSize(IoService service, int increment) {
        AtomicInteger serviceBufferSize = serviceBufferSizes.get(service);
        if (serviceBufferSize == null) {
            synchronized (serviceBufferSizes) {
                serviceBufferSize = serviceBufferSizes.get(service);
                if (serviceBufferSize == null) {
                    serviceBufferSize = new AtomicInteger(increment);
                    serviceBufferSizes.put(service, serviceBufferSize);
                    return increment;
                }
            }
        }
        return serviceBufferSize.addAndGet(increment);
    }
    
    private static void resetServiceBufferSize(IoService service) {
        serviceBufferSizes.remove(service);
    }
    
    private final AttributeKey STATE =
        new AttributeKey(ReadThrottleFilter.class, "state");

    private volatile ReadThrottlePolicy policy;
    private final MessageSizeEstimator messageSizeEstimator;
    
    private volatile int maxSessionBufferSize;
    private volatile int maxServiceBufferSize;
    private volatile int maxGlobalBufferSize;
    
    private final IoFilter enterFilter = new EnterFilter();

    /**
     * Creates a new instance with 64KB <tt>maxSessionBufferSize</tt>,
     * 128MB <tt>maxGlobalBufferSize</tt> and a new {@link DefaultMessageSizeEstimator}.
     */
    public ReadThrottleFilter() {
        this(ReadThrottlePolicy.LOG);
    }
    
    public ReadThrottleFilter(ReadThrottlePolicy policy) {
        this(policy, null);
    }
    
    public ReadThrottleFilter(ReadThrottlePolicy policy, MessageSizeEstimator messageSizeEstimator) {
        // 64KB, 64MB, 128MB.
        this(policy, messageSizeEstimator, 65536, 1048576 * 64, 1048576 * 128);
    }
    
    /**
     * Creates a new instance with the specified <tt>maxSessionBufferSize</tt>,
     * <tt>maxGlobalBufferSize</tt> and a new {@link DefaultMessageSizeEstimator}.
     */
    public ReadThrottleFilter(int maxSessionBufferSize, int maxServiceBufferSize, int maxGlobalBufferSize) {
        this(ReadThrottlePolicy.LOG, maxSessionBufferSize, maxServiceBufferSize, maxGlobalBufferSize);
    }

    public ReadThrottleFilter(
            ReadThrottlePolicy policy,
            int maxSessionBufferSize, int maxServiceBufferSize, int maxGlobalBufferSize) {
        this(policy, null, maxSessionBufferSize, maxServiceBufferSize, maxGlobalBufferSize);
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
    public ReadThrottleFilter(
            ReadThrottlePolicy policy, MessageSizeEstimator messageSizeEstimator,
            int maxSessionBufferSize, int maxServiceBufferSize, int maxGlobalBufferSize) {
        if (messageSizeEstimator == null) {
            messageSizeEstimator = new DefaultMessageSizeEstimator();
        }
        this.messageSizeEstimator = messageSizeEstimator;
        setPolicy(policy);
        setMaxSessionBufferSize(maxSessionBufferSize);
        setMaxServiceBufferSize(maxServiceBufferSize);
        setMaxGlobalBufferSize(maxGlobalBufferSize);
    }

    public ReadThrottlePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(ReadThrottlePolicy policy) {
        if (policy == null) {
            throw new NullPointerException("policy");
        }
        
        this.policy = policy;
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
     * Returns the current amount of data in the buffer of the {@link ExecutorFilter}
     * for the specified {@link IoSession}.
     */
    public int getSessionBufferSize(IoSession session) {
        State state = (State) session.getAttribute(STATE);
        if (state == null) {
            return 0;
        }
        
        synchronized (state) {
            return state.sessionBufferSize;
        }
    }
    
    @Override
    public void onPreAdd(
            IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (!parent.contains(AbstractExecutorFilter.class)) {
            throw new IllegalStateException(
                    "At least one " + ExecutorFilter.class.getName() + " must exist in the chain.");
        }
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }
    
    @Override
    public void onPostAdd(
            IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        
        // My previous filter must be an ExecutorFilter.
        IoFilter lastFilter = null;
        for (IoFilterChain.Entry e: parent.getAll()) {
            IoFilter currentFilter = e.getFilter();
            if (currentFilter == this) {
                if (lastFilter instanceof AbstractExecutorFilter) {
                    // Good!
                    break;
                } else {
                    throw new IllegalStateException(
                            ReadThrottleFilter.class.getName() + " must be placed after " +
                            "an " + ExecutorFilter.class.getName() + " in the chain");
                }
            }
            
            lastFilter = currentFilter;
        }
        
        // Add an entering filter before the ExecutorFilter.
        parent.getEntry(lastFilter).addBefore(name + ".preprocessor", enterFilter);
    }

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
    public void filterSetTrafficMask(NextFilter nextFilter, IoSession session,
            TrafficMask trafficMask) throws Exception {
        
        if (trafficMask.isReadable()) {
            State state = getState(session);
            boolean suspendedRead;
            synchronized (state) {
                suspendedRead = state.suspendedRead;
            }
            
            // Suppress resumeRead() if read is suspended by this filter.
            if (suspendedRead) {
                trafficMask = trafficMask.and(TrafficMask.WRITE);
            }
        }
        
        nextFilter.filterSetTrafficMask(session, trafficMask);
    }

    private class EnterFilter extends IoFilterAdapter {
        @Override
        public void onPreRemove(
                IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
            // Remove the exit filter together.
            try {
                parent.remove(ReadThrottleFilter.this);
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

    private void enter(IoSession session, int size) {
        State state = getState(session);
        Logger logger = IoSessionLogger.getLogger(session, getClass());

        int globalBufferSize = ReadThrottleFilter.globalBufferSize.addAndGet(size);
        int serviceBufferSize = increaseServiceBufferSize(session.getService(), size);

        int maxGlobalBufferSize = this.maxGlobalBufferSize;
        int maxServiceBufferSize = this.maxServiceBufferSize;
        int maxSessionBufferSize = this.maxSessionBufferSize;
        
        ReadThrottlePolicy policy = getPolicy();
        
        boolean enforcePolicy = false;
        int sessionBufferSize;
        synchronized (state) {
            sessionBufferSize = (state.sessionBufferSize += size);
            if ((maxSessionBufferSize != 0 && sessionBufferSize >= maxSessionBufferSize) ||
                (maxServiceBufferSize != 0 && serviceBufferSize >= maxServiceBufferSize) ||
                (maxGlobalBufferSize  != 0 && globalBufferSize  >= maxGlobalBufferSize)) {
                enforcePolicy = true;
                switch (policy) {
                case EXCEPTION:
                case BLOCK:
                    state.suspendedRead = true;
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(getMessage(session, "  Entered - "));
        }
        
        if (enforcePolicy) {
            switch (policy) {
            case CLOSE:
                log(session, state);
                session.close();
                raiseException(session);
                break;
            case EXCEPTION:
                suspend(session, state, logger);
                raiseException(session);
                break;
            case BLOCK:
                suspend(session, state, logger);
                break;
            case LOG:
                log(session, state);
                break;
            }
        }
    }

    private void suspend(IoSession session, State state, Logger logger) {
        log(session, state);
        session.suspendRead();
        if (logger.isDebugEnabled()) {
            logger.debug(getMessage(session, "Suspended - "));
        }
    }
    
    private void exit(IoSession session, int size) {
        State state = getState(session);
        Logger logger = IoSessionLogger.getLogger(session, getClass());

        int globalBufferSize = ReadThrottleFilter.globalBufferSize.addAndGet(-size);
        if (globalBufferSize < 0) {
            ReadThrottleFilter.globalBufferSize.set(0);
            throw new IllegalStateException("globalBufferSize < 0");
        }
        
        int serviceBufferSize = increaseServiceBufferSize(session.getService(), -size);
        if (serviceBufferSize < 0) {
            resetServiceBufferSize(session.getService());
            throw new IllegalStateException("serviceBufferSize < 0");
        }

        int maxGlobalBufferSize = this.maxGlobalBufferSize;
        int maxServiceBufferSize = this.maxServiceBufferSize;
        int maxSessionBufferSize = this.maxSessionBufferSize;
        
        int sessionBufferSize;
        
        boolean enforcePolicy = false;
        synchronized (state) {
            sessionBufferSize = (state.sessionBufferSize -= size);
            if (sessionBufferSize < 0) {
                state.sessionBufferSize = sessionBufferSize = 0;
                throw new IllegalStateException("sessionBufferSize < 0");
            }
            if (state.suspendedRead &&
                (maxGlobalBufferSize == 0 || globalBufferSize < maxGlobalBufferSize) &&
                (maxServiceBufferSize == 0 || serviceBufferSize < maxServiceBufferSize) &&
                (maxSessionBufferSize == 0 || sessionBufferSize < maxSessionBufferSize)) {
                state.suspendedRead = false;
                enforcePolicy = true;
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(getMessage(session, "   Exited - "));
        }
        
        if (enforcePolicy) {
            session.resumeRead();
            if (logger.isDebugEnabled()) {
                logger.debug(getMessage(session, "  Resumed - "));
            }
        }
        
        resumeOthers();
    }
    
    private void resumeOthers() {
        long currentTime = System.currentTimeMillis();
        
        // Try to resume other sessions every other second.
        boolean resumeOthers;
        synchronized (globalResumeLock) {
            if (currentTime - lastGlobalResumeTime > 1000) {
                lastGlobalResumeTime = currentTime;
                resumeOthers = true;
            } else {
                resumeOthers = false;
            }
        }
        
        if (resumeOthers) {
            int maxGlobalBufferSize = this.maxGlobalBufferSize;
            if (maxGlobalBufferSize == 0 || globalBufferSize.get() < maxGlobalBufferSize) {
                for (IoService service: serviceBufferSizes.keySet()) {
                    resumeService(service);
                }
            }
            
            synchronized (globalResumeLock) {
                lastGlobalResumeTime = System.currentTimeMillis();
            }
        }
    }
    
    private void resumeService(IoService service) {
        int maxServiceBufferSize = this.maxServiceBufferSize;
        if (maxServiceBufferSize == 0 || getServiceBufferSize(service) < maxServiceBufferSize) {
            for (IoSession session: service.getManagedSessions()) {
                resume(session);
            }
        }
    }
    
    private void resume(IoSession session) {
        State state = (State) session.getAttribute(STATE);
        if (state == null) {
            return;
        }
        
        int maxSessionBufferSize = this.maxSessionBufferSize;
        boolean resume = false;
        synchronized (state) {
            if (state.suspendedRead &&
                (maxSessionBufferSize == 0 || state.sessionBufferSize < maxSessionBufferSize)) {
                state.suspendedRead = false;
                resume = true;
            }
        }

        if (resume) {
            session.resumeRead();
            Logger logger = IoSessionLogger.getLogger(session, getClass());
            if (logger.isDebugEnabled()) {
                logger.debug(getMessage(session, "  Resumed - "));
            }
        }
    }

    private void log(IoSession session, State state) {
        long currentTime = System.currentTimeMillis();
        
        // Prevent log flood by logging every 3 seconds.
        boolean log;
        synchronized (state.logLock) {
            if (currentTime - state.lastLogTime > 3000) {
                state.lastLogTime = currentTime;
                log = true;
            } else {
                log = false;
            }
        }
        
        if (log) {
            IoSessionLogger.getLogger(session, getClass()).warn(getMessage(session));
        }
    }
    
    private void raiseException(IoSession session) {
        throw new ReadFloodException(getMessage(session));
    }
    
    private String getMessage(IoSession session) {
        return getMessage(session, "Read buffer flooded - ");
    }
    
    private String getMessage(IoSession session, String prefix) {
        int  sessionLimit = maxSessionBufferSize;
        int  serviceLimit = maxServiceBufferSize;
        int  globalLimit  = maxGlobalBufferSize;

        StringBuilder buf = new StringBuilder(512);
        buf.append(prefix);
        buf.append("session: ");
        if (sessionLimit != 0) {
            buf.append(getSessionBufferSize(session));
            buf.append(" / ");
            buf.append(sessionLimit);
            buf.append(" bytes, ");
        } else {
            buf.append(getSessionBufferSize(session));
            buf.append(" / unlimited bytes, ");
        }
        
        buf.append("service: ");
        if (serviceLimit != 0) {
            buf.append(getServiceBufferSize(session.getService()));
            buf.append(" / ");
            buf.append(serviceLimit);
            buf.append(" bytes, ");
        } else {
            buf.append(getServiceBufferSize(session.getService()));
            buf.append(" / unlimited bytes, ");
        }
        
        buf.append("global: ");
        if (globalLimit != 0) {
            buf.append(getGlobalBufferSize());
            buf.append(" / ");
            buf.append(globalLimit);
            buf.append(" bytes.");
        } else {
            buf.append(getGlobalBufferSize());
            buf.append(" / unlimited bytes.");
        }
        
        return buf.toString();
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
    
    @Override
    public String toString() {
        return String.valueOf(getGlobalBufferSize()) + '/' + getMaxGlobalBufferSize();
    }

    private static class State {
        private int sessionBufferSize;
        private boolean suspendedRead;

        private final Object logLock = new Object();
        private long lastLogTime = 0;
    }
}
