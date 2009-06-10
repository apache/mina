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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A {@link PropertyEditor} which converts a {@link String} into
 * a {@link Date} and vice versa.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DateEditor extends AbstractPropertyEditor {
    private static final Pattern MILLIS = Pattern.compile("[0-9][0-9]*");
    
    private final DateFormat[] formats = new DateFormat[] {
            new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM", Locale.ENGLISH),
            new SimpleDateFormat("yyyy", Locale.ENGLISH),
    };
            
    public DateEditor() {
        for (DateFormat f: formats) {
            f.setLenient(true);
        }
    }

    @Override
    protected String toText(Object value) {
        if (value instanceof Number) {
            long time = ((Number) value).longValue();
            if (time <= 0) {
                return null;
            }
            value = new Date(time);
        }
        return formats[0].format((Date) value);
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        if (MILLIS.matcher(text).matches()) {
            long time = Long.parseLong(text);
            if (time <= 0) {
                return null;
            }
            return new Date(time);
        }
        
        for (DateFormat f: formats) {
            try {
                return f.parse(text);
            } catch (ParseException e) {
            }
        }
        
        throw new IllegalArgumentException("Wrong date: " + text);
    }
}
