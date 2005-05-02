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

import org.apache.mina.common.TransportType;
import org.apache.mina.examples.echoserver.ssl.BogusSSLContextFactory;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.filter.SSLFilter;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;

/**
 * (<b>Entry point</b>) Echo server
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Main
{
    /** Choose your favorite port number. */
    private static final int PORT = 8080;
    
    /** Set this to true if you want to make the server SSL */
    private static final boolean USE_SSL = false;

    public static void main( String[] args ) throws Exception
    {
        ServiceRegistry registry = new SimpleServiceRegistry();
        
        // Add SSL filter if SSL is enabled.
        if( USE_SSL )
        {
            addSSLSupport( registry );
        }

        // Bind
        Service service = new Service( "echo", TransportType.SOCKET, PORT );
        registry.bind( service, new EchoProtocolHandler() );

        System.out.println( "Listening on port " + PORT );
    }

    private static void addSSLSupport( ServiceRegistry registry )
        throws Exception
    {
        System.out.println( "SSL is enabled." );
        SSLFilter sslFilter =
            new SSLFilter( BogusSSLContextFactory.getInstance( true ) );
        IoAcceptor acceptor = registry.getIoAcceptor( TransportType.SOCKET );
        acceptor.getFilterChain().addLast( "sslFilter", sslFilter );
    }
}
