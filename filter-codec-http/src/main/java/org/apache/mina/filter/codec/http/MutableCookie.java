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
 * A mutable {@link Cookie}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface MutableCookie extends Cookie {

    /**
     * Sets the value of this cookie
     *
     * @param value The cookie value
     */
    void setValue(String value);

    /**
     * Sets the domain of this cookie.
     */
    void setDomain(String domain);

    /**
     * Sets the path on the server to which the client returns this cookie.
     *
     * @param path  The path
     */
    void setPath(String path);

    /**
     * Sets the version nubmer of this cookie
     *
     * @param version  The version number
     */
    void setVersion(int version);

    /**
     * Sets whether this cookie should be marked as "secure".
     * Secure cookies should be sent back by a client over a transport as least as
     * secure as that upon which they were received
     *
     * @param secure  <code>true</code> if this cookie should be marked as secure
     */
    void setSecure(boolean secure);

    /**
     * Sets the maximum age of the cookie in seconds.
     * A positive value indicates that the cookie will expire after the specified number
     * of seconds.
     * <p>
     * A value of zero indicates that the cookie should be deleted.
     * <p>
     * A negative value indicates that the cookie should be discarded at the end of the
     * user session
     *
     * @param expiry  The expiry time in seconds
     */
    void setMaxAge(int expiry);

    /**
     * Sets the comment of this cookie.  Comments are not supported by version 0 cookies.
     */
    void setComment(String comment);
}