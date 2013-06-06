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
package org.apache.mina.coap;

/**
 * A type of CoAP option following the CoAP RFC list.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum CoapOptionType {
    IF_MATCH(1), URI_HOST(3), ETAG(4), IF_NONE_MATCH(5), OBSERVE(6), URI_PORT(7), LOCATION_PATH(8), URI_PATH(11),
    CONTENT_FORMAT(12), MAX_AGE(14), URI_QUERY(15), ACCEPT(16), LOCATION_QUERY(20), PROXY_URI(35), PROXY_SCHEME(39);

    private final int code;

    private CoapOptionType(int code) {
        this.code = code;
    }

    /**
     * @return the numeric code for this option type
     */
    public int getCode() {
        return code;
    }

    /**
     * Find the {@link CoapOptionType} for the given value code (<code>null</code> if not found)
     */
    public static CoapOptionType fromCode(int code) {
        for (CoapOptionType t : CoapOptionType.values()) {
            if (t.getCode() == code) {
                return t;
            }
        }
        return null;
    }
}