/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;

/**
 * {@link ProtocolHandler} for SumUp client.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ClientSessionHandler implements ProtocolHandler
{
    private final int[] values;

    private boolean finished;

    public ClientSessionHandler( int[] values )
    {
        this.values = values;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public void sessionOpened( ProtocolSession session )
    {
        System.out.println( "OPENED" );
        // send summation requests
        for( int i = 0; i < values.length; i++ )
        {
            AddMessage m = new AddMessage();
            m.setSequence( i );
            m.setValue( values[ i ] );
            session.write( m );
        }
    }

    public void sessionClosed( ProtocolSession session )
    {
        System.out.println( "CLOSED" );
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
        System.out.println( "RCVD: " + message );
        // server only sends ResultMessage. otherwise, we will have to identify
        // its type using instanceof operator.
        ResultMessage rm = ( ResultMessage ) message;
        if( rm.isOk() )
        {
            // server returned OK code.
            // if received the result message which has the last sequence
            // number,
            // it is time to disconnect.
            if( rm.getSequence() == values.length - 1 )
            {
                // print the sum and disconnect.
                System.out.println( "The sum: " + rm.getValue() );
                session.close();
                finished = true;
            }
        }
        else
        {
            // seever returned error code because of overflow, etc.
            System.err.println( "Server error, disconnecting..." );
            session.close();
            finished = true;
        }
    }

    public void messageSent( ProtocolSession session, Object message )
    {
        System.out.println( "SENT: " + message );
    }

    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        // there is no idle time for client
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        cause.printStackTrace();
        session.close();
    }
}