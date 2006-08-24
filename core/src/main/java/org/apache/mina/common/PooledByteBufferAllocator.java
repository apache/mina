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

import org.apache.mina.util.ExpiringStack;

import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * A {@link ByteBufferAllocator} which pools allocated buffers.
 * <p>
 * All buffers are allocated with the size of power of 2 (e.g. 16, 32, 64, ...)
 * This means that you cannot simply assume that the actual capacity of the
 * buffer and the capacity you requested are same.
 * </p>
 * <p>
 * This allocator releases the buffers which have not been in use for a certain
 * period.  You can adjust the period by calling {@link #setTimeout(int)}.
 * The default timeout is 1 minute (60 seconds).  To release these buffers
 * periodically, a daemon thread is started when a new instance of the allocator
 * is created.  You can stop the thread by calling {@link #dispose()}.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class PooledByteBufferAllocator implements ByteBufferAllocator
{
    private static final int MINIMUM_CAPACITY = 1;
    private static int threadId = 0;

    private final Expirer expirer;
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

    /**
     * Creates a new instance with the default timeout.
     */
    public PooledByteBufferAllocator()
    {
        this( 60 );
    }

    /**
     * Creates a new instance with the specified <tt>timeout</tt>.
     */
    public PooledByteBufferAllocator( int timeout )
    {
        setTimeout( timeout );
        expirer = new Expirer();
        expirer.start();
    }

    /**
     * Stops the thread which releases unused buffers and make this allocator
     * unusable from now on.
     */
    public void dispose()
    {
        if( this == ByteBuffer.getAllocator() )
        {
            throw new IllegalStateException( "This allocator is in use." );
        }

        expirer.shutdown();

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

    /**
     * Returns the timeout value of this allocator in seconds. 
     */
    public int getTimeout()
    {
        return timeout;
    }
    
    /**
     * Returns the timeout value of this allocator in milliseconds. 
     */
    public long getTimeoutMillis()
    {
        return timeout * 1000L;
    }
    
    /**
     * Sets the timeout value of this allocator in seconds.
     * 
     * @param timeout <tt>0</tt> or negative value to disable timeout.
     */
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
        UnexpandableByteBuffer ubb = allocate0( capacity, direct );
        PooledByteBuffer buf = allocateContainer();
        buf.init( ubb, true );
        return buf;
    }

    private PooledByteBuffer allocateContainer()
    {
		return new PooledByteBuffer();
    }
    
    private UnexpandableByteBuffer allocate0( int capacity, boolean direct )
    {
        ExpiringStack[] bufferStacks = direct? directBufferStacks : heapBufferStacks;
        int idx = getBufferStackIndex( bufferStacks, capacity );
        ExpiringStack stack = bufferStacks[ idx ];

        UnexpandableByteBuffer buf;
        synchronized( stack )
        {
            buf = ( UnexpandableByteBuffer ) stack.pop();
        }

        if( buf == null )
        {
            java.nio.ByteBuffer nioBuf = 
                direct ? java.nio.ByteBuffer.allocateDirect( MINIMUM_CAPACITY << idx ) :
                java.nio.ByteBuffer.allocate( MINIMUM_CAPACITY << idx );
            buf = new UnexpandableByteBuffer( nioBuf );
        }

        buf.init();
        
        return buf;
    }
    
    private void release0( UnexpandableByteBuffer buf )
    {
        ExpiringStack[] bufferStacks = buf.buf().isDirect()? directBufferStacks : heapBufferStacks;
        ExpiringStack stack = bufferStacks[ getBufferStackIndex( bufferStacks, buf.buf().capacity() ) ];
        
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
        buf.init( new UnexpandableByteBuffer( nioBuffer ), false );
        buf.buf.init();
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

        Expirer()
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
                    //ignore since this is shutdown time
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
                    //ignore
                }

                // Check if expiration is disabled.
                long timeout = getTimeoutMillis();
                if( timeout <= 0L )
                {
                    continue;
                }

                // Expire old buffers
                long expirationTime = System.currentTimeMillis() - timeout;

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
        private UnexpandableByteBuffer buf;
        private int refCount = 1;
        private boolean autoExpand;

        protected PooledByteBuffer()
        {
        }
        
        public synchronized void init( UnexpandableByteBuffer buf, boolean clear )
        {
            this.buf = buf;
            if( clear )
            {
                buf.buf().clear();
            }
            buf.buf().order( ByteOrder.BIG_ENDIAN );
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

            // No need to return buffers to the pool if it is disposed already.
            if( disposed )
            {
                return;
            }

            buf.release();
        }

        public java.nio.ByteBuffer buf()
        {
            return buf.buf();
        }
        
        public boolean isDirect()
        {
            return buf.buf().isDirect();
        }
        
        public boolean isReadOnly()
        {
            return buf.buf().isReadOnly();
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
            return buf.isPooled();
        }
        
        public void setPooled( boolean pooled )
        {
            buf.setPooled(pooled);
        }

        public int capacity()
        {
            return buf.buf().capacity();
        }
        
        public int position()
        {
            return buf.buf().position();
        }

        public ByteBuffer position( int newPosition )
        {
            autoExpand( newPosition, 0 );
            buf.buf().position( newPosition );
            return this;
        }

        public int limit()
        {
            return buf.buf().limit();
        }

        public ByteBuffer limit( int newLimit )
        {
            autoExpand( newLimit, 0 );
            buf.buf().limit( newLimit );
            return this;
        }

        public ByteBuffer mark()
        {
            buf.buf().mark();
            return this;
        }

        public ByteBuffer reset()
        {
            buf.buf().reset();
            return this;
        }

        public ByteBuffer clear()
        {
            buf.buf().clear();
            return this;
        }

        public ByteBuffer flip()
        {
            buf.buf().flip();
            return this;
        }

        public ByteBuffer rewind()
        {
            buf.buf().rewind();
            return this;
        }

        public int remaining()
        {
            return buf.buf().remaining();
        }

        public ByteBuffer duplicate()
        {
            PooledByteBuffer newBuf = allocateContainer();
            newBuf.init(
                    new UnexpandableByteBuffer( buf.buf().duplicate(), buf ), false );
            return newBuf;
        }

        public ByteBuffer slice()
        {
            PooledByteBuffer newBuf = allocateContainer();
            newBuf.init(
                    new UnexpandableByteBuffer( buf.buf().slice(), buf ), false );
            return newBuf;
        }

        public ByteBuffer asReadOnlyBuffer()
        {
            PooledByteBuffer newBuf = allocateContainer();
            newBuf.init(
                    new UnexpandableByteBuffer( buf.buf().asReadOnlyBuffer(), buf ), false );
            return newBuf;
        }

        public byte get()
        {
            return buf.buf().get();
        }

        public ByteBuffer put( byte b )
        {
            autoExpand( 1 );
            buf.buf().put( b );
            return this;
        }

        public byte get( int index )
        {
            return buf.buf().get( index );
        }

        public ByteBuffer put( int index, byte b )
        {
            autoExpand( index, 1 );
            buf.buf().put( index, b );
            return this;
        }

        public ByteBuffer get( byte[] dst, int offset, int length )
        {
            buf.buf().get( dst, offset, length );
            return this;
        }

        public ByteBuffer put( java.nio.ByteBuffer src )
        {
            autoExpand( src.remaining() );
            buf.buf().put( src );
            return this;
        }

        public ByteBuffer put( byte[] src, int offset, int length )
        {
            autoExpand( length );
            buf.buf().put( src, offset, length );
            return this;
        }

        public ByteBuffer compact()
        {
            buf.buf().compact();
            return this;
        }

        public int compareTo( ByteBuffer that )
        {
            return this.buf.buf().compareTo( that.buf() );
        }

        public ByteOrder order()
        {
            return buf.buf().order();
        }

        public ByteBuffer order( ByteOrder bo )
        {
            buf.buf().order( bo );
            return this;
        }

        public char getChar()
        {
            return buf.buf().getChar();
        }

        public ByteBuffer putChar( char value )
        {
            autoExpand( 2 );
            buf.buf().putChar( value );
            return this;
        }

        public char getChar( int index )
        {
            return buf.buf().getChar( index );
        }

        public ByteBuffer putChar( int index, char value )
        {
            autoExpand( index, 2 );
            buf.buf().putChar( index, value );
            return this;
        }

        public CharBuffer asCharBuffer()
        {
            return buf.buf().asCharBuffer();
        }

        public short getShort()
        {
            return buf.buf().getShort();
        }

        public ByteBuffer putShort( short value )
        {
            autoExpand( 2 );
            buf.buf().putShort( value );
            return this;
        }

        public short getShort( int index )
        {
            return buf.buf().getShort( index );
        }

        public ByteBuffer putShort( int index, short value )
        {
            autoExpand( index, 2 );
            buf.buf().putShort( index, value );
            return this;
        }

        public ShortBuffer asShortBuffer()
        {
            return buf.buf().asShortBuffer();
        }

        public int getInt()
        {
            return buf.buf().getInt();
        }

        public ByteBuffer putInt( int value )
        {
            autoExpand( 4 );
            buf.buf().putInt( value );
            return this;
        }

        public int getInt( int index )
        {
            return buf.buf().getInt( index );
        }

        public ByteBuffer putInt( int index, int value )
        {
            autoExpand( index, 4 );
            buf.buf().putInt( index, value );
            return this;
        }

        public IntBuffer asIntBuffer()
        {
            return buf.buf().asIntBuffer();
        }

        public long getLong()
        {
            return buf.buf().getLong();
        }

        public ByteBuffer putLong( long value )
        {
            autoExpand( 8 );
            buf.buf().putLong( value );
            return this;
        }

        public long getLong( int index )
        {
            return buf.buf().getLong( index );
        }

        public ByteBuffer putLong( int index, long value )
        {
            autoExpand( index, 8 );
            buf.buf().putLong( index, value );
            return this;
        }

        public LongBuffer asLongBuffer()
        {
            return buf.buf().asLongBuffer();
        }

        public float getFloat()
        {
            return buf.buf().getFloat();
        }

        public ByteBuffer putFloat( float value )
        {
            autoExpand( 4 );
            buf.buf().putFloat( value );
            return this;
        }

        public float getFloat( int index )
        {
            return buf.buf().getFloat( index );
        }

        public ByteBuffer putFloat( int index, float value )
        {
            autoExpand( index, 4 );
            buf.buf().putFloat( index, value );
            return this;
        }

        public FloatBuffer asFloatBuffer()
        {
            return buf.buf().asFloatBuffer();
        }

        public double getDouble()
        {
            return buf.buf().getDouble();
        }

        public ByteBuffer putDouble( double value )
        {
            autoExpand( 8 );
            buf.buf().putDouble( value );
            return this;
        }

        public double getDouble( int index )
        {
            return buf.buf().getDouble( index );
        }

        public ByteBuffer putDouble( int index, double value )
        {
            autoExpand( index, 8 );
            buf.buf().putDouble( index, value );
            return this;
        }

        public DoubleBuffer asDoubleBuffer()
        {
            return buf.buf().asDoubleBuffer();
        }

        public ByteBuffer expand( int expectedRemaining )
        {
            if( autoExpand )
            {
                int pos = buf.buf().position();
                int limit = buf.buf().limit();
                int end = pos + expectedRemaining;
                if( end > limit ) {
                    ensureCapacity( end );
                    buf.buf().limit( end );
                }
            }
            return this;
        }
        
        public ByteBuffer expand( int pos, int expectedRemaining )
        {
            if( autoExpand )
            {
                int limit = buf.buf().limit();
                int end = pos + expectedRemaining;
                if( end > limit ) {
                    ensureCapacity( end );
                    buf.buf().limit( end );
                }
            }
            return this;
        }
        
        private void ensureCapacity( int requestedCapacity )
        {
            if( requestedCapacity <= buf.buf().capacity() )
            {
                return;
            }
            
            if( buf.isDerived() )
            {
                throw new IllegalStateException( "Derived buffers cannot be expanded." );
            }
            
            int newCapacity = MINIMUM_CAPACITY;
            while( newCapacity < requestedCapacity )
            {
                newCapacity <<= 1;
            }
            
            UnexpandableByteBuffer oldBuf = this.buf;
            UnexpandableByteBuffer newBuf = allocate0( newCapacity, isDirect() );
            newBuf.buf().clear();
            newBuf.buf().order( oldBuf.buf().order() );

            int pos = oldBuf.buf().position();
            int limit = oldBuf.buf().limit();
            oldBuf.buf().clear();
            newBuf.buf().put( oldBuf.buf() );
            newBuf.buf().position( 0 );
            newBuf.buf().limit( limit );
            newBuf.buf().position( pos );
            this.buf = newBuf;
            oldBuf.release();
        }
    }

    private class UnexpandableByteBuffer
    {
        private final java.nio.ByteBuffer buf;
        private final UnexpandableByteBuffer parentBuf;
        private int refCount;
        private boolean pooled;

        protected UnexpandableByteBuffer( java.nio.ByteBuffer buf )
        {
            this.buf = buf;
            this.parentBuf = null;
        }
        
        protected UnexpandableByteBuffer(
                java.nio.ByteBuffer buf,
                UnexpandableByteBuffer parentBuf )
        {
            parentBuf.acquire();
            this.buf = buf;
            this.parentBuf = parentBuf;
        }
        
        public void init()
        {
            refCount = 1;
            pooled = true;
        }
        
        public synchronized void acquire()
        {
            if( isDerived() ) {
                parentBuf.acquire();
                return;
            }
            
            if( refCount <= 0 )
            {
                throw new IllegalStateException( "Already released buffer." );
            }

            refCount ++;
        }

        public void release()
        {
            if( isDerived() ) {
                parentBuf.release();
                return;
            }
            
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
                if( parentBuf != null )
                {
                    release0( parentBuf );
                }
                else
                {
                    release0( this );
                }
            }
        }

        public java.nio.ByteBuffer buf()
        {
            return buf;
        }
        
        public boolean isPooled()
        {
            return pooled;
        }
        
        public void setPooled( boolean pooled )
        {
            this.pooled = pooled;
        }
        
        public boolean isDerived()
        {
            return parentBuf != null;
        }
    }
}
