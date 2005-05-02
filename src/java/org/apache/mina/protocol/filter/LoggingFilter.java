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
package org.apache.mina.protocol.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.protocol.ProtocolFilter;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.util.SessionLog;

/**
 * Logs all MINA protocol events to {@link Logger}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see SessionLog
 */
public class LoggingFilter implements ProtocolFilter
{
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = SessionLog.PREFIX;

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = SessionLog.LOGGER;
    
    /**
     * Session attribute key: {@link Level}
     */
    public static final String LEVEL = SessionLog.LEVEL;
    
    public void sessionOpened( NextFilter nextFilter, ProtocolSession session )
    {
        SessionLog.log( session, "OPENED" );
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, ProtocolSession session )
    {
        SessionLog.log( session, "CLOSED" );
        nextFilter.sessionClosed( session );
    }

    public void sessionIdle( NextFilter nextFilter, ProtocolSession session, IdleStatus status )
    {
        SessionLog.log( session, "IDLE: " + status );
        nextFilter.sessionIdle( session, status );
    }

    public void exceptionCaught( NextFilter nextFilter, ProtocolSession session, Throwable cause )
    {
        SessionLog.log( session, "EXCEPTION:", cause );
        nextFilter.exceptionCaught( session, cause );
    }

    public void messageReceived( NextFilter nextFilter, ProtocolSession session, Object message )
    {
        SessionLog.log( session, "RECEIVED: " + message );
        nextFilter.messageReceived( session, message );
    }

    public void messageSent( NextFilter nextFilter, ProtocolSession session, Object message )
    {
        SessionLog.log( session, "SENT: " + message );
        nextFilter.messageSent( session, message );
    }

    public void filterWrite( NextFilter nextFilter, ProtocolSession session, Object message)
    {
        SessionLog.log( session, "WRITE: " + message );
        nextFilter.filterWrite( session, message );
    }
}
