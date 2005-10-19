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

import org.apache.mina.examples.sumup.message.AddMessage;
import org.apache.mina.examples.sumup.message.ResultMessage;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerAdapter;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.filter.ProtocolLoggingFilter;
import org.apache.mina.util.SessionLog;

/**
 * {@link ProtocolHandler} for SumUp client.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class ClientSessionHandler extends ProtocolHandlerAdapter
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

    public void sessionCreated( ProtocolSession session )
    {
        session.getFilterChain().addLast(
                "logger", new ProtocolLoggingFilter() );
    }

    public void sessionOpened( ProtocolSession session )
    {
        // send summation requests
        for( int i = 0; i < values.length; i++ )
        {
            AddMessage m = new AddMessage();
            m.setSequence( i );
            m.setValue( values[ i ] );
            session.write( m );
        }
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
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
                SessionLog.info( session, "The sum: " + rm.getValue() );
                session.close();
                finished = true;
            }
        }
        else
        {
            // seever returned error code because of overflow, etc.
            SessionLog.warn( session, "Server error, disconnecting..." );
            session.close();
            finished = true;
        }
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
        session.close();
    }
}