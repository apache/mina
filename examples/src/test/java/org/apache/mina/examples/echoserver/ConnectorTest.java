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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.examples.echoserver.ssl.BogusSSLContextFactory;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.apache.mina.util.SessionLog;

/**
 * Tests echo server example.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ConnectorTest extends AbstractTest
{
    private static final int TIMEOUT = 10000; // 10 seconds
    private final int COUNT = 10;
    private final int DATA_SIZE = 16;
    private SSLFilter connectorSSLFilter;

    public ConnectorTest()
    {
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        connectorSSLFilter =
            new SSLFilter( BogusSSLContextFactory.getInstance( false ) );
        connectorSSLFilter.setUseClientMode( true ); // set client mode
    }

    public void testTCP() throws Exception
    {
        IoConnector connector = new SocketConnector();
        testConnector( connector );
    }
    
    public void testTCPWithSSL() throws Exception
    {
        useSSL = true;
        // Create a connector
        IoConnector connector = new SocketConnector();
        
        // Add an SSL filter to connector
        connector.getDefaultConfig().getFilterChain().addLast( "SSL", connectorSSLFilter );
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
        
        IoSession session = null;
        if( !useLocalAddress )
        {
            ConnectFuture future = connector.connect(
                    new InetSocketAddress( "localhost", port ),
                    handler );
            future.join();
            session = future.getSession();
        }
        else
        {
            int clientPort = port;
            for( int i = 0; i < 65536; i ++ )
            {
                clientPort = AvailablePortFinder.getNextAvailable( clientPort + 1 );
                try
                {
                    ConnectFuture future = connector.connect(
                            new InetSocketAddress( "localhost", port ),
                            new InetSocketAddress( clientPort ),
                            handler );
                    future.join();
                    session = future.getSession();
                    break;
                }
                catch( IOException e )
                {
                    // Try again until we succeed to bind.
                }
            }

            if( session == null )
            {
                Assert.fail( "Failed to find out an appropriate local address." );
            }
        }
        
        // Run a basic connector test.
        testConnector0( session );
        
        // Send closeNotify to test TLS closure if it is TLS connection.
        if( useSSL )
        {
            connectorSSLFilter.stopSSL( session ).join();
            
            System.out.println( "-------------------------------------------------------------------------------" );
            // Test again after we finished TLS session.
            testConnector0( session );
            
            System.out.println( "-------------------------------------------------------------------------------" );
            
            // Test if we can enter TLS mode again.
            //// Send StartTLS request.
            handler.readBuf.clear();
            ByteBuffer buf = ByteBuffer.allocate( 1 );
            buf.put( ( byte ) '.' );
            buf.flip();
            session.write( buf ).join();
            
            //// Wait for StartTLS response.
            waitForResponse( handler, 1 );

            handler.readBuf.flip();
            Assert.assertEquals( 1, handler.readBuf.remaining() );
            Assert.assertEquals( ( byte ) '.', handler.readBuf.get() );
            
            // Now start TLS connection
            Assert.assertTrue( connectorSSLFilter.startSSL( session ) );
            testConnector0( session );
        }
        
        session.close().join();
    }

    private void testConnector0( IoSession session ) throws InterruptedException
    {
        EchoConnectorHandler handler = ( EchoConnectorHandler ) session.getHandler();
        ByteBuffer readBuf = handler.readBuf;
        readBuf.clear();
        WriteFuture writeFuture = null;
        for( int i = 0; i < COUNT; i ++ )
        {
            ByteBuffer buf = ByteBuffer.allocate( DATA_SIZE );
            buf.limit( DATA_SIZE );
            fillWriteBuffer( buf, i );
            buf.flip();
            
            writeFuture = session.write( buf );
            
            if( session.getTransportType().isConnectionless() ) 
            {
                // This will align message arrival order in connectionless transport types
                waitForResponse( handler, ( i + 1 ) * DATA_SIZE );
            }
        }
        
        writeFuture.join();

        waitForResponse( handler, DATA_SIZE * COUNT );

        // Assert data
        //// Please note that BufferOverflowException can be thrown
        //// in SocketIoProcessor if there was a read timeout because
        //// we share readBuf.
        readBuf.flip();
        SessionLog.info( session, "readBuf: " + readBuf );
        Assert.assertEquals( DATA_SIZE * COUNT, readBuf.remaining() );
        ByteBuffer expectedBuf = ByteBuffer.allocate( DATA_SIZE * COUNT );
        for( int i = 0; i < COUNT; i ++ ) {
            expectedBuf.limit( ( i + 1 ) * DATA_SIZE );
            fillWriteBuffer( expectedBuf, i );
        }
        expectedBuf.position( 0 );
        
        assertEquals(expectedBuf, readBuf);
    }

    private void waitForResponse( EchoConnectorHandler handler, int bytes ) throws InterruptedException
    {
        for( int j = 0; j < TIMEOUT / 10; j ++ )
        {
            if( handler.readBuf.position() >= bytes )
            {
                break;
            }
            Thread.sleep( 10 );
        }
        
        Assert.assertEquals( bytes, handler.readBuf.position() );
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
        
        private EchoConnectorHandler()
        {
            readBuf.setAutoExpand( true );
        }

        public void messageReceived( IoSession session, Object message )
        {
            readBuf.put( ( ByteBuffer ) message );
        }
        
        public void messageSent( IoSession session, Object message )
        {
        }

        public void exceptionCaught( IoSession session, Throwable cause )
        {
            cause.printStackTrace();
        }
    }
}
