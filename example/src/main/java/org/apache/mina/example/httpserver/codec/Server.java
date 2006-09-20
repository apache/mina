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
package org.apache.mina.example.httpserver.codec;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

/**
 * (<b>Entry point</b>) HTTP server
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Server
{
    /** Default HTTP port */
    private static int DEFAULT_PORT = 80;

    /** Tile server revision number */
    public static final String VERSION_STRING = "$Revision$ $Date$";

    public static void main( String[] args )
    {
        int port = DEFAULT_PORT;

        for( int i = 0; i < args.length; i++ )
        {
            if( args[ i ].equals( "-port" ) )
            {
                port = Integer.parseInt( args[ i + 1 ] );
            }
        }

        try
        {
            // Create ServiceRegistry.
            IoAcceptor acceptor = new SocketAcceptor();
            ( ( SocketAcceptorConfig ) acceptor.getDefaultConfig() )
                    .setReuseAddress( true );
            acceptor.bind( new InetSocketAddress( port ), new ServerHandler() );

            System.out.println( "Server now listening on port " + port );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}
