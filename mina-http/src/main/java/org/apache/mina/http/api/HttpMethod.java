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

/**
 * The HTTP method, one of GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, CONNECT
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum HttpMethod {
    // HTTP 1.0 official methods
    /** The GET method */
    GET, 
    
    /** The HEAD method */
    HEAD, 
    
    /** The POST method */
    POST, 
    
    // HTTP 1.1 official methods
    /** The CONNECT method */
    CONNECT,
    
    /** The DELETE method */
    DELETE, 
    
    /** The OPTIONS method */
    OPTIONS, 
    
    /** The PUT method */
    PUT, 
    
    /** The TRACE method */
    TRACE, 
    
    // Additional HTTP 1.0 methods
    /** The LINK method */
    LINK,
    
    /** The UNLINK method */
    UNLINK,
    
    // Additional HTTP 1.1 methods
    /** The PATCH method, RFC 5789 */
    PATCH,
    
    // Other methods
    /** The COPY method, RFC 4918*/
    COPY,
    
    /** The MOVE method, RFC 5789 */
    MOVE,
    
    /** The LOCK method, RFC 5789 */
    LOCK,
    
    /** The UNLOCK method, RFC 5789 */
    UNLOCK,
    
    /** The WRAPPED method ??? */
    WRAPPED,
    
    /** Unknown method */
    UNKNOWN;
    
    String name;
    
    public void setName(String name) {
        this.name = name;
    }
}
