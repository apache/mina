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

import java.io.Serializable;

/**
 * An HTTP cookie.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 592337 $, $Date: 2007-11-06 17:59:36 +0900 (화, 06 11월 2007) $
 */
public interface Cookie extends Serializable, Comparable<Cookie> {

    /**
     * Returns the cookie version number. The default (if not specified) is 0
     *
     * @return  The version number
     */
    int getVersion();

    /**
     * Returns the name of this cookie
     *
     * @return  The cookie name
     */
    String getName();

    /**
     * Returns the value of this cookie
     *
     * @return  The cookie value - or <code>null</code> if this cookie does not have
     *          a value
     */
    String getValue();

    /**
     * Returns the domain of this cookie.
     */
    String getDomain();

    /**
     * Returns the path on the server to which the client returns this cookie.
     *
     * @return  The path
     */
    String getPath();

    /**
     * Returns if this cookie is marked as "secure".
     * Secure cookies should be sent back by a client over a transport as least as
     * secure as that upon which they were received
     */
    boolean isSecure();

    /**
     * Returns the maximum age of this cookie in seconds.
     */
    int getMaxAge();

    /**
     * Returns the comment of this cookie.  Comments are not supported by version 0 cookies.
     *
     * @return <tt>null</tt> if no comment is specified
     */
    String getComment();
}
