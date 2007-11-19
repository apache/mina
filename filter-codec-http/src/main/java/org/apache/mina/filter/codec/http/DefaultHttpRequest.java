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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.CharacterCodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.mina.common.IoBuffer;

/**
 * A default implementation of {@link MutableHttpRequest}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultHttpRequest extends DefaultHttpMessage implements
        MutableHttpRequest {

    private static final long serialVersionUID = 3044997961372568928L;

    private HttpMethod method = HttpMethod.GET;
    private URI requestUri;
    private Map<String, List<String>> parameters = new HashMap<String, List<String>>();

    /**
     * Creates a new instance.
     */
    public DefaultHttpRequest() {
    }

    public void setCookies(String headerValue) {
        clearCookies();

        int version = -1; // -1 means version is not parsed yet.
        int fieldIdx = 0;
        MutableCookie currentCookie = null;

        StringTokenizer tk = new StringTokenizer(headerValue, ";,");

        while (tk.hasMoreTokens()) {
            String pair = tk.nextToken();
            String key;
            String value;

            int equalsPos = pair.indexOf('=');
            if (equalsPos >= 0) {
                key = pair.substring(0, equalsPos).trim();
                value = pair.substring(equalsPos + 1).trim();
            } else {
                key = pair.trim();
                value = "";
            }

            if (version < 0) {
                if (!key.equalsIgnoreCase("$Version")) {
                    // $Version is not specified.  Use the default (0).
                    version = 0;
                } else {
                    version = Integer.parseInt(value);
                    if (version != 0 && version != 1) {
                        throw new IllegalArgumentException("Invalid version: "
                                + version + " (" + headerValue + ")");
                    }
                }
            }

            if (version >= 0) {
                try {
                    switch (fieldIdx) {
                    case 1:
                        if (key.equalsIgnoreCase("$Path")) {
                            currentCookie.setPath(value);
                            fieldIdx++;
                        } else {
                            fieldIdx = 0;
                        }
                        break;
                    case 2:
                        if (key.equalsIgnoreCase("$Domain")) {
                            currentCookie.setDomain(value);
                            fieldIdx++;
                        } else {
                            fieldIdx = 0;
                        }
                        break;
                    case 3:
                        if (key.equalsIgnoreCase("$Port")) {
                            // Ignoring for now
                            fieldIdx++;
                        } else {
                            fieldIdx = 0;
                        }
                        break;
                    }
                } catch (NullPointerException e) {
                    throw new IllegalArgumentException(
                            "Cookie key-value pair not found (" + headerValue
                                    + ")");
                }

                if (fieldIdx == 0) {
                    currentCookie = new DefaultCookie(key);
                    currentCookie.setVersion(version);
                    currentCookie.setValue(value);
                    addCookie(currentCookie);
                    fieldIdx++;
                }
            }
        }
    }

    public URI getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(URI requestUri) {
        if (requestUri == null) {
            throw new NullPointerException("requestUri");
        }
        this.requestUri = requestUri;
    }

    public boolean requiresContinuationResponse() {
        if (getProtocolVersion() == HttpVersion.HTTP_1_1) {
            String expectations = getHeader(HttpHeaderConstants.KEY_EXPECT);
            if (expectations != null) {
                return expectations
                        .indexOf(HttpHeaderConstants.VALUE_CONTINUE_EXPECTATION) >= 0;
            }
        }
        return false;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        if (method == null) {
            throw new NullPointerException("method");
        }
        this.method = method;
    }

    public void addParameter(String name, String value) {
        List<String> values = parameters.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            parameters.put(name, values);
        }
        values.add(value);
    }

    public boolean removeParameter(String name) {
        return parameters.remove(name) != null;
    }

    public void setParameter(String name, String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        parameters.put(name, values);
    }

    public void setParameters(Map<String, List<String>> parameters) {
        clearParameters();

        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            for (String value : entry.getValue()) {
                if (value == null) {
                    throw new NullPointerException("Parameter '"
                            + entry.getKey() + "' contains null.");
                }
            }
            if (entry.getValue().size() > 0) {
                this.parameters.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void setParameters(String queryString) {
        try {
            this.setParameters(queryString, HttpCodecUtils.DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(
                    HttpCodecUtils.DEFAULT_CHARSET_NAME +
                    " decoder must be provided by JDK.");
        }
    }

    public void setParameters(String queryString, String encoding)
            throws UnsupportedEncodingException {
        clearParameters();

        if (queryString == null || queryString.length() == 0) {
            return;
        }

        int pos = 0;
        while (pos < queryString.length()) {
            int ampPos = queryString.indexOf('&', pos);

            String value;
            if (ampPos < 0) {
                value = queryString.substring(pos);
                ampPos = queryString.length();
            } else {
                value = queryString.substring(pos, ampPos);
            }

            int equalPos = value.indexOf('=');
            if (equalPos < 0) {
                this.addParameter(URLDecoder.decode(value, encoding), "");
            } else {
                this.addParameter(URLDecoder.decode(value
                        .substring(0, equalPos), encoding), URLDecoder.decode(
                        value.substring(equalPos + 1), encoding));
            }

            pos = ampPos + 1;
        }
    }

    public void clearParameters() {
        this.parameters.clear();
    }

    public boolean containsParameter(String name) {
        return parameters.containsKey(name);
    }

    public String getParameter(String name) {
        List<String> values = parameters.get(name);
        if (values == null) {
            return null;
        }

        return values.get(0);
    }

    public Map<String, List<String>> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public void setContent(IoBuffer content) {
        if (content == null) {
            throw new NullPointerException("content");
        }
        
        if (HttpHeaderConstants.VALUE_URLENCODED_FORM.equalsIgnoreCase(
                getContentType())) {
            content.mark();
            try {
                setParameters(content.getString(
                        HttpCodecUtils.DEFAULT_CHARSET.newDecoder()));
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException(
                        "Failed to decode the url-encoded content.", e);
            } finally {
                content.reset();
            }
        }
        super.setContent(content);
    }

    /**
     * Thread-local DateFormat for old-style cookies
     */
    private static final ThreadLocal<DateFormat> EXPIRY_FORMAT_LOACAL = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat(
                    "EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone(
                    HttpCodecUtils.DEFAULT_TIME_ZONE_NAME));
            return format;
        }

    };

    /**
     * A date long long ago, formatted in the old style cookie expire format
     */
    private static final String EXPIRED_DATE = getFormattedExpiry(0);

    private static String getFormattedExpiry(long time) {
        DateFormat format = EXPIRY_FORMAT_LOACAL.get();
        return format.format(new Date(time));
    }

    public void normalize() {
        // Encode parameters.
        Map<String, List<String>> params = getParameters();
        if (!params.isEmpty()) {
            try {
                boolean first = true;
                StringBuilder buf = new StringBuilder();
                for (Map.Entry<String, List<String>> e: params.entrySet()) {
                    if (e.getValue().isEmpty()) {
                        continue;
                    }
                    
                    for (String v: e.getValue()) {
                        if (!first) {
                            buf.append('&');
                        }
                        
                        buf.append(URLEncoder.encode(e.getKey(), "UTF-8"));
                        buf.append('=');
                        buf.append(URLEncoder.encode(v, "UTF-8"));
                        first = false;
                    }
                }
                
                if (buf.length() > 0) {
                    String uri = getRequestUri().toString();
                    int queryIndex = uri.indexOf('?');
                    switch (getMethod()) {
                    case POST:
                        if (queryIndex >= 0) {
                            setRequestUri(new URI(uri.substring(0, queryIndex)));
                        }
                        IoBuffer content = IoBuffer.allocate(buf.length());
                        content.put(buf.toString().getBytes(HttpCodecUtils.US_ASCII_CHARSET_NAME));
                        content.flip();
                        setContent(content);
                        setHeader(
                                HttpHeaderConstants.KEY_CONTENT_TYPE,
                                HttpHeaderConstants.VALUE_URLENCODED_FORM);
                        break;
                    default:
                        if (queryIndex >= 0) {
                            setRequestUri(new URI(
                                    uri.substring(0, queryIndex + 1) + buf));
                        } else {
                            setRequestUri(new URI(
                                    uri + '?' + buf));
                        }
                    }
                }
            } catch (Exception e) {
                throw (InternalError) new InternalError(
                        "Unexpected exception.").initCause(e);
            }
        }
        
        // Encode Cookies
        Set<Cookie> cookies = getCookies();
        if (!cookies.isEmpty()) {
            // Clear previous values.
            removeHeader(HttpHeaderConstants.KEY_SET_COOKIE);
            
            // And encode.
            for (Cookie c: cookies) {
                StringBuilder buf = new StringBuilder();
                buf.append(c.getName());
                buf.append('=');
                buf.append(c.getValue());
                if (c.getVersion() > 0) {
                    buf.append("; version=");
                    buf.append(c.getVersion());
                }
                if (c.getPath() != null) {
                    buf.append("; path=");
                    buf.append(c.getPath());
                }
                
                long expiry = c.getMaxAge();
                int version = c.getVersion();
                if (expiry >= 0) {
                    if (version == 0) {
                        String expires = expiry == 0 ? EXPIRED_DATE
                                : getFormattedExpiry(System.currentTimeMillis()
                                        + 1000 * expiry);
                        buf.append("; Expires=");
                        buf.append(expires);
                    } else {
                        buf.append("; max-age=");
                        buf.append(c.getMaxAge());
                    }
                }
                
                if (c.isSecure()) {
                    buf.append("; secure");
                }
                
                buf.append(';');
                
                addHeader(HttpHeaderConstants.KEY_SET_COOKIE, buf.toString());
            }
        }
        
        // Add the Host header.
        if (!containsHeader(HttpHeaderConstants.KEY_HOST)) {
            URI uri = getRequestUri();
            String host = uri.getHost();
            if (host != null) {
                if ((uri.getScheme().equalsIgnoreCase("http") &&
                     uri.getPort() != 80 && uri.getPort() > 0) ||
                    (uri.getScheme().equalsIgnoreCase("https") &&
                     uri.getPort() != 443 && uri.getPort() > 0)) {
                    setHeader(HttpHeaderConstants.KEY_HOST, host + ':' + uri.getPort());
                } else {
                    setHeader(HttpHeaderConstants.KEY_HOST, host);
                }
            }
        }
        
        // Set Content-Length.
        if (!containsHeader(HttpHeaderConstants.KEY_TRANSFER_CODING)) {
            IoBuffer content = getContent();
            int contentLength = content == null? 0 : content.remaining();
            setHeader(
                    HttpHeaderConstants.KEY_CONTENT_LENGTH,
                    String.valueOf(contentLength));
        }
    }
}
