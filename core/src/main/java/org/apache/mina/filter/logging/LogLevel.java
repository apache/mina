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

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
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
        public void log(Logger logger, String message) {
        }

        public void log(Logger logger, String message, Throwable cause) {
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the TRACE level.
     */
    TRACE(new LogLevelLogger() {
        public void log(Logger logger, String message) {
            logger.trace(message);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.trace(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the DEBUG level.
     */
    DEBUG(new LogLevelLogger() {
        public void log(Logger logger, String message) {
            logger.debug(message);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.debug(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the INFO level.
     */
    INFO(new LogLevelLogger() {
        public void log(Logger logger, String message) {
            logger.info(message);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.info(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the WARN level.
     */
    WARN(new LogLevelLogger() {
        public void log(Logger logger, String message) {
            logger.warn(message);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.warn(message, cause);
        }
    }),

    /**
     * {@link LogLevel} which logs messages on the ERROR level.
     */
    ERROR(new LogLevelLogger() {
        public void log(Logger logger, String message) {
            logger.error(message);
        }

        public void log(Logger logger, String message, Throwable cause) {
            logger.error(message, cause);
        }
    });
          
    private final LogLevelLogger logger;
    
    private LogLevel(LogLevelLogger logger) {
        this.logger = logger;
    }
    
    void log(IoSession session, String name, String message) {
        this.logger.log(IoSessionLogger.getLogger(session, name), message);
    }
    
    void log(IoSession session, String name, String message, Throwable cause) {
        this.logger.log(IoSessionLogger.getLogger(session, name), message, cause);
    }
    
    private interface LogLevelLogger {
        void log(Logger logger, String message);
        void log(Logger logger, String message, Throwable cause);
    }
}