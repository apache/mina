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
package org.apache.mina.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mina.common.Session;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolSession;

/**
 * Call {@link #getLogger(Session)}, {@link #log(Level,Session, String)}, and
 * {@link #log(Level,Session, String, Throwable)} to log protocol-specific messages.
 * <p>
 * Set {@link #PREFIX} and {@link #LOGGER} session attributes
 * to override prefix string, logger, and log level.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 *
 */
public class SessionLog {
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = SessionLog.class.getName() + ".prefix";

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = SessionLog.class.getName() + ".logger";
    
    public static Logger getLogger( Session session )
    {
        
        Logger log = (Logger) session.getAttribute( LOGGER );
        if( log == null )
        {
            log = Logger.getLogger( getClassName( session ) );
            String prefix = ( String ) session.getAttribute( PREFIX );
            if( prefix == null )
            {
                prefix = "[" + session.getRemoteAddress() + "] ";
                session.setAttribute( PREFIX, prefix );
            }
                
            session.setAttribute( LOGGER, log );
        }
        
        return log;
    }
    
    private static String getClassName( Session session )
    {
        if( session instanceof IoSession )
            return ( ( IoSession ) session ).getHandler().getClass().getName();
        else
            return ( ( ProtocolSession ) session ).getHandler().getClass().getName();
    }

    public static void log( Level level, Session session, String message )
    {
        Logger log = getLogger( session );
        if( log.isLoggable( level ) )
        {
            log.log( level, String.valueOf( session.getAttribute( PREFIX ) ) + message );
        }
    }

    public static void log( Level level, Session session, String message, Throwable cause )
    {
        Logger log = getLogger( session );
        if( log.isLoggable( level ) )
        {
            log.log( level, String.valueOf( session.getAttribute( PREFIX ) ) + message, cause );
        }
    }
}
