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
package org.apache.mina.filter.statistic;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * This class will measure the time it takes for a
 * method in the {@link IoFilterAdapter} class to execute.  The basic
 * premise of the logic in this class is to get the current time
 * at the beginning of the method, call method on nextFilter, and
 * then get the current time again.  An example of how to use
 * the filter is:
 *
 * <pre>
 * ProfilerTimerFilter profiler = new ProfilerTimerFilter(
 *         TimeUnit.MILLISECOND, IoEventType.MESSAGE_RECEIVED);
 * chain.addFirst("Profiler", profiler);
 * </pre>
 * 
 * The profiled {@link IoEventType} are :
 * <ul>
 * <li>IoEventType.MESSAGE_RECEIVED</li>
 * <li>IoEventType.MESSAGE_SENT</li>
 * <li>IoEventType.SESSION_CREATED</li>
 * <li>IoEventType.SESSION_OPENED</li>
 * <li>IoEventType.SESSION_IDLE</li>
 * <li>IoEventType.SESSION_CLOSED</li>
 * </ul>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class ProfilerTimerFilter extends IoFilterAdapter {
    /** TRhe selected time unit */
    private volatile TimeUnit timeUnit;
    
    /** A TimerWorker for the MessageReceived events */
    private TimerWorker messageReceivedTimerWorker;
    
    /** A flag to tell the filter that the MessageReceived must be profiled */
    private boolean profileMessageReceived = false;

    /** A TimerWorker for the MessageSent events */
    private TimerWorker messageSentTimerWorker;

    /** A flag to tell the filter that the MessageSent must be profiled */
    private boolean profileMessageSent = false;

    /** A TimerWorker for the SessionCreated events */
    private TimerWorker sessionCreatedTimerWorker;

    /** A flag to tell the filter that the SessionCreated must be profiled */
    private boolean profileSessionCreated = false;

    /** A TimerWorker for the SessionOpened events */
    private TimerWorker sessionOpenedTimerWorker;

    /** A flag to tell the filter that the SessionOpened must be profiled */
    private boolean profileSessionOpened = false;

    /** A TimerWorker for the SessionIdle events */
    private TimerWorker sessionIdleTimerWorker;

    /** A flag to tell the filter that the SessionIdle must be profiled */
    private boolean profileSessionIdle = false;

    /** A TimerWorker for the SessionClosed events */
    private TimerWorker sessionClosedTimerWorker;

    /** A flag to tell the filter that the SessionClosed must be profiled */
    private boolean profileSessionClosed = false;

    /**
     * Creates a new instance of ProfilerFilter.  This is the
     * default constructor and will print out timings for
     * messageReceived and messageSent and the time increment
     * will be in milliseconds.
     */
    public ProfilerTimerFilter() {
        this(
                TimeUnit.MILLISECONDS, 
                IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT);
    }
    
    /**
     * Creates a new instance of ProfilerFilter.  This is the
     * default constructor and will print out timings for
     * messageReceived and messageSent.
     * 
     * @param timeUnit the time increment to set
     */
    public ProfilerTimerFilter(TimeUnit timeUnit) {
        this(
                timeUnit, 
                IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT);
    }
    
    /**
     * Creates a new instance of ProfilerFilter.  An example
     * of this call would be:
     *
     * <pre>
     * new ProfilerTimerFilter(
     *         TimeUnit.MILLISECONDS,
     *         IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT);
     * </pre>
     * 
     * Note : you can add as many {@link IoEventType} as you want. The method accepts
     * a variable number of arguments.
     * 
     * @param timeUnit Used to determine the level of precision you need in your timing.
     * @param eventTypes A list of {@link IoEventType} representation of the methods to profile
     */
    public ProfilerTimerFilter(TimeUnit timeUnit, IoEventType... eventTypes) {
        this.timeUnit = timeUnit;

        setProfilers(eventTypes);
    }
    
    /**
     * Create the profilers for a list of {@link IoEventType}.
     * 
     * @param eventTypes the list of {@link IoEventType} to profile
     */
    private void setProfilers(IoEventType... eventTypes) {
        for (IoEventType type : eventTypes) {
            switch (type) {
                case MESSAGE_RECEIVED :
                    messageReceivedTimerWorker = new TimerWorker();
                    profileMessageReceived = true;
                    break;

                case MESSAGE_SENT :
                    messageSentTimerWorker = new TimerWorker();
                    profileMessageSent = true;
                    break;

                case SESSION_CREATED :
                    sessionCreatedTimerWorker = new TimerWorker();
                    profileSessionCreated = true;
                    break;
                    
                case SESSION_OPENED :
                    sessionOpenedTimerWorker = new TimerWorker();
                    profileSessionOpened = true;
                    break;
                    
                case SESSION_IDLE :
                    sessionIdleTimerWorker = new TimerWorker();
                    profileSessionIdle = true;
                    break;
                    
                case SESSION_CLOSED :
                    sessionClosedTimerWorker = new TimerWorker();
                    profileSessionClosed = true;
                    break;
            }
        }
    }

    /**
     * Sets the {@link TimeUnit} being used.
     *
     * @param timeUnit the new {@link TimeUnit} to be used.
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * Set the {@link IoEventType} to be profiled
     *
     * @param type The {@link IoEventType} to profile
     */
    public void profile(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                profileMessageReceived = true;
                
                if (messageReceivedTimerWorker == null) {
                    messageReceivedTimerWorker = new TimerWorker();
                }
                
                return;
                
            case MESSAGE_SENT :
                profileMessageSent = true;
                
                if (messageSentTimerWorker == null) {
                    messageSentTimerWorker = new TimerWorker();
                }
                
                return;
                
            case SESSION_CREATED :
                profileSessionCreated = true;
                
                if (sessionCreatedTimerWorker == null) {
                    sessionCreatedTimerWorker = new TimerWorker();
                }
                
                return;
                
            case SESSION_OPENED :
                profileSessionOpened = true;
                
                if (sessionOpenedTimerWorker == null) {
                    sessionOpenedTimerWorker = new TimerWorker();
                }
                
                return;
                
            case SESSION_IDLE :
                profileSessionIdle = true;
                
                if (sessionIdleTimerWorker == null) {
                    sessionIdleTimerWorker = new TimerWorker();
                }
                
                return;
                
            case SESSION_CLOSED :
                profileSessionClosed = true;
                
                if (sessionClosedTimerWorker == null) {
                    sessionClosedTimerWorker = new TimerWorker();
                }
                
                return;
        }
    }

    /**
     * Stop profiling an {@link IoEventType}
     *
     * @param type The {@link IoEventType} to stop profiling
     */
    public void stopProfile(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                profileMessageReceived = false;
                return;
                
            case MESSAGE_SENT :
                profileMessageSent = false;
                return;
                
            case SESSION_CREATED :
                profileSessionCreated = false;
                return;

            case SESSION_OPENED :
                profileSessionOpened = false;
                return;

            case SESSION_IDLE :
                profileSessionIdle = false;
                return;

            case SESSION_CLOSED :
                profileSessionClosed = false;
                return;
        }
    }

    /**
     * Return the set of {@link IoEventType} which are profiled.
     *
     * @return a Set containing all the profiled {@link IoEventType} 
     */
    public Set<IoEventType> getEventsToProfile() {
        Set<IoEventType> set = new HashSet<IoEventType>();
        
        if ( profileMessageReceived ) {
            set.add(IoEventType.MESSAGE_RECEIVED);
        }
        
        if ( profileMessageSent) {
            set.add(IoEventType.MESSAGE_SENT);
        }
        
        if ( profileSessionCreated ) {
            set.add(IoEventType.SESSION_CREATED);
        }
        
        if ( profileSessionOpened ) {
            set.add(IoEventType.SESSION_OPENED);
        }
        
        if ( profileSessionIdle ) {
            set.add(IoEventType.SESSION_IDLE);
        }
        
        if ( profileSessionClosed ) {
            set.add(IoEventType.SESSION_CLOSED);
        }
        
        return set;
    }

    /**
     * Set the profilers for a list of {@link IoEventType}
     * 
     * @param eventTypes the list of {@link IoEventType} to profile
     */
    public void setEventsToProfile(IoEventType... eventTypes) {
        setProfilers(eventTypes);
    }

    /**
     * Profile a MessageReceived event. This method will gather the following
     * informations :
     * - the method duration
     * - the shortest execution time
     * - the slowest execution time
     * - the average execution time
     * - the global number of calls
     * 
     * @param nextFilter The filter to call next
     * @param session The associated session
     * @param message the received message
     */
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (profileMessageReceived) {
            long start = timeNow();
            nextFilter.messageReceived(session, message);
            long end = timeNow();
            messageReceivedTimerWorker.addNewDuration(end - start);
        } else {
            nextFilter.messageReceived(session, message);
        }
    }

    /**
     * Profile a MessageSent event. This method will gather the following
     * informations :
     * - the method duration
     * - the shortest execution time
     * - the slowest execution time
     * - the average execution time
     * - the global number of calls
     * 
     * @param nextFilter The filter to call next
     * @param session The associated session
     * @param writeRequest the sent message
     */
    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        if (profileMessageSent) {
            long start = timeNow();
            nextFilter.messageSent(session, writeRequest);
            long end = timeNow();
            messageSentTimerWorker.addNewDuration(end - start);
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    /**
     * Profile a SessionCreated event. This method will gather the following
     * informations :
     * - the method duration
     * - the shortest execution time
     * - the slowest execution time
     * - the average execution time
     * - the global number of calls
     * 
     * @param nextFilter The filter to call next
     * @param session The associated session
     */
    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (profileSessionCreated) {
            long start = timeNow();
            nextFilter.sessionCreated(session);
            long end = timeNow();
            sessionCreatedTimerWorker.addNewDuration(end - start);
        } else {
            nextFilter.sessionCreated(session);
        }
    }

    /**
     * Profile a SessionOpened event. This method will gather the following
     * informations :
     * - the method duration
     * - the shortest execution time
     * - the slowest execution time
     * - the average execution time
     * - the global number of calls
     * 
     * @param nextFilter The filter to call next
     * @param session The associated session
     */
    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (profileSessionOpened) {
            long start = timeNow();
            nextFilter.sessionOpened(session);
            long end = timeNow();
            sessionOpenedTimerWorker.addNewDuration(end - start);
        } else {
            nextFilter.sessionOpened(session);
        }
    }

    /**
     * Profile a SessionIdle event. This method will gather the following
     * informations :
     * - the method duration
     * - the shortest execution time
     * - the slowest execution time
     * - the average execution time
     * - the global number of calls
     * 
     * @param nextFilter The filter to call next
     * @param session The associated session
     * @param status The session's status
     */
    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        if (profileSessionIdle) {
            long start = timeNow();
            nextFilter.sessionIdle(session, status);
            long end = timeNow();
            sessionIdleTimerWorker.addNewDuration(end - start);
        } else {
            nextFilter.sessionIdle(session, status);
        }
    }

    /**
     * Profile a SessionClosed event. This method will gather the following
     * informations :
     * - the method duration
     * - the shortest execution time
     * - the slowest execution time
     * - the average execution time
     * - the global number of calls
     * 
     * @param nextFilter The filter to call next
     * @param session The associated session
     */
    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (profileSessionClosed) {
            long start = timeNow();
            nextFilter.sessionClosed(session);
            long end = timeNow();
            sessionClosedTimerWorker.addNewDuration(end - start);
        } else {
            nextFilter.sessionClosed(session);
        }
    }

    /**
     * Get the average time for the specified method represented by the {@link IoEventType}
     *
     * @param type
     *  The {@link IoEventType} that the user wants to get the average method call time
     * @return
     *  The average time it took to execute the method represented by the {@link IoEventType}
     */
    public double getAverageTime(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                if (profileMessageReceived) {
                    return messageReceivedTimerWorker.getAverage();
                }
                
                break;
                
            case MESSAGE_SENT :
                if (profileMessageSent) {
                    return messageSentTimerWorker.getAverage();
                }
                
                break;
                
            case SESSION_CREATED :
                if (profileSessionCreated) {
                    return sessionCreatedTimerWorker.getAverage();
                }
                
                break;
                
            case SESSION_OPENED :
                if (profileSessionOpened) {
                    return sessionOpenedTimerWorker.getAverage();
                }
                
                break;
                
            case SESSION_IDLE :
                if (profileSessionIdle) {
                    return sessionIdleTimerWorker.getAverage();
                }
                
                break;
                
            case SESSION_CLOSED :
                if (profileSessionClosed) {
                    return sessionClosedTimerWorker.getAverage();
                }
                
                break;
        }

        throw new IllegalArgumentException(
                "You are not monitoring this event.  Please add this event first.");
    }

    /**
     * Gets the total number of times the method has been called that is represented by the
     * {@link IoEventType}
     *
     * @param type
     *  The {@link IoEventType} that the user wants to get the total number of method calls
     * @return
     *  The total number of method calls for the method represented by the {@link IoEventType}
     */
    public long getTotalCalls(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                if (profileMessageReceived) {
                    return messageReceivedTimerWorker.getCallsNumber();
                }
                
                break;
                
            case MESSAGE_SENT :
                if (profileMessageSent) {
                    return messageSentTimerWorker.getCallsNumber();
                }
                
                break;
                
            case SESSION_CREATED :
                if (profileSessionCreated) {
                    return sessionCreatedTimerWorker.getCallsNumber();
                }
                
                break;
                
            case SESSION_OPENED :
                if (profileSessionOpened) {
                    return sessionOpenedTimerWorker.getCallsNumber();
                }
                
                break;
                
            case SESSION_IDLE :
                if (profileSessionIdle) {
                    return sessionIdleTimerWorker.getCallsNumber();
                }
                
                break;
                
            case SESSION_CLOSED :
                if (profileSessionClosed) {
                    return sessionClosedTimerWorker.getCallsNumber();
                }
                
                break;
        }
    
        throw new IllegalArgumentException(
                "You are not monitoring this event.  Please add this event first.");
    }

    /**
     * The total time this method has been executing
     *
     * @param type
     *  The {@link IoEventType} that the user wants to get the total time this method has
     *  been executing
     * @return
     *  The total time for the method represented by the {@link IoEventType}
     */
    public long getTotalTime(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                if (profileMessageReceived) {
                    return messageReceivedTimerWorker.getTotal();
                }
                
                break;
                
            case MESSAGE_SENT :
                if (profileMessageSent) {
                    return messageSentTimerWorker.getTotal();
                }
                
                break;
                
            case SESSION_CREATED :
                if (profileSessionCreated) {
                    return sessionCreatedTimerWorker.getTotal();
                }
                
                break;
                
            case SESSION_OPENED :
                if (profileSessionOpened) {
                    return sessionOpenedTimerWorker.getTotal();
                }
                
                break;
                
            case SESSION_IDLE :
                if (profileSessionIdle) {
                    return sessionIdleTimerWorker.getTotal();
                }
                
                break;
                
            case SESSION_CLOSED :
                if (profileSessionClosed) {
                    return sessionClosedTimerWorker.getTotal();
                }
                
                break;
        }
    
        throw new IllegalArgumentException(
                "You are not monitoring this event.  Please add this event first.");
    }

    /**
     * The minimum time the method represented by {@link IoEventType} has executed
     *
     * @param type
     *  The {@link IoEventType} that the user wants to get the minimum time this method has
     *  executed
     * @return
     *  The minimum time this method has executed represented by the {@link IoEventType}
     */
    public long getMinimumTime(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                if (profileMessageReceived) {
                    return messageReceivedTimerWorker.getMinimum();
                }
                
                break;
                
            case MESSAGE_SENT :
                if (profileMessageSent) {
                    return messageSentTimerWorker.getMinimum();
                }
                
                break;
                
            case SESSION_CREATED :
                if (profileSessionCreated) {
                    return sessionCreatedTimerWorker.getMinimum();
                }
                
                break;
                
            case SESSION_OPENED :
                if (profileSessionOpened) {
                    return sessionOpenedTimerWorker.getMinimum();
                }
                
                break;
                
            case SESSION_IDLE :
                if (profileSessionIdle) {
                    return sessionIdleTimerWorker.getMinimum();
                }
                
                break;
                
            case SESSION_CLOSED :
                if (profileSessionClosed) {
                    return sessionClosedTimerWorker.getMinimum();
                }
                
                break;
        }
    
        throw new IllegalArgumentException(
                "You are not monitoring this event.  Please add this event first.");
    }

    /**
     * The maximum time the method represented by {@link IoEventType} has executed
     *
     * @param type
     *  The {@link IoEventType} that the user wants to get the maximum time this method has
     *  executed
     * @return
     *  The maximum time this method has executed represented by the {@link IoEventType}
     */
    public long getMaximumTime(IoEventType type) {
        switch (type) {
            case MESSAGE_RECEIVED :
                if (profileMessageReceived) {
                    return messageReceivedTimerWorker.getMaximum();
                }
                
                break;
                
            case MESSAGE_SENT :
                if (profileMessageSent) {
                    return messageSentTimerWorker.getMaximum();
                }
                
                break;
                
            case SESSION_CREATED :
                if (profileSessionCreated) {
                    return sessionCreatedTimerWorker.getMaximum();
                }
                
                break;
                
            case SESSION_OPENED :
                if (profileSessionOpened) {
                    return sessionOpenedTimerWorker.getMaximum();
                }
                
                break;
                
            case SESSION_IDLE :
                if (profileSessionIdle) {
                    return sessionIdleTimerWorker.getMaximum();
                }
                
                break;
                
            case SESSION_CLOSED :
                if (profileSessionClosed) {
                    return sessionClosedTimerWorker.getMaximum();
                }
                
                break;
        }
        
        throw new IllegalArgumentException(
                "You are not monitoring this event.  Please add this event first.");
    }

    /**
     * Class that will track the time each method takes and be able to provide information
     * for each method.
     *
     */
    private class TimerWorker {
        /** The sum of all operation durations */
        private final AtomicLong total;
        
        /** The number of calls */
        private final AtomicLong callsNumber;
        
        /** The fastest operation */
        private final AtomicLong minimum;
        
        /** The slowest operation */
        private final AtomicLong maximum;
        
        /** A lock for synchinized blocks */
        private final Object lock = new Object();

        /**
         * Creates a new instance of TimerWorker.
         *
         */
        public TimerWorker() {
            total = new AtomicLong();
            callsNumber = new AtomicLong();
            minimum = new AtomicLong();
            maximum = new AtomicLong();
        }

        /**
         * Add a new operation duration to this class.  Total is updated
         * and calls is incremented
         *
         * @param duration
         *  The new operation duration
         */
        public void addNewDuration(long duration) {
            callsNumber.incrementAndGet();
            total.addAndGet(duration);

            synchronized (lock) {
                // this is not entirely thread-safe, must lock
                if (duration < minimum.longValue()) {
                    minimum.set(duration);
                }

                // this is not entirely thread-safe, must lock
                if (duration > maximum.longValue()) {
                    maximum.set(duration);
                }
            }
        }

        /**
         * Gets the average reading for this event
         *
         * @return the average reading for this event
         */
        public double getAverage() {
            synchronized (lock) {
                // There are two operations, we need to synchronize the block
                return total.longValue() / callsNumber.longValue();
            }
        }

        /**
         * Returns the total number of profiled operations
         *
         * @return The total number of profiled operation 
         */
        public long getCallsNumber() {
            return callsNumber.longValue();
        }

        /**
         * Returns the total time
         *
         * @return the total time
         */
        public long getTotal() {
            return total.longValue();
        }

        /**
         * Returns the lowest execution time 
         *
         * @return the lowest execution time
         */
        public long getMinimum() {
            return minimum.longValue();
        }

        /**
         * Returns the longest execution time
         *
         * @return the longest execution time
         */
        public long getMaximum() {
            return maximum.longValue();
        }
    }

    /**
     * @return the current time, expressed using the fixed TimeUnit.
     */
    private long timeNow() {
        switch (timeUnit) {
            case SECONDS :
                return System.currentTimeMillis()/1000;
                
            case MICROSECONDS :
                return System.nanoTime()/1000;
                
            case NANOSECONDS :
                return System.nanoTime();
                
            default :
                return System.currentTimeMillis();
        }
    }
}
