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

import java.net.ProtocolException;
import java.util.Map;
import java.util.HashMap;

/**
 * TODO HttpRequestMessage.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HttpRequestMessage extends HttpMessage {

    public static String REQUEST_GET = "GET";

    public static String REQUEST_POST = "POST";

    public static String REQUEST_HEAD = "HEAD";

    public static String REQUEST_OPTIONS = "OPTIONS";

    public static String REQUEST_PUT = "PUT";

    public static String REQUEST_DELETE = "DELETE";

    public static String REQUEST_TRACE = "TRACE";

    private String requestMethod = REQUEST_GET;

    private String path;

    private Map<String, String> parameters = new HashMap<String, String>();

    public HttpRequestMessage(String path) {
        this.path = path;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) throws ProtocolException {
        if (requestMethod.equals(REQUEST_GET)
                || requestMethod.equals(REQUEST_POST)
                || requestMethod.equals(REQUEST_HEAD)
                || requestMethod.equals(REQUEST_OPTIONS)
                || requestMethod.equals(REQUEST_PUT)
                || requestMethod.equals(REQUEST_DELETE)
                || requestMethod.equals(REQUEST_TRACE)) {
            this.requestMethod = requestMethod;
            return;
        }

        throw new ProtocolException("Invalid request method type.");
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (path == null || path.trim().length() == 0)
            path = "/";
        this.path = path;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }
}
