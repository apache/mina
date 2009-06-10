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
package org.apache.mina.proxy.handlers.http;

import java.util.List;
import java.util.Map;

/**
 * HttpProxyResponse.java - Wrapper class for HTTP requests.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpProxyResponse {
    /**
     * The HTTP response protocol version.
     */
    public final String httpVersion;

    /**
     * The HTTP response status line.
     */
    public final String statusLine;

    /**
     * The HTTP response status code;
     */
    public final int statusCode;

    /**
     * The HTTP response headers.
     */
    public final Map<String, List<String>> headers;

    /**
     * The HTTP response body.
     */
    public String body;

    /**
     * Constructor of an HTTP proxy response.
     *  
     * @param httpVersion the protocol version
     * @param statusLine the response status line
     * @param headers the response headers
     */
    protected HttpProxyResponse(final String httpVersion,
            final String statusLine, final Map<String, List<String>> headers) {
        this.httpVersion = httpVersion;
        this.statusLine = statusLine;

        // parses the status code from the status line
        this.statusCode = statusLine.charAt(0) == ' ' ? Integer
                .parseInt(statusLine.substring(1, 4)) : Integer
                .parseInt(statusLine.substring(0, 3));

        this.headers = headers;
    }

    /**
     * Returns the HTTP response protocol version.
     */
    public final String getHttpVersion() {
        return httpVersion;
    }

    /**
     * Returns the HTTP response status code.
     */
    public final int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP response status line.
     */
    public final String getStatusLine() {
        return statusLine;
    }

    /**
     * Returns the HTTP response body.
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the HTTP response body.
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Returns the HTTP response headers.
     */
    public final Map<String, List<String>> getHeaders() {
        return headers;
    }
}