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

import java.net.InetSocketAddress;

import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.datagram.DatagramAcceptor;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;

/**
 * (<b>Entry point</b>) Echo server
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class Main
{
    private static final int PORT = 8080;

    public static void main( String[] args ) throws Exception
    {
        // Create a I/O thread pool filter 
        IoThreadPoolFilter threadPoolFilter = new IoThreadPoolFilter();
        threadPoolFilter.start(); // and start it

        // Create a TCP/IP acceptor
        IoAcceptor acceptor = new SocketAcceptor();

        // Add thread pool filter
        // MINA runs in a single thread if you don't add this filter.
        acceptor.addFilter( Integer.MAX_VALUE, threadPoolFilter );

        // Bind
        acceptor.bind( new InetSocketAddress( PORT ),
                new EchoProtocolHandler() );

        // Create a UDP/IP acceptor
        IoAcceptor datagramAcceptor = new DatagramAcceptor();

        // Add thread pool filter
        datagramAcceptor.addFilter( Integer.MAX_VALUE, threadPoolFilter );

        // Bind
        datagramAcceptor.bind( new InetSocketAddress( PORT ),
                new EchoProtocolHandler() );

        System.out.println( "Listening on port " + PORT );
    }
}
