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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a one-dimensional array and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ArrayEditor extends AbstractPropertyEditor {
    private final Class<?> componentType;
    
    public ArrayEditor(Class<?> componentType) {
        if (componentType == null) {
            throw new IllegalArgumentException("componentType");
        }
        
        this.componentType = componentType;
        getComponentEditor();
        setTrimText(false);
    }

    private PropertyEditor getComponentEditor() {
        PropertyEditor e = PropertyEditorFactory.getInstance(componentType);
        if (e == null) {
            throw new IllegalArgumentException(
                    "No " + PropertyEditor.class.getSimpleName() + 
                    " found for " + componentType.getSimpleName() + '.');
        }
        return e;
    }

    @Override
    protected String toText(Object value) {
        Class<?> componentType = value.getClass().getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("not an array: " + value);
        }
        
        PropertyEditor e = PropertyEditorFactory.getInstance(componentType);
        if (e == null) {
            throw new IllegalArgumentException(
                    "No " + PropertyEditor.class.getSimpleName() + 
                    " found for " + componentType.getSimpleName() + '.');
        }
        
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < Array.getLength(value); i ++) {
            e.setValue(Array.get(value, i));
            // TODO normalize.
            String s = e.getAsText();
            buf.append(s);
            buf.append(", ");
        }
        
        // Remove the last delimiter.
        if (buf.length() >= 2) {
            buf.setLength(buf.length() - 2);
        }
        return buf.toString();
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        PropertyEditor e = getComponentEditor();
        List<Object> values = new ArrayList<Object>();
        Matcher m = CollectionEditor.ELEMENT.matcher(text);
        boolean matchedDelimiter = true;

        while (m.find()) {
            if (m.group(1) != null) {
                matchedDelimiter = true;
                continue;
            }
            
            if (!matchedDelimiter) {
                throw new IllegalArgumentException("No delimiter between elements: " + text);
            }

            // TODO escape here.
            e.setAsText(m.group());
            values.add(e.getValue());
            
            matchedDelimiter = false;
            if (m.group(2) != null || m.group(3) != null) {
                // Skip the last '"'.
                m.region(m.end() + 1, m.regionEnd());
            }
        }
        
        Object answer = Array.newInstance(componentType, values.size());
        for (int i = 0; i < Array.getLength(answer); i ++) {
            Array.set(answer, i, values.get(i));
        }
        return answer;
    }
}
