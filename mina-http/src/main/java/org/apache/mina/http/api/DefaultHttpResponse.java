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
package org.apache.mina.http.api;

import java.util.Map;

/**
 * The default implementation for the HTTP response element.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultHttpResponse implements HttpResponse {

    private final HttpVersion version;

    private final HttpStatus status;

    private final Map<String, String> headers;

    /**
     * Creates a new DefaultHttpResponse instance
     * 
     * @param version The HTTP version
     * @param status The HTTP status
     * @param headers The HTTP headers
     */
    public DefaultHttpResponse(HttpVersion version, HttpStatus status, Map<String, String> headers) {
        this.version = version;
        this.status = status;
        this.headers = headers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpVersion getProtocolVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        return headers.get("content-type");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isKeepAlive() {
        // TODO check header and version for keep alive
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP RESPONSE STATUS: " ).append(status).append('\n');
        sb.append("VERSION: ").append(version).append('\n');
        
        sb.append("-- HEADER --- \n");
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }

        return sb.toString();
    }
}
