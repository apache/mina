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
package org.apache.mina.proxy.handlers.http.digest;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.security.sasl.AuthenticationException;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.ByteUtilities;
import org.apache.mina.proxy.utils.StringUtilities;

/**
 * DigestUtilities.java - A class supporting the HTTP DIGEST authentication (see RFC 2617).
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class DigestUtilities {

    public final static String SESSION_HA1 = DigestUtilities.class
            + ".SessionHA1";

    private static MessageDigest md5;

    static {
        // Initialize secure random generator 
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The supported qualities of protections.
     */
    public final static String[] SUPPORTED_QOPS = new String[] { "auth",
            "auth-int" };

    /**
     * Computes the response to the DIGEST challenge.
     * 
     * @param session the current session
     * @param map the map holding the directives sent by the proxy
     * @param method the HTTP verb
     * @param pwd the password
     * @param charsetName the name of the charset used for the challenge
     * @param body the html body to be hashed for integrity calculations
     */
    public static String computeResponseValue(IoSession session,
            HashMap<String, String> map, String method, String pwd,
            String charsetName, String body) throws AuthenticationException,
            UnsupportedEncodingException {

        byte[] hA1;
        StringBuilder sb;
        boolean isMD5Sess = "md5-sess".equalsIgnoreCase(StringUtilities
                .getDirectiveValue(map, "algorithm", false));

        if (!isMD5Sess || (session.getAttribute(SESSION_HA1) == null)) {
            // Build A1
            sb = new StringBuilder();
            sb.append(
                    StringUtilities.stringTo8859_1(StringUtilities
                            .getDirectiveValue(map, "username", true))).append(
                    ':');

            String realm = StringUtilities.stringTo8859_1(StringUtilities
                    .getDirectiveValue(map, "realm", false));
            if (realm != null) {
                sb.append(realm);
            }

            sb.append(':').append(pwd);

            if (isMD5Sess) {
                byte[] prehA1;
                synchronized (md5) {
                    md5.reset();
                    prehA1 = md5.digest(sb.toString().getBytes(charsetName));
                }

                sb = new StringBuilder();
                sb.append(ByteUtilities.asHex(prehA1));
                sb.append(':').append(
                        StringUtilities.stringTo8859_1(StringUtilities
                                .getDirectiveValue(map, "nonce", true)));
                sb.append(':').append(
                        StringUtilities.stringTo8859_1(StringUtilities
                                .getDirectiveValue(map, "cnonce", true)));

                synchronized (md5) {
                    md5.reset();
                    hA1 = md5.digest(sb.toString().getBytes(charsetName));
                }

                session.setAttribute(SESSION_HA1, hA1);
            } else {
                synchronized (md5) {
                    md5.reset();
                    hA1 = md5.digest(sb.toString().getBytes(charsetName));
                }
            }
        } else {
            hA1 = (byte[]) session.getAttribute(SESSION_HA1);
        }

        sb = new StringBuilder(method);
        sb.append(':');
        sb.append(StringUtilities.getDirectiveValue(map, "uri", false));

        String qop = StringUtilities.getDirectiveValue(map, "qop", false);
        if ("auth-int".equalsIgnoreCase(qop)) {
            ProxyIoSession proxyIoSession = (ProxyIoSession) session
                    .getAttribute(ProxyIoSession.PROXY_SESSION);
            byte[] hEntity;

            synchronized (md5) {
                md5.reset();
                hEntity = md5.digest(body.getBytes(proxyIoSession
                        .getCharsetName()));
            }
            sb.append(':').append(hEntity);
        }

        byte[] hA2;
        synchronized (md5) {
            md5.reset();
            hA2 = md5.digest(sb.toString().getBytes(charsetName));
        }

        sb = new StringBuilder();
        sb.append(ByteUtilities.asHex(hA1));
        sb.append(':').append(
                StringUtilities.getDirectiveValue(map, "nonce", true));
        sb.append(":00000001:");

        sb.append(StringUtilities.getDirectiveValue(map, "cnonce", true));
        sb.append(':').append(qop).append(':');
        sb.append(ByteUtilities.asHex(hA2));

        byte[] hFinal;
        synchronized (md5) {
            md5.reset();
            hFinal = md5.digest(sb.toString().getBytes(charsetName));
        }

        return ByteUtilities.asHex(hFinal);
    }
}