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
package org.apache.mina.filter.reqres;

import org.apache.mina.core.RuntimeIoException;

/**
 * An {@link RuntimeIoException} which is thrown when a {@link Request} is timed out.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RequestTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 5546784978950631652L;

    private final Request request;

    /**
     * Creates a new exception.
     */
    public RequestTimeoutException(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        this.request = request;
    }

    /**
     * Creates a new exception.
     */
    public RequestTimeoutException(Request request, String s) {
        super(s);
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        this.request = request;
    }

    /**
     * Creates a new exception.
     */
    public RequestTimeoutException(Request request, String message,
            Throwable cause) {
        super(message);
        initCause(cause);
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        this.request = request;
    }

    /**
     * Creates a new exception.
     */
    public RequestTimeoutException(Request request, Throwable cause) {
        initCause(cause);
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        this.request = request;
    }

    /**
     * Returns the request which has timed out.
     */
    public Request getRequest() {
        return request;
    }
}