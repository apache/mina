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
package org.apache.mina.filter;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;


/**
 * This class will measure, the time it takes for a 
 * method in the {@link IoFilterAdapter} class to execute.  The basic
 * premise of the logic in this class is to get the current time
 * at the beginning of the method, call method on nextFilter, and 
 * then get the current time again.  An example of how to use 
 * the filter is:
 * 
 * <pre>
 *  ProfilerTimerFilter profiler = new ProfilerTimerFilter( ProfilerTimerFilter.MSG_RCV, ProfilerTimerUnit.MILLISECOND );
 *  chain.addFirst( "Profiler", profiler);
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ProfilerTimerFilter extends IoFilterAdapter
{
    private EnumSet<IoEventType> eventsToProfile;
    private ProfilerTimerUnit timeUnit;

    private HashMap<IoEventType,TimerWorker> timerManager;
    
    /**
     * Creates a new instance of ProfilerFilter.  This is the
     * default constructor and will print out timings for 
     * messageReceived and messageSent and the time increment 
     * will be in milliseconds.
     */
    public ProfilerTimerFilter()
    {
        this( EnumSet.of( IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT ), 
            TimeUnit.MILLISECONDS );
    }


    /**
     * Creates a new instance of ProfilerFilter.  An example
     * of this call would be:
     * 
     * <code>
     * new ProfilerTimerFilter( EnumSet.of( IoEventType.MESSAGE_RECEIVED, IoEventType.MESSAGE_SENT ), 
     *  TimeUnit.MILLISECONDS );
     * </code>
     * 
     * @param eventsToProfile
     *  A set of {@link IoEventType} representation of the methods to profile
     * @param unit
     *  Used to determine the level of precision you need in your timing. 
     */
    public ProfilerTimerFilter( EnumSet<IoEventType> eventsToProfile, TimeUnit unit )
    {
        this.eventsToProfile = eventsToProfile;
        
        setTimeUnit( unit );
        
        timerManager = new HashMap<IoEventType,TimerWorker>();
        
        for( IoEventType type : eventsToProfile ){
            timerManager.put(  type, new TimerWorker() );
        }
    }


    /**
     * Sets the {@link ProfilerTimerUnit} being used.
     *
     * @param timeUnit
     *  Sets the new {@link ProfilerTimerUnit} to be used.
     */
    public void setTimeUnit( TimeUnit unit )
    {
        if( unit == TimeUnit.MILLISECONDS ){
            this.timeUnit = ProfilerTimerUnit.MILLISECONDS;
        } else if( unit == TimeUnit.NANOSECONDS ){
            this.timeUnit = ProfilerTimerUnit.NANOSECONDS;
        } else if( unit == TimeUnit.SECONDS ){
            this.timeUnit = ProfilerTimerUnit.SECONDS;
        } else {
            throw new IllegalArgumentException( "Invalid Time specified" );
        }
    }

    /**
     * Add an {@link IoEventType} to profile
     *
     * @param type
     *  The {@link IoEventType} to profile
     */
    public void addEventToProfile( IoEventType type ){
        if( !timerManager.containsKey( type )){
            timerManager.put( type, new TimerWorker() );
        }
    }
    
    /**
     * Remove an {@link IoEventType} to profile
     *
     * @param type
     *  The {@link IoEventType} to profile
     */
    public void removeEventToProfile( IoEventType type ){
        timerManager.remove( type );
    }
    
    /**
     * Return the bitmask that is being used to display 
     * timing information for this filter.
     *
     * @return
     *  An int representing the methods that will be logged
     */
    public EnumSet<IoEventType> getEventsToProfile()
    {
        return eventsToProfile;
    }


    /**
     * Set the bitmask in order to tell this filter which
     * methods to print out timing information
     *
     * @param eventsToProfile
     *  An int representing the new methods that should be logged
     */
    public void setEventsToProfile( EnumSet<IoEventType> methodsToLog )
    {
        this.eventsToProfile = methodsToLog;
    }


    @Override
    public void messageReceived( NextFilter nextFilter, IoSession session, Object message ) throws Exception
    {
        if (message instanceof ByteBuffer && !((ByteBuffer) message).hasRemaining()) {
            // Ignore the special signal.
            nextFilter.messageReceived(session, message);
            return;
        }
        
        long start = timeUnit.timeNow();
        nextFilter.messageReceived( session, message );
        long end = timeUnit.timeNow();

        if ( getEventsToProfile().contains( IoEventType.MESSAGE_RECEIVED ) )
            timerManager.get( IoEventType.MESSAGE_RECEIVED ).addNewReading( end-start );
    }


    @Override
    public void messageSent( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.messageSent( session, writeRequest );
        long end = timeUnit.timeNow();

        if ( getEventsToProfile().contains( IoEventType.MESSAGE_SENT ) )
            timerManager.get( IoEventType.MESSAGE_SENT ).addNewReading( end-start );
    }


    @Override
    public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionClosed( session );
        long end = timeUnit.timeNow();

        if ( getEventsToProfile().contains( IoEventType.SESSION_CLOSED ) )
            timerManager.get( IoEventType.SESSION_CLOSED ).addNewReading( end-start );
    }


    @Override
    public void sessionCreated( NextFilter nextFilter, IoSession session ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionCreated( session );
        long end = timeUnit.timeNow();

        if (  getEventsToProfile().contains( IoEventType.SESSION_CREATED ) )
            timerManager.get( IoEventType.SESSION_CREATED ).addNewReading( end-start );
    }


    @Override
    public void sessionIdle( NextFilter nextFilter, IoSession session, IdleStatus status ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionIdle( session, status );
        long end = timeUnit.timeNow();

        if ( getEventsToProfile().contains( IoEventType.SESSION_IDLE ) )
            timerManager.get( IoEventType.SESSION_IDLE ).addNewReading( end-start );
    }


    @Override
    public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionOpened( session );
        long end = timeUnit.timeNow();

        if ( getEventsToProfile().contains( IoEventType.SESSION_OPENED ) )
            timerManager.get( IoEventType.SESSION_OPENED ).addNewReading( end-start );
    }

    /**
     * Get the average time for the specified method represented by the {@link IoEventType}
     *
     * @param type
     *  The {@link IoEventType} that the user wants to get the average method call time
     * @return
     *  The average time it took to execute the method represented by the {@link IoEventType}
     */
    public double getAverageTime( IoEventType type ){
        if( !timerManager.containsKey( type )){
            throw new IllegalArgumentException("You are not monitoring this event.  Please add this event first.");
        }
        
        return timerManager.get( type ).getAverage();
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
    public long getTotalCalls( IoEventType type ){
        if( !timerManager.containsKey( type )){
            throw new IllegalArgumentException("You are not monitoring this event.  Please add this event first.");
        }
        
        return timerManager.get( type ).getCalls();
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
    public long getTotalTime( IoEventType type ){
        if( !timerManager.containsKey( type )){
            throw new IllegalArgumentException("You are not monitoring this event.  Please add this event first.");
        }
        
        return timerManager.get( type ).getTotal();
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
    public long getMinValue( IoEventType type ){
        if( !timerManager.containsKey( type )){
            throw new IllegalArgumentException("You are not monitoring this event.  Please add this event first.");
        }
        
        return timerManager.get( type ).getMin();
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
    public long getMaxValue( IoEventType type ){
        if( !timerManager.containsKey( type )){
            throw new IllegalArgumentException("You are not monitoring this event.  Please add this event first.");
        }
        
        return timerManager.get( type ).getMax();
    }
    
    /**
     * Class that will track the time each method takes and be able to provide information
     * for each method.
     *
     */
    class TimerWorker {
        
        private AtomicLong total;
        private AtomicLong calls;
        private AtomicLong min;
        private AtomicLong max;
        private Object lock = new Object();
        
        /**
         * Creates a new instance of TimerWorker.
         *
         */
        public TimerWorker(){
            total = new AtomicLong();
            calls = new AtomicLong();
            min = new AtomicLong();
            max = new AtomicLong();
        }
        
        /**
         * Add a new reading to this class.  Total is updated 
         * and calls is incremented
         *
         * @param newReading
         *  The new reading
         */
        public void addNewReading( long newReading ){
            calls.incrementAndGet();
            total.addAndGet( newReading );
            
            synchronized( lock ){
                // this is not entirely thread-safe, must lock
                if( newReading < min.longValue() ){
                    min.set( newReading );
                }
                
                // this is not entirely thread-safe, must lock
                if( newReading > max.longValue() ){
                    max.set( newReading );
                }
            }
        }
        
        /**
         * Gets the average reading for this event
         *
         * @return
         *  Gets the average reading for this event
         */
        public double getAverage(){
            return total.longValue() / calls.longValue();
        }
        
        /**
         * Returns the total number of readings
         *
         * @return
         *  total number of readings
         */
        public long getCalls(){
            return calls.longValue();
        }
        
        /**
         * Returns the total time
         *
         * @return
         *  the total time
         */
        public long getTotal(){
            return total.longValue();
        }
        
        /**
         * Returns the minimum value
         *
         * @return
         *  the minimum value
         */
        public long getMin(){
            return min.longValue();
        }
        
        /**
         * Returns the maximum value
         *
         * @return
         *  the maximum value
         */
        public long getMax(){
            return max.longValue();
        }
    }
    
    enum ProfilerTimerUnit
    {
        SECONDS
        {
            public long timeNow()
            {
                return System.currentTimeMillis() / 1000;
            }


            public String getDescription()
            {
                return "seconds";
            }
        },
        MILLISECONDS
        {
            public long timeNow()
            {
                return System.currentTimeMillis();
            }


            public String getDescription()
            {
                return "milliseconds";
            }
        },
        NANOSECONDS
        {
            public long timeNow()
            {
                return System.nanoTime();
            }


            public String getDescription()
            {
                return "nanoseconds";
            }
        };

        /*
         * I was looking at possibly using the java.util.concurrent.TimeUnit
         * and I found this construct for writing enums.  Here is what the 
         * JDK developers say for why these methods below cannot be marked as
         * abstract, but should act in an abstract way...
         * 
         *     To maintain full signature compatibility with 1.5, and to improve the
         *     clarity of the generated javadoc (see 6287639: Abstract methods in
         *     enum classes should not be listed as abstract), method convert
         *     etc. are not declared abstract but otherwise act as abstract methods.
         */
        public long timeNow()
        {
            throw new AbstractMethodError();
        }


        public String getDescription()
        {
            throw new AbstractMethodError();
        }
    }
}
