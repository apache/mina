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
import java.beans.PropertyEditorSupport;

/**
 * An abstract bi-directional {@link PropertyEditor}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractPropertyEditor extends PropertyEditorSupport {

    private String text;
    private Object value;
    private boolean trimText = true;
    
    protected void setTrimText(boolean trimText) {
        this.trimText = trimText;
    }

    @Override
    public String getAsText() {
        return text;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.text = text;
        if (text == null) {
            value = defaultValue();
        } else {
            value = toValue(trimText? text.trim() : text);
        }
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
        if (value == null) {
            text = defaultText();
        } else {
            text = toText(value);
        }
    }
    
    protected String defaultText() {
        return null;
    }
    
    protected Object defaultValue() {
        return null;
    }

    protected abstract String toText(Object value);
    protected abstract Object toValue(String text) throws IllegalArgumentException;
    
}
