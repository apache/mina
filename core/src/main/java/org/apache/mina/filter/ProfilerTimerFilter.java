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


import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.util.SessionLog;


/**
 * This class will measure, the time it takes for a 
 * method in the {@link IoFilterAdapter} class to execute.  The basic
 * premise of the logic in this class is to get the current time
 * at the beginning of the method, call method on nextFilter, and 
 * then get the current time again.  The result will then be sent to 
 * the <tt>SessionLog.debug()</tt> method.  An example of how to use 
 * the filter is:
 * 
 * <pre>
 *  ProfilerTimerFilter profiler = new ProfilerTimerFilter( ProfilerTimerFilter.MSG_RCV, 6, ProfilerTimerUnit.NANOSECONDS );
 *  profiler.setLogLevel( 6 );
 *  chain.addFirst( "Profiler", profiler);
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ProfilerTimerFilter extends IoFilterAdapter
{
    /**
     * Do not profile any methods
     */
    public static final int ZERO = 0;

    /**
     * Profile the messageReceived method
     */
    public static final int MSG_RCV = 1;

    /**
     * Profile the messageSent method
     */
    public static final int MSG_SND = 2;

    /**
     * Profile the sessionCreated method
     */
    public static final int SES_CRT = 4;

    /**
     * Profile the sessionClosed method
     */
    public static final int SES_CLS = 8;

    /**
     * Profile the sessionOpened method
     */
    public static final int SES_OPN = 16;

    /**
     * Profile the sessionIdle method
     */
    public static final int SES_IDL = 32;

    private int methodsToLog;
    private int logLevel;
    private ProfilerTimerUnit timeUnit;


    /**
     * Creates a new instance of ProfilerFilter.  This is the
     * default constructor and will print out timings for 
     * messageReceived and messageSent to the SessionLog.debug
     * level and the time increment will be in nanoseconds.
     */
    public ProfilerTimerFilter()
    {
        this( MSG_RCV | MSG_SND, 7, ProfilerTimerUnit.NANOSECONDS );
    }


    /**
     * Creates a new instance of ProfilerFilter.  An example
     * of this call would be:
     * 
     * <code>
     * new ProfilerTimerFilter( MSG_RCV|MSG_SND, 6, ProfilerTimerUnit.MILLISECONDS );
     * </code>
     * 
     * @param methodsToLog
     *  A bitmask representation of the methods to profile
     * @param logLevel
     *  The {@link SessionLog} level that this filter should
     *  print out the information.  1=error, 5=warn, 6=info,
     *  7=debug.  This follows syslog convention.
     * @param timeUnit
     *  Used to determine the level of precision you need in your timing. 
     */
    public ProfilerTimerFilter( int methodsToLog, int logLevel, ProfilerTimerUnit timeUnit )
    {
        this.methodsToLog = methodsToLog;
        this.logLevel = logLevel;
        this.timeUnit = timeUnit;
    }


    /**
     * Returns the {@link ProfilerTimerUnit} being used.
     *
     * @return
     *  The {@link ProfilerTimerUnit} being used.
     */
    public ProfilerTimerUnit getTimeUnit()
    {
        return timeUnit;
    }


    /**
     * Sets the {@link ProfilerTimerUnit} being used.
     *
     * @param timeUnit
     *  Sets the new {@link ProfilerTimerUnit} to be used.
     */
    public void setTimeUnit( ProfilerTimerUnit timeUnit )
    {
        this.timeUnit = timeUnit;
    }


    /**
     * Return the bitmask that is being used to display 
     * timing information for this filter.
     *
     * @return
     *  An int representing the methods that will be logged
     */
    public int getMethodsToLog()
    {
        return methodsToLog;
    }


    /**
     * Set the bitmask in order to tell this filter which
     * methods to print out timing information
     *
     * @param methodsToLog
     *  An int representing the new methods that should be logged
     */
    public void setMethodsToLog( int methodsToLog )
    {
        this.methodsToLog = methodsToLog;
    }


    /**
     * Returns the level of logging that this filter is using
     *
     * @return
     *  An int representing the level of logging
     */
    public int getLogLevel()
    {
        return logLevel;
    }


    /**
     * Sets the level of logging that this filter will use
     *
     * @param logLevel
     *  An int representing the level of logging to use
     */
    public void setLogLevel( int logLevel )
    {
        this.logLevel = logLevel;
    }


    private void log( IoSession session, String message )
    {
        switch ( getLogLevel() )
        {
            case 1:
                SessionLog.error( session, message );
                return;
            case 2:
            case 3:
            case 4:
            case 5:
                SessionLog.warn( session, message );
                return;
            case 6:
                SessionLog.info( session, message );
                return;
            case 7:
                SessionLog.debug( session, message );
                return;
            default:
                SessionLog.debug( session, message );
        }
    }


    @Override
    public void messageReceived( NextFilter nextFilter, IoSession session, Object message ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.messageReceived( session, message );
        long end = timeUnit.timeNow();

        if ( ( getMethodsToLog() & MSG_RCV ) > 0 )
            log( session, "Message received time : " + ( end - start ) + " " + timeUnit.getDescription() );
    }


    @Override
    public void messageSent( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.messageSent( session, writeRequest );
        long end = timeUnit.timeNow();

        if ( ( getMethodsToLog() & MSG_SND ) > 0 )
            log( session, "Message sent time : " + ( end - start ) + " " + timeUnit.getDescription() );
    }


    @Override
    public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionClosed( session );
        long end = timeUnit.timeNow();

        if ( ( getMethodsToLog() & SES_CLS ) > 0 )
            log( session, "Session closed time : " + ( end - start ) + " " + timeUnit.getDescription() );
    }


    @Override
    public void sessionCreated( NextFilter nextFilter, IoSession session ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionCreated( session );
        long end = timeUnit.timeNow();

        if ( ( getMethodsToLog() & SES_CRT ) > 0 )
            log( session, "Session created time : " + ( end - start ) + " " + timeUnit.getDescription() );
    }


    @Override
    public void sessionIdle( NextFilter nextFilter, IoSession session, IdleStatus status ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionIdle( session, status );
        long end = timeUnit.timeNow();

        if ( ( getMethodsToLog() & SES_IDL ) > 0 )
            log( session, "Session idle time : " + ( end - start ) + " " + timeUnit.getDescription() );
    }


    @Override
    public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
    {
        long start = timeUnit.timeNow();
        nextFilter.sessionOpened( session );
        long end = timeUnit.timeNow();

        if ( ( getMethodsToLog() & SES_OPN ) > 0 )
            log( session, "Session opened time : " + ( end - start ) + " " + timeUnit.getDescription() );
    }
}
