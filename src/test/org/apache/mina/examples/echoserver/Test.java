/*
 * @(#) $Id$
 */
package org.apache.mina.examples.echoserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import junit.framework.TestCase;

import org.apache.commons.net.EchoTCPClient;
import org.apache.commons.net.EchoUDPClient;
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
public class Test extends TestCase
{
    private int port;

    private IoAcceptor acceptor;

    private IoAcceptor datagramAcceptor;

    private IoThreadPoolFilter threadPoolFilter;

    public static void assertEquals( byte[] expected, byte[] actual )
    {
        assertEquals( toString( expected ), toString( actual ) );
    }

    private static String toString( byte[] buf )
    {
        StringBuffer str = new StringBuffer( buf.length * 4 );
        for( int i = 0; i < buf.length; i ++ )
        {
            str.append( buf[ i ] );
            str.append( ' ' );
        }
        return str.toString();
    }

    protected void setUp() throws Exception
    {
        acceptor = new SocketAcceptor();
        datagramAcceptor = new DatagramAcceptor();

        // Find an availble test port and bind to it.
        boolean socketBound = false;
        boolean datagramBound = false;
        for( port = 1024; port <= 65535; port ++ )
        {
            socketBound = false;
            datagramBound = false;
            try
            {
                acceptor.bind( new InetSocketAddress( port ),
                        new EchoProtocolHandler() );
                socketBound = true;

                datagramAcceptor.bind( new InetSocketAddress( port ),
                        new EchoProtocolHandler() );
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

        acceptor
                .addFilter( IoThreadPoolFilter.MAX_PRIORITY, threadPoolFilter );
        datagramAcceptor.addFilter( IoThreadPoolFilter.MAX_PRIORITY,
                threadPoolFilter );
    }

    protected void tearDown() throws Exception
    {
        acceptor.unbind( new InetSocketAddress( port ) );
        datagramAcceptor.unbind( new InetSocketAddress( port ) );
        threadPoolFilter.stop();
    }

    public void testTCP() throws Exception
    {
        EchoTCPClient client = new EchoTCPClient();
        client.connect( InetAddress.getLocalHost(), port );
        client.setSoTimeout( 3000 );

        byte[] writeBuf = new byte[ 16 ];

        for( int i = 0; i < 10; i ++ )
        {
            fillWriteBuffer( writeBuf, i );
            client.getOutputStream().write( writeBuf );
        }

        byte[] readBuf = new byte[ writeBuf.length ];

        for( int i = 0; i < 10; i ++ )
        {
            fillWriteBuffer( writeBuf, i );

            int readBytes = 0;
            while( readBytes < readBuf.length )
            {
                int nBytes = client.getInputStream().read( readBuf,
                        readBytes, readBuf.length - readBytes );

                if( nBytes < 0 )
                    fail( "Unexpected disconnection." );

                readBytes += nBytes;
            }

            assertEquals( writeBuf, readBuf );
        }

        client.setSoTimeout( 500 );

        try
        {
            client.getInputStream().read();
            fail( "Unexpected incoming data." );
        }
        catch( SocketTimeoutException e )
        {
        }

        client.disconnect();
    }

    public void testUDP() throws Exception
    {
        EchoUDPClient client = new EchoUDPClient();
        client.open();
        client.setSoTimeout( 3000 );

        byte[] writeBuf = new byte[ 16 ];
        byte[] readBuf = new byte[ writeBuf.length ];

        client.setSoTimeout( 500 );

        for( int i = 0; i < 10; i ++ )
        {
            fillWriteBuffer( writeBuf, i );
            client.send( writeBuf, writeBuf.length, InetAddress
                    .getLocalHost(), port );

            assertEquals( readBuf.length, client.receive( readBuf,
                    readBuf.length ) );
            assertEquals( writeBuf, readBuf );
        }

        try
        {
            client.receive( readBuf );
            fail( "Unexpected incoming data." );
        }
        catch( SocketTimeoutException e )
        {
        }

        client.close();
    }

    private void fillWriteBuffer( byte[] writeBuf, int i )
    {
        for( int j = writeBuf.length - 1; j >= 0; j -- )
        {
            writeBuf[ j ] = ( byte ) ( j + i );
        }
    }

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( Test.class );
    }
}
