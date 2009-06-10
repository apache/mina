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
package org.apache.mina.proxy.handlers.http;

/**
 * HttpProxyConstants.java - HTTP Proxy constants.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpProxyConstants {

    /**
     * The HTTP CONNECT verb.
     */
    public final static String CONNECT = "CONNECT";

    /**
     * The HTTP GET verb.
     */
    public final static String GET = "GET";

    /**
     * The HTTP PUT verb.
     */    
    public final static String PUT = "PUT";

    /**
     * The HTTP 1.0 protocol version string.
     */    
    public final static String HTTP_1_0 = "HTTP/1.0";

    /**
     * The HTTP 1.1 protocol version string.
     */        
    public final static String HTTP_1_1 = "HTTP/1.1";

    /**
     * The CRLF character sequence used in HTTP protocol to end each line.
     */        
    public final static String CRLF = "\r\n";

    /**
     * The default keep-alive timeout we set to make proxy
     * connection persistent. Set to 300 ms.
     */
    public final static String DEFAULT_KEEP_ALIVE_TIME = "300";

    // ProxyRequest properties
    
    /**
     * The username property. Used in auth mechs.
     */
    public final static String USER_PROPERTY = "USER";

    /**
     * The password property. Used in auth mechs.
     */
    public final static String PWD_PROPERTY = "PWD";

    /**
     * The domain name property. Used in auth mechs.
     */
    public final static String DOMAIN_PROPERTY = "DOMAIN";

    /**
     * The workstation name property. Used in auth mechs.
     */
    public final static String WORKSTATION_PROPERTY = "WORKSTATION";
}