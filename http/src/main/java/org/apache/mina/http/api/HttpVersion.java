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
 * Type safe enumeration representing HTTP protocol version
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public enum HttpVersion {
    /**
     * HTTP 1/1
     */
    HTTP_1_1("HTTP/1.1"),

    /**
     * HTTP 1/0
     */
    HTTP_1_0("HTTP/1.0");

    private final String value;

    private HttpVersion(String value) {
        this.value = value;
    }

    /**
     * Returns the {@link HttpVersion} instance from the specified string.
     * 
     * @return The version, or <code>null</code> if no version is found
     */
    public static HttpVersion fromString(String string) {
        if (HTTP_1_1.toString().equalsIgnoreCase(string)) {
            return HTTP_1_1;
        }

        if (HTTP_1_0.toString().equalsIgnoreCase(string)) {
            return HTTP_1_0;
        }

        return null;
    }

    /**
     * @return A String representation of this version
     */
    @Override
    public String toString() {
        return value;
    }

}
