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

/**
 * A dummy {@link PropertyEditor} for <tt>null</tt>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NullEditor extends AbstractPropertyEditor {
    @Override
    protected String toText(Object value) {
        if (String.valueOf(value).equalsIgnoreCase("null")) {
            return null;
        }
        throw new IllegalArgumentException("value is not null or 'null'.");
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (text.equalsIgnoreCase("null")) {
            return null;
        }
        throw new IllegalArgumentException("text is not null or 'null'.");
    }
}
