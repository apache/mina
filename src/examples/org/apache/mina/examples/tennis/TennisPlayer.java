/*
 * @(#) $Id$
 */
package org.apache.mina.examples.tennis;

import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerAdapter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;

/**
 * A {@link ProtocolHandler} implementation which plays a tennis game.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class TennisPlayer implements ProtocolProvider
{
    private static final ProtocolHandler HANDLER = new TennisPlayerHandler();
    
    public ProtocolCodecFactory getCodecFactory()
    {
        throw new UnsupportedOperationException();
    }

    public ProtocolHandler getHandler()
    {
        return HANDLER;
    }

    private static class TennisPlayerHandler extends ProtocolHandlerAdapter
    {
        private static int nextId = 0;

        /** Player ID **/
        private final int id = nextId++;

        public void sessionOpened( ProtocolSession session )
        {
            System.out.println( "Player-" + id + ": READY" );
        }

        public void sessionClosed( ProtocolSession session )
        {
            System.out.println( "Player-" + id + ": QUIT" );
        }

        public void messageReceived( ProtocolSession session, Object message )
        {
            System.out.println( "Player-" + id + ": RCVD " + message );

            TennisBall ball = ( TennisBall ) message;

            // Stroke: TTL decreases and PING/PONG state changes.
            ball = ball.stroke();

            if( ball.getTTL() > 0 )
            {
                // If the ball is still alive, pass it back to peer.
                session.write( ball );
            }
            else
            {
                // If the ball is dead, this player loses.
                System.out.println( "Player-" + id + ": LOSE" );
                session.close();
            }
        }

        public void messageSent( ProtocolSession session, Object message )
        {
            System.out.println( "Player-" + id + ": SENT " + message );
        }
    }
}