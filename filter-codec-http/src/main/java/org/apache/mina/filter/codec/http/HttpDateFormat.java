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
package org.apache.mina.filter.codec.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility for generating date strings in the format required by HTTP.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class HttpDateFormat {

    /**
     * By default, we update the format if it is more than a second old
     */
    private static final int DEFAULT_GRANULARITY = 1000;

    private static int granularity = DEFAULT_GRANULARITY;

    /**
     * Thread local <code>HttpDateFormat</code>
     */
    private static final ThreadLocal<HttpDateFormat> FORMAT_LOCAL = new ThreadLocal<HttpDateFormat>() {
        @Override
        protected HttpDateFormat initialValue() {
            return new HttpDateFormat();
        }
    };

    /**
     * Format for HTTP dates
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    /**
     * The time of the last format operation (0 if none have yet taken place)
     */
    private long timeLastGenerated;

    /**
     * The current formatted HTTP date
     */
    private String currentHTTPDate;

    private HttpDateFormat() {
        // HTTP date format specifies GMT
        dateFormat.setTimeZone(TimeZone.getTimeZone(
                HttpCodecUtils.DEFAULT_TIME_ZONE_NAME));
    }

    /**
     * Returns the current time formatted as specified in the HTTP 1.1 specification.
     *
     * @return  The formatted date
     */
    public static String getCurrentHttpDate() {
        return FORMAT_LOCAL.get().getCurrentDate();
    }

    /**
     * Provides the current formatted date to be employed.
     * If we haven't updated our view of the time in the last 'granularity' ms,
     * we format a fresh value.
     *
     * @return  The current http date
     */
    private String getCurrentDate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - timeLastGenerated > granularity) {
            timeLastGenerated = currentTime;
            currentHTTPDate = dateFormat.format(new Date(currentTime));
        }
        return currentHTTPDate;
    }

}
