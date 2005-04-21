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
package org.apache.mina.examples.echoserver;

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.Session;
import org.apache.mina.common.SessionInitializer;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.datagram.DatagramAcceptor;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;

/**
 * Tests echo server example.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class AbstractTest extends TestCase
{
    private final boolean testInitializer;
    
    private MarkingInitializer initializer;

    protected int port;

    protected IoAcceptor acceptor;

    protected IoAcceptor datagramAcceptor;

    protected IoThreadPoolFilter threadPoolFilter;
    
    protected AbstractTest( boolean testInitializer )
    {
        this.testInitializer = testInitializer;
    }

    protected static void assertEquals( byte[] expected, byte[] actual )
    {
        assertEquals( toString( expected ), toString( actual ) );
    }

    protected static void assertEquals( ByteBuffer expected, ByteBuffer actual )
    {
        assertEquals( toString( expected ), toString( actual ) );
    }

    protected static String toString( byte[] buf )
    {
        StringBuffer str = new StringBuffer( buf.length * 4 );
        for( int i = 0; i < buf.length; i ++ )
        {
            str.append( buf[ i ] );
            str.append( ' ' );
        }
        return str.toString();
    }
    
    protected static String toString( ByteBuffer buf )
    {
        return buf.getHexDump();
    }

    protected void setUp() throws Exception
    {
        acceptor = new SocketAcceptor();
        datagramAcceptor = new DatagramAcceptor();
        
        if( testInitializer )
        {
            initializer = new MarkingInitializer();
        }

        // Find an availble test port and bind to it.
        boolean socketBound = false;
        boolean datagramBound = false;

        // Let's start from port #1 to detect possible resource leak
        // because test will fail in port 1-1023 if user run this test
        // as a normal user.
        for( port = 1; port <= 65535; port ++ )
        {
            socketBound = false;
            datagramBound = false;
            try
            {
                acceptor.bind( new InetSocketAddress( port ),
                               new EchoProtocolHandler(), initializer );
                socketBound = true;

                datagramAcceptor.bind( new InetSocketAddress( port ),
                                       new EchoProtocolHandler(),
                                       initializer );
                datagramBound = true;

                break;
            }
            catch( IOException e )
            {
            }
            finally
            {
                if( !socketBound || !datagramBound )
                {
                    if( socketBound )
                    {
                        acceptor.unbind( new InetSocketAddress( port ) );
                    }
                    if( datagramBound )
                    {
                        datagramAcceptor
                                .unbind( new InetSocketAddress( port ) );
                    }
                }
            }
        }

        // If there is no port available, test fails.
        if( !socketBound || !datagramBound )
        {
            throw new IOException( "Cannot bind any test port." );
        }

        System.out.println( "Using port " + port + " for testing." );

        threadPoolFilter = new IoThreadPoolFilter();
        threadPoolFilter.start();

        acceptor.getFilterChain().addFirst( "threadPool", threadPoolFilter );
        datagramAcceptor.getFilterChain().addFirst( "threadPool", threadPoolFilter );
    }

    protected void tearDown() throws Exception
    {
        acceptor.unbind( new InetSocketAddress( port ) );
        datagramAcceptor.unbind( new InetSocketAddress( port ) );
        threadPoolFilter.stop();
        
        if( initializer != null  )
        {
            Assert.assertTrue( initializer.executed );
        }
    }

    private static class MarkingInitializer implements SessionInitializer
    {
        private boolean executed;

        public void initializeSession(Session session) throws IOException
        {
            executed = true;
        }
    }
}
