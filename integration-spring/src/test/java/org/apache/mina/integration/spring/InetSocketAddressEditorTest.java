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
package org.apache.mina.integration.spring;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

/**
 * Tests {@link InetSocketAddressEditor}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class InetSocketAddressEditorTest extends TestCase {
    InetSocketAddressEditor editor;

    protected void setUp() throws Exception {
        editor = new InetSocketAddressEditor();
    }

    public void testSetAsTextWithWildcardAddress() throws Exception {
        editor.setAsText("1");
        assertEquals(new InetSocketAddress(1), editor.getValue());
        editor.setAsText(":10");
        assertEquals(new InetSocketAddress(10), editor.getValue());
    }

    public void testSetAsTextWithHostName() throws Exception {
        editor.setAsText("www.google.com:80");
        assertEquals(new InetSocketAddress("www.google.com", 80), editor
                .getValue());
    }

    public void testSetAsTextWithIpAddress() throws Exception {
        editor.setAsText("192.168.0.1:1000");
        assertEquals(new InetSocketAddress("192.168.0.1", 1000), editor
                .getValue());
    }

    public void testSetAsTextWithIllegalValues() throws Exception {
        try {
            editor.setAsText(null);
            fail("null string. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
        try {
            editor.setAsText("bar");
            fail("Illegal port number. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
        try {
            editor.setAsText(":foo");
            fail("Illegal port number. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
        try {
            editor.setAsText("www.foo.com:yada");
            fail("Illegal port number. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }

}
