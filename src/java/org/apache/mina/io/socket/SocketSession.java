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
package org.apache.mina.io.socket;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.IoHandlerFilterManager;
import org.apache.mina.util.Queue;
import org.apache.mina.util.IoHandlerFilterManager.WriteCommand;

/**
 * TODO Insert type comment.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class SocketSession implements IoSession
{
    private static final int READ_BUFFER_SIZE = 8192;

    private final IoHandlerFilterManager filterManager;

    private final SocketChannel ch;

    private final SocketSessionConfig config;

    private final ByteBuffer readBuf;

    private final Queue writeBufferQueue;

    private final Queue writeMarkerQueue;

    private final IoHandler handler;

    private final SocketAddress remoteAddress;

    private final SocketAddress localAddress;

    private final WriteCommand writeCommand = new WriteCommandImpl();

    private SelectionKey key;

    private Object attachment;

    private long readBytes;

    private long writtenBytes;

    private long lastReadTime;

    private long lastWriteTime;

    private boolean idleForBoth;

    private boolean idleForRead;

    private boolean idleForWrite;
    
    private boolean disposed;

    /**
     * Creates a new instance.
     */
    SocketSession( IoHandlerFilterManager filterManager, SocketChannel ch,
                  IoHandler defaultHandler )
    {
        this.filterManager = filterManager;
        this.ch = ch;
        this.config = new SocketSessionConfig( ch );
        this.readBuf = ByteBuffer.allocate( READ_BUFFER_SIZE ).limit( 0 );
        this.writeBufferQueue = new Queue();
        this.writeMarkerQueue = new Queue();
        this.handler = defaultHandler;
        this.remoteAddress = ch.socket().getRemoteSocketAddress();
        this.localAddress = ch.socket().getLocalSocketAddress();
    }

    IoHandlerFilterManager getFilterManager()
    {
        return filterManager;
    }

    SocketChannel getChannel()
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

    void dispose()
    {
        if (disposed)
        {
            return;
        }
        disposed = true;
        readBuf.release();
    }

    public IoHandler getHandler()
    {
        return handler;
    }

    public void close()
    {
        SocketIoProcessor.getInstance().removeSession( this );
    }

    public Object getAttachment()
    {
        return attachment;
    }

    public void setAttachment( Object attachment )
    {
        this.attachment = attachment;
    }

    ByteBuffer getReadBuffer()
    {
        return readBuf;
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
        filterManager.write( this, writeCommand, buf, marker );
    }

    public TransportType getTransportType()
    {
        return TransportType.SOCKET;
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

    void setIdle( IdleStatus status, boolean value )
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

    private class WriteCommandImpl implements WriteCommand
    {
        public void execute( ByteBuffer buf, Object marker )
        {
            synchronized( writeBufferQueue )
            {
                writeBufferQueue.push( buf );
                writeMarkerQueue.push( marker );
            }

            SocketIoProcessor.getInstance().flushSession( SocketSession.this );
        }
    }
}
