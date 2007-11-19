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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.mina.common.IoBuffer;

/**
 * A default implementation of {@link MutableHttpMessage}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultHttpMessage implements MutableHttpMessage {

    private static final long serialVersionUID = -7559479748566065541L;

    private HttpVersion protocolVersion = HttpVersion.HTTP_1_1;

    private final Map<String, List<String>> headers = new TreeMap<String, List<String>>(
            HttpHeaderNameComparator.INSTANCE);

    private final Set<Cookie> cookies = new TreeSet<Cookie>(
            CookieComparator.INSTANCE);

    private IoBuffer content = IoBuffer.allocate(0);

    public HttpVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(HttpVersion protocolVersion) {
        if (protocolVersion == null) {
            throw new NullPointerException("protocolVersion");
        }
        this.protocolVersion = protocolVersion;
    }

    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    public String getHeader(String name) {
        List<String> values = headers.get(name);
        if (values == null) {
            return null;
        }

        return values.get(0);
    }

    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void addHeader(String name, String value) {
        validateHeaderName(name);
        if (value == null) {
            throw new NullPointerException("value");
        }

        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            headers.put(name, values);
        }
        values.add(value);
    }

    public boolean removeHeader(String name) {
        return headers.remove(name) != null;
    }

    public void setHeader(String name, String value) {
        validateHeaderName(name);
        if (value == null) {
            throw new NullPointerException("value");
        }

        List<String> values = new ArrayList<String>();
        values.add(value);
        headers.put(name, values);
    }

    public void setHeaders(Map<String, List<String>> headers) {
        clearHeaders();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            validateHeaderName(entry.getKey());
            for (String value : entry.getValue()) {
                if (value == null) {
                    throw new NullPointerException("Header '" + entry.getKey()
                            + "' contains null.");
                }
            }
            if (entry.getValue().size() > 0) {
                this.headers.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void clearHeaders() {
        this.headers.clear();
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public void setContentType(String type) {
        setHeader("Content-Type", type);
    }

    public boolean isKeepAlive() {
        String connection = getHeader(HttpHeaderConstants.KEY_CONNECTION);
        if (getProtocolVersion() == HttpVersion.HTTP_1_1) {
            return !HttpHeaderConstants.VALUE_CLOSE
                    .equalsIgnoreCase(connection);
        } else {
            return HttpHeaderConstants.VALUE_KEEP_ALIVE
                    .equalsIgnoreCase(connection);
        }
    }

    public void setKeepAlive(boolean keepAlive) {
        setHeader(HttpHeaderConstants.KEY_CONNECTION,
                keepAlive ? HttpHeaderConstants.VALUE_KEEP_ALIVE
                        : HttpHeaderConstants.VALUE_CLOSE);
    }

    public IoBuffer getContent() {
        return content;
    }

    public void setContent(IoBuffer content) {
        if (content == null) {
            throw new NullPointerException("content");
        }
        
        this.content = content;
    }

    public void removeCookie(String name) {
        cookies.remove(name);
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public boolean removeCookie(Cookie cookie) {
        return cookies.remove(cookie);
    }

    public void setCookies(Collection<Cookie> cookies) {
        clearCookies();

        for (Cookie c : cookies) {
            if (c == null) {
                throw new NullPointerException("cookies contains null.");
            }
            this.cookies.add(c);
        }
    }

    public void clearCookies() {
        this.cookies.clear();
    }

    public Set<Cookie> getCookies() {
        return Collections.unmodifiableSet(cookies);
    }

    private static void validateHeaderName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c > 127) {
                throw new IllegalArgumentException(
                        "Name contains an illegal character: " + name);
            }

            byte b = (byte) c;
            if (HttpCodecUtils.isHttpControl(b)
                    || HttpCodecUtils.isHttpSeparator(b)) {
                throw new IllegalArgumentException(
                        "Name contains an illegal character: " + name);
            }
        }
    }
}
