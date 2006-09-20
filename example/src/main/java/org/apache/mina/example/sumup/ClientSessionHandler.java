/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.example.sumup;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.sumup.codec.SumUpProtocolCodecFactory;
import org.apache.mina.example.sumup.message.AddMessage;
import org.apache.mina.example.sumup.message.ResultMessage;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.util.SessionLog;

/**
 * {@link IoHandler} for SumUp client.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ClientSessionHandler extends IoHandlerAdapter
{
    private final boolean useCustomCodec;
    private final int[] values;
    private boolean finished;

    public ClientSessionHandler( boolean useCustomCodec, int[] values )
    {
        this.useCustomCodec = useCustomCodec;
        this.values = values;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public void sessionCreated( IoSession session ) throws Exception
    {
        ProtocolCodecFactory codec;
        if( useCustomCodec )
        {
            codec = new SumUpProtocolCodecFactory( false );
        }
        else
        {
            codec = new ObjectSerializationCodecFactory();
        }

        session.getFilterChain().addLast(
                "protocolFilter", new ProtocolCodecFilter( codec ) );
        session.getFilterChain().addLast(
                "logger", new LoggingFilter() );
    }

    public void sessionOpened( IoSession session )
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

    public void messageReceived( IoSession session, Object message )
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

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        session.close();
    }
}