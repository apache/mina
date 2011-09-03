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
 * An <code>Enumeration</code> of all known HTTP status codes.
 */
public enum HttpStatus {

    /**
     * 200 - OK
     */
    SUCCESS_OK(200, "HTTP/1.1 200 OK"),
    /**
     * 201 - Created
     */
    SUCCESS_CREATED(201, "HTTP/1.1 201 Created"),
    /**
     * 202 - Accepted
     */
    SUCCESS_ACCEPTED(202, "HTTP/1.1 202 Accepted"),
    /**
     * 203 - Non-Authoritative Information
     */
    SUCCESS_NON_AUTHORATIVE_INFORMATION(203, "HTTP/1.1 203 Non-Authoritative Information"),
    /**
     * 204 - No Content
     */
    SUCCESS_NO_CONTENT(204, "HTTP/1.1 204 No Content"),
    /**
     * 205 - Reset Content
     */
    SUCCESS_RESET_CONTENT(205, "HTTP/1.1 205 Reset Content"),
    /**
     * 206 - Created
     */
    SUCCESS_PARTIAL_CONTENT(206, "HTTP/1.1 206 Partial Content"),

    /**
     * 300 - Multiple Choices
     */
    REDIRECTION_MULTIPLE_CHOICES(300, "HTTP/1.1 300 Multiple Choices"),
    /**
     * 301 - Moved Permanently
     */
    REDIRECTION_MOVED_PERMANENTLY(301, "HTTP/1.1 301 Moved Permanently"),
    /**
     * 302 - Found / Moved Temporarily
     */
    REDIRECTION_FOUND(302, "HTTP/1.1 302 Found"),
    /**
     * 303 - See Others
     */
    REDIRECTION_SEE_OTHER(303, "HTTP/1.1 303 See Other"),
    /**
     * 304 - Not Modified
     */
    REDIRECTION_NOT_MODIFIED(304, "HTTP/1.1 304 Not Modified"),
    /**
     * 305 - Use Proxy
     */
    REDIRECTION_USE_PROXY(305, "HTTP/1.1 305 Use Proxy"),
    /**
     * 307 - Temporary Redirect
     */
    REDIRECTION_TEMPORARILY_REDIRECT(307, "HTTP/1.1 307 Temporary Redirect"),

    /**
     * 400 - Bad Request
     */
    CLIENT_ERROR_BAD_REQUEST(400, "HTTP/1.1 400 Bad Request"),
    /**
     * 401 - Unauthorized
     */
    CLIENT_ERROR_UNAUTHORIZED(401, "HTTP/1.1 401 Unauthorized"),
    /**
     * 403 - Forbidden
     */
    CLIENT_ERROR_FORBIDDEN(403, "HTTP/1.1 403 Forbidden"),
    /**
     * 404 - Not Found
     */
    CLIENT_ERROR_NOT_FOUND(404, "HTTP/1.1 404 Not Found"),
    /**
     * 405 - Method Not Allowed
     */
    CLIENT_ERROR_METHOD_NOT_ALLOWED(405, "HTTP/1.1 405 Method Not Allowed"),
    /**
     * 406 - Not Acceptable
     */
    CLIENT_ERROR_NOT_ACCEPTABLE(406, "HTTP/1.1 406 Not Acceptable"),
    /**
     * 407 - Proxy Authentication Required
     */
    CLIENT_ERROR_PROXY_AUTHENTICATION_REQUIRED(407, "HTTP/1.1 407 Proxy Authentication Required"),
    /**
     * 408 - Request Timeout
     */
    CLIENT_ERROR_REQUEST_TIMEOUT(408, "HTTP/1.1 408 Request Timeout"),
    /**
     * 409 - Conflict
     */
    CLIENT_ERROR_CONFLICT(409, "HTTP/1.1 409 Conflict"),
    /**
     * 410 - Gone
     */
    CLIENT_ERROR_GONE(410, "HTTP/1.1 410 Gone"),
    /**
     * 411 - Length Required
     */
    CLIENT_ERROR_LENGTH_REQUIRED(411, "HTTP/1.1 411 Length Required"),
    /**
     * 412 - Precondition Failed
     */
    CLIENT_ERROR_PRECONDITION_FAILED(412, "HTTP/1.1 412 Precondition Failed"),
    /**
     * 413 - Request Entity Too Large
     */
    CLIENT_ERROR_REQUEST_ENTITY_TOO_LARGE(413, "HTTP/1.1 413 Request Entity Too Large"),
    /**
     * 414 - Bad Request
     */
    CLIENT_ERROR_REQUEST_URI_TOO_LONG(414, "HTTP/1.1 414 Request-URI Too Long"),
    /**
     * 415 - Unsupported Media Type
     */
    CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE(415, "HTTP/1.1 415 Unsupported Media Type"),
    /**
     * 416 - Requested Range Not Satisfiable
     */
    CLIENT_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE(416, "HTTP/1.1 416 Requested Range Not Satisfiable"),
    /**
     * 417 - Expectation Failed
     */
    CLIENT_ERROR_EXPECTATION_FAILED(417, "HTTP/1.1 417 Expectation Failed"),

    /**
     * 500 - Internal Server Error
     */
    SERVER_ERROR_INTERNAL_SERVER_ERROR(500, "HTTP/1.1 500 Internal Server Error"),
    /**
     * 501 - Not Implemented
     */
    SERVER_ERROR_NOT_IMPLEMENTED(501, "HTTP/1.1 501 Not Implemented"),
    /**
     * 502 - Bad Gateway
     */
    SERVER_ERROR_BAD_GATEWAY(502, "HTTP/1.1 502 Bad Gateway"),
    /**
     * 503 - Service Unavailable
     */
    SERVER_ERROR_SERVICE_UNAVAILABLE(503, "HTTP/1.1 503 Service Unavailable"),
    /**
     * 504 - Gateway Timeout
     */
    SERVER_ERROR_GATEWAY_TIMEOUT(504, "HTTP/1.1 504 Gateway Timeout"),
    /**
     * 505 - HTTP Version Not Supported
     */
    SERVER_ERROR_HTTP_VERSION_NOT_SUPPORTED(505, "HTTP/1.1 505 HTTP Version Not Supported");

    /** The code associated with this status, for example "404" for "Not Found". */
    private int code;

    /**
     * The line associated with this status, "HTTP/1.1 501 Not Implemented".
     */
    private String line;

    /**
     * Create an instance of this type.
     * 
     * @param code the status code.
     * @param phrase the associated phrase.
     */
    private HttpStatus(int code, String phrase) {
        this.code = code;
        line = phrase;
    }

    /**
     * Retrieve the status code for this instance.
     * 
     * @return the status code.
     */
    public int code() {
        return code;
    }

    /**
     * Retrieve the status line for this instance.
     * 
     * @return the status line.
     */
    public String line() {
        return line + "\r\n";
    }
}
