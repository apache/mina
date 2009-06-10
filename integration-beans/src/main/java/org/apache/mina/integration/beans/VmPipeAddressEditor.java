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

import org.apache.mina.transport.vmpipe.VmPipeAddress;

import java.beans.PropertyEditor;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link VmPipeAddress} and vice versa. Valid values specify an integer port
 * number optionally prefixed with a ':'. E.g.: <code>:80</code>, <code>22</code>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 * @see VmPipeAddress
 */
public class VmPipeAddressEditor extends AbstractPropertyEditor {
    @Override
    protected String toText(Object value) {
        return ":" + ((VmPipeAddress) value).getPort();
    }

    @Override
    protected String defaultText() {
        return ":0";
    }

    @Override
    protected Object defaultValue() {
        return new VmPipeAddress(0);
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (text.startsWith(":")) {
            text = text.substring(1);
        }
        try {
            return new VmPipeAddress(Integer.parseInt(text.trim()));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Illegal VmPipeAddress: " + text);
        }
    }
}
