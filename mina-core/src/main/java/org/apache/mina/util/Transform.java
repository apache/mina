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
package org.apache.mina.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Utility class for working with xml data
 *
 * Implementation is heavily based on org.apache.log4j.helpers.Transform
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Transform {

    private static final String CDATA_START  = "<![CDATA[";
    private static final String CDATA_END    = "]]>";
    private static final String CDATA_PSEUDO_END = "]]&gt;";
    private static final String CDATA_EMBEDED_END = CDATA_END + CDATA_PSEUDO_END + CDATA_START;
    private static final int CDATA_END_LEN = CDATA_END.length();

    /**
     * This method takes a string which may contain HTML tags (ie,
     * &lt;b&gt;, &lt;table&gt;, etc) and replaces any
     * '<',  '>' , '&' or '"'
     * characters with respective predefined entity references.
     *
     * @param input The text to be converted.
     * @return The input string with the special characters replaced.
     * */
    static public String escapeTags(final String input) {
        // Check if the string is null, zero length or devoid of special characters
        // if so, return what was sent in.

        if(input == null
            || input.length() == 0
            || (input.indexOf('"') == -1 &&
            input.indexOf('&') == -1 &&
            input.indexOf('<') == -1 &&
            input.indexOf('>') == -1)) {
            return input;
        }

        StringBuilder buf = new StringBuilder(input.length() + 6);
        char ch;

        int len = input.length();
        for(int i=0; i < len; i++) {
            ch = input.charAt(i);
            if (ch > '>') {
                buf.append(ch);
            } else if(ch == '<') {
                buf.append("&lt;");
            } else if(ch == '>') {
                buf.append("&gt;");
            } else if(ch == '&') {
                buf.append("&amp;");
            } else if(ch == '"') {
                buf.append("&quot;");
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    /**
     * Ensures that embeded CDEnd strings (]]>) are handled properly
     * within message, NDC and throwable tag text.
     *
     * @param buf StringBuffer holding the XML data to this point.  The
     * initial CDStart (<![CDATA[) and final CDEnd (]]>) of the CDATA
     * section are the responsibility of the calling method.
     * @param str The String that is inserted into an existing CDATA Section within buf.
     * */
    static public void appendEscapingCDATA(final StringBuffer buf,
                                           final String str) {
        if (str != null) {
            int end = str.indexOf(CDATA_END);
            if (end < 0) {
                buf.append(str);
            } else {
                int start = 0;
                while (end > -1) {
                    buf.append(str.substring(start, end));
                    buf.append(CDATA_EMBEDED_END);
                    start = end + CDATA_END_LEN;
                    if (start < str.length()) {
                        end = str.indexOf(CDATA_END, start);
                    } else {
                        return;
                    }
                }
                buf.append(str.substring(start));
            }
        }
    }

    /**
     * convert a Throwable into an array of Strings
     * @param throwable
     * @return string representation of the throwable
     */
    public static String[] getThrowableStrRep(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        ArrayList<String> lines = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while(line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch(IOException ex) {
            lines.add(ex.toString());
        }
        String[] rep = new String[lines.size()];
        lines.toArray(rep);
        return rep;
    }

}
