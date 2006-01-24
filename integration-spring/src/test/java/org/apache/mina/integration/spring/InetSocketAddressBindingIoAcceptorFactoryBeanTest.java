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

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.apache.mina.common.IoAcceptor;

/**
 * Tests {@link InetSocketAddressBindingIoAcceptorFactoryBean}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class InetSocketAddressBindingIoAcceptorFactoryBeanTest extends TestCase
{

    public void testParseSocketAddress() throws Exception
    {
        InetSocketAddressBindingIoAcceptorFactoryBean factory = new InetSocketAddressBindingIoAcceptorFactoryBean()
        {
            protected IoAcceptor createIoAcceptor() throws Exception
            {
                // Don't care. This will never be called.
                return null;
            }
        };

        assertEquals( new InetSocketAddress( 1 ), factory
                .parseSocketAddress( "1" ) );
        assertEquals( new InetSocketAddress( 10 ), factory
                .parseSocketAddress( ":10" ) );
        assertEquals( new InetSocketAddress( "foo.bar.com", 100 ), factory
                .parseSocketAddress( "foo.bar.com:100" ) );
        assertEquals( new InetSocketAddress( "192.168.0.1", 1000 ), factory
                .parseSocketAddress( "192.168.0.1:1000" ) );

        try
        {
            factory.parseSocketAddress( null );
            fail( "null string. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
        try
        {
            factory.parseSocketAddress( "bar" );
            fail( "Illegal port number. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
        try
        {
            factory.parseSocketAddress( ":foo" );
            fail( "Illegal port number. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
        try
        {
            factory.parseSocketAddress( "www.foo.com:yada" );
            fail( "Illegal port number. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
    }

}
