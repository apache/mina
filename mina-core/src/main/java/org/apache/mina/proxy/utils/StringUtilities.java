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
package org.apache.mina.proxy.utils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.sasl.AuthenticationException;
import javax.security.sasl.SaslException;

/**
 * StringUtilities.java - Various methods to handle strings.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class StringUtilities {

    /**
     * A directive is a parameter of the digest authentication process.
     * Returns the value of a directive from the map. If mandatory is true and the 
     * value is null, then it throws an {@link AuthenticationException}.
     *  
     * @param directivesMap the directive's map 
     * @param directive the name of the directive we want to retrieve
     * @param mandatory is the directive mandatory
     * @return the mandatory value as a String
     * @throws AuthenticationException if mandatory is true and if 
     * directivesMap.get(directive) == null
     */
    public static String getDirectiveValue(
            HashMap<String, String> directivesMap, String directive,
            boolean mandatory) throws AuthenticationException {
        String value = directivesMap.get(directive);
        if (value == null) {
            if (mandatory) {
                throw new AuthenticationException("\"" + directive
                        + "\" mandatory directive is missing");
            }

            return "";
        }

        return value;
    }

    /**
     * Copy the directive to the {@link StringBuilder} if not null.
     * (A directive is a parameter of the digest authentication process.)
     * 
     * @param directives the directives map
     * @param sb the output buffer
     * @param directive the directive name to look for
     */
    public static void copyDirective(HashMap<String, String> directives,
            StringBuilder sb, String directive) {
        String directiveValue = directives.get(directive);
        if (directiveValue != null) {
            sb.append(directive).append(" = \"").append(directiveValue).append(
                    "\", ");
        }
    }

    /**
     * Copy the directive from the source map to the destination map, if it's
     * value isn't null.
     * (A directive is a parameter of the digest authentication process.)
     * 
     * @param src the source map
     * @param dst the destination map
     * @param directive the directive name
     * @return the value of the copied directive
     */
    public static String copyDirective(HashMap<String, String> src,
            HashMap<String, String> dst, String directive) {
        String directiveValue = src.get(directive);
        if (directiveValue != null) {
            dst.put(directive, directiveValue);
        }

        return directiveValue;
    }

    /**
     * Parses digest-challenge string, extracting each token and value(s). Each token
     * is a directive.
     *
     * @param buf A non-null digest-challenge string.
     * @throws UnsupportedEncodingException 
     * @throws SaslException if the String cannot be parsed according to RFC 2831
     */
    public static HashMap<String, String> parseDirectives(byte[] buf)
            throws SaslException {
        HashMap<String, String> map = new HashMap<String, String>();
        boolean gettingKey = true;
        boolean gettingQuotedValue = false;
        boolean expectSeparator = false;
        byte bch;

        ByteArrayOutputStream key = new ByteArrayOutputStream(10);
        ByteArrayOutputStream value = new ByteArrayOutputStream(10);

        int i = skipLws(buf, 0);
        while (i < buf.length) {
            bch = buf[i];

            if (gettingKey) {
                if (bch == ',') {
                    if (key.size() != 0) {
                        throw new SaslException("Directive key contains a ',':"
                                + key);
                    }

                    // Empty element, skip separator and lws
                    i = skipLws(buf, i + 1);
                } else if (bch == '=') {
                    if (key.size() == 0) {
                        throw new SaslException("Empty directive key");
                    }

                    gettingKey = false; // Termination of key
                    i = skipLws(buf, i + 1); // Skip to next non whitespace

                    // Check whether value is quoted
                    if (i < buf.length) {
                        if (buf[i] == '"') {
                            gettingQuotedValue = true;
                            ++i; // Skip quote
                        }
                    } else {
                        throw new SaslException("Valueless directive found: "
                                + key.toString());
                    }
                } else if (isLws(bch)) {
                    // LWS that occurs after key
                    i = skipLws(buf, i + 1);

                    // Expecting '='
                    if (i < buf.length) {
                        if (buf[i] != '=') {
                            throw new SaslException("'=' expected after key: "
                                    + key.toString());
                        }
                    } else {
                        throw new SaslException("'=' expected after key: "
                                + key.toString());
                    }
                } else {
                    key.write(bch); // Append to key
                    ++i; // Advance
                }
            } else if (gettingQuotedValue) {
                // Getting a quoted value
                if (bch == '\\') {
                    // quoted-pair = "\" CHAR ==> CHAR
                    ++i; // Skip escape
                    if (i < buf.length) {
                        value.write(buf[i]);
                        ++i; // Advance
                    } else {
                        // Trailing escape in a quoted value
                        throw new SaslException(
                                "Unmatched quote found for directive: "
                                        + key.toString() + " with value: "
                                        + value.toString());
                    }
                } else if (bch == '"') {
                    // closing quote
                    ++i; // Skip closing quote
                    gettingQuotedValue = false;
                    expectSeparator = true;
                } else {
                    value.write(bch);
                    ++i; // Advance
                }
            } else if (isLws(bch) || bch == ',') {
                // Value terminated
                extractDirective(map, key.toString(), value.toString());
                key.reset();
                value.reset();
                gettingKey = true;
                gettingQuotedValue = expectSeparator = false;
                i = skipLws(buf, i + 1); // Skip separator and LWS
            } else if (expectSeparator) {
                throw new SaslException(
                        "Expecting comma or linear whitespace after quoted string: \""
                                + value.toString() + "\"");
            } else {
                value.write(bch); // Unquoted value
                ++i; // Advance
            }
        }

        if (gettingQuotedValue) {
            throw new SaslException("Unmatched quote found for directive: "
                    + key.toString() + " with value: " + value.toString());
        }

        // Get last pair
        if (key.size() > 0) {
            extractDirective(map, key.toString(), value.toString());
        }

        return map;
    }

    /**
     * Processes directive/value pairs from the digest-challenge and
     * fill out the provided map.
     * 
     * @param key A non-null String challenge token name.
     * @param value A non-null String token value.
     * @throws SaslException if either the key or the value is null or
     * if the key already has a value. 
     */
    private static void extractDirective(HashMap<String, String> map,
            String key, String value) throws SaslException {
        if (map.get(key) != null) {
            throw new SaslException("Peer sent more than one " + key
                    + " directive");
        }

        map.put(key, value);
    }

    /**
     * Is character a linear white space ?
     * LWS            = [CRLF] 1*( SP | HT )
     * Note that we're checking individual bytes instead of CRLF
     * 
     * @param b the byte to check
     * @return <code>true</code> if it's a linear white space
     */
    public static boolean isLws(byte b) {
        switch (b) {
        case 13: // US-ASCII CR, carriage return
        case 10: // US-ASCII LF, line feed
        case 32: // US-ASCII SP, space
        case 9: // US-ASCII HT, horizontal-tab
            return true;
        }

        return false;
    }

    /**
     * Skip all linear white spaces
     * 
     * @param buf the buf which is being scanned for lws
     * @param start the offset to start at
     * @return the next position in buf which isn't a lws character
     */
    private static int skipLws(byte[] buf, int start) {
        int i;

        for (i = start; i < buf.length; i++) {
            if (!isLws(buf[i])) {
                return i;
            }
        }

        return i;
    }

    /**
     * Used to convert username-value, passwd or realm to 8859_1 encoding
     * if all chars in string are within the 8859_1 (Latin 1) encoding range.
     * 
     * @param str a non-null String
     * @return a non-null String containing the 8859_1 encoded string
     * @throws AuthenticationException 
     */
    public static String stringTo8859_1(String str)
            throws UnsupportedEncodingException {
        if (str == null) {
            return "";
        }

        return new String(str.getBytes("UTF8"), "8859_1");
    }

    /**
     * Returns the value of the named header. If it has multiple values
     * then an {@link IllegalArgumentException} is thrown
     * 
     * @param headers the http headers map
     * @param key the key of the header 
     * @return the value of the http header
     */
    public static String getSingleValuedHeader(
            Map<String, List<String>> headers, String key) {
        List<String> values = headers.get(key);

        if (values == null) {
            return null;
        }

        if (values.size() > 1) {
            throw new IllegalArgumentException("Header with key [\"" + key
                    + "\"] isn't single valued !");
        }

        return values.get(0);
    }

    /**
     * Adds an header to the provided map of headers.
     * 
     * @param headers the http headers map
     * @param key the name of the new header to add
     * @param value the value of the added header
     * @param singleValued if true and the map already contains one value
     * then it is replaced by the new value. Otherwise it simply adds a new
     * value to this multi-valued header.
     */
    public static void addValueToHeader(Map<String, List<String>> headers,
            String key, String value, boolean singleValued) {
        List<String> values = headers.get(key);

        if (values == null) {
            values = new ArrayList<String>(1);
            headers.put(key, values);
        }

        if (singleValued && values.size() == 1) {
            values.set(0, value);
        } else {
            values.add(value);
        }
    }
}