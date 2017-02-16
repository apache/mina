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
package org.apache.mina.core.buffer;

/**
 * A {@link RuntimeException} which is thrown when the data the {@link IoBuffer}
 * contains is corrupt.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class BufferDataException extends RuntimeException {
    private static final long serialVersionUID = -4138189188602563502L;

    /**
     * Create a new BufferDataException instance
     */
    public BufferDataException() {
        super();
    }

    /**
     * Create a new BufferDataException instance
     * 
     * @param message The exception message
     */
    public BufferDataException(String message) {
        super(message);
    }

    /**
     * Create a new BufferDataException instance
     * 
     * @param message The exception message
     * @param cause The original cause
     */
    public BufferDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new BufferDataException instance
     * @param cause The original cause
     */
    public BufferDataException(Throwable cause) {
        super(cause);
    }

}
