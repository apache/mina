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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.examples.sumup.message.AddMessage;
import org.apache.mina.examples.sumup.message.ResultMessage;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.filter.ProtocolLoggingFilter;
import org.apache.mina.util.SessionLog;

/**
 * {@link ProtocolHandler} for SumUp server.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class ServerSessionHandler implements ProtocolHandler
{

    public ServerSessionHandler()
    {
    }

    public void sessionCreated( ProtocolSession session ) throws Exception
    {
        session.getFilterChain().addLast(
                "logger", new ProtocolLoggingFilter() );
    }

    public void sessionOpened( ProtocolSession session )
    {
        // set idle time to 60 seconds
        session.getConfig().setIdleTime( IdleStatus.BOTH_IDLE, 60 );

        // initial sum is zero
        session.setAttachment( new Integer( 0 ) );
    }

    public void sessionClosed( ProtocolSession session )
    {
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
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
    }

    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
        SessionLog.warn( session, "Disconnecting the idle." );
        // disconnect an idle client
        session.close();
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        // close the connection on exceptional situation
        session.close();
    }
}