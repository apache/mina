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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.io.IoFilter;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.SessionLog;
import org.slf4j.Logger;

/**
 * Logs all MINA I/O events to {@link Logger}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see SessionLog
 */
public class IoLoggingFilter implements IoFilter
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
     * Creates a new instance.
     */
    public IoLoggingFilter()
    {
    }
    
    public void sessionOpened( NextFilter nextFilter, IoSession session )
    {
        SessionLog.info( session, "OPENED" );
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session )
    {
        SessionLog.info( session, "CLOSED" );
        nextFilter.sessionClosed( session );
    }

    public void sessionIdle( NextFilter nextFilter, IoSession session, IdleStatus status )
    {
        SessionLog.info( session, "IDLE: " + status );
        nextFilter.sessionIdle( session, status );
    }

    public void exceptionCaught( NextFilter nextFilter, IoSession session, Throwable cause )
    {
        SessionLog.error( session, "EXCEPTION: ", cause );
        nextFilter.exceptionCaught( session, cause );
    }

    public void dataRead( NextFilter nextFilter, IoSession session, ByteBuffer buf)
    {
        SessionLog.info( session, "READ: " + buf.getHexDump() );
        nextFilter.dataRead( session, buf );
    }

    public void dataWritten( NextFilter nextFilter, IoSession session, Object marker)
    {
        SessionLog.info( session, "WRITTEN: " + marker );
        nextFilter.dataWritten( session, marker );
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker)
    {
        SessionLog.info( session, "WRITE: " + marker + ", " + buf.getHexDump() );
        nextFilter.filterWrite( session, buf, marker );
    }
}
