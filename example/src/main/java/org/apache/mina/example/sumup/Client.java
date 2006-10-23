/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.example.sumup;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.example.sumup.codec.SumUpProtocolCodecFactory;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;

/**
 * (<strong>Entry Point</strong>) Starts SumUp client.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
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

        SocketConnector connector = new SocketConnector();

        // Change the worker timeout to 1 second to make the I/O thread quit soon
        // when there's no connection to manage.
        connector.setWorkerTimeout( 1 );
        
        // Configure the service.
        connector.setConnectTimeout( CONNECT_TIMEOUT );
        if( USE_CUSTOM_CODEC )
        {
            connector.getFilterChain().addLast(
                    "codec",
                    new ProtocolCodecFilter( new SumUpProtocolCodecFactory( false ) ) );
        }
        else
        {
            connector.getFilterChain().addLast(
                    "codec",
                    new ProtocolCodecFilter( new ObjectSerializationCodecFactory() ) );
        }
        connector.getFilterChain().addLast( "logger", new LoggingFilter() );
        
        connector.setHandler( new ClientSessionHandler( values ) );
        
        IoSession session;
        for( ;; )
        {
            try
            {
                ConnectFuture future = connector.connect(
                        new InetSocketAddress( HOSTNAME, PORT ) );
                future.join();
                session = future.getSession();
                break;
            }
            catch( RuntimeIOException e )
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
