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
package org.apache.mina.protocol;

import org.apache.mina.common.IdleStatus;

/**
 * An abstract adapter class for {@link ProtocolHandlerFilter}.  You can extend
 * this class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolHandlerFilterAdapter implements ProtocolHandlerFilter
{
    public void sessionOpened( ProtocolHandler nextHandler,
                              ProtocolSession session )
    {
        nextHandler.sessionOpened( session );
    }

    public void sessionClosed( ProtocolHandler nextHandler,
                              ProtocolSession session )
    {
        nextHandler.sessionClosed( session );
    }

    public void sessionIdle( ProtocolHandler nextHandler,
                            ProtocolSession session, IdleStatus status )
    {
        nextHandler.sessionIdle( session, status );
    }

    public void exceptionCaught( ProtocolHandler nextHandler,
                                ProtocolSession session, Throwable cause )
    {
        nextHandler.exceptionCaught( session, cause );
    }

    public void messageReceived( ProtocolHandler nextHandler,
                                ProtocolSession session, Object message )
    {
        nextHandler.messageReceived( session, message );
    }

    public void messageSent( ProtocolHandler nextHandler,
                            ProtocolSession session, Object message )
    {
        nextHandler.messageSent( session, message );
    }

    public Object filterWrite( ProtocolSession session, Object message )
    {
        return message;
    }
}