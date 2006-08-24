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
package org.apache.mina.examples.httpserver;

import java.net.InetSocketAddress;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.examples.echoserver.ssl.BogusSSLContextFactory;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

/**
 * (<b>Entry point</b>) HTTP server
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Main
{
    /** Choose your favorite port number. */
    private static final int PORT = 8080;
    
    private static final boolean USE_SSL = false;

    public static void main( String[] args ) throws Exception
    {
        IoAcceptor acceptor = new SocketAcceptor();
        IoAcceptorConfig config = new SocketAcceptorConfig();
        DefaultIoFilterChainBuilder chain = config.getFilterChain();

        // Add SSL filter if SSL is enabled.
        if( USE_SSL )
        {
            addSSLSupport( chain );
        }

        // Bind
        acceptor.bind(
                new InetSocketAddress( PORT ),
                new HttpProtocolHandler(),
                config );

        System.out.println( "Listening on port " + PORT );
    }


    private static void addSSLSupport( DefaultIoFilterChainBuilder chain )
        throws Exception
    {
        System.out.println( "SSL is enabled." );
        SSLFilter sslFilter =
            new SSLFilter( BogusSSLContextFactory.getInstance( true ) );
        chain.addLast( "sslFilter", sslFilter );
    }
}
