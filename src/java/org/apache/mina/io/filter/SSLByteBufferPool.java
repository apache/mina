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

import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

import org.apache.mina.util.Stack;

/**
 * Simple ByteBuffer pool used by SSLHandler.
 * ByteBuffers are by default allocated as direct byte buffers. To use non-direct
 * ByteBuffers, set system property mina.sslfilter.directbuffer to false.
 *
 * @author Jan Andersson (janne@minq.se)
 * @version $Rev$, $Date$
 */
class SSLByteBufferPool
{
    private static final int PACKET_BUFFER_INDEX = 0;

    private static final int APPLICATION_BUFFER_INDEX = 1;

    private static boolean initiated = false;

    private static final String DIRECT_MEMORY_PROP = "mina.sslfilter.directbuffer";

    private static boolean useDirectAllocatedBuffers = true;

    private static int packetBufferSize;

    private static int appBufferSize;

    private static int[] bufferStackSizes;

    private static final Stack[] bufferStacks = new Stack[] { new Stack(),
                                                             new Stack(), };

    /**
     * Initiate buffer pool and buffer sizes from SSLEngine session.
     *
     * @param sslEngine SSLEngine
     */
    static synchronized void initiate( SSLEngine sslEngine )
    {
        if( !initiated )
        {
            // Use direct allocated memory or not?
            String prop = System.getProperty( DIRECT_MEMORY_PROP );
            if( prop != null )
            {
                useDirectAllocatedBuffers = Boolean
                        .getBoolean( DIRECT_MEMORY_PROP );
            }
            // init buffer sizes from SSLEngine
            packetBufferSize = sslEngine.getSession().getPacketBufferSize();
            
            // application buffer size has been doubled because SSLEngine
            // returns BUFFER_OVERFLOW even if there is enough room for the buffer.
            // So I doubled the size as a workaround.
            appBufferSize = sslEngine.getSession().getApplicationBufferSize() * 2;
            initiateBufferStacks();
            initiated = true;
        }
    }

    /**
     * Get bytebuffer with size the size of the largest SSL/TLS packet that may occur
     * (as defined by SSLSession).
     */
    static ByteBuffer getPacketBuffer()
    {
        if( !initiated )
        {
            throw new IllegalStateException( "Not initialized" );
        }
        return get( PACKET_BUFFER_INDEX );
    }

    /**
     * Get ByteBuffer with the size of the largest application buffer that may occur
     * (as defined by SSLSession).
     */
    static ByteBuffer getApplicationBuffer()
    {
        if( !initiated )
        {
            throw new IllegalStateException( "Not initialized" );
        }
        return get( APPLICATION_BUFFER_INDEX );
    }

    /**
     * Get the buffer which is capable of the specified size.
     */
    private static ByteBuffer get( int idx )
    {
        Stack stack = bufferStacks[ idx ];

        ByteBuffer buf;
        synchronized( stack )
        {
            buf = ( ByteBuffer ) stack.pop();
            if( buf == null )
            {
                buf = createBuffer( bufferStackSizes[ idx ] );
            }
        }

        buf.clear();
        return buf;
    }

    /**
     * Returns the specified buffer to buffer pool.
     */
    public static void put( ByteBuffer buf )
    {
        Stack stack = bufferStacks[ getBufferStackIndex( buf.capacity() ) ];
        synchronized( stack )
        {
            stack.push( buf );
        }
    }

    private static void initiateBufferStacks()
    {
        bufferStackSizes = new int[ 2 ];
        bufferStackSizes[ PACKET_BUFFER_INDEX ] = packetBufferSize;
        bufferStackSizes[ APPLICATION_BUFFER_INDEX ] = appBufferSize;
    }

    private static int getBufferStackIndex( int size )
    {
        if( size == packetBufferSize )
            return PACKET_BUFFER_INDEX;
        if( size == appBufferSize )
            return APPLICATION_BUFFER_INDEX;
        throw new IllegalArgumentException( "Unknown buffer size: " + size );
    }

    private static ByteBuffer createBuffer( int capacity )
    {
        if( useDirectAllocatedBuffers )
        {
            try
            {
                return ByteBuffer.allocateDirect( capacity );
            }
            catch( OutOfMemoryError e )
            {
                useDirectAllocatedBuffers = false;
                System.err
                        .println( "OutOfMemoryError: No more direct buffers available; trying heap buffer instead" );
            }
        }
        return ByteBuffer.allocate( capacity );
    }

}
