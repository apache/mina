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
import org.slf4j.Marker;

/**
 * A SLF4J {@link Logger} that prepends the prefix to all logging messages.
 * The default prefix is the remote address of the specified {@link IoSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class IoSessionLogger implements Logger {

    private static final String UNEXPECTED_EXCEPTION = "Unexpected exception.";

    private static final AttributeKey PREFIX = new AttributeKey(
            IoSessionLogger.class, "prefix");

    private static boolean usePrefix = true;

    public static IoSessionLogger getLogger(IoSession session) {
        return getLogger(session, session.getHandler().getClass());
    }

    public static IoSessionLogger getLogger(IoSession session, String name) {
        if (name != null) {
            return getLogger(session, LoggerFactory.getLogger(name));
        } else {
            return getLogger(session, (Logger) null);
        }
    }

    public static IoSessionLogger getLogger(IoSession session, Class<?> clazz) {
        if (clazz != null) {
            return getLogger(session, LoggerFactory.getLogger(clazz));
        } else {
            return getLogger(session, (Logger) null);
        }
    }

    public static IoSessionLogger getLogger(IoSession session, Logger logger) {
        if (logger == null) {
            logger = LoggerFactory.getLogger(session.getHandler().getClass());
        }

        IoSessionLogger decoratedLogger = (IoSessionLogger) session.getAttribute(logger);
        if (decoratedLogger == null) {
            decoratedLogger = new IoSessionLogger(session, logger);
            IoSessionLogger newDecoratedLogger =
                (IoSessionLogger) session.setAttributeIfAbsent(logger, decoratedLogger);
            if (newDecoratedLogger != null) {
                decoratedLogger = newDecoratedLogger;
            }
        }

        return decoratedLogger;
    }

    public static boolean isUsePrefix() {
        return usePrefix;
    }

    public static void setUsePrefix(boolean usePrefix) {
        IoSessionLogger.usePrefix = usePrefix;
    }

    public static String getPrefix(IoSession session) {
        String prefix = (String) session.getAttribute(PREFIX);
        if (prefix == null) {
            return "";
        } else {
            return prefix;
        }
    }

    public static void setPrefix(IoSession session, String prefix) {
        if (prefix == null || !usePrefix) {
            removePrefix(session);
            return;
        }

        if (prefix.contains("{}")) {
            throw new IllegalArgumentException("prefix cannot contain '{}': "
                    + prefix);
        }

        session.setAttribute(PREFIX, prefix);
    }

    public static void removePrefix(IoSession session) {
        session.removeAttribute(PREFIX);
    }

    private final IoSession session;

    private final Logger logger;

    private IoSessionLogger(IoSession session, Logger logger) {
        if (session == null) {
            throw new NullPointerException("session");
        }

        if (logger == null) {
            throw new NullPointerException("logger");
        }

        this.session = session;
        this.logger = logger;

        // Set the default prefix if not set already.
        if (usePrefix && session.containsAttribute(PREFIX)) {
            session.setAttributeIfAbsent(PREFIX, "["
                    + session.getRemoteAddress() + "] ");
        }
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(marker, getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.debug(marker, format, arg1, arg2);
        }
    }

    public void debug(Marker marker, String format, Object arg) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(marker, getPrefix(session) + format, arg);
            }
        } else {
            logger.debug(marker, format, arg);
        }
    }

    public void debug(Marker marker, String format, Object[] args) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(marker, getPrefix(session) + format, args);
            }
        } else {
            logger.debug(marker, format, args);
        }
    }

    public void debug(Marker marker, String msg, Throwable t) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(marker, getPrefix(session) + msg, t);
            }
        } else {
            logger.debug(marker, msg, t);
        }
    }

    public void debug(Marker marker, String msg) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(marker, getPrefix(session) + msg);
            }
        } else {
            logger.debug(marker, msg);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.debug(format, arg1, arg2);
        }
    }

    public void debug(String format, Object arg) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(getPrefix(session) + format, arg);
            }
        } else {
            logger.debug(format, arg);
        }
    }

    public void debug(String format, Object[] args) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(getPrefix(session) + format, args);
            }
        } else {
            logger.debug(format, args);
        }
    }

    public void debug(String msg, Throwable t) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(getPrefix(session) + msg, t);
            }
        } else {
            logger.debug(msg, t);
        }
    }

    public void debug(Throwable t) {
        debug(UNEXPECTED_EXCEPTION, t);
    }

    public void debug(String msg) {
        if (usePrefix) {
            if (isDebugEnabled()) {
                logger.debug(getPrefix(session) + msg);
            }
        } else {
            logger.debug(msg);
        }
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(marker, getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.error(marker, format, arg1, arg2);
        }
    }

    public void error(Marker marker, String format, Object arg) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(marker, getPrefix(session) + format, arg);
            }
        } else {
            logger.error(marker, format, arg);
        }
    }

    public void error(Marker marker, String format, Object[] args) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(marker, getPrefix(session) + format, args);
            }
        } else {
            logger.error(marker, format, args);
        }
    }

    public void error(Marker marker, String msg, Throwable t) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(marker, getPrefix(session) + msg, t);
            }
        } else {
            logger.error(marker, msg, t);
        }
    }

    public void error(Marker marker, String msg) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(marker, getPrefix(session) + msg);
            }
        } else {
            logger.error(marker, msg);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.error(format, arg1, arg2);
        }
    }

    public void error(String format, Object arg) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(getPrefix(session) + format, arg);
            }
        } else {
            logger.error(format, arg);
        }
    }

    public void error(String format, Object[] args) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(getPrefix(session) + format, args);
            }
        } else {
            logger.error(format, args);
        }
    }

    public void error(String msg, Throwable t) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(getPrefix(session) + msg, t);
            }
        } else {
            logger.error(msg, t);
        }
    }

    public void error(Throwable t) {
        error(UNEXPECTED_EXCEPTION, t);
    }

    public void error(String msg) {
        if (usePrefix) {
            if (isErrorEnabled()) {
                logger.error(getPrefix(session) + msg);
            }
        } else {
            logger.error(msg);
        }
    }

    public String getName() {
        return logger.getName();
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(marker, getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.info(marker, format, arg1, arg2);
        }
    }

    public void info(Marker marker, String format, Object arg) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(marker, getPrefix(session) + format, arg);
            }
        } else {
            logger.info(marker, format, arg);
        }
    }

    public void info(Marker marker, String format, Object[] args) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(marker, getPrefix(session) + format, args);
            }
        } else {
            logger.info(marker, format, args);
        }
    }

    public void info(Marker marker, String msg, Throwable t) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(marker, getPrefix(session) + msg, t);
            }
        } else {
            logger.info(marker, msg, t);
        }
    }

    public void info(Marker marker, String msg) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(marker, getPrefix(session) + msg);
            }
        } else {
            logger.info(marker, msg);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.info(format, arg1, arg2);
        }
    }

    public void info(String format, Object arg) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(getPrefix(session) + format, arg);
            }
        } else {
            logger.info(format, arg);
        }
    }

    public void info(String format, Object[] args) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(getPrefix(session) + format, args);
            }
        } else {
            logger.info(format, args);
        }
    }

    public void info(String msg, Throwable t) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(getPrefix(session) + msg, t);
            }
        } else {
            logger.info(msg, t);
        }
    }

    public void info(Throwable t) {
        info(UNEXPECTED_EXCEPTION, t);
    }

    public void info(String msg) {
        if (usePrefix) {
            if (isInfoEnabled()) {
                logger.info(getPrefix(session) + msg);
            }
        } else {
            logger.info(msg);
        }
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(marker, getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.trace(marker, format, arg1, arg2);
        }
    }

    public void trace(Marker marker, String format, Object arg) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(marker, getPrefix(session) + format, arg);
            }
        } else {
            logger.trace(marker, format, arg);
        }
    }

    public void trace(Marker marker, String format, Object[] args) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(marker, getPrefix(session) + format, args);
            }
        } else {
            logger.trace(marker, format, args);
        }
    }

    public void trace(Marker marker, String msg, Throwable t) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(marker, getPrefix(session) + msg, t);
            }
        } else {
            logger.trace(marker, msg, t);
        }
    }

    public void trace(Marker marker, String msg) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(marker, getPrefix(session) + msg);
            }
        } else {
            logger.trace(marker, msg);
        }
    }

    public void trace(String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.trace(format, arg1, arg2);
        }
    }

    public void trace(String format, Object arg) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(getPrefix(session) + format, arg);
            }
        } else {
            logger.trace(format, arg);
        }
    }

    public void trace(String format, Object[] args) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(getPrefix(session) + format, args);
            }
        } else {
            logger.trace(format, args);
        }
    }

    public void trace(String msg, Throwable t) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(getPrefix(session) + msg, t);
            }
        } else {
            logger.trace(msg, t);
        }
    }

    public void trace(Throwable t) {
        trace(UNEXPECTED_EXCEPTION, t);
    }

    public void trace(String msg) {
        if (usePrefix) {
            if (isTraceEnabled()) {
                logger.trace(getPrefix(session) + msg);
            }
        } else {
            logger.trace(msg);
        }
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(marker, getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.warn(marker, format, arg1, arg2);
        }
    }

    public void warn(Marker marker, String format, Object arg) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(marker, getPrefix(session) + format, arg);
            }
        } else {
            logger.warn(marker, format, arg);
        }
    }

    public void warn(Marker marker, String format, Object[] args) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(marker, getPrefix(session) + format, args);
            }
        } else {
            logger.warn(marker, format, args);
        }
    }

    public void warn(Marker marker, String msg, Throwable t) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(marker, getPrefix(session) + msg, t);
            }
        } else {
            logger.warn(marker, msg, t);
        }
    }

    public void warn(Marker marker, String msg) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(marker, getPrefix(session) + msg);
            }
        } else {
            logger.warn(marker, msg);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(getPrefix(session) + format, arg1, arg2);
            }
        } else {
            logger.warn(format, arg1, arg2);
        }
    }

    public void warn(String format, Object arg) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(getPrefix(session) + format, arg);
            }
        } else {
            logger.warn(format, arg);
        }
    }

    public void warn(String format, Object[] args) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(getPrefix(session) + format, args);
            }
        } else {
            logger.warn(format, args);
        }
    }

    public void warn(String msg, Throwable t) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(getPrefix(session) + msg, t);
            }
        } else {
            logger.warn(msg, t);
        }
    }

    public void warn(Throwable t) {
        warn(UNEXPECTED_EXCEPTION, t);
    }

    public void warn(String msg) {
        if (usePrefix) {
            if (isWarnEnabled()) {
                logger.warn(getPrefix(session) + msg);
            }
        } else {
            logger.warn(msg);
        }
    }

    @Override
    public String toString() {
        return logger.toString();
    }
}
