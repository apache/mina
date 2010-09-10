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
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A {@link PropertyEditor} which converts a {@link String} into an
 * {@link InetAddress}.
 * This editor simply calls {@link InetAddress#getByName(java.lang.String)}
 * when converting from a {@link String}, and {@link InetAddress#getHostAddress()}
 * when converting to a {@link String}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see java.net.InetAddress
 */
public class InetAddressEditor extends AbstractPropertyEditor {
    @Override
    protected String toText(Object value) {
        String hostname = ((InetAddress) value).getHostAddress();
        if (hostname.equals("0:0:0:0:0:0:0:0") || hostname.equals("0.0.0.0") ||
                hostname.equals("00:00:00:00:00:00:00:00")) {
            hostname = "*";
        }
        return hostname;
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (text.length() == 0 || text.equals("*")) {
            return defaultValue();
        }

        try {
            return InetAddress.getByName(text);
        } catch (UnknownHostException uhe) {
            IllegalArgumentException iae = new IllegalArgumentException();
            iae.initCause(uhe);
            throw iae;
        }
    }

    @Override
    protected String defaultText() {
        return "*";
    }

    @Override
    protected Object defaultValue() {
        try {
            return InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            throw new InternalError();
        }
    }
}
