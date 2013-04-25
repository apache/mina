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
 * Extract of the CoAP RFC :
 * 
 * <pre>
 *     |    1 | GET    | [RFCXXXX] |
 *     |    2 | POST   | [RFCXXXX] |
 *     |    3 | PUT    | [RFCXXXX] |
 *     |    4 | DELETE | [RFCXXXX] |
 * 
 *     |   65 | 2.01 Created                    | [RFCXXXX] |
 *     |   66 | 2.02 Deleted                    | [RFCXXXX] |
 *     |   67 | 2.03 Valid                      | [RFCXXXX] |
 *     |   68 | 2.04 Changed                    | [RFCXXXX] |
 *     |   69 | 2.05 Content                    | [RFCXXXX] |
 *     |  128 | 4.00 Bad Request                | [RFCXXXX] |
 *     |  129 | 4.01 Unauthorized               | [RFCXXXX] |
 *     |  130 | 4.02 Bad Option                 | [RFCXXXX] |
 *     |  131 | 4.03 Forbidden                  | [RFCXXXX] |
 *     |  132 | 4.04 Not Found                  | [RFCXXXX] |
 *     |  133 | 4.05 Method Not Allowed         | [RFCXXXX] |
 *     |  134 | 4.06 Not Acceptable             | [RFCXXXX] |
 *     |  140 | 4.12 Precondition Failed        | [RFCXXXX] |
 *     |  141 | 4.13 Request Entity Too Large   | [RFCXXXX] |
 *     |  143 | 4.15 Unsupported Content-Format | [RFCXXXX] |
 *     |  160 | 5.00 Internal Server Error      | [RFCXXXX] |
 *     |  161 | 5.01 Not Implemented            | [RFCXXXX] |
 *     |  162 | 5.02 Bad Gateway                | [RFCXXXX] |
 *     |  163 | 5.03 Service Unavailable        | [RFCXXXX] |
 *     |  164 | 5.04 Gateway Timeout            | [RFCXXXX] |
 *     |  165 | 5.05 Proxying Not Supported     | [RFCXXXX] |
 * </pre>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum CoapCode {
    GET("GET", 1), POST("POST", 2), PUT("PUT", 3), DELETE("DELETE", 4), CREATED("2.01", 65), DELETED("2.02", 66),
    VALID("2.03", 67), CHANGED("2.04", 68), CONTENT("2.05", 69), BAD_REQUEST("4.00", 128), UNAUTHORIZED("4.01", 129),
    BAD_OPTION("4.02", 130), FORBIDDEN("4.03", 131), NOT_FOUND("4.04", 132), METHOD_NOT_ALLOWED("4.05", 133),
    NOT_ACCEPTABLE("4.06", 134), PRECONDITION_FAILED("4.12", 140), REQUEST_ENTITY_TOO_LARGE("4.13", 141),
    UNSUPPORTED_CONTENT_FORMAT("4.15", 143), INTERNAL_SERVER_ERROR("5.00", 160), NOT_IMPLEMENTED("5.01", 161),
    BAD_GATEWAY("5.02", 162), SERVICE_UNAVAILABLE("5.03", 163), GATEWAY_TIMEOUT("5.04", 164), PROXYING_NOT_SUPPORTED(
            "5.05", 165)

    ;

    private final String text;
    private final int code;

    private CoapCode(String text, int code) {
        this.text = text;
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public int getCode() {
        return code;
    }

    /**
     * Find the {@link CoapCode} for the given value code (<code>null</code> if not found)
     */
    public static CoapCode fromCode(int code) {
        for (CoapCode t : CoapCode.values()) {
            if (t.getCode() == code) {
                return t;
            }
        }
        return null;
    }
}
