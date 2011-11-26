package org.apache.mina.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.junit.Test;

public class IoBufferTest
{
    /**
     * Test the addition of 3 heap buffers with data
     */
    @Test
    public void testAddHeapBuffers() {
        ByteBuffer bb1 = ByteBuffer.allocate(5);
        bb1.put("012".getBytes());
        bb1.flip();

        ByteBuffer bb2 = ByteBuffer.allocate(5);
        bb2.put("345".getBytes());
        bb2.flip();

        ByteBuffer bb3 = ByteBuffer.allocate(5);
        bb3.put("6789".getBytes());
        bb3.flip();

        IoBuffer ioBuffer = new IoBuffer();
        ioBuffer.add(bb1, bb2).add(bb3);
        
        assertEquals(0, ioBuffer.position());
        assertEquals(10, ioBuffer.limit());
        assertEquals(10, ioBuffer.capacity());
        assertTrue(ioBuffer.hasRemaining());
        
        for (int i=0;i<10;i++) {
            assertTrue(ioBuffer.hasRemaining());
            assertEquals("0123456789".charAt(i), ioBuffer.get());
        }
        
        try {
            assertFalse(ioBuffer.hasRemaining());
            ioBuffer.get();
            fail();
        } catch (BufferUnderflowException bufe) {
            assertTrue(true);
        }
    }
    
    /**
     * Test the addition of 3 heap buffers, one being empty
     */
    @Test
    public void testAddHeapBuffersOneEmpty() {
        ByteBuffer bb1 = ByteBuffer.allocate(5);
        bb1.put("012".getBytes());
        bb1.flip();

        ByteBuffer bb2 = ByteBuffer.allocate(0);

        ByteBuffer bb3 = ByteBuffer.allocate(5);
        bb3.put("3456".getBytes());
        bb3.flip();

        IoBuffer ioBuffer = new IoBuffer();
        ioBuffer.add(bb1, bb2).add(bb3);
        
        assertEquals(0, ioBuffer.position());
        assertEquals(7, ioBuffer.limit());
        assertEquals(7, ioBuffer.capacity());
        
        for (int i=0;i<7;i++) {
            assertTrue(ioBuffer.hasRemaining());
            assertEquals("0123456".charAt(i), ioBuffer.get());
        }
        
        try {
            ioBuffer.get();
            fail();
        } catch (BufferUnderflowException bufe) {
            assertTrue(true);
        }
    }
    
    /**
     * Test the additio of mixed type buffers
     */
    @Test ( expected=RuntimeException.class)
    public void testAddMixedTypeBuffers() {
        ByteBuffer bb1 = ByteBuffer.allocate(5);
        bb1.put("012".getBytes());
        bb1.flip();

        ByteBuffer bb2 = ByteBuffer.allocateDirect(5);
        bb2.put("3456".getBytes());
        bb2.flip();

        IoBuffer ioBuffer = new IoBuffer();
        ioBuffer.add(bb1, bb2);
    }
    
    /**
     * Test the allocation of a new heap IoBuffer with no byte in it
     */
    @Test
    public void testAllocate0()
    {
        IoBuffer ioBuffer = IoBuffer.allocate(0);
        
        assertFalse(ioBuffer.isDirect());
        assertEquals(0, ioBuffer.capacity());
        assertEquals(0, ioBuffer.limit());
        assertEquals(0, ioBuffer.position());
        assertFalse(ioBuffer.hasRemaining());
    }

    /**
     * Test the allocation of a new heap IoBuffer with 1024 bytes
     */
    @Test
    public void testAllocate1024()
    {
        IoBuffer ioBuffer = IoBuffer.allocate(1024);
        
        assertFalse(ioBuffer.isDirect());
        assertEquals(1024, ioBuffer.capacity());
        assertEquals(1024, ioBuffer.limit());
        assertEquals(0, ioBuffer.position());
        assertTrue(ioBuffer.hasRemaining());
    }
    
    /**
     * Test the allocation of a new direct IoBuffer with no byte in it
     */
    @Test
    public void testAllocateDirect0()
    {
        IoBuffer ioBuffer = IoBuffer.allocateDirect(0);
        
        assertTrue(ioBuffer.isDirect());
        assertEquals(0, ioBuffer.capacity());
        assertEquals(0, ioBuffer.limit());
        assertEquals(0, ioBuffer.position());
        assertFalse(ioBuffer.hasRemaining());
    }

    /**
     * Test the allocation of a new direct IoBuffer with 1024 bytes
     */
    @Test
    public void testAllocateDirect1024()
    {
        IoBuffer ioBuffer = IoBuffer.allocateDirect(1024);
        
        assertTrue(ioBuffer.isDirect());
        assertEquals(1024, ioBuffer.capacity());
        assertEquals(1024, ioBuffer.limit());
        assertEquals(0, ioBuffer.position());
        assertTrue(ioBuffer.hasRemaining());
    }
    
    /**
     * Test the get() method on a IoBuffer containing one ByteBuffer with 3 bytes
     */
    @Test
    public void testGetOneBuffer3Bytes() {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.put("012".getBytes());
        bb.flip();
        
        IoBuffer ioBuffer = new IoBuffer(bb);
        assertEquals(0, ioBuffer.position());
        assertEquals(3, ioBuffer.limit());

        assertTrue(ioBuffer.hasRemaining());
        assertEquals('0', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('1', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('2', ioBuffer.get());
        
        try {
            assertFalse(ioBuffer.hasRemaining());
            ioBuffer.get();
            fail();
        } catch (BufferUnderflowException bufe) {
            // expected
            assertEquals(3, ioBuffer.position());
        }
    }
    
    /**
     * Test the get() method on a IoBuffer containing one ByteBuffer with 0 bytes
     */
    @Test
    public void testGetOneBuffer0Bytes() {
        ByteBuffer bb = ByteBuffer.allocate(0);
        
        IoBuffer ioBuffer = new IoBuffer(bb);
        assertEquals(0, ioBuffer.position());
        assertEquals(0, ioBuffer.limit());

        try {
            assertFalse(ioBuffer.hasRemaining());
            ioBuffer.get();
            fail();
        } catch (BufferUnderflowException bufe) {
            // expected
            assertEquals(0, ioBuffer.position());
        }
    }
    
    /**
     * Test the get() method on a IoBuffer containing two ByteBuffer with 3 bytes
     */
    @Test
    public void testGetTwoBuffer3Bytes() {
        ByteBuffer bb1 = ByteBuffer.allocate(5);
        bb1.put("012".getBytes());
        bb1.flip();

        ByteBuffer bb2 = ByteBuffer.allocate(5);
        bb2.put("345".getBytes());
        bb2.flip();
        
        IoBuffer ioBuffer = new IoBuffer();
        ioBuffer.add(bb1, bb2);
        assertEquals(0, ioBuffer.position());
        assertEquals(6, ioBuffer.limit());
        assertTrue(ioBuffer.hasRemaining());

        assertEquals('0', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('1', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('2', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('3', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('4', ioBuffer.get());
        assertTrue(ioBuffer.hasRemaining());
        assertEquals('5', ioBuffer.get());
        
        try {
            assertFalse(ioBuffer.hasRemaining());
            ioBuffer.get();
            fail();
        } catch (BufferUnderflowException bufe) {
            // expected
            assertEquals(6, ioBuffer.position());
        }
    }
}
