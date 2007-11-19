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
 * HTTP Header Constants.
 *
 * @author irvingd
 * @author trustin
 * @version $Rev$, $Date$
 */
public class HttpHeaderConstants {

    /**
     * The name of the "connection" header
     */
    public static final String KEY_CONNECTION = "Connection";

    /**
     * The server header
     */
    public static final String KEY_SERVER = "Server";

    /**
     * The header value to indicate connection closure
     */
    public static final String VALUE_CLOSE = "close";

    /**
     * The header value to indicate connection keep-alive (http 1.0)
     */
    public static final String VALUE_KEEP_ALIVE = "Keep-Alive";
    
    /**
     * The "content-type" header name.
     */
    public static final String KEY_CONTENT_TYPE = "Content-Type";
    
    /**
     * The value of "content-type" header that indicates the content contains a
     * URL-encoded form.
     */
    public static final String VALUE_URLENCODED_FORM =
        "application/x-www-form-urlencoded";

    /**
     * The "content-length" header name
     */
    public static final String KEY_CONTENT_LENGTH = "Content-Length";

    /**
     * The "transfer-coding" header name
     */
    public static final String KEY_TRANSFER_CODING = "Transfer-Coding";

    /**
     * The "expect" header name
     */
    public static final String KEY_EXPECT = "Expect";

    /**
     * The continue expectation
     */
    public static final String VALUE_CONTINUE_EXPECTATION = "100-continue";

    /**
     * The "date" header
     */
    public static final String KEY_DATE = "Date";

    private HttpHeaderConstants() {
    }
}
