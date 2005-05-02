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
package org.apache.mina.examples.httpserver;

import org.apache.mina.common.TransportType;
import org.apache.mina.examples.echoserver.ssl.BogusSSLContextFactory;
import org.apache.mina.io.IoFilterChain;
import org.apache.mina.io.filter.SSLFilter;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;

/**
 * (<b>Entry point</b>) HTTP server
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Main
{
    /** Choose your favorite port number. */
    private static final int PORT = 8081;
    
    private static final boolean USE_SSL = true;

    public static void main( String[] args ) throws Exception
    {
        ServiceRegistry registry = new SimpleServiceRegistry();
        
        // Add SSL filter if SSL is enabled.
        if( USE_SSL )
        {
            addSSLSupport( registry );
        }

        // Bind
        Service service = new Service( "http", TransportType.SOCKET, PORT );
        registry.bind( service, new HttpProtocolHandler() );

        System.out.println( "Listening on port " + PORT );
    }


    private static void addSSLSupport( ServiceRegistry registry )
        throws Exception
    {
        System.out.println( "SSL is enabled." );
        SSLFilter sslFilter =
            new SSLFilter( BogusSSLContextFactory.getInstance( true ) );
        IoFilterChain filters = registry.getIoFilterChain( TransportType.SOCKET );
        filters.addLast( "sslFilter", sslFilter );
    }
}
