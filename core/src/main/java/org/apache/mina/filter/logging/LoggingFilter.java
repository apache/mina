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
package org.apache.mina.filter.logging;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs all MINA protocol events.  Each event can be
 * tuned to use a different level based on the user's specific requirements.  Methods
 * are in place that allow the user to use either the get or set method for each event
 * and pass in the {@link IoEventType} and the {@link LogLevel}.
 *
 * By default, all events are logged to the {@link LogLevel#INFO} level except
 * {@link IoFilterAdapter#exceptionCaught(IoFilter.NextFilter, IoSession, Throwable)},
 * which is logged to {@link LogLevel#WARN}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @org.apache.xbean.XBean
 */
public class LoggingFilter extends IoFilterAdapter {
    // Set the filter's name
    static {
    	name = "logging";
    }
    
	/** The logger name */
    private final String loggerName;
    
    /** The logger */
    private final Logger logger;
    
    /** The log level for the exceptionCaught event. Default to WARN. */
    private LogLevel exceptionCaughtLevel = LogLevel.WARN;
    
    /** The log level for the messageSent event. Default to INFO. */
    private LogLevel messageSentLevel = LogLevel.INFO;
    
    /** The log level for the messageReceived event. Default to INFO. */
    private LogLevel messageReceivedLevel = LogLevel.INFO;
    
    /** The log level for the sessionCreated event. Default to INFO. */
    private LogLevel sessionCreatedLevel = LogLevel.INFO;
    
    /** The log level for the sessionOpened event. Default to INFO. */
    private LogLevel sessionOpenedLevel = LogLevel.INFO;
    
    /** The log level for the sessionIdle event. Default to INFO. */
    private LogLevel sessionIdleLevel = LogLevel.INFO;
    
    /** The log level for the sessionClosed event. Default to INFO. */
    private LogLevel sessionClosedLevel = LogLevel.INFO;
    
    /**
     * Default Constructor.
     */
    public LoggingFilter() {
        this(LoggingFilter.class.getName());
    }
    
    /**
     * Create a new NoopFilter using a class name
     * 
     * @param clazz the cass which name will be used to create the logger
     */
    public LoggingFilter(Class<?> clazz) {
        this(clazz.getName());
    }

    /**
     * Create a new NoopFilter using a name
     * 
     * @param name the name used to create the logger. If null, will default to "NoopFilter"
     */
    public LoggingFilter(String loggerName) {
        if (loggerName == null) {
            this.loggerName = LoggingFilter.class.getName();
        } else {
        	this.loggerName = loggerName;
        }
        
        logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * @return The logger's name
     */
    public String getLoggerName() {
        return loggerName;
    }
    
    /**
     * Log if the logger and the current event log level are compatible. We log
     * a message and an exception.
     * 
     * @param eventLevel the event log level as requested by the user
     * @param message the message to log
     * @param cause the exception cause to log
     */
    private void log(LogLevel eventLevel, String message, Throwable cause) {
    	if (eventLevel == LogLevel.TRACE) {
    		logger.trace(message, cause);
    	} else if (eventLevel.getLevel() > LogLevel.INFO.getLevel()) {
    		logger.info(message, cause);
    	} else if (eventLevel.getLevel() > LogLevel.WARN.getLevel()) {
    		logger.warn(message, cause);
    	} else if (eventLevel.getLevel() > LogLevel.ERROR.getLevel()) {
    		logger.error(message, cause);
    	}
    }

    /**
     * Log if the logger and the current event log level are compatible. We log
     * a formated message and its parameters. 
     * 
     * @param eventLevel the event log level as requested by the user
     * @param message the formated message to log
     * @param param the parameter injected into the message
     */
    private void log(LogLevel eventLevel, String message, Object param) {
    	if (eventLevel == LogLevel.TRACE) {
    		logger.trace(message, param);
    	} else if (eventLevel.getLevel() > LogLevel.INFO.getLevel()) {
    		logger.info(message, param);
    	} else if (eventLevel.getLevel() > LogLevel.WARN.getLevel()) {
    		logger.warn(message, param);
    	} else if (eventLevel.getLevel() > LogLevel.ERROR.getLevel()) {
    		logger.error(message, param);
    	} 
    }

    /**
     * Log if the logger and the current event log level are compatible. We log
     * a simple message. 
     * 
     * @param eventLevel the event log level as requested by the user
     * @param message the message to log
     */
    private void log(LogLevel eventLevel, String message) {
    	if (eventLevel == LogLevel.TRACE) {
    		logger.trace(message);
    	} else if (eventLevel.getLevel() > LogLevel.INFO.getLevel()) {
    		logger.info(message);
    	} else if (eventLevel.getLevel() > LogLevel.WARN.getLevel()) {
    		logger.warn(message);
    	} else if (eventLevel.getLevel() > LogLevel.ERROR.getLevel()) {
    		logger.error(message);
    	}
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
    	log(exceptionCaughtLevel, "EXCEPTION :", cause);
        nextFilter.exceptionCaught(session, cause);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
    	log(messageReceivedLevel, "RECEIVED: {}", message );
    	nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
    	log(messageSentLevel, "SENT: {}", writeRequest.getMessage() );
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
    		throws Exception {
    	log(sessionCreatedLevel, "CREATED");
        nextFilter.sessionCreated(session);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
    throws Exception {
    	log(sessionOpenedLevel, "OPENED");
        nextFilter.sessionOpened(session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
    	log(sessionIdleLevel, "IDLE");
        nextFilter.sessionIdle(session, status);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
    	log(sessionClosedLevel, "CLOSED");
        nextFilter.sessionClosed(session);
    }
    
    /**
     * Set the LogLevel for the ExceptionCaught event.
     * 
     * @param level The LogLevel to set
     */
    public void setExceptionCaughtLoglevel(LogLevel level) {
    	exceptionCaughtLevel = level;
    }
    
    /**
     * Get the LogLevel for the ExceptionCaught event.
     * 
     * @return The LogLevel for the ExceptionCaught eventType
     */
    public LogLevel getExceptionCaughtLoglevel() {
    	return exceptionCaughtLevel;
    }
    
    /**
     * Set the LogLevel for the MessageReceived event.
     * 
     * @param level The LogLevel to set
     */
    public void setMessageReceivedLoglevel(LogLevel level) {
    	messageReceivedLevel = level;
    }
    
    /**
     * Get the LogLevel for the MessageReceived event.
     * 
     * @return The LogLevel for the MessageReceived eventType
     */
    public LogLevel getMessageReceivedLoglevel() {
    	return messageReceivedLevel;
    }
    
    /**
     * Set the LogLevel for the MessageSent event.
     * 
     * @param level The LogLevel to set
     */
    public void setMessageSentLoglevel(LogLevel level) {
    	messageSentLevel = level;
    }
    
    /**
     * Get the LogLevel for the MessageSent event.
     * 
     * @return The LogLevel for the MessageSent eventType
     */
    public LogLevel getMessageSentLoglevel() {
    	return messageSentLevel;
    }
    
    /**
     * Set the LogLevel for the SessionCreated event.
     * 
     * @param level The LogLevel to set
     */
    public void setSessionCreatedLoglevel(LogLevel level) {
    	sessionCreatedLevel = level;
    }
    
    /**
     * Get the LogLevel for the SessionCreated event.
     * 
     * @return The LogLevel for the SessionCreated eventType
     */
    public LogLevel getSessionCreatedLoglevel() {
    	return sessionCreatedLevel;
    }
    
    /**
     * Set the LogLevel for the SessionOpened event.
     * 
     * @param level The LogLevel to set
     */
    public void setSessionOpenedLoglevel(LogLevel level) {
    	sessionOpenedLevel = level;
    }
    
    /**
     * Get the LogLevel for the SessionOpened event.
     * 
     * @return The LogLevel for the SessionOpened eventType
     */
    public LogLevel getSessionOpenedLoglevel() {
    	return sessionOpenedLevel;
    }
    
    /**
     * Set the LogLevel for the SessionIdle event.
     * 
     * @param level The LogLevel to set
     */
    public void setSessionIdleLoglevel(LogLevel level) {
    	sessionIdleLevel = level;
    }
    
    /**
     * Get the LogLevel for the SessionIdle event.
     * 
     * @return The LogLevel for the SessionIdle eventType
     */
    public LogLevel getSessionIdleLoglevel() {
    	return sessionIdleLevel;
    }
    
    /**
     * Set the LogLevel for the SessionClosed event.
     * 
     * @param level The LogLevel to set
     */
    public void setSessionClosedLoglevel(LogLevel level) {
    	sessionClosedLevel = level;
    }

    /**
     * Get the LogLevel for the SessionClosed event.
     * 
     * @return The LogLevel for the SessionClosed eventType
     */
    public LogLevel getSessionClosedLoglevel() {
    	return sessionClosedLevel;
    }
}
