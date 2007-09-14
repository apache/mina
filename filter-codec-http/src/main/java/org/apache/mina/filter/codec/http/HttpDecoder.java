/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.mina.filter.codec.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;

import org.apache.mina.common.ByteBuffer;

/**
 * TODO HttpDecoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HttpDecoder {
    /**
     * Carriage return character
     */
    private static final byte CR = 13;

    /**
     * Line feed character
     */
    private static final byte LF = 10;

    public final static String COOKIE_COMMENT = "comment";

    public final static String COOKIE_DOMAIN = "domain";

    public final static String COOKIE_EXPIRES = "expires";

    public final static String COOKIE_MAX_AGE = "max-age";

    public final static String COOKIE_PATH = "path";

    public final static String COOKIE_SECURE = "secure";

    public final static String COOKIE_VERSION = "version";

    public final static String SET_COOKIE = "Set-Cookie";

    public final static String TRANSFER_ENCODING = "Transfer-Encoding";

    public final static String CHUNKED = "chunked";

    private CharsetDecoder decoder = Charset.defaultCharset().newDecoder();

    public String decodeLine(ByteBuffer in) throws Exception {
        int beginPos = in.position();
        int limit = in.limit();
        boolean lastIsCR = false;
        int terminatorPos = -1;

        for (int i = beginPos; i < limit; i++) {
            byte b = in.get(i);
            if (b == CR) {
                lastIsCR = true;
            } else {
                if (b == LF && lastIsCR) {
                    terminatorPos = i;
                    break;
                }
                lastIsCR = false;
            }
        }

        //Check if we don't have enough data to process or found a full readable line
        if (terminatorPos == -1)
            return null;

        String result = null;
        if (terminatorPos > 1) {
            ByteBuffer line = in.slice();
            line.limit(terminatorPos - beginPos - 1);
            result = line.getString(decoder);
        }

        in.position(terminatorPos + 1);

        return result;
    }

    public void decodeStatus(String line, HttpResponseMessage msg)
            throws Exception {
        String magic = line.substring(0, 8);
        if (!magic.equals("HTTP/1.1") && !magic.equals("HTTP/1.0"))
            throw new IOException("Invalid HTTP response");

        String status = line.substring(9, 12);
        msg.setStatusCode(Integer.parseInt(status));
        msg.setStatusMessage(line.substring(13));
    }

    public void decodeHeader(String line, HttpResponseMessage msg)
            throws Exception {
        int pos = line.indexOf(": ");
        String name = line.substring(0, pos);
        String value = line.substring(pos + 2);
        msg.addHeader(name, value);

        if (name.equalsIgnoreCase(SET_COOKIE)) {
            Cookie cookie = decodeCookie(value);
            if (cookie != null)
                msg.addCookie(cookie);
        }

        if (name.equalsIgnoreCase(HttpMessage.CONTENT_TYPE)) {
            msg.setContentType(value);
        }

        if (name.equalsIgnoreCase(HttpMessage.CONTENT_LENGTH)) {
            msg.setContentLength(Integer.parseInt(value));
        }

        if (name.equalsIgnoreCase(TRANSFER_ENCODING) && value != null
                && value.equalsIgnoreCase(CHUNKED)) {
            msg.setChunked(true);
        }

    }

    public int decodeSize(String line) throws Exception {
        String strippedLine = line.trim().toLowerCase();
        for (int i = 0; i < strippedLine.length(); i++) {
            char ch = strippedLine.charAt(i);
            //Once we hit a non-numeric character, parse the number we have
            if ((ch < '0' || (ch > '9' && ch < 'a') || ch > 'f')) {
                return Integer.parseInt(strippedLine.substring(0, i), 16);
            }
        }

        //We got here, so the entire line passes
        return Integer.parseInt(strippedLine, 16);
    }

    public void decodeContent(ByteBuffer in, HttpResponseMessage msg)
            throws Exception {
        byte content[] = new byte[msg.getContentLength()];
        in.get(content);
        msg.addContent(content);
    }

    public void decodeChunkedContent(ByteBuffer in, HttpResponseMessage msg)
            throws Exception {
        int toRead = msg.getExpectedToRead();
        if ((in.get(in.position() + toRead) != CR)
                && (in.get(in.position() + toRead + 1) != LF)) {
            throw new IOException(
                    "Invalid HTTP response - chunk does not end with CRLF");

        }
        byte content[] = new byte[toRead];
        in.get(content);
        msg.addContent(content);

        //Pop the CRLF
        in.get();
        in.get();
    }

    public Cookie decodeCookie(String cookieStr) throws Exception {

        Cookie cookie = null;

        String pairs[] = cookieStr.split(";");
        for (int i = 0; i < pairs.length; i++) {
            String nameValue[] = pairs[i].trim().split("=");
            String name = nameValue[0].trim();

            //First one is the cookie name/value pair
            if (i == 0) {
                cookie = new Cookie(name, nameValue[1].trim());
                continue;
            }

            if (name.equalsIgnoreCase(COOKIE_COMMENT)) {
                cookie.setComment(nameValue[1].trim());
                continue;
            }

            if (name.equalsIgnoreCase(COOKIE_PATH)) {
                cookie.setPath(nameValue[1].trim());
            }

            if (name.equalsIgnoreCase(COOKIE_SECURE)) {
                cookie.setSecure(true);
            }

            if (name.equalsIgnoreCase(COOKIE_VERSION)) {
                cookie.setVersion(Integer.parseInt(nameValue[1]));
            }

            if (name.equalsIgnoreCase(COOKIE_MAX_AGE)) {
                int age = Integer.parseInt(nameValue[1]);
                cookie.setExpires(new Date(System.currentTimeMillis() + age
                        * 1000L));
            }

            if (name.equalsIgnoreCase(COOKIE_EXPIRES)) {
                cookie.setExpires(DateUtil.parseDate(nameValue[1]));
            }

            if (name.equalsIgnoreCase(COOKIE_DOMAIN)) {
                cookie.setDomain(nameValue[1]);
            }
        }

        return cookie;
    }
}
