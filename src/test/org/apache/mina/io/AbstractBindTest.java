/*
 * @(#) $Id$
 */
package org.apache.mina.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.examples.echoserver.EchoProtocolHandler;

public class AbstractBindTest extends TestCase
{
    protected final IoAcceptor acceptor;
    protected int port;

    public AbstractBindTest( IoAcceptor acceptor )
    {
        this.acceptor = acceptor;
    }
    
    public void setUp() throws IOException
    {
        // Find an availble test port and bind to it.
        boolean socketBound = false;

        // Let's start from port #1 to detect possible resource leak
        // because test will fail in port 1-1023 if user run this test
        // as a normal user.
        for( port = 1; port <= 65535; port ++ )
        {
            socketBound = false;
            try
            {
                acceptor.bind( new InetSocketAddress( port ),
                        new EchoProtocolHandler() );
                socketBound = true;
                break;
            }
            catch( IOException e )
            {
            }
        }

        // If there is no port available, test fails.
        if( !socketBound )
        {
            throw new IOException( "Cannot bind any test port." );
        }

        //System.out.println( "Using port " + port + " for testing." );
    }
    
    public void tearDown()
    {
        try
        {
            acceptor.unbind( new InetSocketAddress( port ) );
        }
        catch( Exception e )
        {
            // ignore
        }
    }
    
    public void testDuplicateBind()
    {
        try
        {
            acceptor.bind( new InetSocketAddress( port ), new EchoProtocolHandler() );
            Assert.fail( "IOException is not thrown" );
        }
        catch( IOException e )
        {
        }
    }

    public void testDuplicateUnbind()
    {
        // this should succeed
        acceptor.unbind( new InetSocketAddress( port ) );
        
        try
        {
            // this should fail
            acceptor.unbind( new InetSocketAddress( port ) );
            Assert.fail( "Exception is not thrown" );
        }
        catch( Exception e )
        {
        }
    }
    
    public void testManyTimes() throws IOException
    {
        InetSocketAddress addr = new InetSocketAddress( port );
        EchoProtocolHandler handler = new EchoProtocolHandler();
        // we test only 8192 times to avoid too many open files exception.
        for( int i = 0; i < 8192; i++ ) 
        {
            acceptor.unbind( addr );
            acceptor.bind( addr, handler );
        }
    }
    
    public void _testRegressively() throws IOException
    {
        tearDown();

        InetSocketAddress addr = new InetSocketAddress( port );
        EchoProtocolHandler handler = new EchoProtocolHandler();
        for( int i = 0; i < 1048576; i++ )
        {
            acceptor.bind( addr, handler );
            testDuplicateBind();
            testDuplicateUnbind();
            if( i % 100 == 0 )
            {
                System.out.println( i + " (" + new Date() + ")" );
            }
        }
        setUp();
    }

}
