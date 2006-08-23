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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.Session;

/**
 * Base implementation of {@link Session}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseSession implements Session
{
    private final Map attributes = new HashMap();
    private final long creationTime;

    private long readBytes;
    private long writtenBytes;
    private long writtenWriteRequests;
    
    private long lastReadTime;
    private long lastWriteTime;

    private int idleCountForBoth;
    private int idleCountForRead;
    private int idleCountForWrite;
    
    private long lastIdleTimeForBoth;
    private long lastIdleTimeForRead;
    private long lastIdleTimeForWrite;

    protected BaseSession()
    {
        creationTime = lastReadTime = lastWriteTime =
            lastIdleTimeForBoth = lastIdleTimeForRead = lastIdleTimeForWrite =
                System.currentTimeMillis();
    }
    
    public void close()
    {
        this.close( false );
    }

    public Object getAttachment()
    {
        return attributes.get( "" );
    }

    public Object setAttachment( Object attachment )
    {
        synchronized( attributes )
        {
            return attributes.put( "", attachment );
        }
    }

    public Object getAttribute( String key )
    {
        return attributes.get( key );
    }

    public Object setAttribute( String key, Object value )
    {
        synchronized( attributes )
        {
            return attributes.put( key, value );
        }
    }
    
    public Object removeAttribute( String key )
    {
        synchronized( attributes )
        {
            return attributes.remove( key );
        }
    }

    public Set getAttributeKeys() {
        synchronized( attributes )
        {
            return attributes.keySet();
        }
    }
    
    public long getReadBytes()
    {
        return readBytes;
    }

    public long getWrittenBytes()
    {
        return writtenBytes;
    }

    public long getWrittenWriteRequests()
    {
        return writtenWriteRequests;
    }
    
    public void increaseReadBytes( int increment )
    {
        readBytes += increment;
        lastReadTime = System.currentTimeMillis();
    }

    public void increaseWrittenBytes( int increment )
    {
        writtenBytes += increment;
        lastWriteTime = System.currentTimeMillis();
    }

    public void increaseWrittenWriteRequests()
    {
        writtenWriteRequests ++;
    }
    
    public long getCreationTime()
    {
        return creationTime;
    }

    public long getLastIoTime()
    {
        return Math.max( lastReadTime, lastWriteTime );
    }

    public long getLastReadTime()
    {
        return lastReadTime;
    }

    public long getLastWriteTime()
    {
        return lastWriteTime;
    }

    public boolean isIdle( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
            return idleCountForBoth > 0;

        if( status == IdleStatus.READER_IDLE )
            return idleCountForRead > 0;

        if( status == IdleStatus.WRITER_IDLE )
            return idleCountForWrite > 0;

        throw new IllegalArgumentException( "Unknown idle status: " + status );
    }

    public int getIdleCount( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
            return idleCountForBoth;

        if( status == IdleStatus.READER_IDLE )
            return idleCountForRead;

        if( status == IdleStatus.WRITER_IDLE )
            return idleCountForWrite;

        throw new IllegalArgumentException( "Unknown idle status: " + status );
    }
    
    public long getLastIdleTime( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
            return lastIdleTimeForBoth;

        if( status == IdleStatus.READER_IDLE )
            return lastIdleTimeForRead;

        if( status == IdleStatus.WRITER_IDLE )
            return lastIdleTimeForWrite;

        throw new IllegalArgumentException( "Unknown idle status: " + status );
    }

    public void increaseIdleCount( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
        {
            idleCountForBoth ++;
            lastIdleTimeForBoth = System.currentTimeMillis();
        }
        else if( status == IdleStatus.READER_IDLE )
        {
            idleCountForRead ++;
            lastIdleTimeForRead = System.currentTimeMillis();
        }
        else if( status == IdleStatus.WRITER_IDLE )
        {
            idleCountForWrite ++;
            lastIdleTimeForWrite = System.currentTimeMillis();
        }
        else
            throw new IllegalArgumentException( "Unknown idle status: "
                                                + status );
    }

    public void resetIdleCount( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
            idleCountForBoth = 0;
        else if( status == IdleStatus.READER_IDLE )
            idleCountForRead = 0;
        else if( status == IdleStatus.WRITER_IDLE )
            idleCountForWrite = 0;
        else
            throw new IllegalArgumentException( "Unknown idle status: "
                                                + status );
    }
}
