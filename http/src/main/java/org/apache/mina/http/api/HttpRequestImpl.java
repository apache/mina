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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpRequestImpl implements HttpRequest {

    private final HttpVersion version;
    private final HttpMethod method;
    private final String requestedPath;
    private final Map<String, String> headers;

    public HttpRequestImpl(HttpVersion version, HttpMethod method, String requestedPath, Map<String, String> headers) {
        this.version = version;
        this.method = method;
        this.requestedPath = requestedPath;
        this.headers = Collections.unmodifiableMap(headers);
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return version;
    }

    @Override
    public String getContentType() {
        return headers.get("content-type");
    }

    @Override
    public boolean isKeepAlive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean containsParameter(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getParameter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String toString() {
        String result = "METHOD: " + method + "\n";
        result += "VERSION: " + version + "\n";
        result += "PATH: " + requestedPath + "\n";

        result += "--- HEADER --- \n";
        for (String key : headers.keySet()) {
            String value = headers.get(key);
            result += key + ":" + value + "\n";
        }

        /*
         * result += "--- PARAMETERS --- \n"; for (String key : parameters.keySet()) { Collection<String> values =
         * parameters.get(key); for (String value : values) { result += key + ":" + value + "\n"; } }
         */
        return result;
    }
}
