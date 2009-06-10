/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.integration.xbean;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;


/**
 * TODO : Add documentation
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SpringXBeanTest
{
    /**
     * Checks to see we can easily configure a NIO based DatagramAcceptor 
     * using XBean-Spring.  Tests various configuration settings for the 
     * NIO based DatagramAcceptor.
     */
    @Test
    public void testNioDatagramAcceptor() throws Exception 
    {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL configURL = classLoader.getResource( "org/apache/mina/integration/xbean/datagramAcceptor.xml" );

        File configF = new File( configURL.toURI() );
        ApplicationContext factory = new FileSystemXmlApplicationContext( configF.toURI().toURL().toString() );
        
        // test default without any properties
        NioDatagramAcceptor acceptor0 = ( NioDatagramAcceptor ) factory.getBean( "datagramAcceptor0" );
        assertNotNull( "acceptor0 should not be null", acceptor0 );
        assertTrue( 
            "Default constructor for NioDatagramAcceptor should have true value for closeOnDeactivation property", 
            acceptor0.isCloseOnDeactivation() );
        
        // test setting the port and IP for the acceptor
        NioDatagramAcceptor acceptor1 = ( NioDatagramAcceptor ) factory.getBean( "datagramAcceptor1" );
        assertNotNull( "acceptor1 should not be null", acceptor1 );
        assertEquals( "192.168.0.1", acceptor1.getDefaultLocalAddress().getAddress().getHostAddress() );
        assertEquals( 110, acceptor1.getDefaultLocalAddress().getPort() );
        
        // test creating with executor and some primitive properties
        NioDatagramAcceptor acceptor2 = ( NioDatagramAcceptor ) factory.getBean( "datagramAcceptor2" );
        assertNotNull( acceptor2 );
        assertFalse( acceptor2.isCloseOnDeactivation() );
        assertFalse( 
            "NioDatagramAcceptor should have false value for closeOnDeactivation property", 
            acceptor2.isCloseOnDeactivation() );
        
        // test creating with multiple addresses
        NioDatagramAcceptor acceptor3 = ( NioDatagramAcceptor ) factory.getBean( "datagramAcceptor3" );
        assertNotNull( acceptor3 );
        assertEquals( 3, acceptor3.getDefaultLocalAddresses().size() );

        InetSocketAddress address1 = ( InetSocketAddress ) acceptor3.getDefaultLocalAddresses().get( 0 );
        assertEquals( "192.168.0.1", address1.getAddress().getHostAddress() );
        assertEquals( 10001, address1.getPort() );
        
        InetSocketAddress address2 = ( InetSocketAddress ) acceptor3.getDefaultLocalAddresses().get( 1 );
        assertEquals( "192.168.0.2", address2.getAddress().getHostAddress() );
        assertEquals( 10002, address2.getPort() );

        InetSocketAddress address3 = ( InetSocketAddress ) acceptor3.getDefaultLocalAddresses().get( 2 );
        assertEquals( "192.168.0.3", address3.getAddress().getHostAddress() );
        assertEquals( 10003, address3.getPort() );
        
        
        // test with multiple default addresses 
//        NioDatagramAcceptor acceptor3 = ( NioDatagramAcceptor ) factory.getBean( "datagramAcceptor3" );
//        assertNotNull( acceptor3 );
//        assertEquals( 3, acceptor3.getDefaultLocalAddresses().size() );
//
//        SocketAddress address0 = acceptor3.getDefaultLocalAddresses().get( 0 );
//        assertNotNull( address0 );
//        
//        SocketAddress address1 = acceptor3.getDefaultLocalAddresses().get( 1 );
//        assertNotNull( address1 );
//
//        SocketAddress address2 = acceptor3.getDefaultLocalAddresses().get( 2 );
//        assertNotNull( address2 );
    }
}
