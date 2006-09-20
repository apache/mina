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
package org.apache.mina.example.proxy;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;

/**
 * (<b>Entry point</b>) Demonstrates how to write a very simple tunneling proxy 
 * using MINA. The proxy only logs all data passing through it. This is only 
 * suitable for text based protocols since received data will be converted into 
 * strings before being logged.
 * <p>
 * Start a proxy like this:<br/>
 * <code>org.apache.mina.example.proxy.Main 12345 www.google.com 80</code><br/>
 * and open <a href="http://localhost:12345">http://localhost:12345</a> in a
 * browser window.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Main
{
    
    public static void main( String[] args ) throws Exception
    {
        if( args.length != 3 )
        {
            System.out.println( Main.class.getName() + " <proxy-port> <server-hostname> <server-port>" );
            return;
        }

        // Create TCP/IP acceptor.
        IoAcceptor acceptor = new SocketAcceptor();
        ( ( SocketAcceptorConfig ) acceptor.getDefaultConfig() ).setReuseAddress( true );
        
        // Create TCP/IP connector.
        IoConnector connector = new SocketConnector();

        // Set connect timeout.
        ( ( IoConnectorConfig ) connector.getDefaultConfig()).setConnectTimeout( 30 );
        
        ClientToProxyIoHandler handler = new ClientToProxyIoHandler( 
                new ServerToProxyIoHandler(), connector,
                new InetSocketAddress( args[ 1 ],
                        Integer.parseInt( args[ 2 ] ) ) );
        
        // Start proxy.
        acceptor.bind( new InetSocketAddress( Integer.parseInt( args[ 0 ] ) ), handler );

        System.out.println( "Listening on port " + Integer.parseInt( args[ 0 ] ) );
    }
    
}
