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

import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

/**
 * A {@link ByteBuffer} that wraps a buffer and proxies any operations to it.
 * <p>
 * You can think this class like a {@link FilterOutputStream}.  All operations
 * are proxied by default so that you can extend this class and override existing
 * operations selectively.  You can introduce new operations, too.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ByteBufferProxy extends ByteBuffer
{

    /**
     * The buffer proxied by this proxy.
     */
    protected ByteBuffer buf;

    /**
     * Create a new instance.
     * @param buf the buffer to be proxied
     */
    protected ByteBufferProxy( ByteBuffer buf )
    {
        if( buf == null )
        {
            throw new NullPointerException( "buf" );
        }
        this.buf = buf;
    }

    @Override
    public boolean isDirect()
    {
        return buf.isDirect();
    }
    
    @Override
    public java.nio.ByteBuffer buf()
    {
        return buf.buf();
    }

    @Override
    public int capacity()
    {
        return buf.capacity();
    }

    @Override
    public int position()
    {
        return buf.position();
    }

    @Override
    public ByteBuffer position( int newPosition )
    {
        buf.position( newPosition );
        return this;
    }

    @Override
    public int limit()
    {
        return buf.limit();
    }

    @Override
    public ByteBuffer limit( int newLimit )
    {
        buf.limit( newLimit );
        return this;
    }

    @Override
    public ByteBuffer mark()
    {
        buf.mark();
        return this;
    }

    @Override
    public ByteBuffer reset()
    {
        buf.reset();
        return this;
    }

    @Override
    public ByteBuffer clear()
    {
        buf.clear();
        return this;
    }

    @Override
    public ByteBuffer sweep()
    {
        buf.sweep();
        return this;
    }
    
    @Override
    public ByteBuffer sweep( byte value )
    {
        buf.sweep( value );
        return this;
    }

    @Override
    public ByteBuffer flip()
    {
        buf.flip();
        return this;
    }

    @Override
    public ByteBuffer rewind()
    {
        buf.rewind();
        return this;
    }

    @Override
    public int remaining()
    {
        return buf.remaining();
    }

    @Override
    public boolean hasRemaining()
    {
        return buf.hasRemaining();
    }

    @Override
    public byte get()
    {
        return buf.get();
    }

    @Override
    public short getUnsigned()
    {
        return buf.getUnsigned();
    }

    @Override
    public ByteBuffer put( byte b )
    {
        buf.put( b );
        return this;
    }

    @Override
    public byte get( int index )
    {
        return buf.get( index );
    }

    @Override
    public short getUnsigned( int index )
    {
        return buf.getUnsigned( index );
    }

    @Override
    public ByteBuffer put( int index, byte b )
    {
        buf.put( index, b );
        return this;
    }

    @Override
    public ByteBuffer get( byte[] dst, int offset, int length )
    {
        buf.get( dst, offset, length );
        return this;
    }

    @Override
    public ByteBuffer get( byte[] dst )
    {
        buf.get( dst );
        return this;
    }

    @Override
    public ByteBuffer put( ByteBuffer src )
    {
        buf.put( src );
        return this;
    }

    @Override
    public ByteBuffer put( java.nio.ByteBuffer src )
    {
        buf.put( src );
        return this;
    }

    @Override
    public ByteBuffer put( byte[] src, int offset, int length )
    {
        buf.put( src, offset, length );
        return this;
    }

    @Override
    public ByteBuffer put( byte[] src )
    {
        buf.put( src );
        return this;
    }

    @Override
    public ByteBuffer compact()
    {
        buf.compact();
        return this;
    }

    @Override
    public String toString()
    {
        return buf.toString();
    }

    @Override
    public int hashCode()
    {
        return buf.hashCode();
    }

    @Override
    public boolean equals( Object ob )
    {
        return buf.equals( ob );
    }

    @Override
    public int compareTo( Object o )
    {
        return buf.compareTo( o );
    }

    @Override
    public ByteOrder order()
    {
        return buf.order();
    }

    @Override
    public ByteBuffer order( ByteOrder bo )
    {
        buf.order( bo );
        return this;
    }

    @Override
    public char getChar()
    {
        return buf.getChar();
    }

    @Override
    public ByteBuffer putChar( char value )
    {
        buf.putChar( value );
        return this;
    }

    @Override
    public char getChar( int index )
    {
        return buf.getChar( index );
    }

    @Override
    public ByteBuffer putChar( int index, char value )
    {
        buf.putChar( index, value );
        return this;
    }

    @Override
    public CharBuffer asCharBuffer()
    {
        return buf.asCharBuffer();
    }

    @Override
    public short getShort()
    {
        return buf.getShort();
    }

    @Override
    public int getUnsignedShort()
    {
        return buf.getUnsignedShort();
    }

    @Override
    public ByteBuffer putShort( short value )
    {
        buf.putShort( value );
        return this;
    }

    @Override
    public short getShort( int index )
    {
        return buf.getShort( index );
    }

    @Override
    public int getUnsignedShort( int index )
    {
        return buf.getUnsignedShort( index );
    }

    @Override
    public ByteBuffer putShort( int index, short value )
    {
        buf.putShort( index, value );
        return this;
    }

    @Override
    public ShortBuffer asShortBuffer()
    {
        return buf.asShortBuffer();
    }

    @Override
    public int getInt()
    {
        return buf.getInt();
    }

    @Override
    public long getUnsignedInt()
    {
        return buf.getUnsignedInt();
    }

    @Override
    public ByteBuffer putInt( int value )
    {
        buf.putInt( value );
        return this;
    }

    @Override
    public int getInt( int index )
    {
        return buf.getInt( index );
    }

    @Override
    public long getUnsignedInt( int index )
    {
        return buf.getUnsignedInt( index );
    }

    @Override
    public ByteBuffer putInt( int index, int value )
    {
        buf.putInt( index, value );
        return this;
    }

    @Override
    public IntBuffer asIntBuffer()
    {
        return buf.asIntBuffer();
    }

    @Override
    public long getLong()
    {
        return buf.getLong();
    }

    @Override
    public ByteBuffer putLong( long value )
    {
        buf.putLong( value );
        return this;
    }

    @Override
    public long getLong( int index )
    {
        return buf.getLong( index );
    }

    @Override
    public ByteBuffer putLong( int index, long value )
    {
        buf.putLong( index, value );
        return this;
    }

    @Override
    public LongBuffer asLongBuffer()
    {
        return buf.asLongBuffer();
    }

    @Override
    public float getFloat()
    {
        return buf.getFloat();
    }

    @Override
    public ByteBuffer putFloat( float value )
    {
        buf.putFloat( value );
        return this;
    }

    @Override
    public float getFloat( int index )
    {
        return buf.getFloat( index );
    }

    @Override
    public ByteBuffer putFloat( int index, float value )
    {
        buf.putFloat( index, value );
        return this;
    }

    @Override
    public FloatBuffer asFloatBuffer()
    {
        return buf.asFloatBuffer();
    }

    @Override
    public double getDouble()
    {
        return buf.getDouble();
    }

    @Override
    public ByteBuffer putDouble( double value )
    {
        buf.putDouble( value );
        return this;
    }

    @Override
    public double getDouble( int index )
    {
        return buf.getDouble( index );
    }

    @Override
    public ByteBuffer putDouble( int index, double value )
    {
        buf.putDouble( index, value );
        return this;
    }

    @Override
    public DoubleBuffer asDoubleBuffer()
    {
        return buf.asDoubleBuffer();
    }

    @Override
    public String getHexDump()
    {
        return buf.getHexDump();
    }

    @Override
    public String getString( int fieldSize, CharsetDecoder decoder )
            throws CharacterCodingException
    {
        return buf.getString( fieldSize, decoder );
    }

    @Override
    public String getString( CharsetDecoder decoder )
            throws CharacterCodingException
    {
        return buf.getString( decoder );
    }
    
    @Override
    public String getPrefixedString( CharsetDecoder decoder )
            throws CharacterCodingException
    {
        return buf.getPrefixedString( decoder );
    }

    @Override
    public String getPrefixedString( int prefixLength, CharsetDecoder decoder )
            throws CharacterCodingException
    {
        return buf.getPrefixedString( prefixLength, decoder );
    }

    @Override
    public ByteBuffer putString( CharSequence in, int fieldSize,
                                CharsetEncoder encoder )
            throws CharacterCodingException
    {
        buf.putString( in, fieldSize, encoder );
        return this;
    }

    @Override
    public ByteBuffer putString( CharSequence in, CharsetEncoder encoder )
            throws CharacterCodingException
    {
        buf.putString( in, encoder );
        return this;
    }
    
    @Override
    public ByteBuffer putPrefixedString( CharSequence in, CharsetEncoder encoder )
            throws CharacterCodingException
    {
        buf.putPrefixedString( in, encoder );
        return this;
    }

    @Override
    public ByteBuffer putPrefixedString( CharSequence in, int prefixLength, CharsetEncoder encoder ) throws CharacterCodingException
    {
        buf.putPrefixedString( in, prefixLength, encoder );
        return this;
    }

    @Override
    public ByteBuffer putPrefixedString( CharSequence in, int prefixLength, int padding, CharsetEncoder encoder )
            throws CharacterCodingException
    {
        buf.putPrefixedString( in, prefixLength, padding, encoder );
        return this;
    }

    @Override
    public ByteBuffer putPrefixedString( CharSequence in, int prefixLength, int padding, byte padValue, CharsetEncoder encoder )
            throws CharacterCodingException
    {
        buf.putPrefixedString( in, prefixLength, padding, padValue, encoder );
        return this;
    }

    @Override
    public ByteBuffer skip( int size )
    {
        buf.skip( size );
        return this;
    }

    @Override
    public ByteBuffer fill( byte value, int size )
    {
        buf.fill( value, size );
        return this;
    }

    @Override
    public ByteBuffer fillAndReset( byte value, int size )
    {
        buf.fillAndReset( value, size );
        return this;
    }

    @Override
    public ByteBuffer fill( int size )
    {
        buf.fill( size );
        return this;
    }

    @Override
    public ByteBuffer fillAndReset( int size )
    {
        buf.fillAndReset( size );
        return this;
    }

    @Override
    public boolean isAutoExpand()
    {
        return buf.isAutoExpand();
    }

    @Override
    public ByteBuffer setAutoExpand( boolean autoExpand )
    {
        buf.setAutoExpand( autoExpand );
        return this;
    }
    
    @Override
    public ByteBuffer expand( int pos, int expectedRemaining )
    {
        buf.expand( pos, expectedRemaining );
        return this;
    }

    @Override
    public ByteBuffer expand( int expectedRemaining )
    {
        buf.expand( expectedRemaining );
        return this;
    }

    @Override
    public Object getObject() throws ClassNotFoundException
    {
        return buf.getObject();
    }

    @Override
    public Object getObject( ClassLoader classLoader ) throws ClassNotFoundException
    {
        return buf.getObject( classLoader );
    }

    @Override
    public ByteBuffer putObject( Object o )
    {
        buf.putObject( o );
        return this;
    }
    
    @Override
    public InputStream asInputStream()
    {
        return buf.asInputStream();
    }
    
    @Override
    public OutputStream asOutputStream()
    {
        return buf.asOutputStream();
    }

    @Override
    public ByteBuffer duplicate()
    {
        return buf.duplicate();
    }

    @Override
    public ByteBuffer slice()
    {
        return buf.slice();
    }

    @Override
    public ByteBuffer asReadOnlyBuffer()
    {
        return buf.asReadOnlyBuffer();
    }

    @Override
    public byte[] array()
    {
        return buf.array();
    }

    @Override
    public int arrayOffset()
    {
        return buf.arrayOffset();
    }

    public ByteBuffer capacity( int newCapacity )
    {
        buf.capacity( newCapacity );
        return this;
    }

    public boolean isReadOnly()
    {
        return buf.isReadOnly();
    }

    public int markValue()
    {
        return buf.markValue();
    }

    @Override
    public boolean hasArray()
    {
        return buf.hasArray();
    }
}
