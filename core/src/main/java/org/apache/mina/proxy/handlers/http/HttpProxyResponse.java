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
 * @author James Furness <a href="mailto:james.furness@lehman.com">james.furness@lehman.com</a>
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a>
 * @version $Id: $
 */
public class HttpProxyResponse {
    public final String httpVersion;

    public final String statusLine;

    public final int statusCode;

    public final Map<String, List<String>> headers;

    public String body;

    protected HttpProxyResponse(final String httpVersion,
            final String statusLine, final Map<String, List<String>> headers) {
        this.httpVersion = httpVersion;
        this.statusLine = statusLine;

        this.statusCode = statusLine.charAt(0) == ' ' ? Integer
                .parseInt(statusLine.substring(1, 4)) : Integer
                .parseInt(statusLine.substring(0, 3));

        this.headers = headers;
    }

    /**
     * The HTTP version.
     */
    public final String getHttpVersion() {
        return httpVersion;
    }

    /**
     * The HTTP status code.
     */
    public final int getStatusCode() {
        return statusCode;
    }

    /**
     * The HTTP status line.
     */
    public final String getStatusLine() {
        return statusLine;
    }

    /**
     * The HTTP entity body.
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the HTTP entity body.
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * HTTP headers.
     */
    public final Map<String, List<String>> getHeaders() {
        return headers;
    }
}