/*
 * @(#) $Id$
 */
package org.apache.mina.examples.tennis;

import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.vmpipe.VmPipeAcceptor;
import org.apache.mina.protocol.vmpipe.VmPipeAddress;
import org.apache.mina.protocol.vmpipe.VmPipeConnector;

/**
 * (<b>Entry point</b>) An 'in-VM pipe' example which simulates a tennis game
 * between client and server.
 * <ol>
 *   <li>Client connects to server</li>
 *   <li>At first, client sends {@link TennisBall} with TTL value '10'.</li>
 *   <li>Received side (either server or client) decreases the TTL value of the
 *     received ball, and returns it to remote peer.</li>
 *   <li>Who gets the ball with 0 TTL loses.</li>
 * </ol> 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Main
{

    public static void main( String[] args ) throws Exception
    {
        VmPipeAddress address = new VmPipeAddress( 8080 );

        // Prepare thread pool
        ProtocolThreadPoolFilter threadPool = new ProtocolThreadPoolFilter();
        threadPool.start();

        // Set up server
        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.bind( address, new TennisPlayer() );
        acceptor.getFilterChain().addFirst( "threadPool", threadPool );

        // Connect to the server.
        VmPipeConnector connector = new VmPipeConnector();
        connector.getFilterChain().addFirst( "threadPool", threadPool );
        ProtocolSession session = connector.connect( address,
                                                     new TennisPlayer() );

        // Send the first ping message
        session.write( new TennisBall( 10 ) );

        // Wait until the match ends.
        while( session.isConnected() )
        {
            Thread.sleep( 100 );
        }

        // Stop the thread pool
        threadPool.stop();
    }
}
