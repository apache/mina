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
package org.apache.mina.util.byteaccess;


import java.util.ArrayList;
import java.util.Stack;

import org.apache.mina.core.buffer.IoBuffer;


/**
 * Creates <code>ByteArray</code>s, using a pool to reduce allocation where possible.
 *
 * WARNING: This code has never been run!
 * 
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteArrayPool implements ByteArrayFactory
{

    private final int MAX_BITS = 32;

    private boolean freed;
    private final boolean direct;
    private ArrayList<Stack<DirectBufferByteArray>> freeBuffers;
    private int freeBufferCount = 0;
    private long freeMemory = 0;
    private final int maxFreeBuffers;
    private final int maxFreeMemory;

    /**
     * Creates a new instance of ByteArrayPool.
     *
     * @param direct
     *  If we should use direct buffers
     * @param maxFreeBuffers
     *  The maximum number of free buffers
     * @param maxFreeMemory
     *  The maximum amount of free memory allowed
     */
    public ByteArrayPool( boolean direct, int maxFreeBuffers, int maxFreeMemory )
    {
        this.direct = direct;
        freeBuffers = new ArrayList<Stack<DirectBufferByteArray>>();
        for ( int i = 0; i < MAX_BITS; i++ )
        {
            freeBuffers.add( new Stack<DirectBufferByteArray>() );
        }
        this.maxFreeBuffers = maxFreeBuffers;
        this.maxFreeMemory = maxFreeMemory;
        this.freed = false;
    }

    /**
     * Creates a new instance of a {@link ByteArray}
     * 
     * @param size
     *  The size of the array to build
     */
    public ByteArray create( int size )
    {
        if ( size < 1 )
        {
            throw new IllegalArgumentException( "Buffer size must be at least 1: " + size );
        }
        int bits = bits( size );
        synchronized ( this )
        {
            if ( !freeBuffers.isEmpty() )
            {
                DirectBufferByteArray ba = freeBuffers.get( bits ).pop();
                ba.setFreed( false );
                ba.getSingleIoBuffer().limit( size );
                return ba;
            }
        }
        IoBuffer bb;
        int bbSize = 1 << bits;
        bb = IoBuffer.allocate( bbSize, direct );
        bb.limit( size );
        DirectBufferByteArray ba = new DirectBufferByteArray( bb );
        ba.setFreed( false );
        return ba;
    }


    private int bits( int index )
    {
        int bits = 0;
        while ( 1 << bits < index )
        {
            bits++;
        }
        return bits;
    }

    /**
     * Frees the buffers
     *
     */
    public void free()
    {
        synchronized ( this )
        {
            if ( freed )
            {
                throw new IllegalStateException( "Already freed." );
            }
            freed = true;
            freeBuffers.clear();
            freeBuffers = null;
        }
    }

    private class DirectBufferByteArray extends BufferByteArray
    {

        public boolean freed;


        public DirectBufferByteArray( IoBuffer bb )
        {
            super( bb );
        }


        public void setFreed( boolean freed )
        {
            this.freed = freed;
        }


        @Override
        public void free()
        {
            synchronized ( this )
            {
                if ( freed )
                {
                    throw new IllegalStateException( "Already freed." );
                }
                freed = true;
            }
            int bits = bits( last() );
            synchronized ( ByteArrayPool.this )
            {
                if ( freeBuffers != null && freeBufferCount < maxFreeBuffers && freeMemory + last() <= maxFreeMemory )
                {
                    freeBuffers.get( bits ).push( this );
                    freeBufferCount++;
                    freeMemory += last();
                    return;
                }
            }
        }

    }

}
