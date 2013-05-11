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

import java.util.Arrays;
import java.util.Comparator;

/**
 * A representation of CoAP message following the CoAP RFC.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CoapMessage {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};

    private final int version;
    private final MessageType type;
    private final int code;
    private final int id;
    private final byte[] token;
    private final byte[] payload;
    private final CoapOption[] options;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CoapMessage [version=").append(version).append(", type=").append(type).append(", code=")
                .append(code).append(", id=").append(id).append(", token=").append(Arrays.toString(token))
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

}
