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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpProxyRequest extends ProxyRequest {
    private final static Logger logger = LoggerFactory
            .getLogger(HttpProxyRequest.class);

    /**
     * The HTTP verb.
     */
    public final String httpVerb;

    /**
     * The HTTP URI.
     */
    public final String httpURI;

    /**
     * The HTTP protocol version.
     */
    private String httpVersion;

    /**
     * The target hostname.
     */
    private String host;

    /**
     * The request headers.
     */
    private Map<String, List<String>> headers;

    /**
     * The additionnal properties supplied to use with the proxy for 
     * authentication for example. 
     */
    private transient Map<String, String> properties;

    /**
     * Constructor which creates a HTTP/1.0 CONNECT request to the specified 
     * endpoint.
     *  
     * @param endpointAddress the endpoint to connect to
     */
    public HttpProxyRequest(final InetSocketAddress endpointAddress) {
        this(endpointAddress, HttpProxyConstants.HTTP_1_0, null);
    }

    /**
     * Constructor which creates a CONNECT request to the specified endpoint
     * using the provided protocol version.
     *  
     * @param endpointAddress the endpoint to connect to
     * @param httpVersion the HTTP protocol version
     */    
    public HttpProxyRequest(final InetSocketAddress endpointAddress,
            final String httpVersion) {
        this(endpointAddress, httpVersion, null);
    }

    /**
     * Constructor which creates a CONNECT request to the specified endpoint
     * using the provided protocol version and setting the requested headers.
     *  
     * @param endpointAddress the endpoint to connect to
     * @param httpVersion the HTTP protocol version
     * @param headers the additionnal http headers
     */    
    public HttpProxyRequest(final InetSocketAddress endpointAddress,
            final String httpVersion, final Map<String, List<String>> headers) {
        this.httpVerb = HttpProxyConstants.CONNECT;
        if (!endpointAddress.isUnresolved()) {
            this.httpURI = endpointAddress.getHostName() + ":"
                            + endpointAddress.getPort();
        } else {
            this.httpURI = endpointAddress.getAddress().getHostAddress() + ":"
                            + endpointAddress.getPort();
        }
        
        this.httpVersion = httpVersion;
        this.headers = headers;
    }

    /**
     * Constructor which creates a HTTP/1.0 GET request to the specified 
     * http URI.
     *  
     * @param httpURI the target URI
     */    
    public HttpProxyRequest(final String httpURI) {
        this(HttpProxyConstants.GET, httpURI, HttpProxyConstants.HTTP_1_0, null);
    }

    /**
     * Constructor which creates a GET request to the specified http URI
     * using the provided protocol version
     *  
     * @param httpURI the target URI
     * @param httpVersion the HTTP protocol version
     */        
    public HttpProxyRequest(final String httpURI, final String httpVersion) {
        this(HttpProxyConstants.GET, httpURI, httpVersion, null);
    }

    /**
     * Constructor which creates a request using the provided HTTP verb targeted at
     * the specified http URI using the provided protocol version.
     * 
     * @param httpVerb the HTTP verb to use 
     * @param httpURI the target URI
     * @param httpVersion the HTTP protocol version
     */        
    public HttpProxyRequest(final String httpVerb, final String httpURI,
            final String httpVersion) {
        this(httpVerb, httpURI, httpVersion, null);
    }

    /**
     * Constructor which creates a request using the provided HTTP verb targeted at
     * the specified http URI using the provided protocol version and setting the 
     * requested headers.
     * 
     * @param httpVerb the HTTP verb to use 
     * @param httpURI the target URI
     * @param httpVersion the HTTP protocol version
     * @param headers the additional http headers
     */
    public HttpProxyRequest(final String httpVerb, final String httpURI,
            final String httpVersion, final Map<String, List<String>> headers) {
        this.httpVerb = httpVerb;
        this.httpURI = httpURI;
        this.httpVersion = httpVersion;
        this.headers = headers;
    }

    /**
     * Returns the HTTP request verb.
     */
    public final String getHttpVerb() {
        return httpVerb;
    }

    /**
     * Returns the HTTP version.
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * Sets the HTTP version.
     * 
     * @param httpVersion the HTTP protocol version
     */
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * Returns the host to which we are connecting.
     */
    public synchronized final String getHost() {
        if (host == null) {
            if (getEndpointAddress() != null && 
                    !getEndpointAddress().isUnresolved()) {
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
     * Returns the request HTTP URI.
     */
    public final String getHttpURI() {
        return httpURI;
    }

    /**
     * Returns the HTTP headers.
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
     * Returns additional properties for the request.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set additional properties for the request.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Check if the given property(ies) is(are) set. Otherwise throws a 
     * {@link ProxyAuthException}.
     */
    public void checkRequiredProperties(String... propNames) throws ProxyAuthException {
        StringBuilder sb = new StringBuilder();
        for (String propertyName : propNames) {
            if (properties.get(propertyName) == null) {
                sb.append(propertyName).append(' ');
            }
        }
        if (sb.length() > 0) {
            sb.append("property(ies) missing in request");
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