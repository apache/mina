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
package org.apache.mina.http;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpPduEncodingVisitor;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpVersion;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HttpRequestImpl implements HttpRequest {

    private final HttpVersion version;

    private final HttpMethod method;

    private final String targetURI;

    private final Map<String, String> headers;

    public HttpRequestImpl(HttpVersion version, HttpMethod method, String targetURI, Map<String, String> headers) {
        this.version = version;
        this.method = method;
        this.targetURI = targetURI;
        this.headers = Collections.unmodifiableMap(headers);
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
    public String getTargetURI() {
        return targetURI;
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
        // TODO check headers
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
    public boolean containsParameter(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParameter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public ByteBuffer encode(HttpPduEncodingVisitor visitor) {
        return visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP REQUEST METHOD: ").append(method).append('\n');
        sb.append("VERSION: ").append(version).append('\n');
        sb.append("PATH: ").append(targetURI).append('\n');

        sb.append("--- HEADER --- \n");

        for (String key : headers.keySet()) {
            String value = headers.get(key);
            sb.append(key).append(':').append(value).append('\n');
        }

        return sb.toString();
    }
}