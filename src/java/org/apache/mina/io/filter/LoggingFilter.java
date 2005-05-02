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
package org.apache.mina.io.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoFilter;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;

/**
 * Logs all MINA I/O events to {@link Logger}.
 * <p>
 * Call {@link #getLogger(IoSession)}, {@link #log(IoSession, String)}, and
 * {@link #log(IoSession, String, Throwable)} to log protocol-specific messages.
 * <p>
 * Set {@link #PREFIX}, {@link #LOGGER}, {@link #LEVEL} session attributes
 * to override prefix string, logger, and log level.
 * (See {@link IoHandler#sessionCreated(IoSession)}))
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class LoggingFilter implements IoFilter
{
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = LoggingFilter.class.getName() + ".prefix";

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = LoggingFilter.class.getName() + ".logger";
    
    /**
     * Session attribute key: {@link Level}
     */
    public static final String LEVEL = LoggingFilter.class.getName() + ".level";
    
    public static Logger getLogger( IoSession session )
    {
        
        Logger log = (Logger) session.getAttribute( LOGGER );
        if( log == null )
        {
            log = Logger.getLogger( session.getHandler().getClass().getName() );
            String prefix = ( String ) session.getAttribute( PREFIX );
            if( prefix != null )
            {
                prefix = "[" + session.getRemoteAddress() + "] ";
                session.setAttribute( PREFIX, prefix );
            }
            
            Level level = ( Level ) session.getAttribute( LEVEL );
            if( level != null )
            {
                level = Level.INFO;
                session.setAttribute( LEVEL, level );
            }
                
            session.setAttribute( LOGGER, log );
        }
        
        return log;
    }

    public static void log( IoSession session, String message )
    {
        Logger log = getLogger( session );
        Level level = ( Level ) session.getAttribute( LEVEL );
        if( log.isLoggable( level ) )
        {
            log.log( level, message );
        }
    }

    public static void log( IoSession session, String message, Throwable cause )
    {
        Logger log = getLogger( session );
        Level level = ( Level ) session.getAttribute( LEVEL );
        if( log.isLoggable( level ) )
        {
            log.log( level, message, cause );
        }
    }

    public void sessionOpened( NextFilter nextFilter, IoSession session )
    {
        log( session, "OPENED" );
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session )
    {
        log( session, "CLOSED" );
        nextFilter.sessionClosed( session );
    }

    public void sessionIdle( NextFilter nextFilter, IoSession session, IdleStatus status )
    {
        log( session, "IDLE: " + status );
        nextFilter.sessionIdle( session, status );
    }

    public void exceptionCaught( NextFilter nextFilter, IoSession session, Throwable cause )
    {
        log( session, "EXCEPTION:", cause );
        nextFilter.exceptionCaught( session, cause );
    }

    public void dataRead( NextFilter nextFilter, IoSession session, ByteBuffer buf)
    {
        log( session, "READ: " + buf.getHexDump() );
        nextFilter.dataRead( session, buf );
    }

    public void dataWritten( NextFilter nextFilter, IoSession session, Object marker)
    {
        log( session, "WRITTEN: " + marker );
        nextFilter.dataWritten( session, marker );
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker)
    {
        log( session, "WRITE:" + marker + ", " + buf.getHexDump() );
        nextFilter.filterWrite( session, buf, marker );
    }
}
