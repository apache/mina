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

import org.apache.mina.util.ExpiringStack;

/**
 * A {@link ByteBufferAllocator} which pools allocated buffers.
 * <p>
 * All buffers are allocated with the size of power of 2 (e.g. 16, 32, 64, ...)
 * This means that you cannot simply assume that the actual capacity of the
 * buffer and the capacity you requested are same.
 * </p>
 * <p>
 * This allocator doesn't deallocate buffers once they are allocated, so the
 * overall memory usage increase as time goes by and cause some applications
 * get {@link OutOfMemoryError} under high load. 
 * </p>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class PooledByteBufferAllocator implements ByteBufferAllocator
{
    private static final int MINIMUM_CAPACITY = 1;
    private static int threadId = 0;

    private final Expirer expirer;
    private final ExpiringStack containerStack = new ExpiringStack();
    private final ExpiringStack[] heapBufferStacks = new ExpiringStack[] {
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), };
    private final ExpiringStack[] directBufferStacks = new ExpiringStack[] {
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), };
    private int timeout;
    private boolean disposed;

    public PooledByteBufferAllocator()
    {
        this( 60 );
    }
    
    public PooledByteBufferAllocator( int timeout )
    {
        setTimeout( timeout );
        expirer = new Expirer();
        expirer.start();
    }
    
    public void dispose()
    {
        if( this == ByteBuffer.getAllocator() )
        {
            throw new IllegalStateException( "This allocator is in use." );
        }

        expirer.shutdown();
        synchronized( containerStack )
        {
            containerStack.clear();
        }
        
        for( int i = directBufferStacks.length - 1; i >= 0; i -- )
        {
            ExpiringStack stack = directBufferStacks[i];
            synchronized( stack )
            {
                stack.clear();
            }
        }
        for( int i = heapBufferStacks.length - 1; i >= 0; i -- )
        {
            ExpiringStack stack = heapBufferStacks[i];
            synchronized( stack )
            {
                stack.clear();
            }
        }
        disposed = true;
    }
    
    public int getTimeout()
    {
        return timeout;
    }
    
    public long getTimeoutMillis()
    {
        return timeout * 1000L;
    }
    
    public void setTimeout( int timeout )
    {
        if( timeout < 0 )
        {
            timeout = 0;
        }

        this.timeout = timeout;
        
        if( timeout > 0 )
        {
            
        }
    }
    
    public ByteBuffer allocate( int capacity, boolean direct )
    {
        ensureNotDisposed();
        java.nio.ByteBuffer nioBuffer = allocate0( capacity, direct );
        PooledByteBuffer buf = allocateContainer();
        buf.init( nioBuffer, true );
        return buf;
    }

    private PooledByteBuffer allocateContainer()
    {
        PooledByteBuffer buf;
        synchronized( containerStack )
        {
            buf = ( PooledByteBuffer ) containerStack.pop();
        }
        
        if( buf == null )
        {
            buf = new PooledByteBuffer();
        }
        return buf;
    }
    
    private java.nio.ByteBuffer allocate0( int capacity, boolean direct )
    {
        ExpiringStack[] bufferStacks = direct? directBufferStacks : heapBufferStacks;
        int idx = getBufferStackIndex( bufferStacks, capacity );
        ExpiringStack stack = bufferStacks[ idx ];

        java.nio.ByteBuffer buf;
        synchronized( stack )
        {
            buf = ( java.nio.ByteBuffer ) stack.pop();
        }

        if( buf == null )
        {
            buf = direct ? java.nio.ByteBuffer.allocateDirect( MINIMUM_CAPACITY << idx ) :
                           java.nio.ByteBuffer.allocate( MINIMUM_CAPACITY << idx );
        }
        
        return buf;
    }
    
    private void release0( java.nio.ByteBuffer buf )
    {
        ExpiringStack[] bufferStacks = buf.isDirect()? directBufferStacks : heapBufferStacks;
        ExpiringStack stack = bufferStacks[ getBufferStackIndex( bufferStacks, buf.capacity() ) ];
        synchronized( stack )
        {
            // push back
            stack.push( buf );
        }
    }
    
    public ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        ensureNotDisposed();
        PooledByteBuffer buf = allocateContainer();
        buf.init( nioBuffer, false );
        buf.setPooled( false );
        return buf;
    }

    private int getBufferStackIndex( ExpiringStack[] bufferStacks, int size )
    {
        int targetSize = MINIMUM_CAPACITY;
        int stackIdx = 0;
        while( size > targetSize )
        {
            targetSize <<= 1;
            stackIdx ++ ;
            if( stackIdx >= bufferStacks.length )
            {
                throw new IllegalArgumentException(
                        "Buffer size is too big: " + size );
            }
        }

        return stackIdx;
    }
    
    private void ensureNotDisposed()
    {
        if( disposed )
        {
            throw new IllegalStateException( "This allocator is disposed already." );
        }
    }

    private class Expirer extends Thread
    {
        private boolean timeToStop;

        public Expirer()
        {
            super( "PooledByteBufferExpirer-" + threadId++ );
            setDaemon( true );
        }
        
        public void shutdown()
        {
            timeToStop = true;
            interrupt();
            while( isAlive() )
            {
                try
                {
                    join();
                }
                catch ( InterruptedException e )
                {
                }
            }
        }
        
        public void run()
        {
            // Expire unused buffers every seconds
            while( !timeToStop )
            {
                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException e )
                {
                }

                // Check if expiration is disabled.
                long timeout = getTimeoutMillis();
                if( timeout <= 0L )
                {
                    continue;
                }

                // Expire old buffers
                long expirationTime = System.currentTimeMillis() - timeout;
                synchronized( containerStack )
                {
                    containerStack.expireBefore( expirationTime );
                }
                
                for( int i = directBufferStacks.length - 1; i >= 0; i -- )
                {
                    ExpiringStack stack = directBufferStacks[ i ];
                    synchronized( stack )
                    {
                        stack.expireBefore( expirationTime );
                    }
                }

                for( int i = heapBufferStacks.length - 1; i >= 0; i -- )
                {
                    ExpiringStack stack = heapBufferStacks[ i ];
                    synchronized( stack )
                    {
                        stack.expireBefore( expirationTime );
                    }
                }
            }
        }
    }

    private class PooledByteBuffer extends ByteBuffer
    {
        private java.nio.ByteBuffer buf;
        private int refCount = 1;
        private boolean autoExpand;
        private boolean pooled;
        private long timestamp;

        protected PooledByteBuffer()
        {
        }
        
        private synchronized void init( java.nio.ByteBuffer buf, boolean clear )
        {
            this.buf = buf;
            if( clear )
            {
                buf.clear();
            }
            buf.order( ByteOrder.BIG_ENDIAN );
            autoExpand = false;
            pooled = true;
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

            // No need to return buffers to the pool if it is disposed already.
            if( disposed )
            {
                return;
            }

            if( pooled )
            {
                release0( buf );
            }

            // Update timestamp.
            timestamp = System.currentTimeMillis();

            synchronized( containerStack )
            {
                containerStack.push( this );
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
            return pooled;
        }
        
        public void setPooled( boolean pooled )
        {
            this.pooled = pooled;
        }

        public long getTimestamp()
        {
            return timestamp;
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
            java.nio.ByteBuffer newBuf = allocate0( newCapacity, isDirect() );
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
            release0( oldBuf );
        }
    }
}
