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
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.examples.sumup.codec.SumUpProtocolCodecFactory;
import org.apache.mina.examples.sumup.message.AddMessage;
import org.apache.mina.examples.sumup.message.ResultMessage;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.util.SessionLog;

/**
 * {@link IoHandler} for SumUp server.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ServerSessionHandler implements IoHandler
{
    private final boolean useCustomCodec;

    public ServerSessionHandler( boolean useCustomCodec )
    {
        this.useCustomCodec = useCustomCodec;
    }

    public void sessionCreated( IoSession session ) throws Exception
    {
        ProtocolCodecFactory codec;
        if( useCustomCodec )
        {
            codec = new SumUpProtocolCodecFactory( true );
        }
        else
        {
            codec = new ObjectSerializationCodecFactory();
        }

        session.getFilterChain().addFirst(
                "protocolFilter", new ProtocolCodecFilter( codec ) );
        session.getFilterChain().addLast(
                "logger", new LoggingFilter() );
    }

    public void sessionOpened( IoSession session )
    {
        // set idle time to 60 seconds
        session.setIdleTime( IdleStatus.BOTH_IDLE, 60 );

        // initial sum is zero
        session.setAttachment( new Integer( 0 ) );
    }

    public void sessionClosed( IoSession session )
    {
    }

    public void messageReceived( IoSession session, Object message )
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

    public void messageSent( IoSession session, Object message )
    {
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
        SessionLog.info( session, "Disconnecting the idle." );
        // disconnect an idle client
        session.close();
    }

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        // close the connection on exceptional situation
        session.close();
    }
}