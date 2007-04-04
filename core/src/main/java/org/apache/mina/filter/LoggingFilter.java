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
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs all MINA protocol events using the {@link SessionLog}. Each event will
 * be logged to its own {@link Logger}. The names of these loggers will share a 
 * common prefix. By default the name of the logger used by {@link SessionLog}
 * will be used as prefix. Use {@link #setLoggerNamePrefix(String)} to override
 * the default prefix.
 * <p>
 * For most of the events the name of the corresponding {@link IoHandler} method
 * which handles that event will be added to the prefix to form the logger name.
 * Here's a complete list of the logger names for each event:
 * <table>
 *   <tr><th>Event</th><th>Name of logger</th></tr>
 *   <tr><td>sessionCreated</td><td>&lt;prefix&gt;.sessionCreated</td></tr>
 *   <tr><td>sessionOpened</td><td>&lt;prefix&gt;.sessionOpened</td></tr>
 *   <tr><td>sessionClosed</td><td>&lt;prefix&gt;.sessionClosed</td></tr>
 *   <tr><td>sessionIdle</td><td>&lt;prefix&gt;.sessionIdle</td></tr>
 *   <tr><td>exceptionCaught</td><td>&lt;prefix&gt;.exceptionCaught</td></tr>
 *   <tr><td>messageReceived</td><td>&lt;prefix&gt;.messageReceived</td></tr>
 *   <tr><td>messageSent</td><td>&lt;prefix&gt;.messageSent</td></tr>
 *   <tr><td>{@link IoSession#write(Object)}</td><td>&lt;prefix&gt;.write</td></tr>
 *   <tr><td>{@link IoSession#close()}</td><td>&lt;prefix&gt;.close</td></tr>
 * </table>
 * </p>
 * <p>
 * By default all events will be logged on the INFO level. Use 
 * {@link #setDefaultLogLevel(org.apache.mina.filter.LoggingFilter.LogLevel)} to change
 * the level to one of {@link #DEBUG}, {@link #INFO}, {@link #WARN} or
 * {@link #ERROR}..
 * </p>
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see SessionLog
 */
public class LoggingFilter extends IoFilterAdapter
{
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = SessionLog.PREFIX;

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = SessionLog.LOGGER;
    
    /**
     * {@link LogLevel} which logs messages on the DEBUG level.
     */
    public static final LogLevel DEBUG = new LogLevel() {
        @Override
        public boolean isEnabled(Logger log) {
            return log.isDebugEnabled();
        }

        @Override
        public void log(Logger log, IoSession session, String message) {
            SessionLog.debug(log, session, message);
        }

        @Override
        public void log(Logger log, IoSession session, String message,
                Throwable cause) {
            SessionLog.debug(log, session, message, cause);
        }
    };
    
    /**
     * {@link LogLevel} which logs messages on the INFO level.
     */
    public static final LogLevel INFO = new LogLevel() {
        @Override
        public boolean isEnabled(Logger log) {
            return log.isInfoEnabled();
        }

        @Override
        public void log(Logger log, IoSession session, String message) {
            SessionLog.info(log, session, message);
        }

        @Override
        public void log(Logger log, IoSession session, String message,
                Throwable cause) {
            SessionLog.info(log, session, message, cause);
        }
    };

    /**
     * {@link LogLevel} which logs messages on the WARN level.
     */
    public static final LogLevel WARN = new LogLevel() {
        @Override
        public boolean isEnabled(Logger log) {
            return log.isWarnEnabled();
        }

        @Override
        public void log(Logger log, IoSession session, String message) {
            SessionLog.warn(log, session, message);
        }

        @Override
        public void log(Logger log, IoSession session, String message,
                Throwable cause) {
            SessionLog.warn(log, session, message, cause);
        }
    };
    
    /**
     * {@link LogLevel} which logs messages on the ERROR level.
     */
    public static final LogLevel ERROR = new LogLevel() {
        @Override
        public boolean isEnabled(Logger log) {
            return log.isErrorEnabled();
        }

        @Override
        public void log(Logger log, IoSession session, String message) {
            SessionLog.error(log, session, message);
        }

        @Override
        public void log(Logger log, IoSession session, String message,
                Throwable cause) {
            SessionLog.error(log, session, message, cause);
        }
    };
    
    private static final String EVENT_LOGGER = LoggingFilter.class.getName()
            + ".event";
    private static final String SESSION_CREATED_LOGGER = EVENT_LOGGER + ".sessionCreated";
    private static final String SESSION_OPENED_LOGGER = EVENT_LOGGER + ".sessionOpened";
    private static final String SESSION_CLOSED_LOGGER = EVENT_LOGGER + ".sessionClosed";
    private static final String SESSION_IDLE_LOGGER = EVENT_LOGGER + ".sessionIdle";
    private static final String EXCEPTION_CAUGHT_LOGGER = EVENT_LOGGER + ".exceptionCaught";
    private static final String MESSAGE_RECEIVED_LOGGER = EVENT_LOGGER + ".messageReceived";
    private static final String MESSAGE_SENT_LOGGER = EVENT_LOGGER + ".messageSent";
    private static final String WRITE_LOGGER = EVENT_LOGGER + ".write";
    private static final String CLOSE_LOGGER = EVENT_LOGGER + ".close";
    
    private String loggerNamePrefix = null;
    private LogLevel defaultLogLevel = INFO;
    private LogLevel exceptionCaughtLogLevel = INFO;
    
    /**
     * Creates a new instance.
     */
    public LoggingFilter() {
    }
    
    /**
     * Returns the prefix used for the names of the loggers used to log the different 
     * events. If <code>null</code> the name of the {@link Logger} used by
     * {@link SessionLog} will be used for the prefix. The default value is 
     * <code>null</code>.
     *
     * @return the prefix or <code>null</code>.
     */
    public String getLoggerNamePrefix() {
        return loggerNamePrefix;
    }

    /**
     * Sets the prefix used for the names of the loggers used to log the different 
     * events. If set to <code>null</code> the name of the {@link Logger} used by
     * {@link SessionLog} will be used for the prefix.
     *
     * @param loggerNamePrefix the new prefix.
     */
    public void setLoggerNamePrefix(String loggerNamePrefix) {
        this.loggerNamePrefix = loggerNamePrefix;
    }
    
    /**
     * Returns the current {@link LogLevel} which is used when this filter logs all 
     * events but the <code>exceptionCaught</code> event. The default is 
     * {@link #INFO}.
     * 
     * @return the current {@link LogLevel}.
     * @see #getExceptionCaughtLogLevel()
     */
    public LogLevel getDefaultLogLevel() {
        return defaultLogLevel;
    }

    /**
     * Sets the {@link LogLevel} which will be used when this filter logs all events
     * but the <code>exceptionCaught</code> event.
     * 
     * @param logLevel the new {@link LogLevel}.
     * @throws NullPointerException if the specified {@link LogLevel} is 
     *                <code>null</code>. 
     * @see #setExceptionCaughtLogLevel(org.apache.mina.filter.LoggingFilter.LogLevel)
     */
    public void setDefaultLogLevel(LogLevel logLevel) {
        if (logLevel == null) {
            throw new NullPointerException("defaultLogLevel");
        }
        this.defaultLogLevel = logLevel;
    }

    /**
     * Returns the current {@link LogLevel} which is used when this filter logs 
     * <code>exceptionCaught</code> events. The default is {@link #INFO}.
     * 
     * @return the current {@link LogLevel}.
     * @see #getDefaultLogLevel()
     */
    public LogLevel getExceptionCaughtLogLevel() {
        return exceptionCaughtLogLevel;
    }

    /**
     * Sets the {@link LogLevel} which will be used when this filter logs 
     * <code>exceptionCaught</code> events.
     * 
     * @param logLevel the new {@link LogLevel}.
     * @throws NullPointerException if the specified {@link LogLevel} is 
     *                <code>null</code>. 
     * @see #setDefaultLogLevel(org.apache.mina.filter.LoggingFilter.LogLevel)
     */
    public void setExceptionCaughtLogLevel(LogLevel logLevel) {
        if (logLevel == null) {
            throw new NullPointerException("exceptionCaughtLogLevel");
        }
        this.exceptionCaughtLogLevel = logLevel;
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session) {
        defaultLogLevel.log(getLogger(session, SESSION_CREATED_LOGGER, "sessionCreated"), session, "CREATED");
        nextFilter.sessionCreated(session);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        defaultLogLevel.log(getLogger(session, SESSION_OPENED_LOGGER, "sessionOpened"), session, "OPENED");
        nextFilter.sessionOpened(session);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        defaultLogLevel.log(getLogger(session, SESSION_CLOSED_LOGGER, "sessionClosed"), session, "CLOSED");
        nextFilter.sessionClosed(session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) {
        Logger log = getLogger(session, SESSION_IDLE_LOGGER, "sessionIdle");
        if (defaultLogLevel.isEnabled(log)) {
            defaultLogLevel.log(log, session, "IDLE: " + status);
        }
        nextFilter.sessionIdle(session, status);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) {
        Logger log = getLogger(session, EXCEPTION_CAUGHT_LOGGER, "exceptionCaught");
        if (exceptionCaughtLogLevel.isEnabled(log)) {
            exceptionCaughtLogLevel.log(log, session, "EXCEPTION:", cause);
        }
        nextFilter.exceptionCaught(session, cause);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) {
        Logger log = getLogger(session, MESSAGE_RECEIVED_LOGGER, "messageReceived");
        if (defaultLogLevel.isEnabled(log)) {
            defaultLogLevel.log(log, session, "RECEIVED: " + message);
        }
        nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) {
        Logger log = getLogger(session, MESSAGE_SENT_LOGGER, "messageSent");
        if (defaultLogLevel.isEnabled(log)) {
            defaultLogLevel.log(log, session, "SENT: " + message);
        }
        nextFilter.messageSent(session, message);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) {
        Logger log = getLogger(session, WRITE_LOGGER, "write");
        if (defaultLogLevel.isEnabled(log)) {
            defaultLogLevel.log(log, session, "WRITE: " + writeRequest);
        }
        nextFilter.filterWrite(session, writeRequest);
    }

    @Override
    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        defaultLogLevel.log(getLogger(session, CLOSE_LOGGER, "close"), session, "CLOSE");
        nextFilter.filterClose(session);
    }
    
    private Logger getLogger(IoSession session, String attribute, String event) {
        Logger log = (Logger) session.getAttribute(attribute);
        if (log == null) {
            String prefix = loggerNamePrefix;
            if (prefix == null) {
                prefix = SessionLog.getLogger(session).getName();
            }
            log = LoggerFactory.getLogger(prefix + "." + event);
            session.setAttribute(attribute, log);
        }
        return log;
    }
    
    /**
     * Defines a logging level.
     */
    public static abstract class LogLevel {
        LogLevel() {}
        public abstract boolean isEnabled(Logger log);
        public abstract void log(Logger log, IoSession session, String message);
        public abstract void log(Logger log, IoSession session, String message, 
                Throwable cause);
    }
}
