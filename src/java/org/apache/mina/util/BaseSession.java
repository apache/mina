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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.Session;

/**
 * Base implementation of {@link Session}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseSession implements Session
{
    private final Map attributes = new HashMap();

    private long readBytes;
    
    private long writtenBytes;
    
    private long lastReadTime;
    
    private long lastWriteTime;

    private boolean idleForBoth;

    private boolean idleForRead;

    private boolean idleForWrite;


    protected BaseSession()
    {
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
            return idleForBoth;

        if( status == IdleStatus.READER_IDLE )
            return idleForRead;

        if( status == IdleStatus.WRITER_IDLE )
            return idleForWrite;

        throw new IllegalArgumentException( "Unknown idle status: " + status );
    }

    public void setIdle( IdleStatus status, boolean value )
    {
        if( status == IdleStatus.BOTH_IDLE )
            idleForBoth = value;
        else if( status == IdleStatus.READER_IDLE )
            idleForRead = value;
        else if( status == IdleStatus.WRITER_IDLE )
            idleForWrite = value;
        else
            throw new IllegalArgumentException( "Unknown idle status: "
                                                + status );
    }
}
