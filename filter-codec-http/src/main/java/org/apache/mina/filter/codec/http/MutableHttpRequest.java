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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A mutable {@link HttpRequest}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface MutableHttpRequest extends MutableHttpMessage, HttpRequest {

    /**
     * Sets the cookies of this message by parsing the specified <tt>headerValue</tt>.
     */
    void setCookies(String headerValue);

    /**
     * Adds a query parameter to this request.
     * Adding a query parameter does not cause any existing parameters with the
     * same name to be overwritten.  Please also note that calling this method
     * doesn't update {@link #setRequestUri(URI) the request URI} immediately
     * due to performance overhead.  You have to call {@link #normalize()} by
     * yourself to update the request URI.
     *
     * @param name   The header name
     * @param value  The header value
     */
    void addParameter(String name, String value);

    /**
     * Removes all query parameters with the specified name.
     * Please note that calling this method doesn't update
     * {@link #setRequestUri(URI) the request URI} immediately due to
     * performance overhead.  You have to call {@link #normalize()} by yourself
     * to update the request URI.
     */
    boolean removeParameter(String name);

    /**
     * Sets the value of a query parameter.
     * Any existing query parameters with the specified name are removed.
     * Please also note that calling this method doesn't update
     * {@link #setRequestUri(URI) the request URI} immediately due to
     * performance overhead.  You have to call {@link #normalize()} by yourself
     * to update the request URI.
     */
    void setParameter(String name, String value);

    /**
     * Sets query parameters with the specified {@link Map} whose key is a
     * {@link String} and whose value is a {@link List} of {@link String}s.
     * Please note that calling this method doesn't update
     * {@link #setRequestUri(URI) the request URI} immediately due to
     * performance overhead.  You have to call {@link #normalize()} by yourself
     * to update the request URI.
     */
    void setParameters(Map<String, List<String>> parameters);

    /**
     * Sets query parameters from the specified <tt>queryString</tt> which is
     * encoded with UTF-8 encoding.
     * Please note that calling this method doesn't update
     * {@link #setRequestUri(URI) the request URI} immediately due to
     * performance overhead.  You have to call {@link #normalize()} by yourself
     * to update the request URI.
     */
    void setParameters(String queryString);

    /**
     * Sets query parameters from the specified <tt>queryString</tt> which is
     * encoded with the specified charset <tt>encoding</tt>.
     * Please note that calling this method doesn't update
     * {@link #setRequestUri(URI) the request URI} immediately due to
     * performance overhead.  You have to call {@link #normalize()} by yourself
     * to update the request URI.
     */
    void setParameters(String queryString, String encoding)
            throws UnsupportedEncodingException;

    /**
     * Removes all query parameters from this request.
     * Please note that calling this method doesn't update
     * {@link #setRequestUri(URI) the request URI} immediately due to
     * performance overhead.  You have to call {@link #normalize()} by yourself
     * to update the request URI.
     */
    void clearParameters();

    /**
     * Sets the {@link HttpMethod} associated with this request.
     */
    void setMethod(HttpMethod method);

    /**
     * Sets the URI of the request.
     */
    void setRequestUri(URI requestUri);
    
    /**
     * Normalizes this request to fix possible protocol violations.  The
     * following is the normalization step:
     * <ol>
     * <li>Sets the query part of the <tt>requestUri</tt> with the current
     *     <tt>parameters</tt> if the <tt>method</tt> of this request is
     *     not {@link HttpMethod#POST}.</li>
     * <li>Sets the body content if there's any parameter entry in this
     *     requests and if <tt>method</tt> of this request is 
     *     {@link HttpMethod#POST}.
     * <li>Adds proper header entries for the current <tt>cookies</tt>.</li>
     * <li>Adds '<tt>Host</tt>' header if necessary.</li>
     * <li>Adds '<tt>Content-length</tt>' header if possible.</li>
     * </ol>
     *
     * @param request the request that pairs with this response
     */
    void normalize();
}
