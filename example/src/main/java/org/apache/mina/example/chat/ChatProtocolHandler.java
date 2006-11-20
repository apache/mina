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
package org.apache.mina.example.chat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;

/**
 * {@link IoHandler} implementation of a simple chat server protocol.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ChatProtocolHandler extends IoHandlerAdapter
{
    private Set<IoSession> sessions = Collections.synchronizedSet( new HashSet<IoSession>() );
    private Set<String> users = Collections.synchronizedSet( new HashSet<String>() );

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        SessionLog.error( session, "", cause );
        // Close connection when unexpected exception is caught.
        session.close();
    }

    public void messageReceived( IoSession session, Object message )
    {
        String theMessage = ( String ) message;
        String[] result = theMessage.split( " ", 2 );
        String theCommand = result[ 0 ];

        try
        {

            ChatCommand command = ChatCommand.valueOf( theCommand );
            String user = ( String ) session.getAttribute( "user" );

            switch( command.toInt() )
            {

            case ChatCommand.QUIT:
                session.write( "QUIT OK" );
                session.close();
                break;
            case ChatCommand.LOGIN:

                if( user != null )
                {
                    session.write( "LOGIN ERROR user " + user
                            + " already logged in." );
                    return;
                }

                if( result.length == 2 )
                {
                    user = result[ 1 ];
                }
                else
                {
                    session.write( "LOGIN ERROR invalid login command." );
                    return;
                }

                // check if the username is already used
                if( users.contains( user ) )
                {
                    session.write( "LOGIN ERROR the name " + user
                            + " is already used." );
                    return;
                }

                sessions.add( session );
                session.setAttribute( "user", user );

                // Allow all users
                users.add( user );
                session.write( "LOGIN OK" );
                broadcast( "The user " + user + " has joined the chat session." );
                break;

            case ChatCommand.BROADCAST:

                if( result.length == 2 )
                {
                    broadcast( user + ": " + result[ 1 ] );
                }
                break;
            default:
                SessionLog.info( session, "Unhandled command: " + command );
                break;
            }

        }
        catch( IllegalArgumentException e )
        {
            SessionLog.debug( session, e.getMessage() );
        }
    }

    public void broadcast( String message )
    {
        synchronized( sessions )
        {
            Iterator iter = sessions.iterator();
            while( iter.hasNext() )
            {
                IoSession s = ( IoSession ) iter.next();
                if( s.isConnected() )
                {
                    s.write( "BROADCAST OK " + message );
                }
            }
        }
    }

    public void sessionClosed( IoSession session ) throws Exception
    {
        String user = ( String ) session.getAttribute( "user" );
        users.remove( user );
        sessions.remove( session );
        broadcast( "The user " + user + " has left the chat session." );
    }

    public boolean isChatUser( String name )
    {
        return users.contains( name );
    }

    public int getNumberOfUsers()
    {
        return users.size();
    }

    public void kick( String name )
    {
        synchronized( sessions )
        {
            Iterator iter = sessions.iterator();
            while( iter.hasNext() )
            {
                IoSession s = ( IoSession ) iter.next();
                if( name.equals( s.getAttribute( "user" ) ) )
                {
                    s.close();
                    break;
                }
            }
        }
    }
}
