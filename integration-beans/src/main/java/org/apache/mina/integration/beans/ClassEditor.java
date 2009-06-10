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
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link Character} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ClassEditor extends AbstractPropertyEditor {
    @Override
    @SuppressWarnings("unchecked")
    protected String toText(Object value) {
        return ((Class) value).getName();
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        try {
            return Class.forName(text);
        } catch (ClassNotFoundException e) {
            try {
                return getClass().getClassLoader().loadClass(text);
            } catch (ClassNotFoundException e1) {
                throw new IllegalArgumentException("Failed to load the class: " + text);
            }
        }
    }
}
