/*
 * @(#) $Id$
 */
package org.apache.mina.common;

import java.nio.BufferOverflowException;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

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
 * </ul>
 * <p>
 * You can get a heap buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024);
 * </pre>
 * or you can get a direct buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024, false);
 * </pre>
 * <p>
 * <b>Please note that you never need to release the allocated buffer because
 * MINA will release it automatically.</b>  But, if you didn't pass it to MINA
 * or called {@link #acquire()} by yourself, you will have to release it manually:
 * <pre>
 * ByteBuffer.release(buf);
 * </pre>
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public final class ByteBuffer
{
    private static final Stack[] heapBufferStacks = new Stack[] {
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), };

    private static final Stack[] directBufferStacks = new Stack[] {
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), new Stack(), new Stack(), new Stack(),
            new Stack(), };
    
    /**
     * Returns the direct buffer which is capable of the specified size.
     * 
     * @param capacity the capacity of the buffer
     */
    public static ByteBuffer allocate( int capacity )
    {
        return allocate( capacity, true );
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
        Stack[] bufferStacks = direct? directBufferStacks : heapBufferStacks;
        int idx = getBufferStackIndex( bufferStacks, capacity );
        Stack stack = bufferStacks[ idx ];

        ByteBuffer buf;
        synchronized( stack )
        {
            buf = ( ByteBuffer ) stack.pop();
            if( buf == null )
            {
                buf = new ByteBuffer( 16 << idx, direct );
            }
        }

        buf.clear();
        synchronized( buf )
        {
            buf.refCount = 1;
        }

        return buf;
    }
    
    /**
     * Wraps the specified NIO {@link java.nio.ByteBuffer} into MINA buffer.
     */
    public static ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        return new ByteBuffer( nioBuffer );
    }
    
    private static int getBufferStackIndex( Stack[] bufferStacks, int size )
    {
        int targetSize = 16;
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

    private final java.nio.ByteBuffer buf;

    private int refCount = 1;
    
    private ByteBuffer( java.nio.ByteBuffer buf )
    {
        if( buf == null )
        {
            throw new NullPointerException( "buf" );
        }
        this.buf = buf;
    }

    private ByteBuffer( int capacity, boolean direct )
    {
        if( direct )
        {
            buf = java.nio.ByteBuffer.allocateDirect( capacity );
        }
        else
        {
            buf = java.nio.ByteBuffer.allocate( capacity );
        }
    }
    
    /**
     * Increases the internal reference count of this buffer to defer
     * automatic release.  You have to invoke {@link #release()} as many
     * as you invoked this method to release this buffer.
     * 
     * @throws IllegalStateException if you attempt to acquire already
     *                               released buffer.
     */
    public synchronized void acquire()
    {
        if( refCount <= 0 )
        {
            throw new IllegalStateException( "Already released buffer." );
        }

        refCount ++;
    }

    /**
     * Releases the specified buffer to buffer pool.
     * 
     * @throws IllegalStateException if you attempt to release already
     *                               released buffer.
     */
    public synchronized void release()
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

        Stack[] bufferStacks = buf.isDirect()? directBufferStacks : heapBufferStacks;
        Stack stack = bufferStacks[ getBufferStackIndex( bufferStacks, buf.capacity() ) ];
        synchronized( stack )
        {
            // push back
            stack.push( this );
        }
    }

    /**
     * Returns the underlying NIO buffer instance.
     */
    public java.nio.ByteBuffer buf()
    {
        return buf;
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
        buf.position( newPosition );
        return this;
    }

    public int limit()
    {
        return buf.limit();
    }

    public ByteBuffer limit( int newLimit )
    {
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

    public java.nio.ByteBuffer slice()
    {
        return buf.slice();
    }

    public java.nio.ByteBuffer duplicate()
    {
        return buf.duplicate();
    }

    public java.nio.ByteBuffer asReadOnlyBuffer()
    {
        return buf.asReadOnlyBuffer();
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
        buf.put( src );
        return this;
    }

    public ByteBuffer put( ByteBuffer src )
    {
        buf.put( src.buf() );
        return this;
    }

    public ByteBuffer put( byte[] src, int offset, int length )
    {
        buf.put( src, offset, length );
        return this;
    }

    public ByteBuffer put( byte[] src )
    {
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
        return this.buf.equals( that.buf );
    }

    public int compareTo( ByteBuffer that )
    {
        return this.buf.compareTo( that.buf );
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
        buf.putChar( value );
        return this;
    }

    public char getChar( int index )
    {
        return buf.getChar( index );
    }

    public ByteBuffer putChar( int index, char value )
    {
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
        buf.putLong( value );
        return this;
    }

    public long getLong( int index )
    {
        return buf.getLong( index );
    }

    public ByteBuffer putLong( int index, long value )
    {
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
        buf.putFloat( value );
        return this;
    }

    public float getFloat( int index )
    {
        return buf.getFloat( index );
    }

    public ByteBuffer putFloat( int index, float value )
    {
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
        buf.putDouble( value );
        return this;
    }

    public double getDouble( int index )
    {
        return buf.getDouble( index );
    }

    public ByteBuffer putDouble( int index, double value )
    {
        buf.putDouble( index, value );
        return this;
    }

    public DoubleBuffer asDoubleBuffer()
    {
        return buf.asDoubleBuffer();
    }

    /**
     * Returns hexdump of this buffer.
     */
    public String getHexDump()
    {
        return ByteBufferHexDumper.getHexdump( this );
    }

    ////////////////////////////////
    // String getters and putters //
    ////////////////////////////////

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer and puts it
     * into <code>out</code> using the specified <code>decoder</code>.
     * 
     * @param fieldSize the maximum number of bytes to read
     */
    public ByteBuffer getString( CharBuffer out, int fieldSize,
                                CharsetDecoder decoder )
    {
        checkFieldSize( fieldSize );

        if( fieldSize == 0 )
            return this;

        boolean utf16 = decoder.charset().name().startsWith( "UTF-16" );

        if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
        {
            throw new IllegalArgumentException( "fieldSize is not even." );
        }

        int i;
        int oldLimit = buf.limit();
        int limit = buf.position() + fieldSize;

        if( oldLimit < limit )
        {
            throw new BufferOverflowException();
        }

        buf.mark();

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
                buf.limit( limit );
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
                buf.limit( limit );
            }
            else
            {
                buf.limit( buf.position() - 2 );
            }
        }

        buf.reset();
        decoder.decode( buf, out, true );
        buf.limit( oldLimit );
        buf.position( limit );
        return this;
    }

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     * 
     * @param fieldSize the maximum number of bytes to read
     */
    public String getString( int fieldSize, CharsetDecoder decoder )
    {
        CharBuffer out = CharBuffer.allocate( ( int ) ( decoder
                .maxCharsPerByte() * fieldSize ) + 1 );
        getString( out, fieldSize, decoder );
        return out.flip().toString();
    }

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
     * if the input string is too long.
     * 
     * @param fieldSize the maximum number of bytes to write
     */
    public ByteBuffer putString( CharBuffer in, int fieldSize,
                                CharsetEncoder encoder )
    {
        checkFieldSize( fieldSize );

        if( fieldSize == 0 )
            return this;

        boolean utf16 = encoder.charset().name().startsWith( "UTF-16" );

        if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
        {
            throw new IllegalArgumentException( "fieldSize is not even." );
        }

        int oldLimit = buf.limit();
        int limit = buf.position() + fieldSize;

        if( oldLimit < limit )
        {
            throw new BufferOverflowException();
        }

        buf.limit( limit );
        encoder.encode( in, buf, true );
        buf.limit( oldLimit );

        if( limit > buf.position() )
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

        buf.position( limit );
        return this;
    }

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
     * if the input string is too long.
     * 
     * @param fieldSize the maximum number of bytes to write
     */
    public ByteBuffer putString( CharSequence in, int fieldSize,
                                CharsetEncoder encoder )
    {
        return putString( CharBuffer.wrap( in ), fieldSize, encoder );
    }

    //////////////////////////
    // Skip or fill methods //
    //////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    public ByteBuffer skip( int size )
    {
        return position( position() + size );
    }

    /**
     * Fills this buffer with the specified value.
     * This method moves buffer position forward.
     */
    public ByteBuffer fill( byte value, int size )
    {
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

    /**
     * Fills this buffer with the specified value.
     * This method does not change buffer position.
     */
    public ByteBuffer fillAndReset( byte value, int size )
    {
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

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method moves buffer position forward.
     */
    public ByteBuffer fill( int size )
    {
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

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method does not change buffer position.
     */
    public ByteBuffer fillAndReset( int size )
    {
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

    private static void checkFieldSize( int fieldSize )
    {
        if( fieldSize < 0 )
        {
            throw new IllegalArgumentException(
                    "fieldSize cannot be negative: " + fieldSize );
        }
    }
}
