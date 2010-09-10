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
package org.apache.mina.integration.beans;

import java.beans.PropertyEditor;
import java.net.InetSocketAddress;

/**
 * A {@link PropertyEditor} which converts a {@link String} into an
 * {@link InetSocketAddress}. Valid values include a hostname or IP
 * address and a port number separated by a ':'. If the hostname or IP address
 * is omitted the wildcard address will be used. E.g.:
 * <code>google.com:80</code>, <code>:22</code>, <code>192.168.0.1:110</code>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see java.net.InetSocketAddress
 */
public class InetSocketAddressEditor extends AbstractPropertyEditor {

    @Override
    protected String toText(Object value) {
        InetSocketAddress addr = ((InetSocketAddress) value);
        String hostname;
        if (addr.getAddress() != null) {
            hostname = addr.getAddress().getHostAddress();
        } else {
            hostname = addr.getHostName();
        }
        
        if (hostname.equals("0:0:0:0:0:0:0:0") || hostname.equals("0.0.0.0") ||
            hostname.equals("00:00:00:00:00:00:00:00")) {
            hostname = "*";
        }
        
        return hostname + ':' + addr.getPort();
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (text.length() == 0) {
            return defaultValue();
        }

        int colonIndex = text.lastIndexOf(":");
        if (colonIndex > 0) {
            String host = text.substring(0, colonIndex);
            if (!"*".equals(host)) {
                int port = parsePort(text.substring(colonIndex + 1));
                return new InetSocketAddress(host, port);
            }
        }

        int port = parsePort(text.substring(colonIndex + 1));
        return new InetSocketAddress(port);
    }

    @Override
    protected String defaultText() {
        return "*:0";
    }

    @Override
    protected Object defaultValue() {
        return new InetSocketAddress(0);
    }

    private int parsePort(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Illegal port number: " + s);
        }
    }
}
