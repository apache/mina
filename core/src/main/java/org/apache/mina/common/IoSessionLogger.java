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
package org.apache.mina.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility methods to log protocol-specific messages.
 * <p>
 * Set {@link #PREFIX} and {@link #LOGGER} session attributes
 * to override prefix string and logger or use {@link #setPrefix(IoSession, String)}
 * and {@link #setLogger(IoSession, Logger)}.
 * </p>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class IoSessionLogger {
    /**
     * Session attribute key: prefix string
     */
    public static final AttributeKey PREFIX = new AttributeKey(IoSessionLogger.class, "prefix");

    /**
     * Session attribute key: {@link Logger}
     */
    public static final AttributeKey LOGGER = new AttributeKey(IoSessionLogger.class, "logger");

    private static Class<? extends IoHandler> getClass(IoSession session) {
        return session.getHandler().getClass();
    }

    public static void trace(IoSession session, Object message) {
        trace(getLogger(session), session, message);
    }

    public static void trace(Logger log, IoSession session, Object message) {
        if (log.isTraceEnabled()) {
            log.trace("{}{}", getPrefix(session), message);
        }
    }

    public static void trace(IoSession session, String message, Throwable cause) {
        trace(getLogger(session), session, message, cause);
    }

    public static void trace(Logger log, IoSession session, String message,
            Throwable cause) {
        if (log.isTraceEnabled()) {
            log.trace(getPrefix(session) + message, cause);
        }
    }

    public static void debug(IoSession session, Object message) {
        debug(getLogger(session), session, message);
    }

    public static void debug(Logger log, IoSession session, Object message) {
        if (log.isDebugEnabled()) {
            log.debug("{}{}", getPrefix(session), message);
        }
    }

    public static void debug(IoSession session, String message, Throwable cause) {
        debug(getLogger(session), session, message, cause);
    }

    public static void debug(Logger log, IoSession session, String message,
            Throwable cause) {
        if (log.isDebugEnabled()) {
            log.debug(getPrefix(session) + message, cause);
        }
    }

    public static void info(IoSession session, Object message) {
        info(getLogger(session), session, message);
    }

    public static void info(Logger log, IoSession session, Object message) {
        if (log.isInfoEnabled()) {
            log.info("{}{}", getPrefix(session), message);
        }
    }

    public static void info(IoSession session, String message, Throwable cause) {
        info(getLogger(session), session, message, cause);
    }

    public static void info(Logger log, IoSession session, String message,
            Throwable cause) {
        if (log.isInfoEnabled()) {
            log.info(getPrefix(session) + message, cause);
        }
    }

    public static void warn(IoSession session, Object message) {
        warn(getLogger(session), session, message);
    }

    public static void warn(Logger log, IoSession session, Object message) {
        if (log.isWarnEnabled()) {
            log.warn("{}{}", getPrefix(session), message);
        }
    }

    public static void warn(IoSession session, Throwable cause) {
        warn(session, "Unexpected exception.", cause);
    }

    public static void warn(IoSession session, String message, Throwable cause) {
        warn(getLogger(session), session, message, cause);
    }

    public static void warn(Logger log, IoSession session, String message,
            Throwable cause) {
        if (log.isWarnEnabled()) {
            log.warn(getPrefix(session) + message, cause);
        }
    }

    public static void error(IoSession session, Object message) {
        error(getLogger(session), session, message);
    }

    public static void error(Logger log, IoSession session, Object message) {
        if (log.isErrorEnabled()) {
            log.error("{}{}", getPrefix(session), message);
        }
    }

    public static void error(IoSession session, Throwable cause) {
        error(session, "Unexpected exception.", cause);
    }

    public static void error(IoSession session, String message, Throwable cause) {
        error(getLogger(session), session, message, cause);
    }

    public static void error(Logger log, IoSession session, String message,
            Throwable cause) {
        if (log.isErrorEnabled()) {
            log.error(getPrefix(session) + message, cause);
        }
    }

    public static boolean isDebugEnabled(IoSession session) {
        return getLogger(session).isDebugEnabled();
    }

    public static boolean isInfoEnabled(IoSession session) {
        return getLogger(session).isInfoEnabled();
    }

    public static boolean isWarnEnabled(IoSession session) {
        return getLogger(session).isWarnEnabled();
    }

    public static boolean isErrorEnabled(IoSession session) {
        return getLogger(session).isErrorEnabled();
    }

    public static String getPrefix(IoSession session) {
        String prefix = (String) session.getAttribute(PREFIX);
        if (prefix == null) {
            prefix = "[" + session.getRemoteAddress() + "] ";
            setPrefix(session, prefix);
        }
        return prefix;
    }

    public static void setPrefix(IoSession session, String prefix) {
        session.setAttribute(PREFIX, prefix);
    }

    public static Logger getLogger(IoSession session) {
        Logger log = (Logger) session.getAttribute(LOGGER);
        if (log == null) {
            log = LoggerFactory.getLogger(getClass(session).getName() + "."
                    + IoSessionLogger.class.getSimpleName());
            setLogger(session, log);
        }
        return log;
    }

    public static void setLogger(IoSession session, Logger log) {
        session.setAttribute(LOGGER, log);
    }
}
