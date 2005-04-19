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

/**
 * Tests {@link ByteBuffer}.
 * 
 * @author Trustin Lee (trustin@apache.org)
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
    
    public void testFork() throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        ByteBuffer newBuf;
        
        // initialize buf
        for( int i = 0; i < 16; i ++ )
        {
            buf.put( i, (byte) i );
        }

        // without capacity
        buf.position( 4 );
        buf.limit( 8 );
        newBuf = buf.fork();
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 8, buf.limit() );
        Assert.assertEquals( buf.position(), newBuf.position() );
        Assert.assertEquals( buf.limit(), newBuf.limit() );
        buf.limit( buf.capacity() );
        newBuf.limit( newBuf.capacity() );
        
        // with larger capacity
        buf.position( 4 );
        buf.limit( 8 );
        newBuf = buf.fork( 18 );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 8, buf.limit() );
        Assert.assertEquals( buf.position(), newBuf.position() );
        Assert.assertEquals( buf.limit(), newBuf.limit() );
        buf.limit( buf.capacity() );
        newBuf.limit( newBuf.capacity() );
        for( int i = 0; i < 16; i ++ )
        {
            Assert.assertEquals( buf.get( i ), newBuf.get( i ) );
        }
        
        // with smaller capacity
        buf.position( 4 );
        buf.limit( 8 );
        newBuf = buf.fork( 12 );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 8, buf.limit() );
        Assert.assertEquals( buf.position(), newBuf.position() );
        Assert.assertEquals( buf.limit(), newBuf.limit() );
        buf.limit( buf.capacity() );
        newBuf.limit( newBuf.capacity() );
        for( int i = 0; i < 12; i ++ )
        {
            Assert.assertEquals( buf.get( i ), newBuf.get( i ) );
        }
        
        // with more smaller capacity
        buf.position( 4 );
        buf.limit( 8 );
        newBuf = buf.fork( 6 );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 8, buf.limit() );
        Assert.assertEquals( buf.position(), newBuf.position() );
        Assert.assertEquals( 6, newBuf.limit() );
        buf.limit( buf.capacity() );
        newBuf.limit( newBuf.capacity() );
        for( int i = 0; i < 6; i ++ )
        {
            Assert.assertEquals( buf.get( i ), newBuf.get( i ) );
        }
        
        // with smallest capacity
        buf.position( 4 );
        buf.limit( 8 );
        newBuf = buf.fork( 2 );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 8, buf.limit() );
        Assert.assertEquals( 2, newBuf.position() );
        Assert.assertEquals( 2, newBuf.limit() );
        buf.limit( buf.capacity() );
        newBuf.limit( newBuf.capacity() );
        for( int i = 0; i < 2; i ++ )
        {
            Assert.assertEquals( buf.get( i ), newBuf.get( i ) );
        }
    }
    
    public void testDuplication()
    {
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        buf.fillAndReset( buf.remaining() );

        ByteBuffer buf2 = buf.duplicate();
        buf.putInt( 1234 );
        Assert.assertEquals( 4, buf.position() );
        Assert.assertEquals( 0, buf2.position() );
        Assert.assertEquals( 1234, buf2.getInt() );
    }
}
