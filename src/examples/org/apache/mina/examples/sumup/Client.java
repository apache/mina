/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketConnector;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolConnector;

/**
 * (<strong>Entry Point</strong>) Starts SumUp client.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Client
{
    private static final String HOSTNAME = "localhost";

    private static final int PORT = 8080;

    private static final int CONNECT_TIMEOUT = 30; // seconds

    public static void main( String[] args ) throws Throwable
    {
        if( args.length == 0 )
        {
            System.out.println( "Please specify the list of any integers" );
            return;
        }

        // prepare values to sum up
        int[] values = new int[ args.length ];
        for( int i = 0; i < args.length; i++ )
        {
            values[ i ] = Integer.parseInt( args[ i ] );
        }

        // Create I/O and Protocol thread pool filter.
        // I/O thread pool performs encoding and decoding of messages.
        // Protocol thread pool performs actual protocol flow.
        IoThreadPoolFilter ioThreadPoolFilter = new IoThreadPoolFilter();
        ProtocolThreadPoolFilter protocolThreadPoolFilter = new ProtocolThreadPoolFilter();

        // and start both.
        ioThreadPoolFilter.start();
        protocolThreadPoolFilter.start();

        IoProtocolConnector connector = new IoProtocolConnector(
                                                                 new SocketConnector() );
        connector.getIoConnector().addFilter( Integer.MAX_VALUE,
                                              ioThreadPoolFilter );
        connector.addFilter( Integer.MAX_VALUE, protocolThreadPoolFilter );

        ProtocolProvider protocolProvider = new ClientProtocolProvider( values );
        ProtocolSession session;
        for( ;; )
        {
            try
            {
                session = connector
                        .connect( new InetSocketAddress( HOSTNAME, PORT ),
                                  CONNECT_TIMEOUT, protocolProvider );
                break;
            }
            catch( IOException e )
            {
                System.err.println( "Failed to connect." );
                e.printStackTrace();
                Thread.sleep( 5000 );
            }
        }

        // wait until the summation is done
        while( session.isConnected() )
        {
            Thread.sleep( 100 );
        }

        ioThreadPoolFilter.stop();
        protocolThreadPoolFilter.stop();
    }
}