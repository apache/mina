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

import java.net.InetSocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptor;

/**
 * (<strong>Entry Point</strong>) Starts SumUp server.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class Server
{
    private static final int SERVER_PORT = 8080;
    // Set this to false to use object serialization instead of custom codec.
    private static final boolean USE_CUSTOM_CODEC = true;

    public static void main( String[] args ) throws Throwable
    {
        // Create ServiceRegistry.
        IoAcceptor acceptor = new SocketAcceptor();

        acceptor.bind(
                new InetSocketAddress( SERVER_PORT ),
                new ServerSessionHandler( USE_CUSTOM_CODEC ) );

        System.out.println( "Listening on port " + SERVER_PORT );
    }
}
