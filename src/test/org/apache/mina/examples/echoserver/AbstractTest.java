/*
 * @(#) $Id$
 */
package org.apache.mina.examples.echoserver;

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
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
    protected int port;

    protected IoAcceptor acceptor;

    protected IoAcceptor datagramAcceptor;

    protected IoThreadPoolFilter threadPoolFilter;

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

        acceptor.addFilter( Integer.MAX_VALUE, threadPoolFilter );
        datagramAcceptor.addFilter( Integer.MAX_VALUE, threadPoolFilter );
    }

    protected void tearDown() throws Exception
    {
        acceptor.unbind( new InetSocketAddress( port ) );
        datagramAcceptor.unbind( new InetSocketAddress( port ) );
        threadPoolFilter.stop();
    }
}
