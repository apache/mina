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

import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoBuffer;

/**
 * Represents a response to an <code>HttpRequest</code>.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface HttpResponse extends HttpMessage {

    /**
     * Returns the value of the HTTP header with the specified name.
     * If more than one header with the given name is associated with
     * this response, one is selected and returned.
     *
     * @param name  The name of the desired header
     * @return      The header value - or null if no header is found
     *              with the specified name
     */
    String getHeader(String name);

    /**
     * Returns <tt>true</tt> if the HTTP header with the specified name exists in this response.
     */
    boolean containsHeader(String name);

    /**
     * Returns the {@link Map} of HTTP headers whose key is a {@link String} and whose value
     * is a {@link List} of {@link String}s.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Returns the Content-Type header of the response.
     */
    String getContentType();

    /**
     * Returns the status of this response
     */
    HttpResponseStatus getStatus();

    /**
     * Returns the reason phrase which is associated with the current status of this response.
     */
    String getStatusReasonPhrase();

    /**
     * Returns the content of the response body.
     */
    IoBuffer getContent();
}
