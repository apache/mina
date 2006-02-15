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
package org.apache.mina.filter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.util.Queue;

/**
 * Filter implementation which makes it possible to write {@link InputStream}
 * objects directly using {@link IoSession#write(Object)}. When an 
 * {@link InputStream} is written to a session this filter will read the bytes
 * from the stream into {@link ByteBuffer} objects and write those buffers
 * to the next filter. When end of stream has been reached this filter will
 * call {@link NextFilter#messageSent(IoSession, Object)} using the original
 * {@link InputStream} written to the session and call 
 * {@link org.apache.mina.common.WriteFuture#setWritten(boolean)} on the 
 * original {@link org.apache.mina.common.IoFilter.WriteRequest}.
 * <p>
 * This filter will ignore written messages which aren't {@link InputStream}
 * instances. Such messages will be passed to the next filter directly.
 * </p>
 * <p>
 * NOTE: this filter does not close the stream after all data from stream
 * has been written. The {@link org.apache.mina.common.IoHandler} should take
 * care of that in its 
 * {@link org.apache.mina.common.IoHandler#messageSent(IoSession, Object)} 
 * callback.
 * </p>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class StreamWriteFilter extends IoFilterAdapter
{
    /**
     * The default buffer size this filter uses for writing.
     */
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 4096;

    /**
     * The attribute name used when binding the {@link InputStream} to the session.
     */
    public static final String CURRENT_STREAM = StreamWriteFilter.class.getName() + ".stream";

    protected static final String WRITE_REQUEST_QUEUE = StreamWriteFilter.class.getName() + ".queue";
    protected static final String INITIAL_WRITE_FUTURE = StreamWriteFilter.class.getName() + ".future";

    private int writeBufferSize = DEFAULT_STREAM_BUFFER_SIZE;
    
    public void filterWrite( NextFilter nextFilter, IoSession session, 
                            WriteRequest writeRequest ) throws Exception 
    {
        // If we're already processing a stream we need to queue the WriteRequest.
        if( session.getAttribute( CURRENT_STREAM ) != null )
        {
            Queue queue = ( Queue ) session.getAttribute( WRITE_REQUEST_QUEUE );
            if( queue == null )
            {
                queue = new Queue();
                session.setAttribute( WRITE_REQUEST_QUEUE, queue );
            }
            queue.push( writeRequest );
            return;
        }
        
        Object message = writeRequest.getMessage();
        
        if( message instanceof InputStream )
        {
            
            InputStream inputStream = ( InputStream ) message;
            
            ByteBuffer byteBuffer = getNextByteBuffer( inputStream );
            if ( byteBuffer == null )
            {
                // End of stream reached.
                writeRequest.getFuture().setWritten( true );
                nextFilter.messageSent( session, message );
            }
            else
            {
                session.setAttribute( CURRENT_STREAM, inputStream );
                session.setAttribute( INITIAL_WRITE_FUTURE, writeRequest.getFuture() );
                
                nextFilter.filterWrite( session, new WriteRequest( byteBuffer ) );
            }

        }
        else
        {
            nextFilter.filterWrite( session, writeRequest );
        }
    }

    public void messageSent( NextFilter nextFilter, IoSession session, Object message ) throws Exception
    {
        InputStream inputStream = ( InputStream ) session.getAttribute( CURRENT_STREAM );
        
        if( inputStream == null )
        {
            nextFilter.messageSent( session, message );
        }
        else
        {
            ByteBuffer byteBuffer = getNextByteBuffer( inputStream );
        
            if( byteBuffer == null ) 
            {
                // End of stream reached.
                session.removeAttribute( CURRENT_STREAM );
                WriteFuture writeFuture = ( WriteFuture ) session.removeAttribute( INITIAL_WRITE_FUTURE );
                
                // Write queued WriteRequests.
                Queue queue = ( Queue ) session.removeAttribute( WRITE_REQUEST_QUEUE );
                if( queue != null )
                {
                    WriteRequest wr = ( WriteRequest ) queue.pop();
                    while( wr != null )
                    {
                        filterWrite( nextFilter, session, wr );
                        wr = ( WriteRequest ) queue.pop();
                    }
                }
                
                writeFuture.setWritten( true );
                nextFilter.messageSent( session, inputStream );
            }
            else
            {
                nextFilter.filterWrite( session, new WriteRequest( byteBuffer ) );
            }
        }
    }

    private ByteBuffer getNextByteBuffer( InputStream inputStream ) throws IOException 
    {
        byte[] bytes = new byte[ writeBufferSize ];
        int packetLength = inputStream.read( bytes );
        if( packetLength == -1 )
        {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap( bytes, 0, packetLength );
        buffer.acquire(); // prevent from being pooled.

        return buffer;
    }

    /**
     * Returns the size of the write buffer in bytes. Data will be read from the 
     * stream in chunks of this size and then written to the next filter.
     * 
     * @return the write buffer size.
     */
    public int getWriteBufferSize()
    {
        return writeBufferSize;
    }

    /**
     * Sets the size of the write buffer in bytes. Data will be read from the 
     * stream in chunks of this size and then written to the next filter.
     * 
     * @throws IllegalArgumentException if the specified size is &lt; 1.
     */
    public void setWriteBufferSize( int writeBufferSize )
    {
        if( writeBufferSize < 1 )
        {
            throw new IllegalArgumentException( "writeBufferSize must be at least 1" );
        }
        this.writeBufferSize = writeBufferSize;
    }
    
    
}
