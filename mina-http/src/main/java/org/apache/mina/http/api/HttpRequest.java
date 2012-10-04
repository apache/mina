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

import java.util.List;
import java.util.Map;

/**
 * An HTTP request
 * 
 * @author jvermillar
 * 
 */
public interface HttpRequest extends HttpMessage {

    /**
     * Determines whether this request contains at least one parameter with the specified name
     * 
     * @param name The parameter name
     * @return <code>true</code> if this request contains at least one parameter with the specified name
     */
    boolean containsParameter(String name);

    /**
     * Returns the value of a request parameter as a String, or null if the parameter does not exist.
     * 
     * If the request contained multiple parameters with the same name, this method returns the first parameter
     * encountered in the request with the specified name
     * 
     * @param name The parameter name
     * @return The value
     */
    String getParameter(String name);
    
    String getQueryString();

    /**
     * Returns a read only {@link Map} of query parameters whose key is a {@link String} and whose value is a
     * {@link List} of {@link String}s.
     */
    Map<String, List<String>> getParameters();

    /**
     * Return the HTTP method used for this message {@link HttpMethod}
     * 
     * @return the method
     */
    HttpMethod getMethod();
    
    /**
     * Retrurn the HTTP request path
     * @retrun the request path
     */
    String getRequestPath();
}
