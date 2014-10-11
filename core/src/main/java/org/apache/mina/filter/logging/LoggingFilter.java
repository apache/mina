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

import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.util.ByteBufferDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple filter logging incoming events.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class LoggingFilter extends AbstractIoFilter {
    /** The logger */
    private final Logger logger;

    /** The log level for the messageWritting event. Default to INFO. */
    private LogLevel messageWritingLevel = LogLevel.INFO;

    /** The log level for the messageSent event. Default to INFO. */
    private LogLevel messageSentLevel = LogLevel.INFO;

    /** The log level for the messageReceived event. Default to INFO. */
    private LogLevel messageReceivedLevel = LogLevel.INFO;

    /** The log level for the sessionOpened event. Default to INFO. */
    private LogLevel sessionOpenedLevel = LogLevel.INFO;

    /** The log level for the sessionClosed event. Default to INFO. */
    private LogLevel sessionClosedLevel = LogLevel.INFO;

    /** The log level for the sessionIdle event. Default to INFO. */
    private LogLevel sessionIdleLevel = LogLevel.INFO;

    /**
     * Default Constructor.
     */
    public LoggingFilter() {
        this(LoggingFilter.class.getName());
    }

    /**
     * Create a new LoggingFilter using a class name
     * 
     * @param clazz
     *            the class which name will be used to create the logger
     */
    public LoggingFilter(Class<?> clazz) {
        this(clazz.getName());
    }

    /**
     * Create a new LoggingFilter using a name
     * 
     * @param name
     *            the name used to create the logger. If null, will default to
     *            "LoggingFilter"
     */
    public LoggingFilter(String name) {
        if (name == null) {
            logger = LoggerFactory.getLogger(LoggingFilter.class.getName());
        } else {
            logger = LoggerFactory.getLogger(name);
        }
    }

    /**
     * Log if the logger and the current event log level are compatible. We log
     * a formated message and its parameters.
     * 
     * @param eventLevel
     *            the event log level as requested by the user
     * @param message
     *            the formated message to log
     * @param param
     *            the parameter injected into the message
     */
    private void log(LogLevel eventLevel, String message, Object param) {
        switch (eventLevel) {
        case TRACE:
            logger.trace(message, param);
            return;
        case DEBUG:
            logger.debug(message, param);
            return;
        case INFO:
            logger.info(message, param);
            return;
        case WARN:
            logger.warn(message, param);
            return;
        case ERROR:
            logger.error(message, param);
            return;
        default:
            return;
        }
    }

    /**
     * Log if the logger and the current event log level are compatible. We log
     * a simple message.
     * 
     * @param eventLevel
     *            the event log level as requested by the user
     * @param message
     *            the message to log
     */
    private void log(LogLevel eventLevel, String message) {
        switch (eventLevel) {
        case TRACE:
            logger.trace(message);
            return;
        case DEBUG:
            logger.debug(message);
            return;
        case INFO:
            logger.info(message);
            return;
        case WARN:
            logger.warn(message);
            return;
        case ERROR:
            logger.error(message);
            return;
        default:
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionOpened(IoSession session) {
        log(sessionOpenedLevel, "OPENED");
        super.sessionOpened(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IoSession session) {
        log(sessionClosedLevel, "CLOSED");
        super.sessionClosed(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        log(sessionIdleLevel, "IDLE");
        super.sessionIdle(session, status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent(IoSession session, Object message) {
        log(messageSentLevel, "SENT");
        super.messageSent(session, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
        if (message instanceof ByteBuffer) {
            log(messageReceivedLevel, "RECEIVED: {}", ByteBufferDumper.dump((ByteBuffer) message));
        } else {
            log(messageReceivedLevel, "RECEIVED: {}", message);
        }

        super.messageReceived(session, message, controller);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller) {
        log(messageReceivedLevel, "WRITTING: {}", message);
        super.messageWriting(session, message, controller);
    }

    // =========================
    // SETTERS & GETTERS
    // =========================

    /**
     * Set the LogLevel for the MessageReceived event.
     * 
     * @param level
     *            The LogLevel to set
     */
    public void setMessageReceivedLogLevel(LogLevel level) {
        messageReceivedLevel = level;
    }

    /**
     * Get the LogLevel for the MessageReceived event.
     * 
     * @return The LogLevel for the MessageReceived eventType
     */
    public LogLevel getMessageReceivedLogLevel() {
        return messageReceivedLevel;
    }

    /**
     * Set the LogLevel for the MessageWriting event.
     * 
     * @param level
     *            The LogLevel to set
     */
    public void setMessageWritingLogLevel(LogLevel level) {
        messageWritingLevel = level;
    }

    /**
     * Get the LogLevel for the MessageWriting event.
     * 
     * @return The LogLevel for the MessageWriting eventType
     */
    public LogLevel getMessageWritingLogLevel() {
        return messageWritingLevel;
    }

    /**
     * Set the LogLevel for the SessionOpened event.
     * 
     * @param level
     *            The LogLevel to set
     */
    public void setSessionOpenedLogLevel(LogLevel level) {
        sessionOpenedLevel = level;
    }

    /**
     * Get the LogLevel for the SessionOpened event.
     * 
     * @return The LogLevel for the SessionOpened eventType
     */
    public LogLevel getSessionOpenedLogLevel() {
        return sessionOpenedLevel;
    }

    /**
     * Set the LogLevel for the SessionIdle event.
     * 
     * @param level
     *            The LogLevel to set
     */
    public void setSessionIdleLogLevel(LogLevel level) {
        sessionIdleLevel = level;
    }

    /**
     * Get the LogLevel for the SessionIdle event.
     * 
     * @return The LogLevel for the SessionIdle eventType
     */
    public LogLevel getSessionIdleLogLevel() {
        return sessionIdleLevel;
    }

    /**
     * Set the LogLevel for the SessionClosed event.
     * 
     * @param level
     *            The LogLevel to set
     */
    public void setSessionClosedLogLevel(LogLevel level) {
        sessionClosedLevel = level;
    }

    /**
     * Get the LogLevel for the SessionClosed event.
     * 
     * @return The LogLevel for the SessionClosed eventType
     */
    public LogLevel getSessionClosedLogLevel() {
        return sessionClosedLevel;
    }

    /**
     * Get the LogLevel for the messageSent event.
     * 
     * @return The LogLevel for the messageSent eventType
     */
    public LogLevel getMessageSentLevel() {
        return messageSentLevel;
    }

    /**
     * Set the LogLevel for the messageSent event.
     * 
     * @param level
     *            The LogLevel to set
     */
    public void setMessageSentLevel(LogLevel messageSentLevel) {
        this.messageSentLevel = messageSentLevel;
    }

}