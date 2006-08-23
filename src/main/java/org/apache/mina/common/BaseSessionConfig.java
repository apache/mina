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
package org.apache.mina.common;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;

/**
 * Base implementation of {@link SessionConfig}s.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseSessionConfig implements SessionConfig
{
    private int idleTimeForRead;

    private int idleTimeForWrite;

    private int idleTimeForBoth;

    private int writeTimeout;

    protected BaseSessionConfig()
    {
    }

    public int getIdleTime( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
            return idleTimeForBoth;

        if( status == IdleStatus.READER_IDLE )
            return idleTimeForRead;

        if( status == IdleStatus.WRITER_IDLE )
            return idleTimeForWrite;

        throw new IllegalArgumentException( "Unknown idle status: " + status );
    }

    public long getIdleTimeInMillis( IdleStatus status )
    {
        return getIdleTime( status ) * 1000L;
    }

    public void setIdleTime( IdleStatus status, int idleTime )
    {
        if( idleTime < 0 )
            throw new IllegalArgumentException( "Illegal idle time: "
                                                + idleTime );

        if( status == IdleStatus.BOTH_IDLE )
            idleTimeForBoth = idleTime;
        else if( status == IdleStatus.READER_IDLE )
            idleTimeForRead = idleTime;
        else if( status == IdleStatus.WRITER_IDLE )
            idleTimeForWrite = idleTime;
        else
            throw new IllegalArgumentException( "Unknown idle status: "
                                                + status );
    }

    public int getWriteTimeout()
    {
        return writeTimeout;
    }

    public long getWriteTimeoutInMillis()
    {
        return writeTimeout * 1000L;
    }

    public void setWriteTimeout( int writeTimeout )
    {
        if( writeTimeout < 0 )
            throw new IllegalArgumentException( "Illegal write timeout: "
                                                + writeTimeout );
        this.writeTimeout = writeTimeout;
    }
}