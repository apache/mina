/*
 * @(#) $Id$
 */
package org.apache.mina.common;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * TODO Document me.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
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
}
