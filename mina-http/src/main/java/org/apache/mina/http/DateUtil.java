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
package org.apache.mina.http;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * An utility class for Dates manipulations
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DateUtil {
    private static final Locale LOCALE = Locale.US;
    private static final TimeZone GMT_ZONE;
    private static final String RFC_1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final DateFormat RFC_1123_FORMAT;

    /** Pattern to find digits only. */
    private static final Pattern DIGIT_PATTERN = Pattern.compile("^\\d+$");

    static {
        RFC_1123_FORMAT = new SimpleDateFormat(DateUtil.RFC_1123_PATTERN, DateUtil.LOCALE);
        GMT_ZONE = TimeZone.getTimeZone("GMT");
        DateUtil.RFC_1123_FORMAT.setTimeZone(DateUtil.GMT_ZONE);
    }

    private DateUtil() {
    }

    /**
     * @return The current date as a string
     */
    public static String getCurrentAsString() {
        synchronized(DateUtil.RFC_1123_FORMAT) {
            return DateUtil.RFC_1123_FORMAT.format(new Date()); //NOPMD
        }
    }

    /**
     * Translate a given date <code>String</code> in the <em>RFC 1123</em>
     * format to a <code>long</code> representing the number of milliseconds
     * since epoch.
     * 
     * @param dateString a date <code>String</code> in the <em>RFC 1123</em> format.
     * @return the parsed <code>Date</code> in milliseconds.
     */
    private static long parseDateStringToMilliseconds(String dateString) {
        try {
            synchronized (DateUtil.RFC_1123_FORMAT) {
                return DateUtil.RFC_1123_FORMAT.parse(dateString).getTime(); //NOPMD
            }
        } catch (ParseException e) {
            return 0;
        }
    }

    /**
     * Parse a given date <code>String</code> to a <code>long</code>
     * representation of the time. Where the provided value is all digits the
     * value is returned as a <code>long</code>, otherwise attempt is made to
     * parse the <code>String</code> as a <em>RFC 1123</em> date.
     * 
     * @param dateValue the value to parse.
     * @return the <code>long</code> value following parse, or zero where not successful.
     */
    public static long parseToMilliseconds(String dateValue) {
        if (DateUtil.DIGIT_PATTERN.matcher(dateValue).matches()) {
            return Long.parseLong(dateValue);
        } else {
            return parseDateStringToMilliseconds(dateValue);
        }
    }

    /**
     * Converts a millisecond representation of a date to a
     * <code>RFC 1123</code> formatted <code>String</code>.
     * 
     * @param dateValue the <code>Date</code> represented as milliseconds.
     * @return a <code>String</code> representation of the date.
     */
    public static String parseToRFC1123(long dateValue) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateValue);

        synchronized (DateUtil.RFC_1123_FORMAT) {
            return DateUtil.RFC_1123_FORMAT.format(calendar.getTime()); //NOPMD
        }
    }

    /**
     * Convert a given <code>Date</code> object to a <code>RFC 1123</code>
     * formatted <code>String</code>.
     * 
     * @param date the <code>Date</code> object to convert
     * @return a <code>String</code> representation of the date.
     */
    public static String getDateAsString(Date date) {
        synchronized (DateUtil.RFC_1123_FORMAT) {
            return RFC_1123_FORMAT.format(date); //NOPMD
        }
    }
}
