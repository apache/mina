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

import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Map;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.http.HttpResponseStatus.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class HttpCodecUtils {

    public static final String DEFAULT_CHARSET_NAME = "UTF-8";
    public static final Charset DEFAULT_CHARSET =
        Charset.forName(DEFAULT_CHARSET_NAME);
    public static final String US_ASCII_CHARSET_NAME = "US-ASCII";
    public static final Charset US_ASCII_CHARSET =
        Charset.forName(US_ASCII_CHARSET_NAME);
    public static final String DEFAULT_TIME_ZONE_NAME = "GMT";

    /**
     * Ampersand character
     */
    public static final byte AMP = 38;

    /**
     * Colon character
     */
    public static final byte COLON = 58;

    /**
     * Carriage return character
     */
    public static final byte CR = 13;

    /**
     * Equals character
     */
    public static final byte EQUALS = 61;

    /**
     * Line feed character
     */
    public static final byte LF = 10;

    /**
     * Space character
     */
    public static final byte SP = 32;

    /**
     * Plus character
     */
    public static final byte PLUS = 43;

    /**
     * Question mark character
     */
    public static final byte QS = 63;

    /**
     * Horizontal tab character
     */
    public static final byte HT = 9;

    /**
     * Percent character
     */
    public static final byte PERCENT = 37;

    /**
     * Foward-slash character
     */
    public static final byte FOWARD_SLASH = 47;

    /**
     * Back-slash character
     */
    public static final byte BACK_SLASH = 92;

    /**
     * Quote character
     */
    public static final byte QUOTE = 34;

    /**
     * Semi-colon
     */
    public static final byte SEMI_COLON = 59;

    /**
     * Bytes making up a <code>CR LF</code>
     */
    private static final byte[] CRLF_BYTES = new byte[] { CR, LF };

    /**
     * A lookup table for HTPP separator characters
     */
    private static boolean[] HTTP_SEPARATORS = new boolean[128];

    /**
     * A lookup table for HTTP control characters
     */
    private static boolean[] HTTP_CONTROLS = new boolean[128];

    /**
     * A lookup table from ASCII char values to corresponding decimal values
     */
    private static final int[] HEX_DEC = { -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, 00, 01, 02, 03, 04, 05, 06, 07, 8, 9, -1, -1,
            -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, };

    private static final Logger LOG = LoggerFactory
            .getLogger(HttpCodecUtils.class);

    /**
     * Determines whether a specified (US-ASCII) character is a
     * valid hex character:
     * <ul>
     *   <li>A..F</li>
     *   <li>a..f</li>
     *   <li>0..9</li>
     * </ul>
     *
     * @param b  The character to check
     * @return  <code>true</code> iff the character is a valid hex
     *          character
     */
    public static boolean isHex(byte b) {
        return HEX_DEC[b] != -1;
    }

    /**
     * Determines whether a specified (US-ASCII) character is an
     * HTTP field separator
     *
     * @param b  the US-ASCII character to check
     * @return   <code>true</code> iff the character is an HTTP field
     *           separator
     */
    public static boolean isHttpSeparator(byte b) {
        return HTTP_SEPARATORS[b];
    }

    /**
     * Determines whether a specified (US-ASCII) character is an
     * HTTP control character
     *
     * @param b  the US-ASCII character to check
     * @return   <code>true</code> iff the character is an HTTP control
     *           character
     */
    public static boolean isHttpControl(byte b) {
        return HTTP_CONTROLS[b];
    }

    /**
     * Determines whether a specified (US-ASCII) character is an HTTP
     * whitespace character (Space or Horizontal tab)
     *
     * @param b  the US-ASCII character to check
     * @return  <code>true</code> iff the character is an HTTP whitespace
     *          character
     */
    public static boolean isWhiteSpace(byte b) {
        return b == SP || b == HT;
    }

    /**
     * "Pushes back" a byte on to the specified buffer, by rewinding the
     * position by 1 byte
     *
     * @param buffer  The buffer to "push back" to
     */
    public static void pushBack(IoBuffer buffer) {
        buffer.position(buffer.position() - 1);
    }

    /**
     * Obtains the decimal value for a hex value encoded in ASCII
     *
     * @param b  The ASCII encoded byte
     * @return   The decimal value - or <code>-1</code> if the specified byte
     *           is not a valid ASCII hex character
     */
    public static int hexASCIIToDecimal(byte b) {
        return HEX_DEC[b];
    }

    /**
     * Returns the ASCII bytes for a specified string.
     *
     * @param str  The string
     * @return        The ASCII bytes making up the string
     */
    public static byte[] getASCIIBytes(String str) {
        try {
            return str.getBytes(US_ASCII_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Required charset: "
                    + US_ASCII_CHARSET_NAME);
        }
    }

    /**
     * Throws a {@link HttpRequestDecoderException} with a specified cause.
     * This method should be used when the request is badly formed.
     *
     * @param cause  The cause
     */
    public static void throwDecoderException(String cause)
            throws ProtocolDecoderException {
        throwDecoderException(cause, HttpResponseStatus.BAD_REQUEST);
    }

    /**
     * Throws a {@link HttpRequestDecoderException} with a specified response
     * status
     *
     * @param message   The cause
     * @param status  The response status
     */
    public static void throwDecoderException(String message,
            HttpResponseStatus status) throws ProtocolDecoderException {
        if (!(status.getCategory() == Category.CLIENT_ERROR || status
                .getCategory() == Category.SERVER_ERROR)) {
            throw new IllegalArgumentException(
                    "Cant fail with successful response");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Decode failure. Cause: " + message);
        }
        throw new HttpRequestDecoderException(message, status);
    }

    /**
     * Appends a string to a specified {@link IoBuffer}.
     * This method assumes ascii encoding and is primarily used for encoding
     * http header names and values.<br/>
     * Note that encoding header values this way is not stricly correct
     * (character encodings). However, existing containers do it this way
     * (e.g. Tomcat), and we're probably safer doing it a similar way for the
     * time being
     *
     * @param buffer  The buffer to append to
     * @param string  The string to append
     */
    public static void appendString(IoBuffer buffer, String string) {
        if (string == null) {
            return;
        }
        int len = string.length();

        for (int i = 0; i < len; i++) {
            byte b = (byte) string.charAt(i);
            if (isHttpControl(b) && b != HT) {
                b = SP;
            }
            buffer.put(b);
        }
    }

    /**
     * Appends a <code>CR LF</code> to the specified buffer
     *
     * @param buffer  The buffer
     */
    public static void appendCRLF(IoBuffer buffer) {
        buffer.put(CRLF_BYTES);
    }
    
    /**
     * Encodes the headers of a <code>Response</code> to a specified buffer.
     * This encoder does not make smart decisions about which headers to write -
     * the response is expected to already contain self-consistent headers.
     *
     * @param message  The response whose headers are to be encoded
     * @param buffer   The buffer
     */
    public static void encodeHeaders(
            HttpMessage message, IoBuffer buffer, CharsetEncoder encoder) throws CharacterCodingException {
        
        try {
            for (Map.Entry<String, List<String>> header : message.getHeaders()
                    .entrySet()) {
                byte[] key = header.getKey().getBytes(
                        HttpCodecUtils.US_ASCII_CHARSET_NAME);

                for (String value : header.getValue()) {
                    buffer.put(key);
                    buffer.put((byte) ':');
                    buffer.put((byte) ' ');
                    buffer.putString(value, encoder);
                    HttpCodecUtils.appendCRLF(buffer);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(
                    HttpCodecUtils.US_ASCII_CHARSET_NAME + 
                    " should be available.");
        }

        HttpCodecUtils.appendCRLF(buffer);
    }

    /**
     * Writes the response body bytes, if any, to the specified buffer
     *
     * @param message  The response
     * @param buffer   The buffer to write to
     */
    public static void encodeBody(HttpMessage message, IoBuffer buffer) {
        IoBuffer content = (IoBuffer) message.getContent();
        buffer.put(content);
    }

    static {
        // HTTP Separator characters
        HTTP_SEPARATORS[34] = true; // "
        HTTP_SEPARATORS[40] = true; // )
        HTTP_SEPARATORS[41] = true; // (
        HTTP_SEPARATORS[44] = true; // ,
        HTTP_SEPARATORS[47] = true; // /
        HTTP_SEPARATORS[58] = true; // :
        HTTP_SEPARATORS[59] = true; // ;
        HTTP_SEPARATORS[60] = true; // <
        HTTP_SEPARATORS[61] = true; // =
        HTTP_SEPARATORS[62] = true; // >
        HTTP_SEPARATORS[63] = true; // ?
        HTTP_SEPARATORS[64] = true; // @
        HTTP_SEPARATORS[91] = true; // [
        HTTP_SEPARATORS[93] = true; // ]
        HTTP_SEPARATORS[92] = true; // \
        HTTP_SEPARATORS[123] = true; // {
        HTTP_SEPARATORS[125] = true; // }
        // TODO: SP, HT

        // HTTP Control characters
        for (int i = 0; i <= 31; ++i) {
            HTTP_CONTROLS[i] = true;
        }
        HTTP_CONTROLS[127] = true; // DEL

    }

}
