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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.apache.mina.filter.query.Request;
import org.apache.mina.filter.query.Response;

/**
 * A representation of CoAP message following the CoAP RFC.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CoapMessage implements Request, Response {

    public static final CoapMessage get(String url, boolean confimable) {

        return new CoapMessage(1, confimable ? MessageType.CONFIRMABLE : MessageType.NON_CONFIRMABLE,
                CoapCode.GET.getCode(), (int) (System.nanoTime() % 65536), null, optionsForUrl(url), null);
    }

    public static final CoapMessage post(String url, boolean confimable, byte[] payload) {

        return new CoapMessage(1, confimable ? MessageType.CONFIRMABLE : MessageType.NON_CONFIRMABLE,
                CoapCode.GET.getCode(), (int) (System.nanoTime() % 65536), null, optionsForUrl(url), payload);
    }

    private static final CoapOption[] optionsForUrl(String url) {
        String[] parts = url.split("\\?");

        String[] paths = parts[0].split("\\/");

        String[] params = new String[] {};

        if (parts.length > 1) {
            params = parts[1].split("\\&");
        }
        CoapOption[] opt = new CoapOption[paths.length + params.length];
        for (int i = 0; i < paths.length; i++) {
            try {
                opt[i] = new CoapOption(CoapOptionType.URI_PATH, paths[i].getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        for (int i = 0; i < params.length; i++) {
            try {
                opt[paths.length + i] = new CoapOption(CoapOptionType.URI_QUERY, params[i].getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        return opt;
    }

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};

    private int version;

    private MessageType type;

    private int code;

    private int id;

    private byte[] token;

    private byte[] payload;

    private CoapOption[] options;

    /**
     * Create a CoAP message
     * 
     * @param version the version (you probably want 1 here)
     * @param type the type of CoAP message
     * @param code the message code : {@link CoapCode}
     * @param id the identifier for this message
     * @param token the message token (can be <code>null</code>)
     * @param options list of options for this message (can be <code>null</code>)
     * @param payload the payload of the message (can be <code>null</code>
     */
    public CoapMessage(int version, MessageType type, int code, int id, byte[] token, CoapOption[] options,
            byte[] payload) {
        super();
        this.version = version;
        this.type = type;
        this.code = code;
        this.id = id;
        this.token = token == null ? EMPTY_BYTE_ARRAY : token;
        this.payload = payload == null ? EMPTY_BYTE_ARRAY : payload;
        this.options = options == null ? new CoapOption[] {} : options;

        // sort options by code (easier for delta encoding)
        Arrays.<CoapOption> sort(this.options, new Comparator<CoapOption>() {
            @Override
            public int compare(CoapOption o1, CoapOption o2) {
                return o1.getType().getCode() < o2.getType().getCode() ? -1 : (o1.getType().getCode() == o2.getType()
                        .getCode() ? 0 : 1);
            };
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object requestId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public int getCode() {
        return code;
    }

    public int getId() {
        return id;
    }

    public byte[] getToken() {
        return token;
    }

    public byte[] getPayload() {
        return payload;

    }

    public CoapOption[] getOptions() {
        return options;
    }

    public MessageType getType() {
        return type;
    }

    /* Utility methods to rebuild some CoAP options */

    /**
     * @return all segments of the absolute path to the resource
     */
    public String[] getUriPath() {
        return strArrayOptions(CoapOptionType.URI_PATH);
    }

    /**
     * @return all arguments parameterizing the resource
     */
    public String[] getUriQuery() {
        return strArrayOptions(CoapOptionType.URI_QUERY);
    }

    /**
     * @return the Internet host of the resource being requested
     */
    public String getUriHost() {
        return strOption(CoapOptionType.URI_HOST);
    }

    /**
     * @return the transport layer port number of the resource
     */
    public Integer getUriPort() {
        return intOption(CoapOptionType.URI_PORT);
    }

    /**
     * return the absolute URI used to make a request to a forward-proxy
     */
    public String getProxyUri() {
        return strOption(CoapOptionType.PROXY_URI);
    }

    /**
     * @return the scheme to be used in the proxy URI
     */
    public String getProxyScheme() {
        return strOption(CoapOptionType.PROXY_SCHEME);
    }

    /**
     * @return the representation format of the message payload
     */
    public Integer getContentFormat() {
        return intOption(CoapOptionType.CONTENT_FORMAT);
    }

    /**
     * @return which Content-Format is acceptable to the client
     */
    public Integer getAccept() {
        return intOption(CoapOptionType.ACCEPT);
    }

    /**
     * @return the maximum time a response may be cached before it is considered not fresh
     */
    public Integer getMaxAge() {
        return intOption(CoapOptionType.MAX_AGE);
    }

    /**
     * @return all segments of the path to the created resource (as the result of a POST request)
     */
    public String[] getLocationPath() {
        return strArrayOptions(CoapOptionType.LOCATION_PATH);
    }

    /**
     * @return all arguments parameterizing the created resource (as the result of a POST request)
     */
    public String[] getLocationQuery() {
        return strArrayOptions(CoapOptionType.LOCATION_QUERY);
    }

    private String strOption(CoapOptionType type) {
        if (options != null) {
            for (CoapOption option : options) {
                if (type.equals(option.getType())) {
                    try {
                        return new String(option.getData(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return null;
    }

    private String[] strArrayOptions(CoapOptionType type) {
        Collection<String> opts = new ArrayList<>();

        if (options != null) {
            for (CoapOption option : options) {
                if (type.equals(option.getType())) {
                    try {
                        opts.add(new String(option.getData(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        return opts.toArray(new String[0]);
    }

    private Integer intOption(CoapOptionType type) {
        if (options != null) {
            for (CoapOption option : options) {
                if (type.equals(option.getType())) {
                    ByteBuffer bb = ByteBuffer.wrap(option.getData());
                    bb.order(ByteOrder.BIG_ENDIAN);
                    return bb.getInt();
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        CoapCode c = CoapCode.fromCode(code);
        String cstr;
        if (c == null) {
            cstr = String.valueOf(code);
        } else {
            cstr = c.getText();
        }

        builder.append("CoapMessage [version=").append(version).append(", type=").append(type).append(", code=")
                .append(cstr).append(", id=").append(id).append(", token=").append(Arrays.toString(token))
                .append(", payload=").append(Arrays.toString(payload)).append(", options=")
                .append(Arrays.toString(options)).append("]");

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        result = prime * result + id;
        result = prime * result + Arrays.hashCode(options);
        result = prime * result + Arrays.hashCode(payload);
        result = prime * result + Arrays.hashCode(token);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + version;

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        CoapMessage other = (CoapMessage) obj;

        if (code != other.code) {
            return false;
        }

        if (id != other.id) {
            return false;
        }

        if (!Arrays.equals(options, other.options)) {
            return false;
        }

        if (!Arrays.equals(payload, other.payload)) {
            return false;
        }

        if (!Arrays.equals(token, other.token)) {
            return false;
        }

        if (type != other.type) {
            return false;
        }

        if (version != other.version) {
            return false;
        }

        return true;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setOptions(CoapOption[] options) {
        this.options = options;
    }

}
