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

/**
 * A CoAP message option.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CoapOption {

    private final CoapOptionType type;

    private final byte[] data;

    /**
     * Create a CoAP option
     * 
     * @param type the type of the option
     * @param data the content of the option
     */
    public CoapOption(CoapOptionType type, byte[] data) {
        super();
        this.type = type;
        this.data = Arrays.copyOf(data, data.length);
    }

    public CoapOptionType getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CoapOption [type=").append(type).append(", data=").append(Arrays.toString(data)).append("]");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(data);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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

        CoapOption other = (CoapOption) obj;

        if (!Arrays.equals(data, other.data)) {
            return false;
        }

        if (type != other.type) {
            return false;
        }

        return true;
    }
}
