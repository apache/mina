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
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class HttpProxyConstants {
    public final static String CONNECT = "CONNECT";

    public final static String GET = "GET";

    public final static String PUT = "PUT";

    public final static String HTTP_1_0 = "HTTP/1.0";

    public final static String HTTP_1_1 = "HTTP/1.1";

    public final static String CRLF = "\r\n";

    public final static String DEFAULT_KEEP_ALIVE_TIME = "300";

    public final static String USER_PROPERTY = "USER";

    public final static String PWD_PROPERTY = "PWD";

    public final static String DOMAIN_PROPERTY = "DOMAIN";

    public final static String WORKSTATION_PROPERTY = "WORKSTATION";
}