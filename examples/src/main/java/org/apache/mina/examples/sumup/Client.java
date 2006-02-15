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
package org.apache.mina.examples.sumup;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.ThreadPoolFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;

/**
 * (<strong>Entry Point</strong>) Starts SumUp client.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class Client
{
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;
    private static final int CONNECT_TIMEOUT = 30; // seconds
    // Set this to false to use object serialization instead of custom codec.
    private static final boolean USE_CUSTOM_CODEC = true;

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
        ThreadPoolFilter ioThreadPoolFilter = new ThreadPoolFilter();
        ThreadPoolFilter protocolThreadPoolFilter = new ThreadPoolFilter();
        IoConnector connector = new SocketConnector();
        connector.getDefaultConfig().getFilterChain().addFirst(
                "ioThreadPool", ioThreadPoolFilter );
        connector.getDefaultConfig().getFilterChain().addLast(
                "protocolThreadPool", protocolThreadPoolFilter );

        // Set connect timeout.
        ( ( IoConnectorConfig ) connector.getDefaultConfig() ).setConnectTimeout( CONNECT_TIMEOUT );
        
        IoSession session;
        for( ;; )
        {
            try
            {
                ConnectFuture future = connector.connect(
                        new InetSocketAddress( HOSTNAME, PORT ),
                        new ClientSessionHandler( USE_CUSTOM_CODEC, values ) );
                
                future.join();
                session = future.getSession();
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
        session.getCloseFuture().join();
    }
}
