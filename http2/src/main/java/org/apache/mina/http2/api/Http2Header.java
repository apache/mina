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
package org.apache.mina.http2.api;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum Http2Header {

    METHOD(":method"),
    
    PATH(":path"),
    
    STATUS(":status"),
    
    AUTHORITY(":authority"),
    
    SCHEME(":scheme");
    
    private final String name;
    
    private Http2Header(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Check whether a header is an HTTP2 reserved one.
     * 
     * @param name the name of the HTTP header
     * @return true is this is a reserved HTTP2 header, false otherwise
     */
    public static boolean isHTTP2ReservedHeader(String name) {
        for(Http2Header header : Http2Header.values()) {
            if (header.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
