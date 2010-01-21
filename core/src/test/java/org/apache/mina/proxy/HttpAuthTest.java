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
package org.apache.mina.proxy;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.proxy.handlers.http.basic.HttpBasicAuthLogicHandler;
import org.apache.mina.proxy.handlers.http.digest.DigestUtilities;
import org.junit.Test;

/**
 * HttpAuthTest.java - JUNIT tests of the HTTP Basic & Digest authentication mechanisms.
 * See <a href="http://www.ietf.org/rfc/rfc2617.txt">RFC 2617</a> .
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class HttpAuthTest {

    /**
     * The charset in use.
     */
    private final static String CHARSET_IN_USE = "ISO-8859-1";

    /**
     * Tests Basic authentication mechanism.
     */
    @Test

    public void testBasicAuthResponse() {
        String USER = "Aladdin";
        String PWD = "open sesame";

        assertEquals("QWxhZGRpbjpvcGVuIHNlc2FtZQ==", HttpBasicAuthLogicHandler
                .createAuthorization(USER, PWD));
    }

    /**
     * Tests Http Digest authentication mechanism. 
     */
    @Test
    public void testDigestAuthResponse() {
        String USER = "Mufasa";
        String PWD = "Circle Of Life";
        String METHOD = "GET";

        HashMap<String, String> map = new HashMap<String, String>();

        map.put("realm", "testrealm@host.com");
        map.put("qop", "auth");
        map.put("nc", "00000001");

        map.put("cnonce", "0a4f113b");

        map.put("nonce", "dcd98b7102dd2f0e8b11d0f600bfb0c093");
        map.put("opaque", "5ccc069c403ebaf9f0171e9517f40e41");
        map.put("uri", "/dir/index.html");
        map.put("username", USER);

        String response = null;
        try {
            response = DigestUtilities.computeResponseValue(new DummySession(),
                    map, METHOD, PWD, CHARSET_IN_USE, null);
            assertEquals("6629fae49393a05397450978507c4ef1", response);
            writeResponse(map, response);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    /**
     * Pretty prints the digest response header .
     * 
     * @param map the map holding the authentication parameters
     * @param response the built digest response string
     */
    private void writeResponse(HashMap<String, String> map, String response) {
        map.put("response", response);
        StringBuilder sb = new StringBuilder("Digest ");
        boolean addSeparator = false;

        for (String key : map.keySet()) {

            if (addSeparator) {
                sb.append(",\n\t\t\t ");
            } else {
                addSeparator = true;
            }

            boolean quotedValue = !"qop".equals(key) && !"nc".equals(key);
            sb.append(key);
            if (quotedValue) {
                sb.append("=\"").append(map.get(key)).append('\"');
            } else {
                sb.append('=').append(map.get(key));
            }
        }

        //System.out.println("Proxy-Authorization: " + sb.toString());
    }
}