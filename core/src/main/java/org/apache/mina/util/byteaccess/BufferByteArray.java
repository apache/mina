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


import java.nio.ByteOrder;
import java.util.Collections;

import org.apache.mina.core.buffer.IoBuffer;


/**
 * A <code>ByteArray</code> backed by a <code>IoBuffer</code>. This class
 * is abstract. Subclasses need to override the <code>free()</code> method. An
 * implementation backed by a heap <code>IoBuffer</code> can be created with
 * a <code>SimpleByteArrayFactory</code>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class BufferByteArray extends AbstractByteArray
{

    /**
     * The backing <code>IoBuffer</code>.
     */
    protected IoBuffer bb;

    /**
     * 
     * Creates a new instance of BufferByteArray and uses the supplied
     * {@link IoBuffer} to back this class
     *
     * @param bb
     *  The backing buffer
     */
    public BufferByteArray( IoBuffer bb )
    {
        this.bb = bb;
    }


    /**
     * @inheritDoc
     */
    public Iterable<IoBuffer> getIoBuffers()
    {
        return Collections.singletonList( bb );
    }


    /**
     * @inheritDoc
     */
    public IoBuffer getSingleIoBuffer()
    {
        return bb;
    }


    /**
     * @inheritDoc
     * 
     * Calling <code>free()</code> on the returned slice has no effect.
     */
    public ByteArray slice( int index, int length )
    {
        int oldLimit = bb.limit();
        bb.position( index );
        bb.limit( index + length );
        IoBuffer slice = bb.slice();
        bb.limit( oldLimit );
        return new BufferByteArray( slice )
        {

            @Override
            public void free()
            {
                // Do nothing.
            }
        };
    }


    /**
     * @inheritDoc
     */
    public abstract void free();


    /**
     * @inheritDoc
     */
    public Cursor cursor()
    {
        return new CursorImpl();
    }


    /**
     * @inheritDoc
     */
    public Cursor cursor( int index )
    {
        return new CursorImpl( index );
    }


    /**
     * @inheritDoc
     */
    public int first()
    {
        return 0;
    }


    /**
     * @inheritDoc
     */
    public int last()
    {
        return bb.limit();
    }


    /**
     * @inheritDoc
     */
    public ByteOrder order()
    {
        return bb.order();
    }


    /**
     * @inheritDoc
     */
    public void order( ByteOrder order )
    {
        bb.order( order );
    }


    /**
     * @inheritDoc
     */
    public byte get( int index )
    {
        return bb.get( index );
    }


    /**
     * @inheritDoc
     */
    public void put( int index, byte b )
    {
        bb.put( index, b );
    }


    /**
     * @inheritDoc
     */
    public void get( int index, IoBuffer other )
    {
        bb.position( index );
        other.put( bb );
    }


    /**
     * @inheritDoc
     */
    public void put( int index, IoBuffer other )
    {
        bb.position( index );
        bb.put( other );
    }


    /**
     * @inheritDoc
     */
    public short getShort( int index )
    {
        return bb.getShort( index );
    }


    /**
     * @inheritDoc
     */
    public void putShort( int index, short s )
    {
        bb.putShort( index, s );
    }


    /**
     * @inheritDoc
     */
    public int getInt( int index )
    {
        return bb.getInt( index );
    }


    /**
     * @inheritDoc
     */
    public void putInt( int index, int i )
    {
        bb.putInt( index, i );
    }


    /**
     * @inheritDoc
     */
    public long getLong( int index )
    {
        return bb.getLong( index );
    }


    /**
     * @inheritDoc
     */
    public void putLong( int index, long l )
    {
        bb.putLong( index, l );
    }


    /**
     * @inheritDoc
     */
    public float getFloat( int index )
    {
        return bb.getFloat( index );
    }


    /**
     * @inheritDoc
     */
    public void putFloat( int index, float f )
    {
        bb.putFloat( index, f );
    }


    /**
     * @inheritDoc
     */
    public double getDouble( int index )
    {
        return bb.getDouble( index );
    }


    /**
     * @inheritDoc
     */
    public void putDouble( int index, double d )
    {
        bb.putDouble( index, d );
    }


    /**
     * @inheritDoc
     */
    public char getChar( int index )
    {
        return bb.getChar( index );
    }


    /**
     * @inheritDoc
     */
    public void putChar( int index, char c )
    {
        bb.putChar( index, c );
    }

    private class CursorImpl implements Cursor
    {

        private int index;


        public CursorImpl()
        {
            // This space intentionally blank.
        }


        public CursorImpl( int index )
        {
            setIndex( index );
        }


        /**
         * @inheritDoc
         */
        public int getRemaining()
        {
            return last() - index;
        }


        /**
         * @inheritDoc
         */
        public boolean hasRemaining()
        {
            return getRemaining() > 0;
        }


        /**
         * @inheritDoc
         */
        public int getIndex()
        {
            return index;
        }


        /**
         * @inheritDoc
         */
        public void setIndex( int index )
        {
            if ( index < 0 || index > last() )
            {
                throw new IndexOutOfBoundsException();
            }
            this.index = index;
        }


        public void skip( int length )
        {
            setIndex( index + length );
        }


        public ByteArray slice( int length )
        {
            ByteArray slice = BufferByteArray.this.slice( index, length );
            index += length;
            return slice;
        }


        /**
         * @inheritDoc
         */
        public ByteOrder order()
        {
            return BufferByteArray.this.order();
        }


        /**
         * @inheritDoc
         */
        public byte get()
        {
            byte b = BufferByteArray.this.get( index );
            index += 1;
            return b;
        }


        /**
         * @inheritDoc
         */
        public void put( byte b )
        {
            BufferByteArray.this.put( index, b );
            index += 1;
        }


        /**
         * @inheritDoc
         */
        public void get( IoBuffer bb )
        {
            int size = Math.min( getRemaining(), bb.remaining() );
            BufferByteArray.this.get( index, bb );
            index += size;
        }


        /**
         * @inheritDoc
         */
        public void put( IoBuffer bb )
        {
            int size = bb.remaining();
            BufferByteArray.this.put( index, bb );
            index += size;
        }


        /**
         * @inheritDoc
         */
        public short getShort()
        {
            short s = BufferByteArray.this.getShort( index );
            index += 2;
            return s;
        }


        /**
         * @inheritDoc
         */
        public void putShort( short s )
        {
            BufferByteArray.this.putShort( index, s );
            index += 2;
        }


        /**
         * @inheritDoc
         */
        public int getInt()
        {
            int i = BufferByteArray.this.getInt( index );
            index += 4;
            return i;
        }


        /**
         * @inheritDoc
         */
        public void putInt( int i )
        {
            BufferByteArray.this.putInt( index, i );
            index += 4;
        }


        /**
         * @inheritDoc
         */
        public long getLong()
        {
            long l = BufferByteArray.this.getLong( index );
            index += 8;
            return l;
        }


        /**
         * @inheritDoc
         */
        public void putLong( long l )
        {
            BufferByteArray.this.putLong( index, l );
            index += 8;
        }


        /**
         * @inheritDoc
         */
        public float getFloat()
        {
            float f = BufferByteArray.this.getFloat( index );
            index += 4;
            return f;
        }


        /**
         * @inheritDoc
         */
        public void putFloat( float f )
        {
            BufferByteArray.this.putFloat( index, f );
            index += 4;
        }


        /**
         * @inheritDoc
         */
        public double getDouble()
        {
            double d = BufferByteArray.this.getDouble( index );
            index += 8;
            return d;
        }


        /**
         * @inheritDoc
         */
        public void putDouble( double d )
        {
            BufferByteArray.this.putDouble( index, d );
            index += 8;
        }


        /**
         * @inheritDoc
         */
        public char getChar()
        {
            char c = BufferByteArray.this.getChar( index );
            index += 2;
            return c;
        }


        /**
         * @inheritDoc
         */
        public void putChar( char c )
        {
            BufferByteArray.this.putChar( index, c );
            index += 2;
        }
    }
}
