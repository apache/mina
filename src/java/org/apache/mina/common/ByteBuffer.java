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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.util.Stack;

/**
 * A pooled byte buffer used by MINA applications.
 * <p>
 * This is a replacement for {@link java.nio.ByteBuffer}. Please refer to
 * {@link java.nio.ByteBuffer} and {@link java.nio.Buffer} documentation for
 * usage.  MINA does not use NIO {@link java.nio.ByteBuffer} directly for two
 * reasons:
 * <ul>
 *   <li>It doesn't provide useful getters and putters such as
 *       <code>fill</code>, <code>get/putString</code>, and
 *       <code>get/putAsciiInt()</code> enough.</li>
 *   <li>It is hard to distinguish if the buffer is created from MINA buffer
 *       pool or not.  MINA have to return used buffers back to pool.</li>
 *   <li>It is difficult to write variable-length data due to its fixed
 *       capacity</li>
 * </ul>
 * 
 * <h2>Allocation</h2>
 * <p>
 * You can get a heap buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024, false);
 * </pre>
 * you can also get a direct buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024, true);
 * </pre>
 * or you can let MINA choose:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024);
 * </pre>
 * 
 * <h2>Acquire/Release</h2>
 * <p>
 * <b>Please note that you never need to release the allocated buffer</b>
 * because MINA will release it automatically when:
 * <ul>
 *   <li>You pass the buffer by calling {@link IoSession#write(ByteBuffer, Object)}.</li>
 *   <li>You pass the buffer by calling {@link ProtocolEncoderOutput#write(ByteBuffer)}.</li>
 * </ul>
 * And, you don't need to release any {@link ByteBuffer} which is passed as a parameter
 * of {@link IoHandler#dataRead(IoSession, ByteBuffer)} method.  They are released
 * automatically when the method returns.
 * <p>
 * You have to release buffers manually by calling {@link #release()} when:
 * <ul>
 *   <li>You allocated a buffer, but didn't pass the buffer to any of two methods above.</li>
 *   <li>You called {@link #acquire()} to prevent the buffer from being released.</li>
 * </ul>
 * 
 * <h2>Wrapping existing NIO buffers and arrays</h2>
 * <p>
 * This class provides a few <tt>wrap(...)</tt> methods that wraps
 * any NIO buffers and byte arrays.  Wrapped MINA buffers are not returned
 * to the buffer pool by default to prevent unexpected memory leakage by default.
 * In case you want to make it pooled, you can call {@link #setPooled(boolean)}
 * with <tt>true</tt> flag to enable pooling.
 *
 * <h2>AutoExpand</h2>
 * <p>
 * Writing variable-length data using NIO <tt>ByteBuffers</tt> is not really
 * easy, and it is because its size is fixed.  MINA <tt>ByteBuffer</tt>
 * introduces <tt>autoExpand</tt> property.  If <tt>autoExpand</tt> property
 * is true, you never get {@link BufferOverflowException} or
 * {@link IndexOutOfBoundsException} (except when index is negative).
 * It automatically expands its capacity and limit value.  For example:
 * <pre>
 * String greeting = messageBundle.getMessage( "hello" );
 * ByteBuffer buf = ByteBuffer.allocate( 16 );
 * // Turn on autoExpand (it is off by default)
 * buf.setAutoExpand( true );
 * buf.putString( greeting, utf8encoder );
 * </pre>
 * NIO <tt>ByteBuffer</tt> is reallocated by MINA <tt>ByteBuffer</tt> behind
 * the scene if the encoded data is larger than 16 bytes.  Its capacity will
 * increase by two times, and its limit will increase to the last position
 * the string is written.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class ByteBuffer
{
    private static final int MINIMUM_CAPACITY = 1;

    private static final Stack containerStack = new Stack();

    private static final Stack[] heapBufferStacks = new Stack[] {
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(), };
    
    private static final Stack[] directBufferStacks = new Stack[] {
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(), };
    
    /**
     * Returns the direct or heap buffer which is capable of the specified
     * size.  This method tries to allocate direct buffer first, and then
     * tries heap buffer if direct buffer memory is exhausted.  Please use
     * {@link #allocate(int, boolean)} to allocate buffers of specific type.
     * 
     * @param capacity the capacity of the buffer
     */
    public static ByteBuffer allocate( int capacity )
    {
        try
        {
            // first try to allocate direct buffer
            return allocate( capacity, true );
        }
        catch( OutOfMemoryError e )
        {
            // if failed, try heap
            return allocate( capacity, false );
        }
    }
    
    /**
     * Returns the buffer which is capable of the specified size.
     * 
     * @param capacity the capacity of the buffer
     * @param direct <tt>true</tt> to get a direct buffer,
     *               <tt>false</tt> to get a heap buffer.
     */
    public static ByteBuffer allocate( int capacity, boolean direct )
    {
        java.nio.ByteBuffer nioBuffer = allocate0( capacity, direct );
        DefaultByteBuffer buf = allocateContainer();
        buf.init( nioBuffer );
        return buf;
    }

    private static DefaultByteBuffer allocateContainer()
    {
        DefaultByteBuffer buf;
        synchronized( containerStack )
        {
            buf = ( DefaultByteBuffer ) containerStack.pop();
        }
        
        if( buf == null )
        {
            buf = new DefaultByteBuffer();
        }
        return buf;
    }
    
    private static java.nio.ByteBuffer allocate0( int capacity, boolean direct )
    {
        Stack[] bufferStacks = direct? directBufferStacks : heapBufferStacks;
        int idx = getBufferStackIndex( bufferStacks, capacity );
        Stack stack = bufferStacks[ idx ];

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
        
        buf.clear();
        buf.order( ByteOrder.BIG_ENDIAN );
        return buf;
    }
    
    private static void release0( java.nio.ByteBuffer buf )
    {
        Stack[] bufferStacks = buf.isDirect()? directBufferStacks : heapBufferStacks;
        Stack stack = bufferStacks[ getBufferStackIndex( bufferStacks, buf.capacity() ) ];
        synchronized( stack )
        {
            // push back
            stack.push( buf );
        }
    }
    
    /**
     * Wraps the specified NIO {@link java.nio.ByteBuffer} into MINA buffer.
     */
    public static ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        DefaultByteBuffer buf = allocateContainer();
        buf.init( nioBuffer );
        buf.setPooled( false );
        return buf;
    }
    
    /**
     * Wraps the specified byte array into MINA heap buffer.
     */
    public static ByteBuffer wrap( byte[] byteArray )
    {
        return wrap( java.nio.ByteBuffer.wrap( byteArray ) );
    }
    
    /**
     * Wraps the specified byte array into MINA heap buffer.
     * Please note that MINA buffers are going to be pooled, and
     * therefore there can be waste of memory if you wrap
     * your byte array specifying <tt>offset</tt> and <tt>length</tt>.
     */
    public static ByteBuffer wrap( byte[] byteArray, int offset, int length )
    {
        return wrap( java.nio.ByteBuffer.wrap( byteArray, offset, length ) );
    }
    
    private static int getBufferStackIndex( Stack[] bufferStacks, int size )
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

    protected ByteBuffer()
    {
    }

    /**
     * Increases the internal reference count of this buffer to defer
     * automatic release.  You have to invoke {@link #release()} as many
     * as you invoked this method to release this buffer.
     * 
     * @throws IllegalStateException if you attempt to acquire already
     *                               released buffer.
     */
    public abstract void acquire();

    /**
     * Releases the specified buffer to buffer pool.
     * 
     * @throws IllegalStateException if you attempt to release already
     *                               released buffer.
     */
    public abstract void release();

    /**
     * Returns the underlying NIO buffer instance.
     */
    public abstract java.nio.ByteBuffer buf();
    
    public abstract boolean isDirect();
    
    public abstract int capacity();
    
    /**
     * Returns <tt>true</tt> if and only if <tt>autoExpand</tt> is turned on.
     */
    public abstract boolean isAutoExpand();
    
    /**
     * Turns on or off <tt>autoExpand</tt>.
     */
    public abstract ByteBuffer setAutoExpand( boolean autoExpand );
    
    /**
     * Returns <tt>true</tt> if and only if this buffer is returned back
     * to the buffer pool when released.
     * <p>
     * The default value of this property is <tt>true</tt> if and only if you
     * allocated this buffer using {@link #allocate(int)} or {@link #allocate(int, boolean)},
     * or <tt>false</tt> otherwise. (i.e. {@link #wrap(byte[])}, {@link #wrap(byte[], int, int)},
     * and {@link #wrap(java.nio.ByteBuffer)})
     */
    public abstract boolean isPooled();

    /**
     * Sets whether this buffer is returned back to the buffer pool when released.
     * <p>
     * The default value of this property is <tt>true</tt> if and only if you
     * allocated this buffer using {@link #allocate(int)} or {@link #allocate(int, boolean)},
     * or <tt>false</tt> otherwise. (i.e. {@link #wrap(byte[])}, {@link #wrap(byte[], int, int)},
     * and {@link #wrap(java.nio.ByteBuffer)})
     */
    public abstract void setPooled( boolean pooled );
    
    public abstract int position();

    public abstract ByteBuffer position( int newPosition );

    public abstract int limit();

    public abstract ByteBuffer limit( int newLimit );

    public abstract ByteBuffer mark();

    public abstract ByteBuffer reset();

    public abstract ByteBuffer clear();

    public abstract ByteBuffer flip();

    public abstract ByteBuffer rewind();

    public abstract int remaining();

    public abstract boolean hasRemaining();

    public abstract byte get();

    public abstract short getUnsigned();

    public abstract ByteBuffer put( byte b );

    public abstract byte get( int index );

    public abstract short getUnsigned( int index );

    public abstract ByteBuffer put( int index, byte b );

    public abstract ByteBuffer get( byte[] dst, int offset, int length );

    public abstract ByteBuffer get( byte[] dst );

    public abstract ByteBuffer put( java.nio.ByteBuffer src );

    public abstract ByteBuffer put( ByteBuffer src );

    public abstract ByteBuffer put( byte[] src, int offset, int length );

    public abstract ByteBuffer put( byte[] src );

    public abstract ByteBuffer compact();

    public abstract String toString();

    public abstract int hashCode();

    public abstract boolean equals( Object ob );

    public abstract int compareTo( ByteBuffer that );

    public abstract ByteOrder order();

    public abstract ByteBuffer order( ByteOrder bo );

    public abstract char getChar();

    public abstract ByteBuffer putChar( char value );

    public abstract char getChar( int index );

    public abstract ByteBuffer putChar( int index, char value );

    public abstract CharBuffer asCharBuffer();

    public abstract short getShort();

    public abstract int getUnsignedShort();

    public abstract ByteBuffer putShort( short value );

    public abstract short getShort( int index );

    public abstract int getUnsignedShort( int index );

    public abstract ByteBuffer putShort( int index, short value );

    public abstract ShortBuffer asShortBuffer();

    public abstract int getInt();

    public abstract long getUnsignedInt();

    public abstract ByteBuffer putInt( int value );

    public abstract int getInt( int index );

    public abstract long getUnsignedInt( int index );

    public abstract ByteBuffer putInt( int index, int value );

    public abstract IntBuffer asIntBuffer();

    public abstract long getLong();

    public abstract ByteBuffer putLong( long value );

    public abstract long getLong( int index );

    public abstract ByteBuffer putLong( int index, long value );

    public abstract LongBuffer asLongBuffer();

    public abstract float getFloat();

    public abstract ByteBuffer putFloat( float value );

    public abstract float getFloat( int index );

    public abstract ByteBuffer putFloat( int index, float value );

    public abstract FloatBuffer asFloatBuffer();

    public abstract double getDouble();

    public abstract ByteBuffer putDouble( double value );

    public abstract double getDouble( int index );

    public abstract ByteBuffer putDouble( int index, double value );

    public abstract DoubleBuffer asDoubleBuffer();

    /**
     * Returns hexdump of this buffer.
     */
    public abstract String getHexDump();

    ////////////////////////////////
    // String getters and putters //
    ////////////////////////////////

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.  This method reads
     * until the limit of this buffer if no <tt>NUL</tt> is found.
     */
    public abstract String getString( CharsetDecoder decoder ) throws CharacterCodingException;

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     * 
     * @param fieldSize the maximum number of bytes to read
     */
    public abstract String getString( int fieldSize, CharsetDecoder decoder ) throws CharacterCodingException;
    
    /**
     * Writes the content of <code>in</code> into this buffer using the
     * specified <code>encoder</code>.  This method doesn't terminate
     * string with <tt>NUL</tt>.  You have to do it by yourself.
     * 
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public abstract ByteBuffer putString( CharSequence in, CharsetEncoder encoder ) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a 
     * <code>NUL</code>-terminated string using the specified
     * <code>encoder</code>.
     * <p>
     * If the charset name of the encoder is UTF-16, you cannot specify
     * odd <code>fieldSize</code>, and this method will append two
     * <code>NUL</code>s as a terminator.
     * <p>
     * Please note that this method doesn't terminate with <code>NUL</code>
     * if the input string is longer than <tt>fieldSize</tt>.
     * 
     * @param fieldSize the maximum number of bytes to write
     */
    public abstract ByteBuffer putString(
            CharSequence in, int fieldSize, CharsetEncoder encoder ) throws CharacterCodingException;

    //////////////////////////
    // Skip or fill methods //
    //////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    public abstract ByteBuffer skip( int size );

    /**
     * Fills this buffer with the specified value.
     * This method moves buffer position forward.
     */
    public abstract ByteBuffer fill( byte value, int size );

    /**
     * Fills this buffer with the specified value.
     * This method does not change buffer position.
     */
    public abstract ByteBuffer fillAndReset( byte value, int size );

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method moves buffer position forward.
     */
    public abstract ByteBuffer fill( int size );

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method does not change buffer position.
     */
    public abstract ByteBuffer fillAndReset( int size );
    
    private static class DefaultByteBuffer extends ByteBuffer
    {
        private java.nio.ByteBuffer buf;
        private int refCount = 1;
        private boolean autoExpand;
        private boolean pooled;

        protected DefaultByteBuffer()
        {
        }

        private synchronized void init( java.nio.ByteBuffer buf )
        {
            this.buf = buf;
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

            if( pooled )
            {
                release0( buf );
            }

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

        public boolean hasRemaining()
        {
            return buf.hasRemaining();
        }

        public byte get()
        {
            return buf.get();
        }

        public short getUnsigned()
        {
            return ( short ) ( get() & 0xff );
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

        public short getUnsigned( int index )
        {
            return ( short ) ( get( index ) & 0xff );
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

        public ByteBuffer get( byte[] dst )
        {
            buf.get( dst );
            return this;
        }

        public ByteBuffer put( java.nio.ByteBuffer src )
        {
            autoExpand( src.remaining() );
            buf.put( src );
            return this;
        }

        public ByteBuffer put( ByteBuffer src )
        {
            autoExpand( src.remaining() );
            buf.put( src.buf() );
            return this;
        }

        public ByteBuffer put( byte[] src, int offset, int length )
        {
            autoExpand( length );
            buf.put( src, offset, length );
            return this;
        }

        public ByteBuffer put( byte[] src )
        {
            autoExpand( src.length );
            buf.put( src );
            return this;
        }

        public ByteBuffer compact()
        {
            buf.compact();
            return this;
        }

        public String toString()
        {
            return buf.toString();
        }

        public int hashCode()
        {
            return buf.hashCode();
        }

        public boolean equals( Object ob )
        {
            if( !( ob instanceof ByteBuffer ) )
                return false;

            ByteBuffer that = ( ByteBuffer ) ob;
            return this.buf.equals( that.buf() );
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

        public int getUnsignedShort()
        {
            return getShort() & 0xffff;
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

        public int getUnsignedShort( int index )
        {
            return getShort( index ) & 0xffff;
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

        public long getUnsignedInt()
        {
            return getInt() & 0xffffffffL;
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

        public long getUnsignedInt( int index )
        {
            return getInt( index ) & 0xffffffffL;
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

        public String getHexDump()
        {
            return ByteBufferHexDumper.getHexdump( this );
        }

        public String getString( CharsetDecoder decoder ) throws CharacterCodingException
        {
            if( !buf.hasRemaining() )
            {
                return "";
            }

            boolean utf16 = decoder.charset().name().startsWith( "UTF-16" );

            int oldPos = buf.position();
            int oldLimit = buf.limit();
            int end;

            if( !utf16 )
            {
                while( buf.hasRemaining() )
                {
                    if( buf.get() == 0 )
                    {
                        break;
                    }
                }

                end = buf.position();
                if( end == oldLimit )
                {
                    buf.limit( end );
                }
                else
                {
                    buf.limit( end - 1 );
                }
            }
            else
            {
                while( buf.remaining() >= 2 )
                {
                    if( ( buf.get() == 0 ) && ( buf.get() == 0 ) )
                    {
                        break;
                    }
                }

                end = buf.position();
                if( end == oldLimit || end == oldLimit - 1 )
                {
                    buf.limit( end );
                }
                else
                {
                    buf.limit( end - 2 );
                }
            }

            buf.position( oldPos );
            if( !buf.hasRemaining() )
            {
                return "";
            }
            decoder.reset();

            int expectedLength = (int) ( buf.remaining() * decoder.averageCharsPerByte() );
            CharBuffer out = CharBuffer.allocate( expectedLength );
            for( ;; )
            {
                CoderResult cr;
                if ( buf.hasRemaining() )
                {
                    cr = decoder.decode( buf, out, true );
                }
                else
                {
                    cr = decoder.flush( out );
                }
                
                if ( cr.isUnderflow() )
                {
                    break;
                }
                
                if ( cr.isOverflow() )
                {
                    CharBuffer o = CharBuffer.allocate( out.capacity() + expectedLength );
                    out.flip();
                    o.put(out);
                    out = o;
                    continue;
                }

                cr.throwException();
            }
            
            buf.limit( oldLimit );
            buf.position( end );
            return out.flip().toString();
        }
        
        public String getString( int fieldSize, CharsetDecoder decoder ) throws CharacterCodingException
        {
            checkFieldSize( fieldSize );

            if( fieldSize == 0 )
            {
                return "";
            }

            if( !buf.hasRemaining() )
            {
                return "";
            }

            boolean utf16 = decoder.charset().name().startsWith( "UTF-16" );

            if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
            {
                throw new IllegalArgumentException( "fieldSize is not even." );
            }

            int i;
            int oldPos = buf.position();
            int oldLimit = buf.limit();
            int end = buf.position() + fieldSize;

            if( oldLimit < end )
            {
                throw new BufferUnderflowException();
            }

            if( !utf16 )
            {
                for( i = 0; i < fieldSize; i ++ )
                {
                    if( buf.get() == 0 )
                    {
                        break;
                    }
                }

                if( i == fieldSize )
                {
                    buf.limit( end );
                }
                else
                {
                    buf.limit( buf.position() - 1 );
                }
            }
            else
            {
                for( i = 0; i < fieldSize; i += 2 )
                {
                    if( ( buf.get() == 0 ) && ( buf.get() == 0 ) )
                    {
                        break;
                    }
                }

                if( i == fieldSize )
                {
                    buf.limit( end );
                }
                else
                {
                    buf.limit( buf.position() - 2 );
                }
            }

            buf.position( oldPos );
            if( !buf.hasRemaining() )
            {
                return "";
            }
            decoder.reset();

            int expectedLength = (int) ( buf.remaining() * decoder.averageCharsPerByte() );
            CharBuffer out = CharBuffer.allocate( expectedLength );
            for( ;; )
            {
                CoderResult cr;
                if ( buf.hasRemaining() )
                {
                    cr = decoder.decode( buf, out, true );
                }
                else
                {
                    cr = decoder.flush( out );
                }
                
                if ( cr.isUnderflow() )
                {
                    break;
                }
                
                if ( cr.isOverflow() )
                {
                    CharBuffer o = CharBuffer.allocate( out.capacity() + expectedLength );
                    out.flip();
                    o.put(out);
                    out = o;
                    continue;
                }

                cr.throwException();
            }
            
            buf.limit( oldLimit );
            buf.position( end );
            return out.flip().toString();
        }

        public ByteBuffer putString(
                CharSequence val, int fieldSize, CharsetEncoder encoder ) throws CharacterCodingException
        {
            checkFieldSize( fieldSize );

            if( fieldSize == 0 )
                return this;
            
            autoExpand( fieldSize );
            
            boolean utf16 = encoder.charset().name().startsWith( "UTF-16" );

            if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
            {
                throw new IllegalArgumentException( "fieldSize is not even." );
            }

            int oldLimit = buf.limit();
            int end = buf.position() + fieldSize;

            if( oldLimit < end )
            {
                throw new BufferOverflowException();
            }

            if( val.length() == 0 )
            {
                if( !utf16 )
                {
                    buf.put( ( byte ) 0x00 );
                }
                else
                {
                    buf.put( ( byte ) 0x00 );
                    buf.put( ( byte ) 0x00 );
                }
                buf.position( end );
                return this;
            }
            
            CharBuffer in = CharBuffer.wrap( val ); 
            buf.limit( end );
            encoder.reset();

            for (;;) {
                CoderResult cr;
                if( in.hasRemaining() )
                {
                    cr = encoder.encode( in, buf(), true );
                }
                else
                {
                    cr = encoder.flush( buf() );
                }
                
                if( cr.isUnderflow() || cr.isOverflow() )
                {
                    break;
                }
                cr.throwException();
            }

            buf.limit( oldLimit );

            if( buf.position() < end )
            {
                if( !utf16 )
                {
                    buf.put( ( byte ) 0x00 );
                }
                else
                {
                    buf.put( ( byte ) 0x00 );
                    buf.put( ( byte ) 0x00 );
                }
            }

            buf.position( end );
            return this;
        }

        public ByteBuffer putString(
                CharSequence val, CharsetEncoder encoder ) throws CharacterCodingException
        {
            if( val.length() == 0 )
            {
                return this;
            }
            
            CharBuffer in = CharBuffer.wrap( val ); 
            int expectedLength = (int) (in.remaining() * encoder.averageBytesPerChar());

            encoder.reset();

            for (;;) {
                CoderResult cr;
                if( in.hasRemaining() )
                {
                    cr = encoder.encode( in, buf(), true );
                }
                else
                {
                    cr = encoder.flush( buf() );
                }
                
                if( cr.isUnderflow() )
                {
                    break;
                }
                if( cr.isOverflow() && autoExpand )
                {
                    autoExpand( expectedLength );
                    continue;
                }
                cr.throwException();
            }
            return this;
        }
        
        public ByteBuffer skip( int size )
        {
            autoExpand( size );
            return position( position() + size );
        }

        public ByteBuffer fill( byte value, int size )
        {
            autoExpand( size );
            int q = size >>> 3;
            int r = size & 7;

            if( q > 0 )
            {
                int intValue = value | ( value << 8 ) | ( value << 16 )
                               | ( value << 24 );
                long longValue = intValue;
                longValue <<= 32;
                longValue |= intValue;

                for( int i = q; i > 0; i -- )
                {
                    buf.putLong( longValue );
                }
            }

            q = r >>> 2;
            r = r & 3;

            if( q > 0 )
            {
                int intValue = value | ( value << 8 ) | ( value << 16 )
                               | ( value << 24 );
                buf.putInt( intValue );
            }

            q = r >> 1;
            r = r & 1;

            if( q > 0 )
            {
                short shortValue = ( short ) ( value | ( value << 8 ) );
                buf.putShort( shortValue );
            }

            if( r > 0 )
            {
                buf.put( value );
            }

            return this;
        }

        public ByteBuffer fillAndReset( byte value, int size )
        {
            autoExpand( size );
            int pos = buf.position();
            try
            {
                fill( value, size );
            }
            finally
            {
                buf.position( pos );
            }
            return this;
        }

        public ByteBuffer fill( int size )
        {
            autoExpand( size );
            int q = size >>> 3;
            int r = size & 7;

            for( int i = q; i > 0; i -- )
            {
                buf.putLong( 0L );
            }

            q = r >>> 2;
            r = r & 3;

            if( q > 0 )
            {
                buf.putInt( 0 );
            }

            q = r >> 1;
            r = r & 1;

            if( q > 0 )
            {
                buf.putShort( ( short ) 0 );
            }

            if( r > 0 )
            {
                buf.put( ( byte ) 0 );
            }

            return this;
        }

        public ByteBuffer fillAndReset( int size )
        {
            autoExpand( size );
            int pos = buf.position();
            try
            {
                fill( size );
            }
            finally
            {
                buf.position( pos );
            }

            return this;
        }

        private void autoExpand( int delta )
        {
            if( autoExpand )
            {
                int pos = buf.position();
                int limit = buf.limit();
                int end = pos + delta;
                if( end > limit ) {
                    ensureCapacity( end );
                    buf.limit( end );
                }
            }
        }
        
        private void autoExpand( int pos, int delta )
        {
            if( autoExpand )
            {
                int limit = buf.limit();
                int end = pos + delta;
                if( end > limit ) {
                    ensureCapacity( end ); // expand by 50%
                    buf.limit( end );
                }
            }
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
        
        private static void checkFieldSize( int fieldSize )
        {
            if( fieldSize < 0 )
            {
                throw new IllegalArgumentException(
                        "fieldSize cannot be negative: " + fieldSize );
            }
        }
        
    }
}
