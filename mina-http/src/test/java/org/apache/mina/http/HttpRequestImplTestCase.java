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

import java.util.List;
import java.util.Map;

import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpRequestImplTestCase {

    @Test
    public void testGetParameterNoParameter() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/","", null);
        assertNull( req.getParameter("p0"), "p0 doesn't exist");
    }

    @Test
    public void testGetParameterOneEmptyParameter() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "p0=", null);
        assertEquals("", req.getParameter("p0"), "p0 is emtpy");
        assertNull( req.getParameter("p1"), "p1 doesn't exist");
    }

    @Test
    public void testGetParameterOneParameter() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "p0=0", null);
        assertEquals("0", req.getParameter("p0"), "p0 is '0'");
        assertNull( req.getParameter("p1"), "p1 doesn't exist");
    }

    @Test
    public void testGetParameter3Parameters() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "p0=&p1=1&p2=2", null);
        assertEquals("", req.getParameter("p0"), "p0 is emtpy");
        assertEquals("1", req.getParameter("p1"), "p1 is '1'");
        assertEquals("2", req.getParameter("p2"), "p2 is '2'");
        assertNull( req.getParameter("p3"), "p3 doesn't exist");
    }

    @Test
    public void testGetParametersNoParameter() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "", null);
        assertTrue(req.getParameters().isEmpty(), "Empty Map");
    }

    @Test
    public void testGetParameters3Parameters() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/","p0=&p1=1&p2=2", null);
        Map<String, List<String>> parameters = req.getParameters();
        assertEquals(3, parameters.size());
        assertEquals(1, parameters.get("p0").size());
        assertEquals("", parameters.get("p0").get(0));
        assertEquals(1, parameters.get("p1").size());
        assertEquals("1", parameters.get("p1").get(0));
        assertEquals(1, parameters.get("p2").size());
        assertEquals("2", parameters.get("p2").get(0));
    }

    @Test
    public void testGetParameters3ParametersWithDuplicate() {
        HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/","p0=&p1=1&p0=2", null);
        Map<String, List<String>> parameters = req.getParameters();
        assertEquals(2, parameters.size());
        assertEquals(2, parameters.get("p0").size());
        assertEquals("", parameters.get("p0").get(0));
        assertEquals("2", parameters.get("p0").get(1));
        assertEquals(1, parameters.get("p1").size());
        assertEquals("1", parameters.get("p1").get(0));
        assertNull(parameters.get("p2"));
    }

}
