/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import java.net.InetSocketAddress;

import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolAcceptor;

/**
 * (<strong>Entry Point</strong>) Starts SumUp server.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Server
{
    private static final int SERVER_PORT = 8080;

    public static void main( String[] args ) throws Throwable
    {
        // Create I/O and Protocol thread pool filter.
        // I/O thread pool performs encoding and decoding of messages.
        // Protocol thread pool performs actual protocol flow.
        IoThreadPoolFilter ioThreadPoolFilter = new IoThreadPoolFilter();
        ProtocolThreadPoolFilter protocolThreadPoolFilter = new ProtocolThreadPoolFilter();

        // and start both.
        ioThreadPoolFilter.start();
        protocolThreadPoolFilter.start();

        IoProtocolAcceptor acceptor = new IoProtocolAcceptor(
                new SocketAcceptor() );

        acceptor.getIoAcceptor().getFilterChain().addFirst(
                "threadPool", ioThreadPoolFilter );
        acceptor.getFilterChain().addFirst(
                "threadPool", protocolThreadPoolFilter );

        acceptor.bind( new InetSocketAddress( SERVER_PORT ),
                new ServerProtocolProvider() );
        System.out.println( "Listening on port " + SERVER_PORT );
    }
}
