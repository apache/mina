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
import java.util.regex.Pattern;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link Number} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NumberEditor extends AbstractPropertyEditor {
    private static final Pattern DECIMAL = Pattern.compile(
            "[-+]?[0-9]*\\.?[0-9]*(?:[Ee][-+]?[0-9]+)?");
    private static final Pattern HEXADECIMAL = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern OCTET = Pattern.compile("0[0-9][0-9]*");

    @Override
    protected final String toText(Object value) {
        return (value == null ? "" : value.toString());
    }

    @Override
    protected final Object toValue(String text) throws IllegalArgumentException {
        if (text.length() == 0) {
            return defaultValue();
        }

        if (HEXADECIMAL.matcher(text).matches()) {
            return toValue(text.substring(2), 16);
        }

        if (OCTET.matcher(text).matches()) {
            return toValue(text, 8);
        }

        if (DECIMAL.matcher(text).matches()) {
            return toValue(text, 10);
        }

        throw new NumberFormatException("Not a number: " + text);
    }

    protected Object toValue(String text, int radix) {
        return Integer.parseInt(text, radix);
    }

    @Override
    protected Object defaultValue() {
        return Integer.valueOf(0);
    }
}
