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

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

/**
 * Tests {@link org.apache.mina.integration.spring.InetAddressEditor}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class InetAddressEditorTest extends TestCase {
    InetAddressEditor editor;

    protected void setUp() throws Exception {
        editor = new InetAddressEditor();
    }

    public void testSetAsTextWithHostName() throws Exception {
        try {
            InetAddress expected = InetAddress.getByName("www.google.com");
            editor.setAsText("www.google.com");
            assertEquals(expected, editor.getValue());
        } catch (UnknownHostException uhe) {
            // No DNS. Skip the test.
        }

        editor.setAsText("localhost");
        assertEquals(InetAddress.getByName("localhost"), editor.getValue());
    }

    public void testSetAsTextWithIpAddress() throws Exception {
        editor.setAsText("127.0.0.1");
        assertEquals(InetAddress.getByName("127.0.0.1"), editor.getValue());
    }
}
