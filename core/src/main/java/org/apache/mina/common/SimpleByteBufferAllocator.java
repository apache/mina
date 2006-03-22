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

import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * A simplistic {@link ByteBufferAllocator} which simply allocates a new
 * buffer every time.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
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
        return new SimpleByteBuffer( nioBuffer );
    }
    
    public ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        return new SimpleByteBuffer( nioBuffer );
    }

    private static class SimpleByteBuffer extends ByteBuffer
    {
        private java.nio.ByteBuffer buf;
        private int refCount = 1;
        private boolean autoExpand;

        protected SimpleByteBuffer( java.nio.ByteBuffer buf )
        {
            this.buf = buf;
            buf.order( ByteOrder.BIG_ENDIAN );
            autoExpand = false;
            refCount = 1;
        }

        public synchronized void acquire()
        {
            if( refCount <= 0 )
            {
                throw new IllegalStateException( "Already released buffer." );
            }

            refCount ++;
        }

        public void release()
        {
            synchronized( this )
            {
                if( refCount <= 0 )
                {
                    refCount = 0;
                    throw new IllegalStateException(
                            "Already released buffer.  You released the buffer too many times." );
                }

                refCount --;
                if( refCount > 0)
                {
                    return;
                }
            }
        }

        public java.nio.ByteBuffer buf()
        {
            return buf;
        }
        
        public boolean isDirect()
        {
            return buf.isDirect();
        }
        
        public boolean isReadOnly()
        {
            return buf.isReadOnly();
        }
        
        public boolean isAutoExpand()
        {
            return autoExpand;
        }
        
        public ByteBuffer setAutoExpand( boolean autoExpand )
        {
            this.autoExpand = autoExpand;
            return this;
        }
        
        public boolean isPooled()
        {
            return false;
        }
        
        public void setPooled( boolean pooled )
        {
        }

        public int capacity()
        {
            return buf.capacity();
        }
        
        public int position()
        {
            return buf.position();
        }

        public ByteBuffer position( int newPosition )
        {
            autoExpand( newPosition, 0 );
            buf.position( newPosition );
            return this;
        }

        public int limit()
        {
            return buf.limit();
        }

        public ByteBuffer limit( int newLimit )
        {
            autoExpand( newLimit, 0 );
            buf.limit( newLimit );
            return this;
        }

        public ByteBuffer mark()
        {
            buf.mark();
            return this;
        }

        public ByteBuffer reset()
        {
            buf.reset();
            return this;
        }

        public ByteBuffer clear()
        {
            buf.clear();
            return this;
        }

        public ByteBuffer flip()
        {
            buf.flip();
            return this;
        }

        public ByteBuffer rewind()
        {
            buf.rewind();
            return this;
        }

        public int remaining()
        {
            return buf.remaining();
        }

        public byte get()
        {
            return buf.get();
        }

        public ByteBuffer put( byte b )
        {
            autoExpand( 1 );
            buf.put( b );
            return this;
        }

        public byte get( int index )
        {
            return buf.get( index );
        }

        public ByteBuffer put( int index, byte b )
        {
            autoExpand( index, 1 );
            buf.put( index, b );
            return this;
        }

        public ByteBuffer get( byte[] dst, int offset, int length )
        {
            buf.get( dst, offset, length );
            return this;
        }

        public ByteBuffer put( java.nio.ByteBuffer src )
        {
            autoExpand( src.remaining() );
            buf.put( src );
            return this;
        }

        public ByteBuffer put( byte[] src, int offset, int length )
        {
            autoExpand( length );
            buf.put( src, offset, length );
            return this;
        }

        public ByteBuffer compact()
        {
            buf.compact();
            return this;
        }

        public int compareTo( ByteBuffer that )
        {
            return this.buf.compareTo( that.buf() );
        }

        public ByteOrder order()
        {
            return buf.order();
        }

        public ByteBuffer order( ByteOrder bo )
        {
            buf.order( bo );
            return this;
        }

        public char getChar()
        {
            return buf.getChar();
        }

        public ByteBuffer putChar( char value )
        {
            autoExpand( 2 );
            buf.putChar( value );
            return this;
        }

        public char getChar( int index )
        {
            return buf.getChar( index );
        }

        public ByteBuffer putChar( int index, char value )
        {
            autoExpand( index, 2 );
            buf.putChar( index, value );
            return this;
        }

        public CharBuffer asCharBuffer()
        {
            return buf.asCharBuffer();
        }

        public short getShort()
        {
            return buf.getShort();
        }

        public ByteBuffer putShort( short value )
        {
            autoExpand( 2 );
            buf.putShort( value );
            return this;
        }

        public short getShort( int index )
        {
            return buf.getShort( index );
        }

        public ByteBuffer putShort( int index, short value )
        {
            autoExpand( index, 2 );
            buf.putShort( index, value );
            return this;
        }

        public ShortBuffer asShortBuffer()
        {
            return buf.asShortBuffer();
        }

        public int getInt()
        {
            return buf.getInt();
        }

        public ByteBuffer putInt( int value )
        {
            autoExpand( 4 );
            buf.putInt( value );
            return this;
        }

        public int getInt( int index )
        {
            return buf.getInt( index );
        }

        public ByteBuffer putInt( int index, int value )
        {
            autoExpand( index, 4 );
            buf.putInt( index, value );
            return this;
        }

        public IntBuffer asIntBuffer()
        {
            return buf.asIntBuffer();
        }

        public long getLong()
        {
            return buf.getLong();
        }

        public ByteBuffer putLong( long value )
        {
            autoExpand( 8 );
            buf.putLong( value );
            return this;
        }

        public long getLong( int index )
        {
            return buf.getLong( index );
        }

        public ByteBuffer putLong( int index, long value )
        {
            autoExpand( index, 8 );
            buf.putLong( index, value );
            return this;
        }

        public LongBuffer asLongBuffer()
        {
            return buf.asLongBuffer();
        }

        public float getFloat()
        {
            return buf.getFloat();
        }

        public ByteBuffer putFloat( float value )
        {
            autoExpand( 4 );
            buf.putFloat( value );
            return this;
        }

        public float getFloat( int index )
        {
            return buf.getFloat( index );
        }

        public ByteBuffer putFloat( int index, float value )
        {
            autoExpand( index, 4 );
            buf.putFloat( index, value );
            return this;
        }

        public FloatBuffer asFloatBuffer()
        {
            return buf.asFloatBuffer();
        }

        public double getDouble()
        {
            return buf.getDouble();
        }

        public ByteBuffer putDouble( double value )
        {
            autoExpand( 8 );
            buf.putDouble( value );
            return this;
        }

        public double getDouble( int index )
        {
            return buf.getDouble( index );
        }

        public ByteBuffer putDouble( int index, double value )
        {
            autoExpand( index, 8 );
            buf.putDouble( index, value );
            return this;
        }

        public DoubleBuffer asDoubleBuffer()
        {
            return buf.asDoubleBuffer();
        }

        public ByteBuffer expand( int expectedRemaining )
        {
            if( autoExpand )
            {
                int pos = buf.position();
                int limit = buf.limit();
                int end = pos + expectedRemaining;
                if( end > limit ) {
                    ensureCapacity( end );
                    buf.limit( end );
                }
            }
            return this;
        }
        
        public ByteBuffer expand( int pos, int expectedRemaining )
        {
            if( autoExpand )
            {
                int limit = buf.limit();
                int end = pos + expectedRemaining;
                if( end > limit ) {
                    ensureCapacity( end );
                    buf.limit( end );
                }
            }
            return this;
        }
        
        private void ensureCapacity( int requestedCapacity )
        {
            if( requestedCapacity <= buf.capacity() )
            {
                return;
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
            newBuf.order( oldBuf.order() );

            int pos = oldBuf.position();
            int limit = oldBuf.limit();
            oldBuf.clear();
            newBuf.put( oldBuf );
            newBuf.position( 0 );
            newBuf.limit( limit );
            newBuf.position( pos );
            this.buf = newBuf;
        }

        public ByteBuffer duplicate() {
            return new SimpleByteBuffer( this.buf.duplicate() );
        }

        public ByteBuffer slice() {
            return new SimpleByteBuffer( this.buf.slice() );
        }

        public ByteBuffer asReadOnlyBuffer() {
            return new SimpleByteBuffer( this.buf.asReadOnlyBuffer() );
        }
    }
}
