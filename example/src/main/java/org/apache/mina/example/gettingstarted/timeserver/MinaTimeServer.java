/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.mina.example.gettingstarted.timeserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * A minimal 'time' server, returning the current date. Opening
 * a telnet server, you will get the current date by typing
 * any string followed by a new line.
 * 
 * In order to quit, just send the 'quit' message.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MinaTimeServer {
    /** We will use a port above 1024 to be able to launch the server with a standard user */
    private static final int PORT = 9123;

    /**
     * The server implementation. It's based on TCP, and uses a logging filter 
     * plus a text line decoder.
     */
    public static void main(String[] args) throws IOException {
        // Create the acceptor
        IoAcceptor acceptor = new NioSocketAcceptor();
        
        // Add two filters : a logger and a codec
        acceptor.getFilterChain().addLast( "logger", new LoggingFilter() );
        acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));
   
        // Attach the business logic to the server
        acceptor.setHandler( new TimeServerHandler() );

        // Configurate the buffer size and the iddle time
        acceptor.getSessionConfig().setReadBufferSize( 2048 );
        acceptor.getSessionConfig().setIdleTime( IdleStatus.BOTH_IDLE, 10 );
        
        // And bind !
        acceptor.bind( new InetSocketAddress(PORT) );
    }
}