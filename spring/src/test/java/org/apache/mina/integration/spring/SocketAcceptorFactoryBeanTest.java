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

import junit.framework.TestCase;

import org.apache.mina.transport.socket.nio.SocketAcceptor;

/**
 * Tests {@link org.apache.mina.integration.spring.SocketAcceptorFactoryBean}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketAcceptorFactoryBeanTest extends TestCase
{

    public void testCreateIoAcceptorWithDefaults() throws Exception
    {
        SocketAcceptorFactoryBean factory = new SocketAcceptorFactoryBean();

        SocketAcceptor acceptor = ( SocketAcceptor ) factory.createIoAcceptor();
        assertEquals( 50, acceptor.getBacklog() );
        assertEquals( -1, acceptor.getReceiveBufferSize() );
        assertFalse( acceptor.isReuseAddress() );
    }

    public void testCreateIoAcceptorWithNonDefaults() throws Exception
    {
        SocketAcceptorFactoryBean factory = new SocketAcceptorFactoryBean();

        factory.setBacklog( 10000 );
        factory.setReceiveBufferSize( 1024 * 1024 );
        factory.setReuseAddress( true );

        SocketAcceptor acceptor = ( SocketAcceptor ) factory.createIoAcceptor();
        assertEquals( 10000, acceptor.getBacklog() );
        assertEquals( 1024 * 1024, acceptor.getReceiveBufferSize() );
        assertTrue( acceptor.isReuseAddress() );
    }
}
