/*
 * @(#) $Id$
 */
package org.apache.mina.examples.registry;

import java.net.InetSocketAddress;

import org.apache.mina.common.TransportType;
import org.apache.mina.examples.echoserver.EchoProtocolHandler;
import org.apache.mina.examples.reverser.ReverseProtocolProvider;
import org.apache.mina.protocol.vmpipe.VmPipeAddress;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;

/**
 * This example demonstrates the usage of {@link ServiceRegistry} in 
 * <code>org.apache.mina.registry</code> package.
 * 
 * This application starts up echo and reverse protocol server. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
 */
public class Main
{
    public static void main( String[] args ) throws Exception
    {
        ServiceRegistry registry = new SimpleServiceRegistry();

        // Register echo service
        registry.bind( new Service( "echo", TransportType.SOCKET,
                                    new InetSocketAddress( 8080 ) ),
                       new EchoProtocolHandler() );

        registry.bind( new Service( "echo", TransportType.DATAGRAM,
                                    new InetSocketAddress( 8080 ) ),
                       new EchoProtocolHandler() );

        // Register reverse service
        registry.bind( new Service( "reverse", TransportType.SOCKET,
                                    new InetSocketAddress( 8081 ) ),
                       new ReverseProtocolProvider() );

        registry.bind( new Service( "reverse", TransportType.DATAGRAM,
                                    new InetSocketAddress( 8081 ) ),
                       new ReverseProtocolProvider() );

        registry.bind( new Service( "reverse", TransportType.VM_PIPE,
                                    new VmPipeAddress( 8081 ) ),
                       new ReverseProtocolProvider() );
        
        System.out.println( registry.getAllServices() );
    }
}
