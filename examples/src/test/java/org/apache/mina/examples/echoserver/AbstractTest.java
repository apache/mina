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

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.util.SessionLog;

/**
 * Tests echo server example.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractTest extends TestCase
{
    protected int port;

    protected ServiceRegistry registry;
    
    protected AbstractTest()
    {
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
        registry = new SimpleServiceRegistry();

        // Find an availble test port and bind to it.
        boolean socketBound = false;
        boolean datagramBound = false;

        final SocketAcceptor acceptor = ( SocketAcceptor ) registry.getAcceptor( TransportType.SOCKET );
        acceptor.setReuseAddress( true );

        // Let's start from port #1 to detect possible resource leak
        // because test will fail in port 1-1023 if user run this test
        // as a normal user.
        for( port = 1; port <= 65535; port ++ )
        {
            socketBound = false;
            datagramBound = false;
            
            Service socketService = new Service( "echo", TransportType.SOCKET, port );
            Service datagramService = new Service( "echo", TransportType.DATAGRAM, port );
            
            try
            {
                registry.bind( socketService, new EchoProtocolHandler()
                {
                    // This is for TLS reentrance test
                    public void messageReceived( IoSession session, Object message ) throws Exception
                    {
                        if( !( message instanceof ByteBuffer ) )
                        {
                            return;
                        }
                        
                        ByteBuffer buf = ( ByteBuffer ) message;
                        if( buf.remaining() == 1 && buf.get() == ( byte ) '.' )
                        {
                            SessionLog.info( session, "TLS Reentrance" );
                            ( ( SSLFilter ) acceptor.getFilterChain().get( "SSL" ) ).startSSL( session );

                            // Send a response
                            buf = ByteBuffer.allocate( 1 );
                            buf.put( ( byte ) '.' );
                            buf.flip();
                            session.setAttribute( SSLFilter.DISABLE_ENCRYPTION_ONCE );
                            session.write( buf );
                        }
                        else
                        {
                            super.messageReceived( session, message );
                        }
                    }
                } );
                socketBound = true;

                registry.bind( datagramService, new EchoProtocolHandler() );
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
                    registry.unbindAll();
                }
            }
        }

        // If there is no port available, test fails.
        if( !socketBound || !datagramBound )
        {
            throw new IOException( "Cannot bind any test port." );
        }

        registry.getAcceptor( TransportType.SOCKET ).getFilterChain().addLast( "logger", new LoggingFilter() );
        registry.getAcceptor( TransportType.DATAGRAM ).getFilterChain().addLast( "logger", new LoggingFilter() );

        System.out.println( "Using port " + port + " for testing." );
    }

    protected void tearDown() throws Exception
    {
        registry.unbindAll();
        registry.getAcceptor( TransportType.SOCKET ).getFilterChain().remove( "logger" );
        registry.getAcceptor( TransportType.DATAGRAM ).getFilterChain().remove( "logger" );
    }
}
