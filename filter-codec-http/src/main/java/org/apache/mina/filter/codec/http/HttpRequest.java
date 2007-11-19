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

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A HTTP request.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface HttpRequest extends HttpMessage {

    /**
     * Determines whether this request contains at least one parameter with
     * the specified name
     *
     * @param name  The parameter name
     * @return      <code>true</code> if this request contains at least one parameter
     *              with the specified name
     */
    boolean containsParameter(String name);

    /**
     * Returns the value of a request parameter as a String,
     * or null if the parameter does not exist.
     *
     * If the request contained multiple parameters with the same name,
     * this method returns the first parameter encountered in the request
     * with the specified name
     *
     * @param name  The parameter name
     * @return      The value
     */
    String getParameter(String name);

    /**
     * Returns a read only {@link Map} of query parameters whose key is a {@link String} and
     * whose value is a {@link List} of {@link String}s.
     */
    Map<String, List<String>> getParameters();

    /**
     * Returns the {@link HttpMethod} associated with this request.
     */
    HttpMethod getMethod();

    /**
     * Returns the URI of the request.
     */
    URI getRequestUri();

    /**
     * Determines whether the HTTP connection should remain open
     * after handling this request.
     * If the request is a <code>HTTP/1.1</code> request, we keep
     * the connection alive unless an explicit <code>"Connection: close"</code>
     * header is sent.<br/>
     * Otherwise, the connection is only kept alive if an explicit
     * <code>"Connection: keep-alive"</code> header is sent
     *
     * @return  <code>true</code> iff the connection should remain
     *          open following the handling of this request
     */
    boolean isKeepAlive();

    /**
     * Determines whether this request requires a "100-continue" response.
     * A client may set a continuation expectation when sending a request
     * before continuing to send the body of a request (e.g. because it
     * would be inefficient to send the whole body if the server will
     * reject the request based on the headers alone)<br/>
     * If this request requires a continuation response, it should be
     * sent to the client if the server is prepared to handle the request<br/>
     *
     * Note that if a continuation response is sent to the client, the server
     * MUST ultimately also send a final status code.
     *
     * @return <code>true</code> if this request requires a continuation
     *         response to be sent
     */
    boolean requiresContinuationResponse();
}
