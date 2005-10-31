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

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.Assert;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.TransportType;
import org.apache.mina.examples.echoserver.ssl.BogusSSLContextFactory;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoConnector;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.datagram.DatagramConnector;
import org.apache.mina.io.filter.SSLFilter;
import org.apache.mina.io.socket.SocketConnector;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests echo server example.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ConnectorTest extends AbstractTest
{
    public ConnectorTest()
    {
    }

    public void testTCP() throws Exception
    {
        IoConnector connector = new SocketConnector();
        testConnector( connector );
    }
    
    public void testTCPWithSSL() throws Exception
    {
        // Add an SSL filter to acceptor
        SSLFilter acceptorSSLFilter =
            new SSLFilter( BogusSSLContextFactory.getInstance( true ) );
        IoAcceptor acceptor = registry.getIoAcceptor( TransportType.SOCKET );
        acceptor.getFilterChain().addLast( "SSL", acceptorSSLFilter );

        // Create a connector
        IoConnector connector = new SocketConnector();
        
        // Add an SSL filter to connector
        SSLFilter connectorSSLFilter =
            new SSLFilter( BogusSSLContextFactory.getInstance( false ) );
        connectorSSLFilter.setUseClientMode( true ); // set client mode
        connector.getFilterChain().addLast( "SSL", connectorSSLFilter );

        testConnector( connector );
    }
    
    public void testUDP() throws Exception
    {
        IoConnector connector = new DatagramConnector();
        testConnector( connector );
    }
    
    private void testConnector( IoConnector connector ) throws Exception
    {
        System.out.println("* Without localAddress");
        testConnector( connector, false );
        
        System.out.println("* With localAddress");
        testConnector( connector, true );
    }
    
    private void testConnector( IoConnector connector, boolean useLocalAddress ) throws Exception
    {
        EchoConnectorHandler handler = new EchoConnectorHandler();
        ByteBuffer readBuf = handler.readBuf;

        IoSession session = null;
        if( !useLocalAddress )
        {
            session = connector.connect(
                    new InetSocketAddress( InetAddress.getLocalHost(), port ),
                    handler );
        }
        else
        {
            int clientPort = port;
            for( int i = 0; i < 65536; i ++ )
            {
                clientPort = AvailablePortFinder.getNextAvailable( clientPort + 1 );
                try
                {
                    session = connector.connect(
                            new InetSocketAddress( InetAddress.getLocalHost(), port ),
                            new InetSocketAddress( clientPort ),
                            handler );
                    break;
                }
                catch( BindException e )
                {
                    // Try again until we succeed to bind.
                }
            }

            if( session == null )
            {
                Assert.fail( "Failed to find out an appropriate local address." );
            }
        }
        
        for( int i = 0; i < 10; i ++ )
        {
            ByteBuffer buf = ByteBuffer.allocate( 16 );
            buf.limit( 16 );
            fillWriteBuffer( buf, i );
            buf.flip();

            Object marker;
            if( ( i & 1 ) == 0 )
            {
                marker = new Integer( i );
            }
            else
            {
                marker = null;
            }

            session.write( buf, marker );

            // This will align message arrival order in UDP
            for( int j = 0; j < 100; j ++ )
            {
                if( readBuf.position() == ( i + 1 ) * 16 )
                {
                    break;
                }
                Thread.sleep( 10 );
            }
        }
        
        for( int i = 0; i < 100; i++ ) {
            if( readBuf.position() == 160 )
            {
                break;
            }
            else
            {
                Thread.sleep( 10 );
            }
        }

        session.close( true );
        
        Assert.assertEquals( 160, readBuf.position() );
        readBuf.flip();
        
        ByteBuffer expectedBuf = ByteBuffer.allocate( 160 );
        for( int i = 0; i < 10; i ++ ) {
            expectedBuf.limit( ( i + 1 ) * 16 );
            fillWriteBuffer( expectedBuf, i );
        }
        expectedBuf.position( 0 );
        assertEquals(expectedBuf, readBuf);
    }

    private void fillWriteBuffer( ByteBuffer writeBuf, int i )
    {
        while( writeBuf.remaining() > 0 )
        {
            writeBuf.put( ( byte ) ( i ++ ) );
        }
    }

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( ConnectorTest.class );
    }
    
    private static class EchoConnectorHandler extends IoHandlerAdapter
    {
        private ByteBuffer readBuf = ByteBuffer.allocate( 1024 );
        private int counter = 0;

        public void dataRead( IoSession session, ByteBuffer buf )
        {
            readBuf.put( buf );
        }
        
        public void dataWritten( IoSession session, Object marker )
        {
            if( ( counter & 1 ) == 0 )
            {
                Assert.assertEquals( new Integer( counter ), marker );
            }
            else
            {
                Assert.assertNull( marker );
            }
            
            counter ++;
        }

        public void exceptionCaught( IoSession session, Throwable cause )
        {
            cause.printStackTrace();
        }
    }
}
