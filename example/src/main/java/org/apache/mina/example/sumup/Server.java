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

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.example.sumup.codec.SumUpProtocolCodecFactory;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

/**
 * (<strong>Entry Point</strong>) Starts SumUp server.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Server
{
    private static final int SERVER_PORT = 8080;
    // Set this to false to use object serialization instead of custom codec.
    private static final boolean USE_CUSTOM_CODEC = true;

    public static void main( String[] args ) throws Throwable
    {
        IoAcceptor acceptor = new SocketAcceptor();
        
        // Prepare the service configuration.
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.setReuseAddress( true );
        if( USE_CUSTOM_CODEC )
        {
            cfg.getFilterChain().addLast(
                    "codec",
                    new ProtocolCodecFilter( new SumUpProtocolCodecFactory( true ) ) );
        }
        else
        {
            cfg.getFilterChain().addLast(
                    "codec",
                    new ProtocolCodecFilter( new ObjectSerializationCodecFactory() ) );
        }
        cfg.getFilterChain().addLast( "logger", new LoggingFilter() );

        acceptor.bind(
                new InetSocketAddress( SERVER_PORT ),
                new ServerSessionHandler( ), cfg );

        System.out.println( "Listening on port " + SERVER_PORT );
    }
}
