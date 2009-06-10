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

/**
 * Defines a logging level.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 * @see NoopFilter
 */
public enum LogLevel {

    /**
     * {@link LogLevel} which logs messages on the TRACE level.
     */
    TRACE(5),
    
    /**
     * {@link LogLevel} which logs messages on the DEBUG level.
     */
    DEBUG(4),
    
    /**
     * {@link LogLevel} which logs messages on the INFO level.
     */
    INFO(3),
    
    /**
     * {@link LogLevel} which logs messages on the WARN level.
     */
    WARN(2),
    
    /**
     * {@link LogLevel} which logs messages on the ERROR level.
     */
    ERROR(1),
    
    /**
     * {@link LogLevel} which will not log any information
     */
    NONE(0);

    /** The internal numeric value associated with the log level */
    private int level;
    
    /**
     * Create a new instance of a LogLevel.
     * 
     * @param level The log level
     */
    private LogLevel(int level) {
        this.level = level;
    }
    
    
    /**
     * @return The numeric value associated with the log level 
     */
    public int getLevel() {
        return level;
    }
}