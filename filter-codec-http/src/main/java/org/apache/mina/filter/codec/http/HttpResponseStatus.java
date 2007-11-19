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

public class HttpResponseStatus implements Serializable {

    private static final long serialVersionUID = -5885201751942967031L;

    private static final int MIN_ID = 100;

    private static final int MAX_ID = 599;

    private static final HttpResponseStatus[] RESPONSE_TABLE = new HttpResponseStatus[MAX_ID + 1];

    // Informational status codes

    public static final HttpResponseStatus CONTINUE = new HttpResponseStatus(
            100, "Continue", true, false);

    public static final HttpResponseStatus SWITCHING_PROTOCOLS = new HttpResponseStatus(
            101, "Switching Protocols", true, false);

    // Successful status codes

    public static final HttpResponseStatus OK = new HttpResponseStatus(200,
            "OK", true, false);

    public static final HttpResponseStatus CREATED = new HttpResponseStatus(
            201, "Created", true, false);

    public static final HttpResponseStatus ACCEPTED = new HttpResponseStatus(
            202, "Accepted", true, false);

    public static final HttpResponseStatus NON_AUTHORITATIVE = new HttpResponseStatus(
            203, "Non-Authoritative Information", true, false);

    public static final HttpResponseStatus NO_CONTENT = new HttpResponseStatus(
            204, "No Content", false, false);

    public static final HttpResponseStatus RESET_CONTENT = new HttpResponseStatus(
            205, "Reset Content", false, false);

    public static final HttpResponseStatus PARTIAL_CONTENT = new HttpResponseStatus(
            206, "Partial Content", true, false);

    // Redirection status codes

    public static final HttpResponseStatus MULTIPLE_CHOICES = new HttpResponseStatus(
            300, "Multiple Choices", true, false);

    public static final HttpResponseStatus MOVED_PERMANENTLY = new HttpResponseStatus(
            301, "Moved Permanently", true, false);

    public static final HttpResponseStatus FOUND = new HttpResponseStatus(302,
            "Found", true, false);

    public static final HttpResponseStatus SEE_OTHER = new HttpResponseStatus(
            303, "See Other", true, false);

    public static final HttpResponseStatus NOT_MODIFIED = new HttpResponseStatus(
            304, "Not Modified", false, false);

    public static final HttpResponseStatus USE_PROXY = new HttpResponseStatus(
            305, "Use Proxy", true, false);

    public static final HttpResponseStatus TEMPORARY_REDIRECT = new HttpResponseStatus(
            307, "Temporary Redirect", true, false);

    // Client error codes

    public static final HttpResponseStatus BAD_REQUEST = new HttpResponseStatus(
            400, "Bad Request", true, true);

    public static final HttpResponseStatus UNAUTHORIZED = new HttpResponseStatus(
            401, "Unauthorized", true, false);

    public static final HttpResponseStatus PAYMENT_REQUIRED = new HttpResponseStatus(
            402, "Payment Required", true, false);

    public static final HttpResponseStatus FORBIDDEN = new HttpResponseStatus(
            403, "Forbidden", true, false);

    public static final HttpResponseStatus NOT_FOUND = new HttpResponseStatus(
            404, "Not Found", true, false);

    public static final HttpResponseStatus METHOD_NOT_ALLOWED = new HttpResponseStatus(
            405, "Method Not Allowed", true, false);

    public static final HttpResponseStatus NOT_ACCEPTABLE = new HttpResponseStatus(
            406, "Not Acceptable", true, false);

    public static final HttpResponseStatus PROXY_AUTHENTICATION_REQUIRED = new HttpResponseStatus(
            407, "Proxy Authentication Required", true, false);

    public static final HttpResponseStatus REQUEST_TIMEOUT = new HttpResponseStatus(
            408, "Request Time-out", true, true);

    public static final HttpResponseStatus CONFLICT = new HttpResponseStatus(
            409, "Conflict", true, false);

    public static final HttpResponseStatus GONE = new HttpResponseStatus(410,
            "Gone", true, false);

    public static final HttpResponseStatus LENGTH_REQUIRED = new HttpResponseStatus(
            411, "Length Required", true, true);

    public static final HttpResponseStatus PRECONDITION_FAILED = new HttpResponseStatus(
            412, "Precondition Failed", true, false);

    public static final HttpResponseStatus REQUEST_ENTITY_TOO_LARGE = new HttpResponseStatus(
            413, "Request Entity Too Large", true, true);

    public static final HttpResponseStatus REQUEST_URI_TOO_LONG = new HttpResponseStatus(
            414, "Request-URI Too Large", true, true);

    public static final HttpResponseStatus UNSUPPORTED_MEDIA_TYPE = new HttpResponseStatus(
            415, "Unsupported Media Type", true, false);

    public static final HttpResponseStatus REQUEST_RANGE_NOT_SATISFIABLE = new HttpResponseStatus(
            416, "Requested range not satisfiable", true, false);

    public static final HttpResponseStatus EXPECTATION_FAILED = new HttpResponseStatus(
            417, "Expectation Failed", true, false);

    // Server error codes

