/*
 * @(#) $Id$
 */
package org.apache.mina.examples.tennis;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;

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
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Main
{

    public static void main( String[] args ) throws Exception
    {
        IoAcceptor acceptor = new VmPipeAcceptor();
        VmPipeAddress address = new VmPipeAddress( 8080 );

        // Set up server
        acceptor.bind( address, new TennisPlayer() );

        // Connect to the server.
        VmPipeConnector connector = new VmPipeConnector();
        ConnectFuture future = connector.connect( address,
                                                  new TennisPlayer() );
        future.join();
        IoSession session = future.getSession();

        // Send the first ping message
        session.write( new TennisBall( 10 ) );

        // Wait until the match ends.
        session.getCloseFuture().join();
        
        acceptor.unbind( address );
    }
}
