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

import junit.framework.Assert;
import junit.framework.TestCase;

import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests {@link ByteBuffer}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ByteBufferTest extends TestCase
{

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( ByteBufferTest.class );
    }

    protected void setUp() throws Exception
    {
    }

    protected void tearDown() throws Exception
    {
    }

    public void testAllocate() throws Exception
    {
        for( int i = 10; i < 1048576 * 2; i = i * 11 / 10 ) // increase by 10%
        {
            ByteBuffer buf = ByteBuffer.allocate( i );
            Assert.assertEquals( 0, buf.position() );
            Assert.assertEquals( buf.capacity(), buf.remaining() );
            Assert.assertTrue( buf.capacity() >= i );
            Assert.assertTrue( buf.capacity() < i * 2 );
        }
    }

    public void testRelease() throws Exception
    {
        for( int i = 10; i < 1048576 * 2; i = i * 11 / 10 ) // increase by 10%
        {
            ByteBuffer buf = ByteBuffer.allocate( i );
            Assert.assertEquals( 0, buf.position() );
            Assert.assertEquals( buf.capacity(), buf.remaining() );
            Assert.assertTrue( buf.capacity() >= i );
            Assert.assertTrue( buf.capacity() < i * 2 );
            buf.release();
        }
    }

    public void testLeakageDetection() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 1024 );
        buf.release();
        try
        {
            buf.release();
            Assert.fail( "Releasing a buffer twice should fail." );
        }
        catch( IllegalStateException e )
        {

        }
    }

    public void testAcquireRelease() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 1024 );
        buf.acquire();
        buf.release();
        buf.acquire();
        buf.acquire();
        buf.release();
        buf.release();
        buf.release();
        try
        {
            buf.release();
            Assert.fail( "Releasing a buffer twice should fail." );
        }
        catch( IllegalStateException e )
        {
        }
    }

    public void testAutoExpand() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 1 );

        buf.put( (byte) 0 );
        try
        {
            buf.put( (byte) 0 );
            Assert.fail();
        }
        catch( BufferOverflowException e )
        {
            // ignore
        }

        buf.setAutoExpand( true );
        buf.put( (byte) 0 );
        Assert.assertEquals( 2, buf.position() );
        Assert.assertEquals( 2, buf.limit() );
        Assert.assertEquals( 2, buf.capacity() );

        buf.setAutoExpand( false );
        try
        {
            buf.put( 3, (byte) 0 );
            Assert.fail();
        }
        catch( IndexOutOfBoundsException e )
        {
            // ignore
        }

        buf.setAutoExpand( true );
        buf.put( 3, (byte) 0 );
        Assert.assertEquals( 2, buf.position() );
        Assert.assertEquals( 4, buf.limit() );
        Assert.assertEquals( 4, buf.capacity() );
    }

    public void testPooledProperty() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        java.nio.ByteBuffer nioBuf = buf.buf();
        buf.release();
        buf = ByteBuffer.allocate( 16 );
        Assert.assertSame( nioBuf, buf.buf() );
        buf.setPooled( false );
        buf.release();
        Assert.assertNotSame( nioBuf, ByteBuffer.allocate( 16 ).buf() );
    }

    public void testGetString() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        CharsetDecoder decoder;

        Charset charset = Charset.forName( "UTF-8" );
        buf.clear();
        buf.putString( "hello", charset.newEncoder() );
        buf.put( (byte)0 );
        buf.flip();
        Assert.assertEquals( "hello", buf.getString( charset.newDecoder() ) );

        buf.clear();
        buf.putString( "hello", charset.newEncoder() );
        buf.flip();
        Assert.assertEquals( "hello", buf.getString( charset.newDecoder() ) );

        decoder = Charset.forName( "ISO-8859-1" ).newDecoder();
        buf.clear();
        buf.put( (byte) 'A' );
        buf.put( (byte) 'B' );
        buf.put( (byte) 'C' );
        buf.put( (byte) 0 );

        buf.position( 0 );
        Assert.assertEquals( "ABC", buf.getString( decoder ) );
        Assert.assertEquals( 4, buf.position() );

        buf.position( 0 );
        buf.limit( 1 );
        Assert.assertEquals( "A", buf.getString( decoder ) );
        Assert.assertEquals( 1, buf.position() );

        buf.clear();
        Assert.assertEquals( "ABC", buf.getString( 10, decoder ) );
        Assert.assertEquals( 10, buf.position() );

        buf.clear();
        Assert.assertEquals( "A", buf.getString( 1, decoder ) );
        Assert.assertEquals( 1, buf.position() );

        buf.clear();
        buf.fillAndReset( buf.limit() );
        decoder = Charset.forName( "UTF-16" ).newDecoder();
        buf.put( (byte) 0 );
        buf.put( (byte) 'A' );
        buf.put( (byte) 0 );
        buf.put( (byte) 'B' );
        buf.put( (byte) 0 );
        buf.put( (byte) 'C' );
        buf.put( (byte) 0 );
        buf.put( (byte) 0 );

        buf.position( 0 );
        Assert.assertEquals( "ABC", buf.getString( decoder ) );
        Assert.assertEquals( 8, buf.position() );

        buf.position( 0 );
        buf.limit( 2 );
        Assert.assertEquals( "A", buf.getString( decoder ) );
        Assert.assertEquals( 2, buf.position() );

        buf.position( 0 );
        buf.limit( 3 );
        Assert.assertEquals( "A", buf.getString( decoder ) );
        Assert.assertEquals( 2, buf.position() );

        buf.clear();
        Assert.assertEquals( "ABC", buf.getString( 10, decoder ) );
        Assert.assertEquals( 10, buf.position() );

        buf.clear();
        Assert.assertEquals( "A", buf.getString( 2, decoder ) );
        Assert.assertEquals( 2, buf.position() );

        buf.clear();
        try
        {
            buf.getString( 1, decoder );
            Assert.fail();
        }
        catch( IllegalArgumentException e )
        {
            // ignore
        }

        // Test getting strings from an empty buffer.
        buf.clear();
        buf.limit( 0 );
        Assert.assertEquals( "", buf.getString( decoder ) );
        Assert.assertEquals( "", buf.getString( 2, decoder ) );

        // Test getting strings from non-empty buffer which is filled with 0x00
        buf.clear();
        buf.putInt( 0 );
        buf.clear();
        buf.limit( 4 );
        Assert.assertEquals( "", buf.getString( decoder ) );
        Assert.assertEquals( 2, buf.position() );
        Assert.assertEquals( 4, buf.limit() );

        buf.position( 0 );
        Assert.assertEquals( "", buf.getString( 2, decoder ) );
        Assert.assertEquals( 2, buf.position() );
        Assert.assertEquals( 4, buf.limit() );
    }

    public void testPutString() throws Exception
    {
        CharsetEncoder encoder;
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        encoder = Charset.forName( "ISO-8859-1" ).newEncoder();

        buf.putString( "ABC", encoder );
        Assert.assertEquals( 3, buf.position() );
        buf.clear();
        Assert.assertEquals( 'A', buf.get( 0 ) );
        Assert.assertEquals( 'B', buf.get( 1 ) );
        Assert.assertEquals( 'C', buf.get( 2 ) );

        buf.putString( "D", 5, encoder );
        Assert.assertEquals( 5, buf.position() );
        buf.clear();
        Assert.assertEquals( 'D', buf.get( 0 ) );
        Assert.assertEquals( 0, buf.get( 1 ) );

        buf.putString( "EFG", 2, encoder );
        Assert.assertEquals( 2, buf.position() );
        buf.clear();
        Assert.assertEquals( 'E', buf.get( 0 ) );
        Assert.assertEquals( 'F', buf.get( 1 ) );
        Assert.assertEquals( 'C', buf.get( 2 ) ); // C may not be overwritten

        // UTF-16: We specify byte order to omit BOM.
        encoder = Charset.forName( "UTF-16BE" ).newEncoder();
        buf.clear();

        buf.putString( "ABC", encoder );
        Assert.assertEquals( 6, buf.position() );
        buf.clear();

        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 'A', buf.get( 1 ) );
        Assert.assertEquals( 0, buf.get( 2 ) );
        Assert.assertEquals( 'B', buf.get( 3 ) );
        Assert.assertEquals( 0, buf.get( 4 ) );
        Assert.assertEquals( 'C', buf.get( 5 ) );

        buf.putString( "D", 10, encoder );
        Assert.assertEquals( 10, buf.position() );
        buf.clear();
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 'D', buf.get( 1 ) );
        Assert.assertEquals( 0, buf.get( 2 ) );
        Assert.assertEquals( 0, buf.get( 3 ) );

        buf.putString( "EFG", 4, encoder );
        Assert.assertEquals( 4, buf.position() );
        buf.clear();
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 'E', buf.get( 1 ) );
        Assert.assertEquals( 0, buf.get( 2 ) );
        Assert.assertEquals( 'F', buf.get( 3 ) );
        Assert.assertEquals( 0, buf.get( 4 ) );   // C may not be overwritten
        Assert.assertEquals( 'C', buf.get( 5 ) ); // C may not be overwritten

        // Test putting an emptry string
        buf.putString( "", encoder );
        Assert.assertEquals( 0, buf.position() );
        buf.putString( "", 4, encoder );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 0, buf.get( 1 ) );
    }

    public void testGetPrefixedString() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        CharsetEncoder encoder;
        CharsetDecoder decoder;
        encoder = Charset.forName( "ISO-8859-1" ).newEncoder();
        decoder = Charset.forName( "ISO-8859-1" ).newDecoder();

        buf.putShort( ( short ) 3 );
        buf.putString( "ABCD", encoder );
        buf.clear();
        Assert.assertEquals( "ABC", buf.getPrefixedString( decoder ) );
    }

    public void testPutPrefixedString() throws Exception
    {
        CharsetEncoder encoder;
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        buf.fillAndReset( buf.remaining() );
        encoder = Charset.forName( "ISO-8859-1" ).newEncoder();

        // Without autoExpand
        buf.putPrefixedString( "ABC", encoder );
        Assert.assertEquals( 5, buf.position() );
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 3, buf.get( 1 ) );
        Assert.assertEquals( 'A', buf.get( 2 ) );
        Assert.assertEquals( 'B', buf.get( 3 ) );
        Assert.assertEquals( 'C', buf.get( 4 ) );

        buf.clear();
        try
        {
            buf.putPrefixedString( "123456789012345", encoder );
            Assert.fail();
        }
        catch( BufferOverflowException e )
        {
            // OK
        }

        // With autoExpand
        buf.clear();
        buf.setAutoExpand( true );
        buf.putPrefixedString( "123456789012345", encoder );
        Assert.assertEquals( 17, buf.position() );
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 15, buf.get( 1 ) );
        Assert.assertEquals( '1', buf.get( 2 ) );
        Assert.assertEquals( '2', buf.get( 3 ) );
        Assert.assertEquals( '3', buf.get( 4 ) );
        Assert.assertEquals( '4', buf.get( 5 ) );
        Assert.assertEquals( '5', buf.get( 6 ) );
        Assert.assertEquals( '6', buf.get( 7 ) );
        Assert.assertEquals( '7', buf.get( 8 ) );
        Assert.assertEquals( '8', buf.get( 9 ) );
        Assert.assertEquals( '9', buf.get( 10 ) );
        Assert.assertEquals( '0', buf.get( 11 ) );
        Assert.assertEquals( '1', buf.get( 12 ) );
        Assert.assertEquals( '2', buf.get( 13 ) );
        Assert.assertEquals( '3', buf.get( 14 ) );
        Assert.assertEquals( '4', buf.get( 15 ) );
        Assert.assertEquals( '5', buf.get( 16 ) );
    }

    public void testPutPrefixedStringWithPrefixLength() throws Exception
    {
        CharsetEncoder encoder = Charset.forName( "ISO-8859-1" ).newEncoder();
        ByteBuffer buf = ByteBuffer.allocate( 16 ).sweep().setAutoExpand( true );

        buf.putPrefixedString( "A", 1, encoder );
        Assert.assertEquals( 2, buf.position() );
        Assert.assertEquals( 1, buf.get( 0 ) );
        Assert.assertEquals( 'A', buf.get( 1 ) );

        buf.sweep();
        buf.putPrefixedString( "A", 2, encoder );
        Assert.assertEquals( 3, buf.position() );
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 1, buf.get( 1 ) );
        Assert.assertEquals( 'A', buf.get( 2 ) );

        buf.sweep();
        buf.putPrefixedString( "A", 4, encoder );
        Assert.assertEquals( 5, buf.position() );
        Assert.assertEquals( 0, buf.get( 0 ) );
        Assert.assertEquals( 0, buf.get( 1 ) );
        Assert.assertEquals( 0, buf.get( 2 ) );
        Assert.assertEquals( 1, buf.get( 3 ) );
        Assert.assertEquals( 'A', buf.get( 4 ) );
    }

    public void testPutPrefixedStringWithPadding() throws Exception
    {
        CharsetEncoder encoder = Charset.forName( "ISO-8859-1" ).newEncoder();
        ByteBuffer buf = ByteBuffer.allocate( 16 ).sweep().setAutoExpand( true );

        buf.putPrefixedString( "A", 1, 2, ( byte ) 32, encoder );
        Assert.assertEquals( 3, buf.position() );
        Assert.assertEquals( 2, buf.get( 0 ) );
        Assert.assertEquals( 'A', buf.get( 1 ) );
        Assert.assertEquals( ' ', buf.get( 2 ) );

        buf.sweep();
        buf.putPrefixedString( "A", 1, 4, ( byte ) 32, encoder );
        Assert.assertEquals( 5, buf.position() );
        Assert.assertEquals( 4, buf.get( 0 ) );
        Assert.assertEquals( 'A', buf.get( 1 ) );
        Assert.assertEquals( ' ', buf.get( 2 ) );
        Assert.assertEquals( ' ', buf.get( 3 ) );
        Assert.assertEquals( ' ', buf.get( 4 ) );
    }

    public void testWideUtf8Characters() throws Exception
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                ByteBuffer buffer = ByteBuffer.allocate( 1 );
                buffer.setAutoExpand( true );

                Charset charset = Charset.forName( "UTF-8" );

                CharsetEncoder encoder = charset.newEncoder();

                for( int i = 0; i < 5; i++ )
                {
                    try
                    {
                        buffer.putString( "\u89d2", encoder );
                    }
                    catch( CharacterCodingException e )
                    {
                        fail( e.getMessage() );
                    }
                }
            }
        };

        Thread t = new Thread( r );
        t.setDaemon( true );
        t.start();

        for( int i = 0; i < 50; i ++ )
        {
            Thread.sleep( 100 );
            if( !t.isAlive() )
            {
                break;
            }
        }

        if( t.isAlive() )
        {
            t.interrupt();

            fail( "Went into endless loop trying to encode character");
        }
    }

    public void testObjectSerialization() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        buf.setAutoExpand( true );
        List o = new ArrayList();
        o.add( new Date() );

        // Test writing an object.
        buf.putObject( o );

        // Test reading an object.
        buf.clear();
        Object o2 = buf.getObject();
        Assert.assertEquals( o, o2 );

        // This assertion is just to make sure that deserialization occurred.
        Assert.assertNotSame( o, o2 );
    }

    public void testSweepWithZeros() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 4 );
        buf.putInt( 0xdeadbeef );
        buf.clear();
        Assert.assertEquals( 0xdeadbeef, buf.getInt() );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 4, buf.limit() );

        buf.sweep();
        Assert.assertEquals( 0, buf.position() );
        Assert.assertEquals( 4, buf.limit() );
        Assert.assertEquals( 0x0, buf.getInt() );
    }

    public void testSweepNonZeros() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 4 );
        buf.putInt( 0xdeadbeef );
        buf.clear();
        Assert.assertEquals( 0xdeadbeef, buf.getInt() );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 4, buf.limit() );

        buf.sweep( ( byte ) 0x45 );
        Assert.assertEquals( 0, buf.position() );
        Assert.assertEquals( 4, buf.limit() );
        Assert.assertEquals( 0x45454545, buf.getInt() );
    }

    public void testWrapNioBuffer() throws Exception
    {
        java.nio.ByteBuffer nioBuf = java.nio.ByteBuffer.allocate( 10 );
        nioBuf.position( 3 );
        nioBuf.limit( 7 );

        ByteBuffer buf = ByteBuffer.wrap( nioBuf );
        Assert.assertEquals( 3, buf.position() );
        Assert.assertEquals( 7, buf.limit() );
        Assert.assertEquals( 10, buf.capacity() );
    }

    public void testWrapSubArray() throws Exception
    {
        byte[] array = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        ByteBuffer buf = ByteBuffer.wrap( array, 3, 4 );
        Assert.assertEquals( 3, buf.position() );
        Assert.assertEquals( 7, buf.limit() );
        Assert.assertEquals( 10, buf.capacity() );

        buf.clear();
        Assert.assertEquals( 0, buf.position() );
        Assert.assertEquals( 10, buf.limit() );
        Assert.assertEquals( 10, buf.capacity() );
    }

    public void testPoolExpiration() throws Exception
    {
        PooledByteBufferAllocator allocator =
            ( PooledByteBufferAllocator ) ByteBuffer.getAllocator();

        // Make a buffer pooled.
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        buf.release();

        // Let everything flushed.
        allocator.setTimeout( 1 );
        Thread.sleep( 2000 );

        // Make sure old buffers are flushed.
        Assert.assertNotSame( buf, ByteBuffer.allocate( 16 ) );

        // Make sure new buffers are not flushed.
        allocator.setTimeout( 10 );
        buf = ByteBuffer.allocate( 16 );
        buf.release();
        Thread.sleep( 2000 );
        Assert.assertSame( buf.buf(), ByteBuffer.allocate( 16 ).buf() );

        // Return to the default settings
        allocator.setTimeout( 60 );
    }

    public void testAllocatorDisposal() throws Exception
    {
        PooledByteBufferAllocator allocator =
            ( PooledByteBufferAllocator ) ByteBuffer.getAllocator();

        // dispose() should fail because the allocator is in use.
        try
        {
            allocator.dispose();
            Assert.fail();
        }
        catch( IllegalStateException e )
        {
            // OK
        }

        // Change the allocator.
        ByteBuffer.setAllocator( new PooledByteBufferAllocator() );

        // Dispose the old allocator.
        allocator.dispose();

        // Allocation request to the disposed allocator should fail.
        try
        {
            allocator.allocate( 16, true );
            Assert.fail();
        }
        catch( IllegalStateException e )
        {
            // OK
        }
    }

    public void testDuplicate() throws Exception
    {
        java.nio.ByteBuffer nioBuf;
        ByteBuffer original;
        ByteBuffer duplicate;

        // Test if the buffer is duplicated correctly.
        original = ByteBuffer.allocate( 16 ).sweep();
        nioBuf = original.buf();
        original.position( 4 );
        original.limit( 10 );
        duplicate = original.duplicate();
        original.put( 4, ( byte ) 127 );
        Assert.assertEquals( 4, duplicate.position() );
        Assert.assertEquals( 10, duplicate.limit() );
        Assert.assertEquals( 16, duplicate.capacity() );
        Assert.assertNotSame( original.buf(), duplicate.buf() );
        Assert.assertEquals( 127, duplicate.get( 4 ) );
        original.release();
        duplicate.release();

        //// Check if pooled correctly.
        original = ByteBuffer.allocate( 16 );
        Assert.assertSame( nioBuf, original.buf() );
        original.release();

        // Try to release duplicate first.
        original = ByteBuffer.allocate( 16 );
        duplicate = original.duplicate();
        duplicate.release();
        original.release();

        //// Check if pooled correctly.
        original = ByteBuffer.allocate( 16 );
        Assert.assertSame( nioBuf, original.buf() );
        original.release();

        // Test a duplicate of a duplicate.
        original = ByteBuffer.allocate( 16 );
        duplicate = original.duplicate();
        ByteBuffer anotherDuplicate = duplicate.duplicate();
        anotherDuplicate.release();
        original.release();
        duplicate.release();
        try
        {
            duplicate.release();
            Assert.fail();
        }
        catch( IllegalStateException e )
        {
            // OK
        }
        try
        {
            anotherDuplicate.release();
            Assert.fail();
        }
        catch( IllegalStateException e )
        {
            // OK
        }

        //// Check if pooled correctly.
        original = ByteBuffer.allocate( 16 );
        Assert.assertSame( nioBuf, original.buf() );
        original.release();



        // Try to expand.
        try
        {
            original = ByteBuffer.allocate( 16 );
            duplicate = original.duplicate();
            duplicate.setAutoExpand( true );
            duplicate.putString(
                    "A very very very very looooooong string",
                    Charset.forName( "ISO-8859-1" ).newEncoder() );
            Assert.fail();
        }
        catch( IllegalStateException e )
        {
            // OK
        }
    }

    public void testSlice() throws Exception
    {
        ByteBuffer original;
        ByteBuffer slice;

        // Test if the buffer is sliced correctly.
        original = ByteBuffer.allocate( 16 ).sweep();
        original.position( 4 );
        original.limit( 10 );
        slice = original.slice();
        original.put( 4, ( byte ) 127 );
        Assert.assertEquals( 0, slice.position() );
        Assert.assertEquals( 6, slice.limit() );
        Assert.assertEquals( 6, slice.capacity() );
        Assert.assertNotSame( original.buf(), slice.buf() );
        Assert.assertEquals( 127, slice.get( 0 ) );
        original.release();
        slice.release();
    }

    public void testReadOnlyBuffer() throws Exception
    {
        ByteBuffer original;
        ByteBuffer duplicate;

        // Test if the buffer is duplicated correctly.
        original = ByteBuffer.allocate( 16 ).sweep();
        original.position( 4 );
        original.limit( 10 );
        duplicate = original.asReadOnlyBuffer();
        original.put( 4, ( byte ) 127 );
        Assert.assertEquals( 4, duplicate.position() );
        Assert.assertEquals( 10, duplicate.limit() );
        Assert.assertEquals( 16, duplicate.capacity() );
        Assert.assertNotSame( original.buf(), duplicate.buf() );
        Assert.assertEquals( 127, duplicate.get( 4 ) );
        original.release();
        duplicate.release();

        // Try to expand.
        try
        {
            original = ByteBuffer.allocate( 16 );
            duplicate = original.asReadOnlyBuffer();
            duplicate.putString(
                    "A very very very very looooooong string",
                    Charset.forName( "ISO-8859-1" ).newEncoder() );
            Assert.fail();
        }
        catch( ReadOnlyBufferException e )
        {
            // OK
        }
    }
}
