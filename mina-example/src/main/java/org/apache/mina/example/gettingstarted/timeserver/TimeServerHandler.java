/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.mina.example.gettingstarted.timeserver;

import java.util.Date;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * The Time Server handler : it return the current date when a message is received,
 * or close the session if the "quit" message is received.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class TimeServerHandler extends IoHandlerAdapter
{
    /**
     * Trap exceptions.
     */
    @Override
    public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
    {
        cause.printStackTrace();
    }

    /**
     * If the message is 'quit', we exit by closing the session. Otherwise,
     * we return the current date.
     */
    @Override
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        String str = message.toString();
        
        if( str.trim().equalsIgnoreCase("quit") ) {
            // "Quit" ? let's get out ...
            session.close(true);
            return;
        }

        // Send the current date back to the client
        Date date = new Date();
        session.write( date.toString() );
        System.out.println("Message written...");
    }

    /**
     * On idle, we just write a message on the console
     */
    @Override
    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
    {
        System.out.println( "IDLE " + session.getIdleCount( status ));
    }
}
