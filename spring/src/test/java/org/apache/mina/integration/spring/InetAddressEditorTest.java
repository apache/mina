/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.integration.spring;

import java.net.InetAddress;

import junit.framework.TestCase;

/**
 * Tests {@link org.apache.mina.integration.spring.InetAddressEditor}. NOTE:
 * This test does DNS queries so it needs network access to succeed.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class InetAddressEditorTest extends TestCase
{
    InetAddressEditor editor;

    protected void setUp() throws Exception
    {
        editor = new InetAddressEditor();
    }

    public void testSetAsTextWithHostName() throws Exception
    {
        editor.setAsText( "www.google.com" );
        assertEquals( InetAddress.getByName( "www.google.com" ), editor
                .getValue() );
    }

    public void testSetAsTextWithIpAddress() throws Exception
    {
        editor.setAsText( "127.0.0.1" );
        assertEquals( InetAddress.getByName( "127.0.0.1" ), editor.getValue() );
    }
}
