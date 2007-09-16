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
package org.apache.mina.example.httpserver.codec;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A HTTP request message.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class HttpRequestMessage {
    /** Map<String, String[]> */
    private Map headers = null;

    public void setHeaders(Map headers) {
        this.headers = headers;
    }

    public Map getHeaders() {
        return headers;
    }

    public String getContext() {
        String[] context = (String[]) headers.get("Context");
        return context == null ? "" : context[0];
    }

    public String getParameter(String name) {
        String[] param = (String[]) headers.get("@".concat(name));
        return param == null ? "" : param[0];
    }

    public String[] getParameters(String name) {
        String[] param = (String[]) headers.get("@".concat(name));
        return param == null ? new String[] {} : param;
    }

    public String[] getHeader(String name) {
        return (String[]) headers.get(name);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = (Entry) it.next();
            str.append(e.getKey() + " : "
                    + arrayToString((String[]) e.getValue(), ',') + "\n");
        }
        return str.toString();
    }

    public static String arrayToString(String[] s, char sep) {
        if (s == null || s.length == 0) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        if (s != null) {
            for (int i = 0; i < s.length; i++) {
                if (i > 0) {
                    buf.append(sep);
                }
                buf.append(s[i]);
            }
        }
        return buf.toString();
    }
}
