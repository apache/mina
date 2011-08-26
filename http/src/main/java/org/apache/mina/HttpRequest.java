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
package org.apache.mina;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMultimap;

public class HttpRequest {

    private final String requestLine;
    private final HttpVerb method;
    private final String requestedPath; // correct name?
    private final String version;
    private Map<String, String> headers;
    private ImmutableMultimap<String, String> parameters;
    private String body;
    private boolean keepAlive;
    private InetAddress remoteHost;
    private InetAddress serverHost;
    private int remotePort;
    private int serverPort;

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");
    /** Regex to parse out QueryString from HttpRequest */
    public static final Pattern QUERY_STRING_PATTERN = Pattern.compile("\\?");
    /** Regex to parse out parameters from query string */
    public static final Pattern PARAM_STRING_PATTERN = Pattern.compile("\\&|;"); // Delimiter is either & or ;
    /** Regex to parse out key/value pairs */
    public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("=");
    /** Regex to parse raw headers and body */
    public static final Pattern RAW_VALUE_PATTERN = Pattern.compile("\\r\\n\\r\\n"); // TODO fix a better regexp for
                                                                                     // this
    /** Regex to parse raw headers from body */
    public static final Pattern HEADERS_BODY_PATTERN = Pattern.compile("\\r\\n");
    /** Regex to parse header name and value */
    public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(": ");

    /**
     * Creates a new HttpRequest
     * 
     * @param requestLine The Http request text line
     * @param headers The Http request headers
     */
    public HttpRequest(String requestLine, Map<String, String> headers) {
        this.requestLine = requestLine;
        String[] elements = REQUEST_LINE_PATTERN.split(requestLine);
        method = HttpVerb.valueOf(elements[0]);
        String[] pathFrags = QUERY_STRING_PATTERN.split(elements[1]);
        requestedPath = pathFrags[0];
        version = elements[2];
        this.headers = headers;
        body = null;
        initKeepAlive();
        parameters = parseParameters(elements[1]);
    }

    /**
     * Creates a new HttpRequest
     * 
     * @param requestLine The Http request text line
     * @param headers The Http request headers
     * @param body The Http request posted body
     */
    public HttpRequest(String requestLine, Map<String, String> headers, String body) {
        this(requestLine, headers);
        this.body = body;
    }

    public static HttpRequest of(ByteBuffer buffer) {
        try {
            String raw = new String(buffer.array(), 0, buffer.limit(), Charsets.ISO_8859_1);
            String[] headersAndBody = RAW_VALUE_PATTERN.split(raw);
            String[] headerFields = HEADERS_BODY_PATTERN.split(headersAndBody[0]);
            headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

            String requestLine = headerFields[0];
            Map<String, String> generalHeaders = new HashMap<String, String>();
            for (int i = 1; i < headerFields.length; i++) {
                String[] header = HEADER_VALUE_PATTERN.split(headerFields[i]);
                generalHeaders.put(header[0].toLowerCase(), header[1]);
            }

            String body = "";
            for (int i = 1; i < headersAndBody.length; ++i) { // First entry contains headers
                body += headersAndBody[i];
            }

            if (requestLine.contains("POST")) {
                int contentLength = Integer.parseInt(generalHeaders.get("content-length"));
                if (contentLength > body.length()) {
                    return new PartialHttpRequest(requestLine, generalHeaders, body);
                }
            }
            return new HttpRequest(requestLine, generalHeaders, body);
        } catch (Exception t) {
            return MalFormedHttpRequest.instance;
        }
    }

    public static HttpRequest continueParsing(ByteBuffer buffer, PartialHttpRequest unfinished) {
        String nextChunk = new String(buffer.array(), 0, buffer.limit(), Charsets.US_ASCII);
        unfinished.appendBody(nextChunk);

        int contentLength = Integer.parseInt(unfinished.getHeader("Content-Length"));
        if (contentLength > unfinished.getBody().length()) {
            return unfinished;
        } else {
            return new HttpRequest(unfinished.getRequestLine(), unfinished.getHeaders(), unfinished.getBody());
        }
    }

    public String getRequestLine() {
        return requestLine;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public HttpVerb getMethod() {
        return method;
    }

    /**
     * Returns the value of a request parameter as a String, or null if the parameter does not exist.
     * 
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String). If you use this method with a multi-valued
     * parameter, the value returned is equal to the first value in the array returned by getParameterValues.
     */
    public String getParameter(String name) {
        Collection<String> values = parameters.get(name);
        return values.isEmpty() ? null : values.iterator().next();
    }

    public Map<String, Collection<String>> getParameters() {
        return parameters.asMap();
    }

    public String getBody() {
        return body;
    }

    public InetAddress getRemoteHost() {
        return remoteHost;
    }

    public InetAddress getServerHost() {
        return serverHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getServerPort() {
        return serverPort;
    }

    protected void setRemoteHost(InetAddress host) {
        remoteHost = host;
    }

    protected void setServerHost(InetAddress host) {
        serverHost = host;
    }

    protected void setRemotePort(int port) {
        remotePort = port;
    }

    protected void setServerPort(int port) {
        serverPort = port;
    }

    /**
     * Returns a collection of all values associated with the provided parameter. If no values are found and empty
     * collection is returned.
     */
    public Collection<String> getParameterValues(String name) {
        return parameters.get(name);
    }

    public boolean isKeepAlive() {
        return keepAlive;
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

        result += "--- PARAMETERS --- \n";
        for (String key : parameters.keySet()) {
            Collection<String> values = parameters.get(key);
            for (String value : values) {
                result += key + ":" + value + "\n";
            }
        }
        return result;
    }

    private ImmutableMultimap<String, String> parseParameters(String requestLine) {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        String[] str = QUERY_STRING_PATTERN.split(requestLine);

        // Parameters exist
        if (str.length > 1) {
            String[] paramArray = PARAM_STRING_PATTERN.split(str[1]);
            for (String keyValue : paramArray) {
                String[] keyValueArray = KEY_VALUE_PATTERN.split(keyValue);
                // We need to check if the parameter has a value associated with it.
                if (keyValueArray.length > 1) {
                    builder.put(keyValueArray[0], keyValueArray[1]); // name, value
                }
            }
        }
        return builder.build();
    }

    private void initKeepAlive() {
        String connection = getHeader("Connection");
        if ("keep-alive".equalsIgnoreCase(connection)) {
            keepAlive = true;
        } else if ("close".equalsIgnoreCase(connection) || requestLine.contains("1.0")) {
            keepAlive = false;
        } else {
            keepAlive = true;
        }
    }

}
