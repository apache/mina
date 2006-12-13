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
package org.apache.mina.common;

import java.nio.ByteOrder;

import org.apache.mina.common.support.BaseByteBuffer;

/**
 * A simplistic {@link ByteBufferAllocator} which simply allocates a new
 * buffer every time.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SimpleByteBufferAllocator implements ByteBufferAllocator
{
    private static final int MINIMUM_CAPACITY = 1;

    public SimpleByteBufferAllocator()
    {
    }
    
    public ByteBuffer allocate( int capacity, boolean direct )
    {
        java.nio.ByteBuffer nioBuffer;
        if( direct )
        {
            nioBuffer = java.nio.ByteBuffer.allocateDirect( capacity );            
        }
        else
        {
            nioBuffer = java.nio.ByteBuffer.allocate( capacity );            
        }
        return new SimpleByteBuffer( nioBuffer, false );
    }
    
    public ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        return new SimpleByteBuffer( nioBuffer, false );
    }

    public void dispose()
    {
    }

    private static class SimpleByteBuffer extends BaseByteBuffer
    {
        private java.nio.ByteBuffer buf;
        private boolean derived;

        protected SimpleByteBuffer( java.nio.ByteBuffer buf, boolean derived )
        {
            this.buf = buf;
            this.derived = derived;
            buf.order( ByteOrder.BIG_ENDIAN );
        }

        public java.nio.ByteBuffer buf()
        {
            return buf;
        }
        
        protected void capacity0( int requestedCapacity )
        {
            if( derived )
            {
                throw new IllegalStateException( "Derived buffers cannot be expanded." );
            }

            int newCapacity = MINIMUM_CAPACITY;
            while( newCapacity < requestedCapacity )
            {
                newCapacity <<= 1;
            }
            
            java.nio.ByteBuffer oldBuf = this.buf;
            java.nio.ByteBuffer newBuf;
            if( isDirect() )
            {
                newBuf = java.nio.ByteBuffer.allocateDirect( newCapacity );
            }
            else
            {
                newBuf = java.nio.ByteBuffer.allocate( newCapacity );
            }

            newBuf.clear();
            oldBuf.clear();
            newBuf.put( oldBuf );
            this.buf = newBuf;
        }

        public ByteBuffer duplicate() {
            return new SimpleByteBuffer( this.buf.duplicate(), true );
        }

        public ByteBuffer slice() {
            return new SimpleByteBuffer( this.buf.slice(), true );
        }

        public ByteBuffer asReadOnlyBuffer() {
            return new SimpleByteBuffer( this.buf.asReadOnlyBuffer(), true );
        }

        public byte[] array()
        {
            return buf.array();
        }
        
        public int arrayOffset()
        {
            return buf.arrayOffset();
        }

        @Override
        public boolean hasArray()
        {
            return buf.hasArray();
        }
    }
}
