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

import java.io.Serializable;
import java.util.Comparator;

public class CookieComparator implements Serializable, Comparator<Cookie> {

    private static final long serialVersionUID = -222644341851192813L;

    public static final CookieComparator INSTANCE = new CookieComparator();

    public int compare(Cookie o1, Cookie o2) {
        int answer;

        // Compare the name first.
        answer = o1.getName().compareTo(o2.getName());
        if (answer != 0) {
            return answer;
        }

        // and then path
        if (o1.getPath() == null) {
            if (o2.getPath() != null) {
                answer = -1;
            } else {
                answer = 0;
            }
        } else {
            answer = o1.getPath().compareTo(o2.getPath());
        }

        if (answer != 0) {
            return answer;
        }

        // and then domain
        if (o1.getDomain() == null) {
            if (o2.getDomain() != null) {
                answer = -1;
            } else {
                answer = 0;
            }
        } else {
            answer = o1.getDomain().compareTo(o2.getDomain());
        }

        return answer;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
