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
package org.apache.mina.util;

import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utility methods to log protocol-specific messages.
 * <p>
 * Set {@link #PREFIX} and {@link #LOGGER} session attributes
 * to override prefix string and logger.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 *
 */
public class SessionLog {
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = SessionLog.class.getName() + ".prefix";

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = SessionLog.class.getName() + ".logger";

    private static Class getClass(IoSession session) {
        return session.getHandler().getClass();
    }

    public static void debug(IoSession session, String message) {
        Logger log = getLogger(session);
        if (log.isDebugEnabled()) {
            log.debug(String.valueOf(session.getAttribute(PREFIX)) + message);
        }
    }

    public static void debug(IoSession session, String message, Throwable cause) {
        Logger log = getLogger(session);
        if (log.isDebugEnabled()) {
            log.debug(String.valueOf(session.getAttribute(PREFIX)) + message,
                    cause);
        }
    }

    public static void info(IoSession session, String message) {
        Logger log = getLogger(session);
        if (log.isInfoEnabled()) {
            log.info(String.valueOf(session.getAttribute(PREFIX)) + message);
        }
    }

    public static void info(IoSession session, String message, Throwable cause) {
        Logger log = getLogger(session);
        if (log.isInfoEnabled()) {
            log.info(String.valueOf(session.getAttribute(PREFIX)) + message,
                    cause);
        }
    }

    public static void warn(IoSession session, String message) {
        Logger log = getLogger(session);
        if (log.isWarnEnabled()) {
            log.warn(String.valueOf(session.getAttribute(PREFIX)) + message);
        }
    }

    public static void warn(IoSession session, String message, Throwable cause) {
        Logger log = getLogger(session);
        if (log.isWarnEnabled()) {
            log.warn(String.valueOf(session.getAttribute(PREFIX)) + message,
                    cause);
        }
    }

    public static void error(IoSession session, String message) {
        Logger log = getLogger(session);
        if (log.isErrorEnabled()) {
            log.error(String.valueOf(session.getAttribute(PREFIX)) + message);
        }
    }

    public static void error(IoSession session, String message, Throwable cause) {
        Logger log = getLogger(session);
        if (log.isErrorEnabled()) {
            log.error(String.valueOf(session.getAttribute(PREFIX)) + message,
                    cause);
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

    private static Logger getLogger(IoSession session) {
        Logger log = (Logger) session.getAttribute(LOGGER);
        if (log == null) {
            log = LoggerFactory.getLogger(getClass(session));
            String prefix = (String) session.getAttribute(PREFIX);
            if (prefix == null) {
                prefix = "[" + session.getRemoteAddress() + "] ";
                session.setAttribute(PREFIX, prefix);
            }

            session.setAttribute(LOGGER, log);
        }

        return log;
    }
}
