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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link Collection} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MapEditor extends AbstractPropertyEditor {
    static final Pattern ELEMENT = Pattern.compile("([,\\s]+)|"
            + // Entry delimiter
            "(\\s*=\\s*)|"
            + // Key-Value delimiter
            "(?<=\")((?:\\\\\"|\\\\'|\\\\\\\\|\\\\ |[^\"])*)(?=\")|"
            + "(?<=')((?:\\\\\"|\\\\'|\\\\\\\\|\\\\ |[^'])*)(?=')|" + "((?:[^\\\\\\s'\",]|\\\\ |\\\\\"|\\\\')+)");

    private final Class<?> keyType;

    private final Class<?> valueType;
    
    private static final String NO_VALUE = "No value {1} found for {2}.";
    private static final String NO_KEY = "No key {1} found for {2}.";

    /**
     * Creates a new DateEditor instance
     * 
     * @param keyType The key type
     * @param valueType The value type
     */
    public MapEditor(Class<?> keyType, Class<?> valueType) {
        if (keyType == null) {
            throw new IllegalArgumentException("keyType");
        }
        
        if (valueType == null) {
            throw new IllegalArgumentException("valueType");
        }
        
        this.keyType = keyType;
        this.valueType = valueType;
        getKeyEditor();
        getValueEditor();
        setTrimText(false);
    }

    private PropertyEditor getKeyEditor() {
        PropertyEditor e = PropertyEditorFactory.getInstance(keyType);
        
        if (e == null) {
            throw new IllegalArgumentException(MessageFormat.format(NO_KEY, PropertyEditor.class.getSimpleName(),
                    keyType.getSimpleName()));
        }
        
        return e;
    }

    private PropertyEditor getValueEditor() {
        PropertyEditor e = PropertyEditorFactory.getInstance(valueType);
        
        if (e == null) {
            throw new IllegalArgumentException(MessageFormat.format(NO_VALUE, PropertyEditor.class.getSimpleName(),
                    valueType.getSimpleName()));
        }
        
        return e;
    }

    @Override
    protected final String toText(Object value) {
        StringBuilder buf = new StringBuilder();
        
        for (Map.Entry<?,?> entry : ((Map<?,?>) value).entrySet()) {
            Object ekey = entry.getKey();
            Object evalue = entry.getValue();

            PropertyEditor ekeyEditor = PropertyEditorFactory.getInstance(ekey);
            
            if (ekeyEditor == null) {
                throw new IllegalArgumentException(MessageFormat.format(NO_KEY, PropertyEditor.class.getSimpleName(),
                        ekey.getClass().getSimpleName()));
            }
            
            ekeyEditor.setValue(ekey);

            PropertyEditor evalueEditor = PropertyEditorFactory.getInstance(evalue);
            
            if (evalueEditor == null) {
                throw new IllegalArgumentException(MessageFormat.format(NO_VALUE, PropertyEditor.class.getSimpleName(),
                        evalue.getClass().getSimpleName()));
            }
            
            ekeyEditor.setValue(ekey);
            evalueEditor.setValue(evalue);

            // TODO normalize.
            String keyString = ekeyEditor.getAsText();
            String valueString = evalueEditor.getAsText();
            buf.append(keyString);
            buf.append(" = ");
            buf.append(valueString);
            buf.append(", ");
        }

        // Remove the last delimiter.
        if (buf.length() >= 2) {
            buf.setLength(buf.length() - 2);
        }
        
        return buf.toString();
    }

    @Override
    protected final Object toValue(String text) {
        PropertyEditor keyEditor = getKeyEditor();
        PropertyEditor valueEditor = getValueEditor();
        Map<Object, Object> answer = newMap();
        Matcher m = ELEMENT.matcher(text);
        TokenType lastTokenType = TokenType.ENTRY_DELIM;
        Object key = null;
        Object value;

        while (m.find()) {
            if (m.group(1) != null) {
                if ((lastTokenType != TokenType.VALUE) && (lastTokenType != TokenType.ENTRY_DELIM)) {
                    throw new IllegalArgumentException("Unexpected entry delimiter: " + text);
                }

                lastTokenType = TokenType.ENTRY_DELIM;
                continue;
            }

            if (m.group(2) != null) {
                if (lastTokenType != TokenType.KEY) {
                    throw new IllegalArgumentException("Unexpected key-value delimiter: " + text);
                }

                lastTokenType = TokenType.KEY_VALUE_DELIM;
                continue;
            }

            // TODO escape here.
            String region = m.group();

            if (m.group(3) != null || m.group(4) != null) {
                // Skip the last '"'.
                m.region(m.end() + 1, m.regionEnd());
            }

            switch (lastTokenType) {
                case ENTRY_DELIM:
                    keyEditor.setAsText(region);
                    key = keyEditor.getValue();
                    lastTokenType = TokenType.KEY;
                    break;
                case KEY_VALUE_DELIM:
                    valueEditor.setAsText(region);
                    value = valueEditor.getValue();
                    lastTokenType = TokenType.VALUE;
                    answer.put(key, value);
                    break;
                case KEY:
                case VALUE:
                    throw new IllegalArgumentException("Unexpected key or value: " + text);
            }
        }

        return answer;
    }

    protected Map<Object, Object> newMap() {
        return new LinkedHashMap<>();
    }

    private enum TokenType {
        ENTRY_DELIM, KEY_VALUE_DELIM, KEY, VALUE,
    }
}
