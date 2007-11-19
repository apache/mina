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
package org.apache.mina.filter.codec.http;

/**
 * Type-safe enumeration of standard HTTP Methods
 *
 * @author irvingd
 *
 */
public enum HttpMethod {

    /**
     *  A request for information about the communication options
     *  available on the request/response chain
     */
    OPTIONS(true),

    /**
     * Retrieve whatever information (in the form of an entity)
     * is identified by the Request-URI
     */
    GET(true),

    /**
     * Identical to GET except that the server MUST NOT return a
     * message-body in the response
     */
    HEAD(false),

    /**
     * Requests that the origin server accept the entity enclosed in the
     * request as a new subordinate of the resource identified by the
     * Request-URI in the Request-Line
     */
    POST(true),

    /**
     * Requests that the enclosed entity be stored under the supplied Request-URI
     */
    PUT(true),

    /**
     * Requests that the origin server delete the resource identified
     * by the Request-URI
     */
    DELETE(true),

    /**
     * Requests a remote, application-layer loop- back of the request message
     */
    TRACE(true),

    /**
     * Reserved
     */
    CONNECT(true), ;

    private boolean responseBodyAllowed;

    private HttpMethod(boolean responseBodyAllowed) {
        this.responseBodyAllowed = responseBodyAllowed;
    }

    /**
     * Returns <code>true</code> if a response to this method is allowed to contain a message body.
     */
    public boolean isResponseBodyAllowed() {
        return responseBodyAllowed;
    }
}
