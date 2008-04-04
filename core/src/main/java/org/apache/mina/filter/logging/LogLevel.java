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

import org.slf4j.Logger;

/**
 * Defines a logging level.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see LoggingFilter
 */
public enum LogLevel {

    /**
     * {@link LogLevel} which will not log any information
     */
    NONE(new LogLevelLogger() {
        public void log(Logger logger, String message, Object arg) {
            // Do nothing.
        }

        public void log(Logger logger, String message, Object[] args) {
            // Do nothing.
        }

        public void log(Logger logger, String message, Throwable cause) {
            // Do nothing.
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the TRACE level.
     */
    TRACE(new LogLevelLogger() {
        public void log(Logger logger, String message, Object arg) {
            logger.trace(message, arg);
        }

        public void log(Logger logger, String message, Object[] args) {
            logger.trace(message, args);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.trace(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the DEBUG level.
     */
    DEBUG(new LogLevelLogger() {
        public void log(Logger logger, String message, Object arg) {
            logger.debug(message, arg);
        }

        public void log(Logger logger, String message, Object[] args) {
            logger.debug(message, args);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.debug(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the INFO level.
     */
    INFO(new LogLevelLogger() {
        public void log(Logger logger, String message, Object arg) {
            logger.info(message, arg);
        }

        public void log(Logger logger, String message, Object[] args) {
            logger.info(message, args);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.info(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the WARN level.
     */
    WARN(new LogLevelLogger() {
        public void log(Logger logger, String message, Object arg) {
            logger.warn(message, arg);
        }

        public void log(Logger logger, String message, Object[] args) {
            logger.warn(message, args);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.warn(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the ERROR level.
     */
    ERROR(new LogLevelLogger() {
        public void log(Logger logger, String message, Object arg) {
            logger.error(message, arg);
        }

        public void log(Logger logger, String message, Object[] args) {
            logger.error(message, args);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.error(message, cause);
        }
    });

    private final LogLevelLogger logger;

    private LogLevel(LogLevelLogger logger) {
        this.logger = logger;
    }

    void log(Logger logger, String format, Object arg) {
        this.logger.log(logger, format, arg);
    }

    void log(Logger logger, String format, Object[] args) {
        this.logger.log(logger, format, args);
    }

    void log(Logger logger, String message, Throwable cause) {
        this.logger.log(logger, message, cause);
    }

    private interface LogLevelLogger {
        void log(Logger logger, String message, Object arg);
        void log(Logger logger, String message, Object[] args);
        void log(Logger logger, String message, Throwable cause);
    }
}