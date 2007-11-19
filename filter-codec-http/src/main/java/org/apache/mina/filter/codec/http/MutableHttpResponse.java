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

/**
 * A mutable {@link HttpResponse}
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface MutableHttpResponse extends MutableHttpMessage, HttpResponse {

    /**
     * Adds the cookie of this message by parsing the specified
     * <tt>headerValue</tt>.
     */
    void addCookie(String headerValue);

    /**
     * Sets the status of this response
     *
     * @param status  The response status
     */
    void setStatus(HttpResponseStatus status);

    /**
     * Sets the reason phrase which is associated with the current status of
     * this response.
     */
    void setStatusReasonPhrase(String reasonPhrase);

    /**
     * Normalizes this response to fix possible protocol violations.  The
     * following is the normalization step:
     * <ol>
     * <li>Adds '<tt>Connection</tt>' header with an appropriate value
     *     determined from the specified <tt>request</tt> and the status of
     *     this response.</li>
     * <li>Adds '<tt>Date</tt>' header with current time.</li>
     * <li>Removes body content if the {@link HttpMethod} of the specified
     *     <tt>request</tt> doesn't allow body or the status of this response
     *     doesn't allow body.</li>
     * <li>Adds '<tt>Content-length</tt>' header if '<tt>Transfer-coding</tt>'
     *     header doesn't exist.</li>
     * </ol>
     *
     * @param request the request that pairs with this response
     */
    void normalize(HttpRequest request);
}