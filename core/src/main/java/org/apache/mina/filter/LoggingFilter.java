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
package org.apache.mina.filter;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs all MINA protocol events using the {@link SessionLog}. Each event will
 * be logged to its own {@link Logger}. The names of these loggers will share a 
 * common prefix. By default the name of the logger used by {@link SessionLog}
 * will be used as prefix. Use {@link #setLoggerNamePrefix(String)} to override
 * the default prefix.
 * <p>
 * For most of the events the name of the corresponding {@link IoHandler} method
 * which handles that event will be added to the prefix to form the logger name.
 * Here's a complete list of the logger names for each event:
 * <table>
 *   <tr><td>Event</td><td>Name of logger</td></tr>
 *   <tr><td>sessionCreated</td><td>&lt;prefix&gt;.sessionCreated</td></tr>
 *   <tr><td>sessionOpened</td><td>&lt;prefix&gt;.sessionOpened</td></tr>
 *   <tr><td>sessionClosed</td><td>&lt;prefix&gt;.sessionClosed</td></tr>
 *   <tr><td>sessionIdle</td><td>&lt;prefix&gt;.sessionIdle</td></tr>
 *   <tr><td>exceptionCaught</td><td>&lt;prefix&gt;.exceptionCaught</td></tr>
 *   <tr><td>messageReceived</td><td>&lt;prefix&gt;.messageReceived</td></tr>
 *   <tr><td>messageSent</td><td>&lt;prefix&gt;.messageSent</td></tr>
 *   <tr><td>{@link IoSession#write(Object)}</td><td>&lt;prefix&gt;.write</td></tr>
 *   <tr><td>{@link IoSession#close()}</td><td>&lt;prefix&gt;.close</td></tr>
 * </table>
 * </p> 
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see SessionLog
 */
public class LoggingFilter extends IoFilterAdapter
{
    /**
     * Session attribute key: prefix string
     */
    public static final String PREFIX = SessionLog.PREFIX;

    /**
     * Session attribute key: {@link Logger}
     */
    public static final String LOGGER = SessionLog.LOGGER;
    
    private static final String EVENT_LOGGER = LoggingFilter.class.getName() + ".event";
    
    private String loggerNamePrefix = null;
    
    /**
     * Creates a new instance.
     */
    public LoggingFilter()
    {
    }
    
    /**
     * Returns the prefix used for the names of the loggers used to log the different 
     * events. If <code>null</code> the name of the {@link Logger} used by
     * {@link SessionLog} will be used for the prefix. The default value is 
     * <code>null</code>.
     *
     * @return the prefix or <code>null</code>.
     */
    public String getLoggerNamePrefix() {
        return loggerNamePrefix;
    }

    /**
     * Sets the prefix used for the names of the loggers used to log the different 
     * events. If set to <code>null</code> the name of the {@link Logger} used by
     * {@link SessionLog} will be used for the prefix.
     *
     * @param loggerNamePrefix the new prefix.
     */
    public void setLoggerNamePrefix(String loggerNamePrefix) {
        this.loggerNamePrefix = loggerNamePrefix;
    }
    
    public void sessionCreated( NextFilter nextFilter, IoSession session )
    {
        SessionLog.info( getLogger( session, "sessionCreated" ), session, "CREATED" );
        nextFilter.sessionCreated( session );
    }
    
    public void sessionOpened( NextFilter nextFilter, IoSession session )
    {
        SessionLog.info( getLogger( session, "sessionOpened" ), session, "OPENED" );
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session )
    {
        SessionLog.info( getLogger( session, "sessionClosed" ), session, "CLOSED" );
        nextFilter.sessionClosed( session );
    }

    public void sessionIdle( NextFilter nextFilter, IoSession session, IdleStatus status )
    {
    	Logger log = getLogger( session, "sessionIdle" );
        if( log.isInfoEnabled() )
        {
            SessionLog.info( log, session, "IDLE: " + status );
        }
        nextFilter.sessionIdle( session, status );
    }

    public void exceptionCaught( NextFilter nextFilter, IoSession session, Throwable cause )
    {
    	Logger log = getLogger( session, "exceptionCaught" );
        if( log.isInfoEnabled() )
        {
            SessionLog.info( log, session, "EXCEPTION:", cause );
        }
        nextFilter.exceptionCaught( session, cause );
    }

    public void messageReceived( NextFilter nextFilter, IoSession session, Object message )
    {
    	Logger log = getLogger( session, "messageReceived" );
        if( log.isInfoEnabled() )
        {
            SessionLog.info( log, session, "RECEIVED: " + message );
        }
        nextFilter.messageReceived( session, message );
    }

    public void messageSent( NextFilter nextFilter, IoSession session, Object message )
    {
    	Logger log = getLogger( session, "messageSent" );
        if( log.isInfoEnabled() )
        {
            SessionLog.info( log, session, "SENT: " + message );
        }
        nextFilter.messageSent( session, message );
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest )
    {
    	Logger log = getLogger( session, "write" );
        if( log.isInfoEnabled() )
        {
            SessionLog.info( log, session, "WRITE: " + writeRequest );
        }
        nextFilter.filterWrite( session, writeRequest );
    }

    public void filterClose( NextFilter nextFilter, IoSession session ) throws Exception
    {
        SessionLog.info( getLogger( session, "close" ), session, "CLOSE" );
        nextFilter.filterClose( session );
    }
    
    private Logger getLogger( IoSession session, String event )
    {
    	Logger log = ( Logger ) session.getAttribute( EVENT_LOGGER + "." + event );
    	if( log == null )
    	{
    		String prefix = loggerNamePrefix;
    		if( prefix == null )
    		{
    			prefix = SessionLog.getLogger( session ).getName();
    		}
    		log = LoggerFactory.getLogger( prefix + "." + event );
            session.setAttribute( EVENT_LOGGER + "." + event, log );
    	}
    	return log;
    }
}
