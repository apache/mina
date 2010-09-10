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
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link Collection} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CollectionEditor extends AbstractPropertyEditor {
    static final Pattern ELEMENT = Pattern.compile(
            "([,\\s]+)|" + // Delimiter
            "(?<=\")((?:\\\\\"|\\\\'|\\\\\\\\|\\\\ |[^\"])*)(?=\")|" +
            "(?<=')((?:\\\\\"|\\\\'|\\\\\\\\|\\\\ |[^'])*)(?=')|" +
            "((?:[^\\\\\\s'\",]|\\\\ |\\\\\"|\\\\')+)");
    
    private final Class<?> elementType;
    
    public CollectionEditor(Class<?> elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("elementType");
        }
        
        this.elementType = elementType;
        getElementEditor();
        setTrimText(false);
    }

    private PropertyEditor getElementEditor() {
        PropertyEditor e = PropertyEditorFactory.getInstance(elementType);
        if (e == null) {
            throw new IllegalArgumentException(
                    "No " + PropertyEditor.class.getSimpleName() + 
                    " found for " + elementType.getSimpleName() + '.');
        }
        return e;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final String toText(Object value) {
        StringBuilder buf = new StringBuilder();
        for (Object v: (Collection) value) {
            if (v == null) {
                v = defaultElement();
            }
            
            PropertyEditor e = PropertyEditorFactory.getInstance(v);
            if (e == null) {
                throw new IllegalArgumentException(
                        "No " + PropertyEditor.class.getSimpleName() + 
                        " found for " + v.getClass().getSimpleName() + '.');
            }            
            e.setValue(v);
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
    protected final Object toValue(String text) throws IllegalArgumentException {
        PropertyEditor e = getElementEditor();
        Collection<Object> answer = newCollection();
        Matcher m = ELEMENT.matcher(text);
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
            answer.add(e.getValue());
            
            matchedDelimiter = false;
            if (m.group(2) != null || m.group(3) != null) {
                // Skip the last '"'.
                m.region(m.end() + 1, m.regionEnd());
            }
        }
        
        return answer;
    }
    
    protected Collection<Object> newCollection() {
        return new ArrayList<Object>();
    }
    
    protected Object defaultElement() {
        PropertyEditor e = PropertyEditorFactory.getInstance(elementType);
        if (e == null) {
            return null;
        }
        
        if (e instanceof AbstractPropertyEditor) {
            return ((AbstractPropertyEditor) e).defaultValue();
        }

        return null;
    }
}
