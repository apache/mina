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

import org.apache.mina.common.IoBuffer;


/**
 * A default implementation of {@link MutableHttpResponse}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultHttpResponse extends DefaultHttpMessage implements
        MutableHttpResponse {

    private static final long serialVersionUID = -3733889080525034446L;

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private String statusReasonPhrase = HttpResponseStatus.OK.getDescription();

    /**
     * Creates a new instance
     *
     */
    public DefaultHttpResponse() {
    }

    public void addCookie(String headerValue) {
        // TODO Implement addCookie(String headerValue)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setStatus(HttpResponseStatus status) {
        setStatus(status, status.getDescription());
    }

    public void setStatus(HttpResponseStatus status, String statusReasonPhrase) {
        if (status == null) {
            throw new NullPointerException("status");
        }
        this.status = status;

        setStatusReasonPhrase(statusReasonPhrase);
    }

    public String getStatusReasonPhrase() {
        return statusReasonPhrase;
    }

    public void setStatusReasonPhrase(String statusReasonPhrase) {
        if (statusReasonPhrase == null) {
            throw new NullPointerException("statusReasonPhrase");
        }
        this.statusReasonPhrase = statusReasonPhrase;
    }

    public void normalize(HttpRequest request) {
        updateConnectionHeader(request);

        setHeader(HttpHeaderConstants.KEY_DATE, HttpDateFormat
                .getCurrentHttpDate());

        int contentLength;
        if (isBodyAllowed(request)) {
            contentLength = getContent().remaining();
        } else {
            setContent(IoBuffer.allocate(0));
            contentLength = 0;
        }

        if (!containsHeader(HttpHeaderConstants.KEY_TRANSFER_CODING)) {
            setHeader(HttpHeaderConstants.KEY_CONTENT_LENGTH, String
                    .valueOf(contentLength));
        }
    }

    /**
     * Updates our "Connection" header based on we are "keep-aliving" the connection
     * after the response.
     */
    private void updateConnectionHeader(HttpRequest request) {
        if (getStatus().forcesConnectionClosure()) {
            setHeader(HttpHeaderConstants.KEY_CONNECTION,
                    HttpHeaderConstants.VALUE_CLOSE);
        } else if (request.isKeepAlive()) {
            setHeader(HttpHeaderConstants.KEY_CONNECTION,
                    HttpHeaderConstants.VALUE_KEEP_ALIVE);
        } else {
            setHeader(HttpHeaderConstants.KEY_CONNECTION,
                    HttpHeaderConstants.VALUE_CLOSE);
        }
    }

    /**
     * Determines whether we are allowed a response body.
     * A response body is allowed iff our response status allows a
     * body, and the original request method allows a body
     *
     * @return  <code>true</code> if a body is allowed
     */
    private boolean isBodyAllowed(HttpRequest request) {
        HttpMethod method = request.getMethod();
        return getStatus().allowsMessageBody() && method != null
                && method.isResponseBodyAllowed();
    }
}
