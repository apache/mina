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


import org.apache.mina.core.buffer.IoBuffer;


/**
 * Provides restricted, relative, write-only access to the bytes in a
 * <code>CompositeByteArray</code>.
 *
 * Using this interface has the advantage that it can be automatically
 * determined when a component <code>ByteArray</code> can no longer be written
 * to, and thus components can be automatically flushed. This makes it easier to
 * use pooling for underlying <code>ByteArray</code>s.
 *
 * By providing an appropriate <code>Expander</code> it is also possible to
 * automatically add more backing storage as more data is written.
 *<br/><br/>
 * TODO: Get flushing working.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CompositeByteArrayRelativeWriter extends CompositeByteArrayRelativeBase implements IoRelativeWriter
{

    /**
     * An object that knows how to expand a <code>CompositeByteArray</code>.
     */
    public interface Expander
    {
        void expand( CompositeByteArray cba, int minSize );
    }

    /**
     * No-op expander.  The overridden method does nothing.
     * 
     */
    public static class NopExpander implements Expander
    {
        public void expand( CompositeByteArray cba, int minSize )
        {
            // Do nothing.
        }
    }

    /**
     * Expands the supplied {@link CompositeByteArray} by the number of
     * bytes provided in the constructor
     * 
     */
    public static class ChunkedExpander implements Expander
    {

        private final ByteArrayFactory baf;

        private final int newComponentSize;


        public ChunkedExpander( ByteArrayFactory baf, int newComponentSize )
        {
            this.baf = baf;
            this.newComponentSize = newComponentSize;
        }


        public void expand( CompositeByteArray cba, int minSize )
        {
            int remaining = minSize;
            while ( remaining > 0 )
            {
                ByteArray component = baf.create( newComponentSize );
                cba.addLast( component );
                remaining -= newComponentSize;
            }
        }

    }

    /**
     * An object that knows how to flush a <code>ByteArray</code>.
     */
    public interface Flusher
    {
        // document free() behaviour
        void flush( ByteArray ba );
    }

    /**
     * The expander to use when the array underflows.
     */
    private final Expander expander;

    /**
     * The flusher to use when flushing component <code>ByteArray</code>s.
     */
    private final Flusher flusher;

    /**
     * Whether or not to automatically flush a component once the cursor moves
     * past it.
     */
    private final boolean autoFlush;


    /**
     * 
     * Creates a new instance of CompositeByteArrayRelativeWriter.
     *
     * @param cba
     *  The CompositeByteArray to use to back this class
     * @param expander
     *  The expander.  Will increase the size of the internal ByteArray
     * @param flusher
     *  Flushed the ByteArray when necessary
     * @param autoFlush
     *  Should this class automatically flush?
     */
    public CompositeByteArrayRelativeWriter( CompositeByteArray cba, Expander expander, Flusher flusher,
        boolean autoFlush )
    {
        super( cba );
        this.expander = expander;
        this.flusher = flusher;
        this.autoFlush = autoFlush;
    }


    private void prepareForAccess( int size )
    {
        int underflow = cursor.getIndex() + size - last();
        if ( underflow > 0 )
        {
            expander.expand( cba, underflow );
        }
    }


    /**
     * Flush to the current index.
     */
    public void flush()
    {
        flushTo( cursor.getIndex() );
    }


    /**
     * Flush to the given index.
     */
    public void flushTo( int index )
    {
        ByteArray removed = cba.removeTo( index );
        flusher.flush( removed );
    }


    /**
     * @inheritDoc
     */
    public void skip( int length )
    {
        cursor.skip( length );
    }


    @Override
    protected void cursorPassedFirstComponent()
    {
        if ( autoFlush )
        {
            flushTo( cba.first() + cba.getFirst().length() );
        }
    }


    /**
     * @inheritDoc
     */
    public void put( byte b )
    {
        prepareForAccess( 1 );
        cursor.put( b );
    }


    /**
     * @inheritDoc
     */
    public void put( IoBuffer bb )
    {
        prepareForAccess( bb.remaining() );
        cursor.put( bb );
    }


    /**
     * @inheritDoc
     */
    public void putShort( short s )
    {
        prepareForAccess( 2 );
        cursor.putShort( s );
    }


    /**
     * @inheritDoc
     */
    public void putInt( int i )
    {
        prepareForAccess( 4 );
        cursor.putInt( i );
    }


    /**
     * @inheritDoc
     */
    public void putLong( long l )
    {
        prepareForAccess( 8 );
        cursor.putLong( l );
    }


    /**
     * @inheritDoc
     */
    public void putFloat( float f )
    {
        prepareForAccess( 4 );
        cursor.putFloat( f );
    }


    /**
     * @inheritDoc
     */
    public void putDouble( double d )
    {
        prepareForAccess( 8 );
        cursor.putDouble( d );
    }


    /**
     * @inheritDoc
     */
    public void putChar( char c )
    {
        prepareForAccess( 2 );
        cursor.putChar( c );
    }
}
