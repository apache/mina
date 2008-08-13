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

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.handlers.ProxyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpProxyRequest.java - Wrapper class for HTTP requests.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class HttpProxyRequest extends ProxyRequest {
    private final static Logger logger = LoggerFactory
            .getLogger(HttpProxyRequest.class);

    public final String httpVerb;

    public final String httpURI;

    private String httpVersion;

    private String host;

    private Map<String, List<String>> headers;

    private transient Map<String, String> properties;

    public HttpProxyRequest(final InetSocketAddress endpointAddress) {
        this(HttpProxyConstants.CONNECT, endpointAddress.getHostName() + ":"
                + endpointAddress.getPort(), HttpProxyConstants.HTTP_1_0, null);
    }

    public HttpProxyRequest(final InetSocketAddress endpointAddress,
            final String httpVersion) {
        this(HttpProxyConstants.CONNECT, endpointAddress.getHostName() + ":"
                + endpointAddress.getPort(), httpVersion, null);
    }

    public HttpProxyRequest(final InetSocketAddress endpointAddress,
            final String httpVersion, final Map<String, List<String>> headers) {
        this(HttpProxyConstants.CONNECT, endpointAddress.getHostName() + ":"
                + endpointAddress.getPort(), httpVersion, headers);
    }

    public HttpProxyRequest(final String httpURI) {
        this(HttpProxyConstants.GET, httpURI, HttpProxyConstants.HTTP_1_0, null);
    }

    public HttpProxyRequest(final String httpURI, final String httpVersion) {
        this(HttpProxyConstants.GET, httpURI, httpVersion, null);
    }

    public HttpProxyRequest(final String httpVerb, final String httpURI,
            final String httpVersion) {
        this(httpVerb, httpURI, httpVersion, null);
    }

    public HttpProxyRequest(final String httpVerb, final String httpURI,
            final String httpVersion, final Map<String, List<String>> headers) {
        this.httpVerb = httpVerb;
        this.httpURI = httpURI;
        this.httpVersion = httpVersion;
        this.headers = headers;
    }

    /**
     * The request verb.
     */
    public final String getHttpVerb() {
        return httpVerb;
    }

    /**
     * The HTTP version.
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * Sets the HTTP version.
     */
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * Returns the host to which we are connecting.
     */
    public synchronized final String getHost() {
        if (host == null) {
            if (getEndpointAddress() != null) {
                host = getEndpointAddress().getHostName();
            }

            if (host == null && httpURI != null) {
                try {
                    host = (new URL(httpURI)).getHost();
                } catch (MalformedURLException e) {
                    logger.debug("Malformed URL", e);
                }
            }
        }

        return host;
    }

    /**
     * The request URI.
     */
    public final String getHttpURI() {
        return httpURI;
    }

    /**
     * HTTP headers.
     */
    public final Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Set the HTTP headers.
     */
    public final void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    /**
     * Get the additional properties.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the additional properties.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Check if the property is set otherwise throw a{@link ProxyAuthException}.
     */
    public void checkRequiredProperty(String propertyName)
            throws ProxyAuthException {
        if (properties.get(propertyName) == null) {
            StringBuilder sb = new StringBuilder("'");
            sb.append(propertyName).append(
                    "' property not provided in the request properties");
            throw new ProxyAuthException(sb.toString());
        }
    }

    /**
     * Returns the string representation of the HTTP request .
     */
    public String toHttpString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getHttpVerb()).append(' ').append(getHttpURI()).append(' ')
                .append(getHttpVersion()).append(HttpProxyConstants.CRLF);

        boolean hostHeaderFound = false;

        if (getHeaders() != null) {
            for (Map.Entry<String, List<String>> header : getHeaders()
                    .entrySet()) {
                if (!hostHeaderFound) {
                    hostHeaderFound = header.getKey().equalsIgnoreCase("host");
                }

                for (String value : header.getValue()) {
                    sb.append(header.getKey()).append(": ").append(value)
                            .append(HttpProxyConstants.CRLF);
                }
            }

            if (!hostHeaderFound
                    && getHttpVersion() == HttpProxyConstants.HTTP_1_1) {
                sb.append("Host: ").append(getHost()).append(
                        HttpProxyConstants.CRLF);
            }
        }

        sb.append(HttpProxyConstants.CRLF);

        return sb.toString();
    }
}