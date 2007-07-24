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


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.util.SessionLog;


/**
 * Logs all MINA protocol events using the {@link SessionLog}.  Each event can be 
 * tuned to use a different level based on the user's specific requirements.  Methods
 * are in place that allow the user to use either the get or set method for each event
 * and pass in the {@link IoEventType} and the {@link LogLevel}.
 *
 * By default, all events are logged to the {@link IoEventType.INFO} level except 
 * {@link IoFilterAdapter.exceptionCaught()}, which is logged to {@link IoEventType.WARN}. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LoggingFilter extends IoFilterAdapter
{

    /**
     * {@link LogLevel} which will not log any information
     */
    public static final LogLevel NONE = new LogLevel()
    {

        @Override
        public void log( IoSession session, String message )
        {
        }


        @Override
        public void log( IoSession session, String message, Throwable cause )
        {
        }


        @Override
        public String toString()
        {
            return "NONE";
        }
    };

    /**
     * {@link LogLevel} which logs messages on the DEBUG level.
     */
    public static final LogLevel DEBUG = new LogLevel()
    {

        @Override
        public void log( IoSession session, String message )
        {
            SessionLog.debug( session, message );
        }


        @Override
        public void log( IoSession session, String message, Throwable cause )
        {
            SessionLog.debug( session, message, cause );
        }


        @Override
        public String toString()
        {
            return "DEBUG";
        }
    };

    /**
     * {@link LogLevel} which logs messages on the INFO level.
     */
    public static final LogLevel INFO = new LogLevel()
    {
        @Override
        public void log( IoSession session, String message )
        {
            SessionLog.info( session, message );
        }


        @Override
        public void log( IoSession session, String message, Throwable cause )
        {
            SessionLog.info( session, message, cause );
        }


        @Override
        public String toString()
        {
            return "INFO";
        }
    };

    /**
     * {@link LogLevel} which logs messages on the WARN level.
     */
    public static final LogLevel WARN = new LogLevel()
    {
        @Override
        public void log( IoSession session, String message )
        {
            SessionLog.warn( session, message );
        }


        @Override
        public void log( IoSession session, String message, Throwable cause )
        {
            SessionLog.warn( session, message, cause );
        }


        @Override
        public String toString()
        {
            return "WARN";
        }
    };

    /**
     * {@link LogLevel} which logs messages on the ERROR level.
     */
    public static final LogLevel ERROR = new LogLevel()
    {
        @Override
        public void log( IoSession session, String message )
        {
            SessionLog.error( session, message );
        }


        @Override
        public void log( IoSession session, String message, Throwable cause )
        {
            SessionLog.error( session, message, cause );
        }


        @Override
        public String toString()
        {
            return "ERROR";
        }
    };

    private final Map<IoEventType, LogLevel> logSettings;


    /**
     * Default Constructor. 
     */
    public LoggingFilter()
    {
        logSettings = Collections.synchronizedMap( new HashMap<IoEventType, LogLevel>() );

        // Exceptions will be logged to WARN as default...
        logSettings.put( IoEventType.EXCEPTION_CAUGHT, WARN );

        setLogLevel( IoEventType.MESSAGE_RECEIVED, INFO );
        setLogLevel( IoEventType.MESSAGE_SENT, INFO );
        setLogLevel( IoEventType.SESSION_CLOSED, INFO );
        setLogLevel( IoEventType.SESSION_CREATED, INFO );
        setLogLevel( IoEventType.SESSION_IDLE, INFO );
        setLogLevel( IoEventType.SESSION_OPENED, INFO );
    }


    @Override
    public void exceptionCaught( NextFilter nextFilter, IoSession session, Throwable cause ) throws Exception
    {
        logSettings.get( IoEventType.EXCEPTION_CAUGHT ).log( session, "EXCEPTION: ", cause );
        nextFilter.exceptionCaught( session, cause );
    }


    @Override
    public void messageReceived( NextFilter nextFilter, IoSession session, Object message ) throws Exception
    {
        logSettings.get( IoEventType.MESSAGE_RECEIVED ).log( session, "RECEIVED: " + message );
        nextFilter.messageReceived( session, message );
    }


    @Override
    public void messageSent( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws Exception
    {
        logSettings.get( IoEventType.MESSAGE_SENT ).log( session, "SENT: " + writeRequest.getMessage() );
        nextFilter.messageSent( session, writeRequest );
    }


    @Override
    public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
    {
        logSettings.get( IoEventType.SESSION_CLOSED ).log( session, "CLOSED" );
        nextFilter.sessionClosed( session );
    }


    @Override
    public void sessionCreated( NextFilter nextFilter, IoSession session ) throws Exception
    {
        logSettings.get( IoEventType.SESSION_CREATED ).log( session, "CREATED" );
        nextFilter.sessionCreated( session );
    }


    @Override
    public void sessionIdle( NextFilter nextFilter, IoSession session, IdleStatus status ) throws Exception
    {
        logSettings.get( IoEventType.SESSION_IDLE ).log( session, "IDLE: " + status );
        nextFilter.sessionIdle( session, status );
    }


    @Override
    public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
    {
        logSettings.get( IoEventType.SESSION_OPENED ).log( session, "OPENED" );
        nextFilter.sessionOpened( session );
    }


    /**
     * Sets the {@link LogLevel} to be used when exceptions are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when exceptions are logged.
     */
    public void setExceptionCaughtLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.EXCEPTION_CAUGHT, logLevel );
    }


    /**
     * Sets the {@link LogLevel} to be used when message received events are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when message received events are logged.
     */
    public void setMessageReceivedLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.MESSAGE_RECEIVED, logLevel );
    }


    /**
     * Sets the {@link LogLevel} to be used when message sent events are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when message sent events are logged.
     */
    public void setMessageSentLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.MESSAGE_SENT, logLevel );
    }


    /**
     * Sets the {@link LogLevel} to be used when session closed events are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when session closed events are logged.
     */
    public void setSessionClosedLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.SESSION_CLOSED, logLevel );
    }


    /**
     * Sets the {@link LogLevel} to be used when session created events are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when session created events are logged.
     */
    public void setSessionCreatedLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.SESSION_CREATED, logLevel );
    }


    /**
     * Sets the {@link LogLevel} to be used when session idle events are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when session idle events are logged.
     */
    public void setSessionIdleLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.SESSION_IDLE, logLevel );
    }


    /**
     * Sets the {@link LogLevel} to be used when session opened events are logged.
     * 
     * @param logLevel
     * 	The {@link LogLevel} to be used when session opened events are logged.
     */
    public void setSessionOpenedLogLevel( LogLevel logLevel )
    {
        setLogLevel( IoEventType.SESSION_OPENED, logLevel );
    }


    /**
     * This method sets the log level for the suppliend {@link LogLevel}
     * event.
     * 
     * @param event
     * 	The event that is to be updated with the new {@link LogLevel}
     * @param logLevel
     * 	The new {@link LogLevel} to be used to log the specified event
     */
    public void setLogLevel( IoEventType event, LogLevel logLevel )
    {
        logSettings.put( event, logLevel );
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * exception caught events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging exception caught events
     */
    public String getExceptionCaughtLogLevel()
    {
        return logSettings.get( IoEventType.EXCEPTION_CAUGHT ).toString();
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * message received events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging message received events
     */
    public String getMessageReceivedLogLevel()
    {
        return logSettings.get( IoEventType.MESSAGE_RECEIVED ).toString();
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * message sent events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging message sent events
     */
    public String getMessageSentLogLevel()
    {
        return logSettings.get( IoEventType.MESSAGE_SENT ).toString();
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * session closed events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging session closed events
     */
    public String getSessionClosedLogLevel()
    {
        return logSettings.get( IoEventType.SESSION_CLOSED ).toString();
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * session created events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging session created events
     */
    public String getSessionCreatedLogLevel()
    {
        return logSettings.get( IoEventType.SESSION_CREATED ).toString();
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * session idle events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging session idle events
     */
    public String getSessionIdleLogLevel()
    {
        return logSettings.get( IoEventType.SESSION_IDLE ).toString();
    }


    /**
     * This method returns the {@link LogLevel} that is used to log 
     * session opened events.
     * 
     * @return
     * 	The {@link LogLevel} used when logging session opened events
     */
    public String getSessionOpenedLogLevel()
    {
        return logSettings.get( IoEventType.SESSION_OPENED ).toString();
    }

    /**
     * Defines a logging level.
     */
    public static abstract class LogLevel
    {
        LogLevel()
        {
        }


        public abstract void log( IoSession session, String message );


        public abstract void log( IoSession session, String message, Throwable cause );


        public abstract String toString();
    }
}
