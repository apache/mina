/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import net.gleamynode.netty2.SessionListener;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;

/**
 * {@link SessionListener}for SumUp server.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ServerSessionHandler implements ProtocolHandler
{

    public ServerSessionHandler()
    {
    }

    public void sessionOpened( ProtocolSession session )
    {
        System.out.println( "OPENED" );
        // set idle time to 60 seconds
        session.getConfig().setIdleTime( IdleStatus.BOTH_IDLE, 60 );

        // initial sum is zero
        session.setAttachment( new Integer( 0 ) );
    }

    public void sessionClosed( ProtocolSession session )
    {
        System.out.println( "CLOSED" );
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
        System.out.println( "RCVD: " + message );
        // client only sends AddMessage. otherwise, we will have to identify
        // its type using instanceof operator.
        AddMessage am = ( AddMessage ) message;

        // add the value to the current sum.
        int sum = ( ( Integer ) session.getAttachment() ).intValue();
        int value = am.getValue();
        long expectedSum = ( long ) sum + value;
        if( expectedSum > Integer.MAX_VALUE || expectedSum < Integer.MIN_VALUE )
        {
            // if the sum overflows or underflows, return error message
            ResultMessage rm = new ResultMessage();
            rm.setSequence( am.getSequence() ); // copy sequence
            rm.setOk( false );
            session.write( rm );
        }
        else
        {
            // sum up
            sum = ( int ) expectedSum;
            session.setAttachment( new Integer( sum ) );

            // return the result message
            ResultMessage rm = new ResultMessage();
            rm.setSequence( am.getSequence() ); // copy sequence
            rm.setOk( true );
            rm.setValue( sum );
            session.write( rm );
        }
    }

    public void messageSent( ProtocolSession session, Object message )
    {
        System.out.println( "SENT: " + message );
    }

    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        System.out.println( "Disconnecting the idle." );
        // disconnect an idle client
        session.close();
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        cause.printStackTrace();
        // close the connection on exceptional situation
        session.close();
    }
}