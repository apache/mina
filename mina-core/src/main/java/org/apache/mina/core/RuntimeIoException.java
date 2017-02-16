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
package org.apache.mina.core;

/**
 * A unchecked version of {@link IOException}.
 * <p>
 * Please note that {@link RuntimeIoException} is different from
 * {@link IOException} in that doesn't trigger force session close,
 * while {@link IOException} forces disconnection.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RuntimeIoException extends RuntimeException {
    private static final long serialVersionUID = 9029092241311939548L;

    /**
     * Create a new RuntimeIoException instance
     */
    public RuntimeIoException() {
        super();
    }

    /**
     * Create a new RuntimeIoException instance
     * 
     * @param message The error message
     */
    public RuntimeIoException(String message) {
        super(message);
    }

    /**
     * Create a new RuntimeIoException instance
     * 
     * @param message The error message
     * @param cause The original exception
     */
    public RuntimeIoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new RuntimeIoException instance
     * 
     * @param cause The original exception
     */
    public RuntimeIoException(Throwable cause) {
        super(cause);
    }
}
