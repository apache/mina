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
 * An HTTP message, the ancestor of HTTP request & response.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface HttpMessage {

    /**
     * The HTTP version of the message
     * 
     * @return HTTP/1.0 or HTTP/1.1
     */
    public HttpVersion getProtocolVersion();

    /**
     * Gets the <tt>Content-Type</tt> header of the message.
     * 
     * @return The content type.
     */
    public String getContentType();

    /**
     * Returns <tt>true</tt> if this message enables keep-alive connection.
     */
    public boolean isKeepAlive();

    /**
     * Returns the value of the HTTP header with the specified name. If more than one header with the given name is
     * associated with this request, one is selected and returned.
     * 
     * @param name The name of the desired header
     * @return The header value - or null if no header is found with the specified name
     */
    public String getHeader(String name);

    /**
     * Returns <tt>true</tt> if the HTTP header with the specified name exists in this request.
     */
    public boolean containsHeader(String name);

    /**
     * Returns a read-only {@link Map} of HTTP headers whose key is a {@link String} and whose value is a {@link String}
     * s.
     */
    public Map<String, String> getHeaders();
}
