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
package org.apache.mina.examples.reverser;

import java.net.InetSocketAddress;

import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolAcceptor;

/**
 * (<b>Entry point</b>) Reverser server which reverses all text lines from
 * clients.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class Main
{
    private static final int PORT = 8080;

    public static void main( String[] args ) throws Exception
    {
        // Create I/O and Protocol thread pool filter.
        // I/O thread pool performs encoding and decoding of messages.
        // Protocol thread pool performs actual protocol flow.
        IoThreadPoolFilter ioThreadPoolFilter = new IoThreadPoolFilter();
        ProtocolThreadPoolFilter protocolThreadPoolFilter = new ProtocolThreadPoolFilter();

        // and start both.
        ioThreadPoolFilter.start();
        protocolThreadPoolFilter.start();

        // Create a TCP/IP acceptor.
        IoProtocolAcceptor acceptor = new IoProtocolAcceptor(
                new SocketAcceptor() );

        // Add both thread pool filters.
        acceptor.getIoAcceptor().addFilter( Integer.MAX_VALUE, ioThreadPoolFilter );
        acceptor.addFilter( Integer.MAX_VALUE, protocolThreadPoolFilter );

        // Bind
        acceptor.bind( new InetSocketAddress( PORT ),
                new ReverseProtocolProvider() );

        System.out.println( "Listening on port " + PORT );
    }
}