    public static final HttpResponseStatus INTERNAL_SERVER_ERROR = new HttpResponseStatus(
            500, "Internal Server Error", true, true);

    public static final HttpResponseStatus NOT_IMPLEMENTED = new HttpResponseStatus(
            501, "Not Implemented", true, true);

    public static final HttpResponseStatus BAD_GATEWAY = new HttpResponseStatus(
            502, "Bad Gateway", true, false);

    public static final HttpResponseStatus SERVICE_UNAVAILABLE = new HttpResponseStatus(
            503, "Service Unavailable", true, true);

    public static final HttpResponseStatus GATEWAY_TIMEOUT = new HttpResponseStatus(
            504, "Gateway Time-out", true, false);

    public static final HttpResponseStatus HTTP_VERSION_NOT_SUPPORTED = new HttpResponseStatus(
            505, "HTTP Version not supported", true, false);

    private final int code;

    private final transient boolean allowsMessageBody;

    private final transient boolean forcesConnectionClosure;

    private final transient Category category;

    private final transient String description;

    /**
     * @return  <code>true</code> iff a message body may be transmitted in
     *          a response containing this response status
     */
    public boolean allowsMessageBody() {
        return allowsMessageBody;
    }

    /**
     * @return  <code>true</code> iff a connection which would normally be
     *          kept alive should be closed as a result of this response
     */
    public boolean forcesConnectionClosure() {
        return forcesConnectionClosure;
    }

    /**
     * @return The response code of this status
     */
    public int getCode() {
        return code;
    }

    /**
     * @return  The category of this status
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return A description of this status
     */
    public String getDescription() {
        return description;
    }

    /**
     * A string description of this status
     */
    @Override
    public String toString() {
        return code + ": " + category + " - " + description;
    }

    /**
     * Returns the <code>ResponseStatus</code> with the specified
     * status id.
     * If no status exists with the specified id, a new status is created
     * and registered based on the category applicable to the id:<br/>
     * <table border="1">
     *   <tr><td>100 - 199</td><td>Informational</td></tr>
     *   <tr><td>200 - 299</td><td>Successful</td></tr>
     *   <tr><td>300 - 399</td><td>Redirection</td></tr>
     *   <tr><td>400 - 499</td><td>Client Error</td></tr>
     *   <tr><td>500 - 599</td><td>Server Error</td></tr>
     * <table>.
     *
     * @param id  The id of the desired response status
     * @return    The <code>ResponseStatus</code>
     * @throws IllegalStateException  If the specified id is invalid
     */
    public static HttpResponseStatus forId(int id) {
        if (id < MIN_ID || id > MAX_ID) {
            throw new IllegalArgumentException("Illegal response id: " + id);
        }
        HttpResponseStatus status = RESPONSE_TABLE[id];
        if (status == null) {
            Category cat = categoryForId(id);
            if (cat == null) {
                throw new IllegalArgumentException("Illegal response id: " + id);
            }
            status = new HttpResponseStatus(id, cat.toString(), true, cat
                    .isDefaultConnectionClosure());
            RESPONSE_TABLE[id] = status;
        }
        return status;
    }

    /**
     * Obtains the response category which covers the specified response
     * id
     *
     * @param id  The id
     * @return    The category - or <code>null</code> if no category covers
     *            the specified response id
     */
    private static Category categoryForId(int id) {
        int catId = id / 100;
        switch (catId) {
        case 1:
            return Category.INFORMATIONAL;
        case 2:
            return Category.SUCCESSFUL;
        case 3:
            return Category.REDIRECTION;
        case 4:
            return Category.CLIENT_ERROR;
        case 5:
            return Category.SERVER_ERROR;
        default:
            return null;
        }
    }

    private HttpResponseStatus(int code, String description,
            boolean allowsMessageBody, boolean forcesConnectionClosure) {
        RESPONSE_TABLE[code] = this;
        this.code = code;
        this.category = categoryForId(code);
        this.description = description;
        this.allowsMessageBody = allowsMessageBody;
        this.forcesConnectionClosure = forcesConnectionClosure;
    }

    private Object readResolve() {
        return forId(this.code);
    }

    /**
     * Category of response
     *
     * @author irvingd
     */
    public static enum Category {
        /**
         * Indicates a provisional response
         */
        INFORMATIONAL,

        /**
         * Indicates that the client's request was successfully received,
         * understood, and accepted
         */
        SUCCESSFUL,

        /**
         * Indicates that further action needs to be taken by the user agent in order
         * to fulfill the request
         */
        REDIRECTION,

        /**
         * Indicates that the client seems to have erred
         */
        CLIENT_ERROR,

        /**
         * Indicate that the server is aware that it has erred or is incapable
         * of performing the request
         */
        SERVER_ERROR(true), ;

        private boolean defaultConnectionClosure;

        private Category() {
            this(false);
        }

        private Category(boolean defaultConnectionClosure) {
            this.defaultConnectionClosure = defaultConnectionClosure;
        }

        public boolean isDefaultConnectionClosure() {
            return defaultConnectionClosure;
        }
    }
}
