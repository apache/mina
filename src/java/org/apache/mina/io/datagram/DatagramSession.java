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
package org.apache.mina.io.datagram;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.Queue;

/**
 * TODO Insert type comment.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class DatagramSession implements IoSession
{
    private final DatagramProcessor parent;

    private final DatagramFilterChain filters;

    private final DatagramChannel ch;

    private final DatagramSessionConfig config;

    private final Queue writeBufferQueue;

    private final Queue writeMarkerQueue;

    private final IoHandler handler;

    private final SocketAddress localAddress;

    private SocketAddress remoteAddress;

    private SelectionKey key;

    private Object attachment;

    private long readBytes;

    private long writtenBytes;

    private long lastReadTime;

    private long lastWriteTime;

    private boolean idleForBoth;

    private boolean idleForRead;

    private boolean idleForWrite;

    /**
     * Creates a new instance.
     */
    DatagramSession( DatagramProcessor parent,
                    DatagramFilterChain filters, DatagramChannel ch,
                    IoHandler defaultHandler )
    {
        this.parent = parent;
        this.filters = filters;
        this.ch = ch;
        this.config = new DatagramSessionConfig( ch );
        this.writeBufferQueue = new Queue();
        this.writeMarkerQueue = new Queue();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
    }

    DatagramFilterChain getFilters()
    {
        return filters;
    }

    DatagramChannel getChannel()
    {
        return ch;
    }

    SelectionKey getSelectionKey()
    {
        return key;
    }

    void setSelectionKey( SelectionKey key )
    {
        this.key = key;
    }

    public IoHandler getHandler()
    {
        return handler;
    }

    public void close()
    {
    }

    public Object getAttachment()
    {
        return attachment;
    }

    public void setAttachment( Object attachment )
    {
        this.attachment = attachment;
    }

    Queue getWriteBufferQueue()
    {
        return writeBufferQueue;
    }

    Queue getWriteMarkerQueue()
    {
        return writeMarkerQueue;
    }

    public void write( ByteBuffer buf, Object marker )
    {
        filters.filterWrite( null, this, buf, marker );
    }

    public TransportType getTransportType()
    {
        return TransportType.DATAGRAM;
    }

    public boolean isConnected()
    {
        return ch.isConnected();
    }

    public SessionConfig getConfig()
    {
        return config;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    void setRemoteAddress( SocketAddress remoteAddress )
    {
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }

    public long getReadBytes()
    {
        return readBytes;
    }

    public long getWrittenBytes()
    {
        return writtenBytes;
    }

    void increaseReadBytes( int increment )
    {
        readBytes += increment;
        lastReadTime = System.currentTimeMillis();
    }

    void increaseWrittenBytes( int increment )
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

    void setIdle( IdleStatus status )
    {
        if( status == IdleStatus.BOTH_IDLE )
            idleForBoth = true;
        else if( status == IdleStatus.READER_IDLE )
            idleForRead = true;
        else if( status == IdleStatus.WRITER_IDLE )
            idleForWrite = true;
        else
            throw new IllegalArgumentException( "Unknown idle status: "
                                                + status );
    }
}