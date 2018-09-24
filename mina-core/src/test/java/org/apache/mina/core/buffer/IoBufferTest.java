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
package org.apache.mina.core.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.apache.mina.util.Bar;
import org.junit.Test;

/**
 * Tests the {@link IoBuffer} class.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoBufferTest {

    private static interface NonserializableInterface {
    }

    public static class NonserializableClass {
    }

    /**
     * Test the capacity(newCapacity) method.
     */
    @Test
    public void testCapacity() {
        IoBuffer buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        
        // See if we can decrease the capacity (we shouldn't be able to go under the minimul capacity)
        IoBuffer newBuffer = buffer.capacity(7);
        assertEquals(10, newBuffer.capacity());
        assertEquals(buffer, newBuffer);
        
        // See if we can increase the capacity
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        newBuffer = buffer.capacity(14);
        assertEquals(14, newBuffer.capacity());
        assertEquals(buffer, newBuffer);
        newBuffer.put(0, (byte)'9');
        assertEquals((byte)'9', newBuffer.get(0));
        assertEquals((byte)'9', buffer.get(0));
        
        // See if we can go down when the minimum capacity is below the current capacity
        // We should not.
        buffer = IoBuffer.allocate(10);
        buffer.capacity(5);
        assertEquals(10, buffer.minimumCapacity());
        assertEquals(10, buffer.capacity());
    }

    /**
     * Test the expand(expectedRemaining) method.
     */
    @Test
    public void testExpand() {
        IoBuffer buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        
        assertEquals(6, buffer.remaining());
        
        // See if we can expand with a lower number of remaining bytes. We should not.
        IoBuffer newBuffer = buffer.expand(2);
        assertEquals(6, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(0, newBuffer.position());
        
        // Now, let's expand the buffer above the number of current bytes but below the limit
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        newBuffer = buffer.expand(8);
        assertEquals(8, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(0, newBuffer.position());
        
        // Last, expand the buffer above the limit
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        newBuffer = buffer.expand(12);
        assertEquals(12, newBuffer.limit());
        assertEquals(12, newBuffer.capacity());
        assertEquals(0, newBuffer.position());
        
        // Now, move forward in the buffer
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        buffer.position(4);

        // See if we can expand with a lower number of remaining bytes. We should not.
        newBuffer = buffer.expand(2);
        assertEquals(6, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(4, newBuffer.position());

        // Expand above the current limit
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        buffer.position(4);
        newBuffer = buffer.expand(3);
        assertEquals(7, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(4, newBuffer.position());

        // Expand above the current capacity
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        buffer.position(4);
        newBuffer = buffer.expand(7);
        assertEquals(11, newBuffer.limit());
        assertEquals(11, newBuffer.capacity());
        assertEquals(4, newBuffer.position());
    }

    /**
     * Test the expand(position, expectedRemaining) method.
     */
    @Test
    public void testExpandPos() {
        IoBuffer buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        
        assertEquals(6, buffer.remaining());
        
        // See if we can expand with a lower number of remaining bytes. We should not.
        IoBuffer newBuffer = buffer.expand(3, 2);
        assertEquals(6, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(0, newBuffer.position());
        
        // Now, let's expand the buffer above the number of current bytes but below the limit
        buffer = IoBuffer.allocate(10);
        buffer.put("012345".getBytes());
        buffer.flip();

        newBuffer = buffer.expand(3, 5);
        assertEquals(8, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(0, newBuffer.position());
        
        // Last, expand the buffer above the limit
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        newBuffer = buffer.expand(3,9);
        assertEquals(12, newBuffer.limit());
        assertEquals(12, newBuffer.capacity());
        assertEquals(0, newBuffer.position());
        
        // Now, move forward in the buffer
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        buffer.position(4);

        // See if we can expand with a lower number of remaining bytes. We should not be.
        newBuffer = buffer.expand(5, 1);
        assertEquals(6, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(4, newBuffer.position());

        // Expand above the current limit
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        buffer.position(4);
        newBuffer = buffer.expand(5, 2);
        assertEquals(7, newBuffer.limit());
        assertEquals(10, newBuffer.capacity());
        assertEquals(4, newBuffer.position());

        // Expand above the current capacity
        buffer = IoBuffer.allocate(10);
        
        buffer.put("012345".getBytes());
        buffer.flip();
        buffer.position(4);
        newBuffer = buffer.expand(5, 6);
        assertEquals(11, newBuffer.limit());
        assertEquals(11, newBuffer.capacity());
        assertEquals(4, newBuffer.position());
    }

    /**
     * Test the normalizeCapacity(requestedCapacity) method.
     */
    @Test
    public void testNormalizeCapacity() {
        // A few sanity checks
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(-10));
        assertEquals(0, IoBufferImpl.normalizeCapacity(0));
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(Integer.MAX_VALUE - 10));

        // A sanity check test for all the powers of 2
        for (int i = 0; i < 30; i++) {
            int n = 1 << i;

            assertEquals(n, IoBufferImpl.normalizeCapacity(n));

            if (i > 1) {
                // test that n - 1 will be normalized to n (notice that n = 2^i)
                assertEquals(n, IoBufferImpl.normalizeCapacity(n - 1));
            }

            // test that n + 1 will be normalized to 2^(i + 1)
            assertEquals(n << 1, IoBufferImpl.normalizeCapacity(n + 1));
        }

        // The first performance test measures the time to normalize integers
        // from 0 to 2^27 (it tests 2^27 integers)
        long time = System.currentTimeMillis();

        for (int i = 0; i < 1 << 27; i++) {
            int n = IoBufferImpl.normalizeCapacity(i);

            // do a simple superfluous test to prevent possible compiler or JVM
            // optimizations of not executing non used code/variables
            if (n == -1) {
                System.out.println("n should never be -1");
            }
        }

        long time2 = System.currentTimeMillis();
        //System.out.println("Time for performance test 1: " + (time2 - time) + "ms");

        // The second performance test measures the time to normalize integers
        // from Integer.MAX_VALUE to Integer.MAX_VALUE - 2^27 (it tests 2^27
        // integers)
        time = System.currentTimeMillis();
        for (int i = Integer.MAX_VALUE; i > Integer.MAX_VALUE - (1 << 27); i--) {
            int n = IoBufferImpl.normalizeCapacity(i);

            // do a simple superfluous test to prevent possible compiler or JVM
            // optimizations of not executing non used code/variables
            if (n == -1) {
                System.out.println("n should never be -1");
            }
        }

        time2 = System.currentTimeMillis();
        //System.out.println("Time for performance test 2: " + (time2 - time) + "ms");
    }

    @Test
    public void autoExpand() {
        IoBuffer buffer = IoBuffer.allocate(8, false);
        buffer.setAutoExpand(true);

        assertTrue("Should AutoExpand", buffer.isAutoExpand());

        IoBuffer slice = buffer.slice();
        assertFalse("Should *NOT* AutoExpand", buffer.isAutoExpand());
        assertFalse("Should *NOT* AutoExpand", slice.isAutoExpand());
    }

    /**
     * This class extends the AbstractIoBuffer class to have direct access to
     * the protected IoBuffer.normalizeCapacity() method and to expose it for
     * the tests.
     */
    private static class IoBufferImpl extends AbstractIoBuffer {

        public static int normalizeCapacity(int requestedCapacity) {
            return IoBuffer.normalizeCapacity(requestedCapacity);
        }

        protected IoBufferImpl(AbstractIoBuffer parent) {
            super(parent);
        }

        protected IoBuffer asReadOnlyBuffer0() {
            return null;
        }

        protected void buf(ByteBuffer newBuf) {
        }

        protected IoBuffer duplicate0() {
            return null;
        }

        protected IoBuffer slice0() {
            return null;
        }

        public byte[] array() {
            return null;
        }

        public int arrayOffset() {
            return 0;
        }

        public ByteBuffer buf() {
            return null;
        }

        public void free() {
        }

        public boolean hasArray() {
            return false;
        }

    }

    @Test
    public void testObjectSerialization() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        buf.setAutoExpand(true);
        List<Object> o = new ArrayList<Object>();
        o.add(new Date());
        o.add(long.class);

        // Test writing an object.
        buf.putObject(o);

        // Test reading an object.
        buf.clear();
        Object o2 = buf.getObject();
        assertEquals(o, o2);

        // This assertion is just to make sure that deserialization occurred.
        assertNotSame(o, o2);
    }

    @Test
    public void testNonserializableClass() throws Exception {
        Class<?> c = NonserializableClass.class;

        IoBuffer buffer = IoBuffer.allocate(16);
        buffer.setAutoExpand(true);
        buffer.putObject(c);

        buffer.flip();
        Object o = buffer.getObject();

        assertEquals(c, o);
        assertSame(c, o);
    }

    @Test
    public void testNonserializableInterface() throws Exception {
        Class<?> c = NonserializableInterface.class;

        IoBuffer buffer = IoBuffer.allocate(16);
        buffer.setAutoExpand(true);
        buffer.putObject(c);

        buffer.flip();
        Object o = buffer.getObject();

        assertEquals(c, o);
        assertSame(c, o);
    }

    @Test
    public void testAllocate() throws Exception {
        for (int i = 10; i < 1048576 * 2; i = i * 11 / 10) // increase by 10%
        {
            IoBuffer buf = IoBuffer.allocate(i);
            assertEquals(0, buf.position());
            assertEquals(buf.capacity(), buf.remaining());
            assertTrue(buf.capacity() >= i);
            assertTrue(buf.capacity() < i * 2);
        }
    }

    /**
     * Test that we can't allocate a buffer with a negative value
     * @throws Exception If allocation failed
     */
    @Test(expected=IllegalArgumentException.class)
    public void testAllocateNegative() throws Exception {
        IoBuffer.allocate(-1);
    }

    @Test
    public void testAutoExpand() throws Exception {
        IoBuffer buf = IoBuffer.allocate(1);

        buf.put((byte) 0);
        try {
            buf.put((byte) 0);
            fail("Buffer can't auto expand, with autoExpand property set at false");
        } catch (BufferOverflowException e) {
            // Expected Exception as auto expand property is false
            assertTrue(true);
        }

        buf.setAutoExpand(true);
        buf.put((byte) 0);
        assertEquals(2, buf.position());
        assertEquals(2, buf.limit());
        assertEquals(2, buf.capacity());

        buf.setAutoExpand(false);
        try {
            buf.put(3, (byte) 0);
            fail("Buffer can't auto expand, with autoExpand property set at false");
        } catch (IndexOutOfBoundsException e) {
            // Expected Exception as auto expand property is false
            assertTrue(true);
        }

        buf.setAutoExpand(true);
        buf.put(3, (byte) 0);
        assertEquals(2, buf.position());
        assertEquals(4, buf.limit());
        assertEquals(4, buf.capacity());

        // Make sure the buffer is doubled up.
        buf = IoBuffer.allocate(1).setAutoExpand(true);
        int lastCapacity = buf.capacity();
        for (int i = 0; i < 1048576; i++) {
            buf.put((byte) 0);
            if (lastCapacity != buf.capacity()) {
                assertEquals(lastCapacity * 2, buf.capacity());
                lastCapacity = buf.capacity();
            }
        }
    }

    @Test
    public void testAutoExpandMark() throws Exception {
        IoBuffer buf = IoBuffer.allocate(4).setAutoExpand(true);

        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);

        // Position should be 3 when we reset this buffer.
        buf.mark();

        // Overflow it
        buf.put((byte) 0);
        buf.put((byte) 0);

        assertEquals(5, buf.position());
        buf.reset();
        assertEquals(3, buf.position());
    }

    @Test
    public void testAutoShrink() throws Exception {
        IoBuffer buf = IoBuffer.allocate(8).setAutoShrink(true);

        // Make sure the buffer doesn't shrink too much (less than the initial
        // capacity.)
        buf.sweep((byte) 1);
        buf.fill(7);
        buf.compact();
        assertEquals(8, buf.capacity());
        assertEquals(1, buf.position());
        assertEquals(8, buf.limit());
        buf.clear();
        assertEquals(1, buf.get());

        // Expand the buffer.
        buf.capacity(32).clear();
        assertEquals(32, buf.capacity());

        // Make sure the buffer shrinks when only 1/4 is being used.
        buf.sweep((byte) 1);
        buf.fill(24);
        buf.compact();
        assertEquals(16, buf.capacity());
        assertEquals(8, buf.position());
        assertEquals(16, buf.limit());
        buf.clear();
        for (int i = 0; i < 8; i++) {
            assertEquals(1, buf.get());
        }

        // Expand the buffer.
        buf.capacity(32).clear();
        assertEquals(32, buf.capacity());

        // Make sure the buffer shrinks when only 1/8 is being used.
        buf.sweep((byte) 1);
        buf.fill(28);
        buf.compact();
        assertEquals(8, buf.capacity());
        assertEquals(4, buf.position());
        assertEquals(8, buf.limit());
        buf.clear();
        for (int i = 0; i < 4; i++) {
            assertEquals(1, buf.get());
        }

        // Expand the buffer.
        buf.capacity(32).clear();
        assertEquals(32, buf.capacity());

        // Make sure the buffer shrinks when 0 byte is being used.
        buf.fill(32);
        buf.compact();
        assertEquals(8, buf.capacity());
        assertEquals(0, buf.position());
        assertEquals(8, buf.limit());

        // Expand the buffer.
        buf.capacity(32).clear();
        assertEquals(32, buf.capacity());

        // Make sure the buffer doesn't shrink when more than 1/4 is being used.
        buf.sweep((byte) 1);
        buf.fill(23);
        buf.compact();
        assertEquals(32, buf.capacity());
        assertEquals(9, buf.position());
        assertEquals(32, buf.limit());
        buf.clear();
        for (int i = 0; i < 9; i++) {
            assertEquals(1, buf.get());
        }
    }

    @Test
    public void testGetString() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        CharsetDecoder decoder;

        Charset charset = StandardCharsets.UTF_8;
        buf.clear();
        buf.putString("hello", charset.newEncoder());
        buf.put((byte) 0);
        buf.flip();
        assertEquals("hello", buf.getString(charset.newDecoder()));

        buf.clear();
        buf.putString("hello", charset.newEncoder());
        buf.flip();
        assertEquals("hello", buf.getString(charset.newDecoder()));

        decoder = Charset.forName("ISO-8859-1").newDecoder();
        buf.clear();
        buf.put((byte) 'A');
        buf.put((byte) 'B');
        buf.put((byte) 'C');
        buf.put((byte) 0);

        buf.position(0);
        assertEquals("ABC", buf.getString(decoder));
        assertEquals(4, buf.position());

        buf.position(0);
        buf.limit(1);
        assertEquals("A", buf.getString(decoder));
        assertEquals(1, buf.position());

        buf.clear();
        assertEquals("ABC", buf.getString(10, decoder));
        assertEquals(10, buf.position());

        buf.clear();
        assertEquals("A", buf.getString(1, decoder));
        assertEquals(1, buf.position());

        // Test a trailing garbage
        buf.clear();
        buf.put((byte) 'A');
        buf.put((byte) 'B');
        buf.put((byte) 0);
        buf.put((byte) 'C');
        buf.position(0);
        assertEquals("AB", buf.getString(4, decoder));
        assertEquals(4, buf.position());

        buf.clear();
        buf.fillAndReset(buf.limit());
        decoder = Charset.forName("UTF-16").newDecoder();
        buf.put((byte) 0);
        buf.put((byte) 'A');
        buf.put((byte) 0);
        buf.put((byte) 'B');
        buf.put((byte) 0);
        buf.put((byte) 'C');
        buf.put((byte) 0);
        buf.put((byte) 0);

        buf.position(0);
        assertEquals("ABC", buf.getString(decoder));
        assertEquals(8, buf.position());

        buf.position(0);
        buf.limit(2);
        assertEquals("A", buf.getString(decoder));
        assertEquals(2, buf.position());

        buf.position(0);
        buf.limit(3);
        assertEquals("A", buf.getString(decoder));
        assertEquals(2, buf.position());

        buf.clear();
        assertEquals("ABC", buf.getString(10, decoder));
        assertEquals(10, buf.position());

        buf.clear();
        assertEquals("A", buf.getString(2, decoder));
        assertEquals(2, buf.position());

        buf.clear();
        try {
            buf.getString(1, decoder);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }

        // Test getting strings from an empty buffer.
        buf.clear();
        buf.limit(0);
        assertEquals("", buf.getString(decoder));
        assertEquals("", buf.getString(2, decoder));

        // Test getting strings from non-empty buffer which is filled with 0x00
        buf.clear();
        buf.putInt(0);
        buf.clear();
        buf.limit(4);
        assertEquals("", buf.getString(decoder));
        assertEquals(2, buf.position());
        assertEquals(4, buf.limit());

        buf.position(0);
        assertEquals("", buf.getString(2, decoder));
        assertEquals(2, buf.position());
        assertEquals(4, buf.limit());
    }

    @Test
    public void testGetStringWithFailure() throws Exception {
        String test = "\u30b3\u30e1\u30f3\u30c8\u7de8\u96c6";
        IoBuffer buffer = IoBuffer.wrap(test.getBytes("Shift_JIS"));

        // Make sure the limit doesn't change when an exception arose.
        int oldLimit = buffer.limit();
        int oldPos = buffer.position();
        try {
            buffer.getString(3, Charset.forName("ASCII").newDecoder());
            fail();
        } catch (Exception e) {
            assertEquals(oldLimit, buffer.limit());
            assertEquals(oldPos, buffer.position());
        }

        try {
            buffer.getString(Charset.forName("ASCII").newDecoder());
            fail();
        } catch (Exception e) {
            assertEquals(oldLimit, buffer.limit());
            assertEquals(oldPos, buffer.position());
        }
    }

    @Test
    public void testPutString() throws Exception {
        CharsetEncoder encoder;
        IoBuffer buf = IoBuffer.allocate(16);
        encoder = Charset.forName("ISO-8859-1").newEncoder();

        buf.putString("ABC", encoder);
        assertEquals(3, buf.position());
        buf.clear();
        assertEquals('A', buf.get(0));
        assertEquals('B', buf.get(1));
        assertEquals('C', buf.get(2));

        buf.putString("D", 5, encoder);
        assertEquals(5, buf.position());
        buf.clear();
        assertEquals('D', buf.get(0));
        assertEquals(0, buf.get(1));

        buf.putString("EFG", 2, encoder);
        assertEquals(2, buf.position());
        buf.clear();
        assertEquals('E', buf.get(0));
        assertEquals('F', buf.get(1));
        assertEquals('C', buf.get(2)); // C may not be overwritten

        // UTF-16: We specify byte order to omit BOM.
        encoder = Charset.forName("UTF-16BE").newEncoder();
        buf.clear();

        buf.putString("ABC", encoder);
        assertEquals(6, buf.position());
        buf.clear();

        assertEquals(0, buf.get(0));
        assertEquals('A', buf.get(1));
        assertEquals(0, buf.get(2));
        assertEquals('B', buf.get(3));
        assertEquals(0, buf.get(4));
        assertEquals('C', buf.get(5));

        buf.putString("D", 10, encoder);
        assertEquals(10, buf.position());
        buf.clear();
        assertEquals(0, buf.get(0));
        assertEquals('D', buf.get(1));
        assertEquals(0, buf.get(2));
        assertEquals(0, buf.get(3));

        buf.putString("EFG", 4, encoder);
        assertEquals(4, buf.position());
        buf.clear();
        assertEquals(0, buf.get(0));
        assertEquals('E', buf.get(1));
        assertEquals(0, buf.get(2));
        assertEquals('F', buf.get(3));
        assertEquals(0, buf.get(4)); // C may not be overwritten
        assertEquals('C', buf.get(5)); // C may not be overwritten

        // Test putting an emptry string
        buf.putString("", encoder);
        assertEquals(0, buf.position());
        buf.putString("", 4, encoder);
        assertEquals(4, buf.position());
        assertEquals(0, buf.get(0));
        assertEquals(0, buf.get(1));
    }

    @Test
    public void testGetPrefixedString() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        CharsetEncoder encoder;
        CharsetDecoder decoder;
        encoder = Charset.forName("ISO-8859-1").newEncoder();
        decoder = Charset.forName("ISO-8859-1").newDecoder();

        buf.putShort((short) 3);
        buf.putString("ABCD", encoder);
        buf.clear();
        assertEquals("ABC", buf.getPrefixedString(decoder));
    }

    @Test
    public void testPutPrefixedString() throws Exception {
        CharsetEncoder encoder;
        IoBuffer buf = IoBuffer.allocate(16);
        buf.fillAndReset(buf.remaining());
        encoder = Charset.forName("ISO-8859-1").newEncoder();

        // Without autoExpand
        buf.putPrefixedString("ABC", encoder);
        assertEquals(5, buf.position());
        assertEquals(0, buf.get(0));
        assertEquals(3, buf.get(1));
        assertEquals('A', buf.get(2));
        assertEquals('B', buf.get(3));
        assertEquals('C', buf.get(4));

        buf.clear();
        try {
            buf.putPrefixedString("123456789012345", encoder);
            fail();
        } catch (BufferOverflowException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }

        // With autoExpand
        buf.clear();
        buf.setAutoExpand(true);
        buf.putPrefixedString("123456789012345", encoder);
        assertEquals(17, buf.position());
        assertEquals(0, buf.get(0));
        assertEquals(15, buf.get(1));
        assertEquals('1', buf.get(2));
        assertEquals('2', buf.get(3));
        assertEquals('3', buf.get(4));
        assertEquals('4', buf.get(5));
        assertEquals('5', buf.get(6));
        assertEquals('6', buf.get(7));
        assertEquals('7', buf.get(8));
        assertEquals('8', buf.get(9));
        assertEquals('9', buf.get(10));
        assertEquals('0', buf.get(11));
        assertEquals('1', buf.get(12));
        assertEquals('2', buf.get(13));
        assertEquals('3', buf.get(14));
        assertEquals('4', buf.get(15));
        assertEquals('5', buf.get(16));
    }

    @Test
    public void testPutPrefixedStringWithPrefixLength() throws Exception {
        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        IoBuffer buf = IoBuffer.allocate(16).sweep().setAutoExpand(true);

        buf.putPrefixedString("A", 1, encoder);
        assertEquals(2, buf.position());
        assertEquals(1, buf.get(0));
        assertEquals('A', buf.get(1));

        buf.sweep();
        buf.putPrefixedString("A", 2, encoder);
        assertEquals(3, buf.position());
        assertEquals(0, buf.get(0));
        assertEquals(1, buf.get(1));
        assertEquals('A', buf.get(2));

        buf.sweep();
        buf.putPrefixedString("A", 4, encoder);
        assertEquals(5, buf.position());
        assertEquals(0, buf.get(0));
        assertEquals(0, buf.get(1));
        assertEquals(0, buf.get(2));
        assertEquals(1, buf.get(3));
        assertEquals('A', buf.get(4));
    }

    @Test
    public void testPutPrefixedStringWithPadding() throws Exception {
        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        IoBuffer buf = IoBuffer.allocate(16).sweep().setAutoExpand(true);

        buf.putPrefixedString("A", 1, 2, (byte) 32, encoder);
        assertEquals(3, buf.position());
        assertEquals(2, buf.get(0));
        assertEquals('A', buf.get(1));
        assertEquals(' ', buf.get(2));

        buf.sweep();
        buf.putPrefixedString("A", 1, 4, (byte) 32, encoder);
        assertEquals(5, buf.position());
        assertEquals(4, buf.get(0));
        assertEquals('A', buf.get(1));
        assertEquals(' ', buf.get(2));
        assertEquals(' ', buf.get(3));
        assertEquals(' ', buf.get(4));
    }

    @Test
    public void testWideUtf8Characters() throws Exception {
        Runnable r = new Runnable() {
            public void run() {
                IoBuffer buffer = IoBuffer.allocate(1);
                buffer.setAutoExpand(true);

                Charset charset = StandardCharsets.UTF_8;

                CharsetEncoder encoder = charset.newEncoder();

                for (int i = 0; i < 5; i++) {
                    try {
                        buffer.putString("\u89d2", encoder);
                        buffer.putPrefixedString("\u89d2", encoder);
                    } catch (CharacterCodingException e) {
                        fail(e.getMessage());
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();

        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);
            if (!t.isAlive()) {
                break;
            }
        }

        if (t.isAlive()) {
            t.interrupt();

            fail("Went into endless loop trying to encode character");
        }
    }

    @Test
    public void testInheritedObjectSerialization() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        buf.setAutoExpand(true);

        Bar expected = new Bar();
        expected.setFooValue(0x12345678);
        expected.setBarValue(0x90ABCDEF);

        // Test writing an object.
        buf.putObject(expected);

        // Test reading an object.
        buf.clear();
        Bar actual = (Bar) buf.getObject();
        assertSame(Bar.class, actual.getClass());
        assertEquals(expected.getFooValue(), actual.getFooValue());
        assertEquals(expected.getBarValue(), actual.getBarValue());

        // This assertion is just to make sure that deserialization occurred.
        assertNotSame(expected, actual);
    }

    @Test
    public void testSweepWithZeros() throws Exception {
        IoBuffer buf = IoBuffer.allocate(4);
        buf.putInt(0xdeadbeef);
        buf.clear();
        assertEquals(0xdeadbeef, buf.getInt());
        assertEquals(4, buf.position());
        assertEquals(4, buf.limit());

        buf.sweep();
        assertEquals(0, buf.position());
        assertEquals(4, buf.limit());
        assertEquals(0x0, buf.getInt());
    }

    @Test
    public void testSweepNonZeros() throws Exception {
        IoBuffer buf = IoBuffer.allocate(4);
        buf.putInt(0xdeadbeef);
        buf.clear();
        assertEquals(0xdeadbeef, buf.getInt());
        assertEquals(4, buf.position());
        assertEquals(4, buf.limit());

        buf.sweep((byte) 0x45);
        assertEquals(0, buf.position());
        assertEquals(4, buf.limit());
        assertEquals(0x45454545, buf.getInt());
    }

    @Test
    public void testWrapNioBuffer() throws Exception {
        ByteBuffer nioBuf = ByteBuffer.allocate(10);
        nioBuf.position(3);
        nioBuf.limit(7);

        IoBuffer buf = IoBuffer.wrap(nioBuf);
        assertEquals(3, buf.position());
        assertEquals(7, buf.limit());
        assertEquals(10, buf.capacity());
    }

    @Test
    public void testWrapSubArray() throws Exception {
        byte[] array = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        IoBuffer buf = IoBuffer.wrap(array, 3, 4);
        assertEquals(3, buf.position());
        assertEquals(7, buf.limit());
        assertEquals(10, buf.capacity());

        buf.clear();
        assertEquals(0, buf.position());
        assertEquals(10, buf.limit());
        assertEquals(10, buf.capacity());
    }

    @Test
    public void testDuplicate() throws Exception {
        IoBuffer original;
        IoBuffer duplicate;

        // Test if the buffer is duplicated correctly.
        original = IoBuffer.allocate(16).sweep();
        original.position(4);
        original.limit(10);
        duplicate = original.duplicate();
        original.put(4, (byte) 127);
        assertEquals(4, duplicate.position());
        assertEquals(10, duplicate.limit());
        assertEquals(16, duplicate.capacity());
        assertNotSame(original.buf(), duplicate.buf());
        assertSame(original.buf().array(), duplicate.buf().array());
        assertEquals(127, duplicate.get(4));

        // Test a duplicate of a duplicate.
        original = IoBuffer.allocate(16);
        duplicate = original.duplicate().duplicate();
        assertNotSame(original.buf(), duplicate.buf());
        assertSame(original.buf().array(), duplicate.buf().array());

        // Try to expand.
        original = IoBuffer.allocate(16);
        original.setAutoExpand(true);
        duplicate = original.duplicate();
        assertFalse(original.isAutoExpand());

        try {
            original.setAutoExpand(true);
            fail("Derived buffers and their parent can't be expanded");
        } catch (IllegalStateException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }

        try {
            duplicate.setAutoExpand(true);
            fail("Derived buffers and their parent can't be expanded");
        } catch (IllegalStateException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }
    }

    @Test
    public void testSlice() throws Exception {
        IoBuffer original;
        IoBuffer slice;

        // Test if the buffer is sliced correctly.
        original = IoBuffer.allocate(16).sweep();
        original.position(4);
        original.limit(10);
        slice = original.slice();
        original.put(4, (byte) 127);
        assertEquals(0, slice.position());
        assertEquals(6, slice.limit());
        assertEquals(6, slice.capacity());
        assertNotSame(original.buf(), slice.buf());
        assertEquals(127, slice.get(0));
    }

    @Test
    public void testReadOnlyBuffer() throws Exception {
        IoBuffer original;
        IoBuffer duplicate;

        // Test if the buffer is duplicated correctly.
        original = IoBuffer.allocate(16).sweep();
        original.position(4);
        original.limit(10);
        duplicate = original.asReadOnlyBuffer();
        original.put(4, (byte) 127);
        assertEquals(4, duplicate.position());
        assertEquals(10, duplicate.limit());
        assertEquals(16, duplicate.capacity());
        assertNotSame(original.buf(), duplicate.buf());
        assertEquals(127, duplicate.get(4));

        // Try to expand.
        try {
            original = IoBuffer.allocate(16);
            duplicate = original.asReadOnlyBuffer();
            duplicate.putString("A very very very very looooooong string", Charset.forName("ISO-8859-1").newEncoder());
            fail("ReadOnly buffer's can't be expanded");
        } catch (ReadOnlyBufferException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }
    }

    @Test
    public void testGetUnsigned() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        buf.put((byte) 0xA4);
        buf.put((byte) 0xD0);
        buf.put((byte) 0xB3);
        buf.put((byte) 0xCD);
        buf.flip();

        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.mark();
        assertEquals(0xA4, buf.getUnsigned());
        buf.reset();
        assertEquals(0xD0A4, buf.getUnsignedShort());
        buf.reset();
        assertEquals(0xCDB3D0A4L, buf.getUnsignedInt());
    }

    @Test
    public void testIndexOf() throws Exception {
        boolean direct = false;
        for (int i = 0; i < 2; i++, direct = !direct) {
            IoBuffer buf = IoBuffer.allocate(16, direct);
            buf.put((byte) 0x1);
            buf.put((byte) 0x2);
            buf.put((byte) 0x3);
            buf.put((byte) 0x4);
            buf.put((byte) 0x1);
            buf.put((byte) 0x2);
            buf.put((byte) 0x3);
            buf.put((byte) 0x4);
            buf.position(2);
            buf.limit(5);

            assertEquals(4, buf.indexOf((byte) 0x1));
            assertEquals(-1, buf.indexOf((byte) 0x2));
            assertEquals(2, buf.indexOf((byte) 0x3));
            assertEquals(3, buf.indexOf((byte) 0x4));
        }
    }

    // We need an enum with 64 values
    private static enum TestEnum {
        E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17, E18, E19, E20, E21, E22, E23, E24, E25, E26, E27, E28, E29, E30, E31, E32, E33, E34, E35, E36, E37, E38, E39, E40, E41, E42, E43, E44, E45, E46, E77, E48, E49, E50, E51, E52, E53, E54, E55, E56, E57, E58, E59, E60, E61, E62, E63, E64
    }

    private static enum TooBigEnum {
        E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17, E18, E19, E20, E21, E22, E23, E24, E25, E26, E27, E28, E29, E30, E31, E32, E33, E34, E35, E36, E37, E38, E39, E40, E41, E42, E43, E44, E45, E46, E77, E48, E49, E50, E51, E52, E53, E54, E55, E56, E57, E58, E59, E60, E61, E62, E63, E64, E65
    }

    @Test
    public void testPutEnumSet() {
        IoBuffer buf = IoBuffer.allocate(8);

        // Test empty set
        buf.putEnumSet(EnumSet.noneOf(TestEnum.class));
        buf.flip();
        assertEquals(0, buf.get());

        buf.clear();
        buf.putEnumSetShort(EnumSet.noneOf(TestEnum.class));
        buf.flip();
        assertEquals(0, buf.getShort());

        buf.clear();
        buf.putEnumSetInt(EnumSet.noneOf(TestEnum.class));
        buf.flip();
        assertEquals(0, buf.getInt());

        buf.clear();
        buf.putEnumSetLong(EnumSet.noneOf(TestEnum.class));
        buf.flip();
        assertEquals(0, buf.getLong());

        // Test complete set
        buf.clear();
        buf.putEnumSet(EnumSet.range(TestEnum.E1, TestEnum.E8));
        buf.flip();
        assertEquals((byte) -1, buf.get());

        buf.clear();
        buf.putEnumSetShort(EnumSet.range(TestEnum.E1, TestEnum.E16));
        buf.flip();
        assertEquals((short) -1, buf.getShort());

        buf.clear();
        buf.putEnumSetInt(EnumSet.range(TestEnum.E1, TestEnum.E32));
        buf.flip();
        assertEquals(-1, buf.getInt());

        buf.clear();
        buf.putEnumSetLong(EnumSet.allOf(TestEnum.class));
        buf.flip();
        assertEquals(-1L, buf.getLong());

        // Test high bit set
        buf.clear();
        buf.putEnumSet(EnumSet.of(TestEnum.E8));
        buf.flip();
        assertEquals(Byte.MIN_VALUE, buf.get());

        buf.clear();
        buf.putEnumSetShort(EnumSet.of(TestEnum.E16));
        buf.flip();
        assertEquals(Short.MIN_VALUE, buf.getShort());

        buf.clear();
        buf.putEnumSetInt(EnumSet.of(TestEnum.E32));
        buf.flip();
        assertEquals(Integer.MIN_VALUE, buf.getInt());

        buf.clear();
        buf.putEnumSetLong(EnumSet.of(TestEnum.E64));
        buf.flip();
        assertEquals(Long.MIN_VALUE, buf.getLong());

        // Test high low bits set
        buf.clear();
        buf.putEnumSet(EnumSet.of(TestEnum.E1, TestEnum.E8));
        buf.flip();
        assertEquals(Byte.MIN_VALUE + 1, buf.get());

        buf.clear();
        buf.putEnumSetShort(EnumSet.of(TestEnum.E1, TestEnum.E16));
        buf.flip();
        assertEquals(Short.MIN_VALUE + 1, buf.getShort());

        buf.clear();
        buf.putEnumSetInt(EnumSet.of(TestEnum.E1, TestEnum.E32));
        buf.flip();
        assertEquals(Integer.MIN_VALUE + 1, buf.getInt());

        buf.clear();
        buf.putEnumSetLong(EnumSet.of(TestEnum.E1, TestEnum.E64));
        buf.flip();
        assertEquals(Long.MIN_VALUE + 1, buf.getLong());
    }

    @Test
    public void testGetEnumSet() {
        IoBuffer buf = IoBuffer.allocate(8);

        // Test empty set
        buf.put((byte) 0);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf.getEnumSet(TestEnum.class));

        buf.clear();
        buf.putShort((short) 0);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf.getEnumSet(TestEnum.class));

        buf.clear();
        buf.putInt(0);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf.getEnumSet(TestEnum.class));

        buf.clear();
        buf.putLong(0L);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf.getEnumSet(TestEnum.class));

        // Test complete set
        buf.clear();
        buf.put((byte) -1);
        buf.flip();
        assertEquals(EnumSet.range(TestEnum.E1, TestEnum.E8), buf.getEnumSet(TestEnum.class));

        buf.clear();
        buf.putShort((short) -1);
        buf.flip();
        assertEquals(EnumSet.range(TestEnum.E1, TestEnum.E16), buf.getEnumSetShort(TestEnum.class));

        buf.clear();
        buf.putInt(-1);
        buf.flip();
        assertEquals(EnumSet.range(TestEnum.E1, TestEnum.E32), buf.getEnumSetInt(TestEnum.class));

        buf.clear();
        buf.putLong(-1L);
        buf.flip();
        assertEquals(EnumSet.allOf(TestEnum.class), buf.getEnumSetLong(TestEnum.class));

        // Test high bit set
        buf.clear();
        buf.put(Byte.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E8), buf.getEnumSet(TestEnum.class));

        buf.clear();
        buf.putShort(Short.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E16), buf.getEnumSetShort(TestEnum.class));

        buf.clear();
        buf.putInt(Integer.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E32), buf.getEnumSetInt(TestEnum.class));

        buf.clear();
        buf.putLong(Long.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E64), buf.getEnumSetLong(TestEnum.class));

        // Test high low bits set
        buf.clear();
        byte b = Byte.MIN_VALUE + 1;
        buf.put(b);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E8), buf.getEnumSet(TestEnum.class));

        buf.clear();
        short s = Short.MIN_VALUE + 1;
        buf.putShort(s);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E16), buf.getEnumSetShort(TestEnum.class));

        buf.clear();
        buf.putInt(Integer.MIN_VALUE + 1);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E32), buf.getEnumSetInt(TestEnum.class));

        buf.clear();
        buf.putLong(Long.MIN_VALUE + 1);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E64), buf.getEnumSetLong(TestEnum.class));
    }

    @Test
    public void testBitVectorOverFlow() {
        IoBuffer buf = IoBuffer.allocate(8);
        try {
            buf.putEnumSet(EnumSet.of(TestEnum.E9));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }

        try {
            buf.putEnumSetShort(EnumSet.of(TestEnum.E17));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }

        try {
            buf.putEnumSetInt(EnumSet.of(TestEnum.E33));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }

        try {
            buf.putEnumSetLong(EnumSet.of(TooBigEnum.E65));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected an Exception, signifies test success
            assertTrue(true);
        }
    }

    @Test
    public void testGetPutEnum() {
        IoBuffer buf = IoBuffer.allocate(4);

        buf.putEnum(TestEnum.E64);
        buf.flip();
        assertEquals(TestEnum.E64, buf.getEnum(TestEnum.class));

        buf.clear();
        buf.putEnumShort(TestEnum.E64);
        buf.flip();
        assertEquals(TestEnum.E64, buf.getEnumShort(TestEnum.class));

        buf.clear();
        buf.putEnumInt(TestEnum.E64);
        buf.flip();
        assertEquals(TestEnum.E64, buf.getEnumInt(TestEnum.class));
    }

    @Test
    public void testGetMediumInt() {
        IoBuffer buf = IoBuffer.allocate(3);

        buf.put((byte) 0x01);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        assertEquals(3, buf.position());

        buf.flip();
        assertEquals(0x010203, buf.getMediumInt());
        assertEquals(0x010203, buf.getMediumInt(0));
        buf.flip();
        assertEquals(0x010203, buf.getUnsignedMediumInt());
        assertEquals(0x010203, buf.getUnsignedMediumInt(0));
        buf.flip();
        assertEquals(0x010203, buf.getUnsignedMediumInt());
        buf.flip().order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x030201, buf.getMediumInt());
        assertEquals(0x030201, buf.getMediumInt(0));

        // Test max medium int
        buf.flip().order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) 0x7f);
        buf.put((byte) 0xff);
        buf.put((byte) 0xff);
        buf.flip();
        assertEquals(0x7fffff, buf.getMediumInt());
        assertEquals(0x7fffff, buf.getMediumInt(0));

        // Test negative number
        buf.flip().order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) 0xff);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        buf.flip();

        assertEquals(0xffff0203, buf.getMediumInt());
        assertEquals(0xffff0203, buf.getMediumInt(0));
        buf.flip();

        assertEquals(0x00ff0203, buf.getUnsignedMediumInt());
        assertEquals(0x00ff0203, buf.getUnsignedMediumInt(0));
    }

    @Test
    public void testPutMediumInt() {
        IoBuffer buf = IoBuffer.allocate(3);

        checkMediumInt(buf, 0);
        checkMediumInt(buf, 1);
        checkMediumInt(buf, -1);
        checkMediumInt(buf, 0x7fffff);
    }

    private void checkMediumInt(IoBuffer buf, int x) {
        buf.putMediumInt(x);
        assertEquals(3, buf.position());
        buf.flip();
        assertEquals(x, buf.getMediumInt());
        assertEquals(3, buf.position());

        buf.putMediumInt(0, x);
        assertEquals(3, buf.position());
        assertEquals(x, buf.getMediumInt(0));

        buf.flip();
    }

    @Test
    public void testPutUnsigned() {
        IoBuffer buf = IoBuffer.allocate(4);
        byte b = (byte) 0x80; // We should get 0x0080
        short s = (short) 0x8F81; // We should get 0x0081
        int i = 0x8FFFFF82; // We should get 0x0082
        long l = 0x8FFFFFFFFFFFFF83L; // We should get 0x0083

        buf.mark();

        // Put the unsigned bytes
        buf.putUnsigned(b);
        buf.putUnsigned(s);
        buf.putUnsigned(i);
        buf.putUnsigned(l);

        buf.reset();

        // Read back the unsigned bytes
        assertEquals(0x0080, buf.getUnsigned());
        assertEquals(0x0081, buf.getUnsigned());
        assertEquals(0x0082, buf.getUnsigned());
        assertEquals(0x0083, buf.getUnsigned());
    }

    @Test
    public void testPutUnsignedIndex() {
        IoBuffer buf = IoBuffer.allocate(4);
        byte b = (byte) 0x80; // We should get 0x0080
        short s = (short) 0x8F81; // We should get 0x0081
        int i = 0x8FFFFF82; // We should get 0x0082
        long l = 0x8FFFFFFFFFFFFF83L; // We should get 0x0083

        buf.mark();

        // Put the unsigned bytes
        buf.putUnsigned(3, b);
        buf.putUnsigned(2, s);
        buf.putUnsigned(1, i);
        buf.putUnsigned(0, l);

        buf.reset();

        // Read back the unsigned bytes
        assertEquals(0x0083, buf.getUnsigned());
        assertEquals(0x0082, buf.getUnsigned());
        assertEquals(0x0081, buf.getUnsigned());
        assertEquals(0x0080, buf.getUnsigned());
    }

    @Test
    public void testPutUnsignedShort() {
        IoBuffer buf = IoBuffer.allocate(8);
        byte b = (byte) 0x80; // We should get 0x0080
        short s = (short) 0x8181; // We should get 0x8181
        int i = 0x82828282; // We should get 0x8282
        long l = 0x8383838383838383L; // We should get 0x8383

        buf.mark();

        // Put the unsigned bytes
        buf.putUnsignedShort(b);
        buf.putUnsignedShort(s);
        buf.putUnsignedShort(i);
        buf.putUnsignedShort(l);

        buf.reset();

        // Read back the unsigned bytes
        assertEquals(0x0080L, buf.getUnsignedShort());
        assertEquals(0x8181L, buf.getUnsignedShort());
        assertEquals(0x8282L, buf.getUnsignedShort());
        assertEquals(0x8383L, buf.getUnsignedShort());
    }

    @Test
    public void testPutUnsignedShortIndex() {
        IoBuffer buf = IoBuffer.allocate(8);
        byte b = (byte) 0x80; // We should get 0x00000080
        short s = (short) 0x8181; // We should get 0x00008181
        int i = 0x82828282; // We should get 0x82828282
        long l = 0x8383838383838383L; // We should get 0x83838383

        buf.mark();

        // Put the unsigned shorts
        buf.putUnsignedShort(6, b);
        buf.putUnsignedShort(4, s);
        buf.putUnsignedShort(2, i);
        buf.putUnsignedShort(0, l);

        buf.reset();

        // Read back the unsigned bytes
        assertEquals(0x8383L, buf.getUnsignedShort());
        assertEquals(0x8282L, buf.getUnsignedShort());
        assertEquals(0x8181L, buf.getUnsignedShort());
        assertEquals(0x0080L, buf.getUnsignedShort());
    }

    @Test
    public void testPutUnsignedInt() {
        IoBuffer buf = IoBuffer.allocate(16);
        byte b = (byte) 0x80; // We should get 0x00000080
        short s = (short) 0x8181; // We should get 0x00008181
        int i = 0x82828282; // We should get 0x82828282
        long l = 0x8383838383838383L; // We should get 0x83838383

        buf.mark();

        // Put the unsigned bytes
        buf.putUnsignedInt(b);
        buf.putUnsignedInt(s);
        buf.putUnsignedInt(i);
        buf.putUnsignedInt(l);

        buf.reset();

        // Read back the unsigned bytes
        assertEquals(0x0000000000000080L, buf.getUnsignedInt());
        assertEquals(0x0000000000008181L, buf.getUnsignedInt());
        assertEquals(0x0000000082828282L, buf.getUnsignedInt());
        assertEquals(0x0000000083838383L, buf.getUnsignedInt());
    }

    /**
     * Test the IoBuffer.putUnsignedInIndex() method.
     */
    @Test
    public void testPutUnsignedIntIndex() {
        IoBuffer buf = IoBuffer.allocate(16);
        byte b = (byte) 0x80; // We should get 0x00000080
        short s = (short) 0x8181; // We should get 0x00008181
        int i = 0x82828282; // We should get 0x82828282
        long l = 0x8383838383838383L; // We should get 0x83838383

        buf.mark();

        // Put the unsigned bytes
        buf.putUnsignedInt(12, b);
        buf.putUnsignedInt(8, s);
        buf.putUnsignedInt(4, i);
        buf.putUnsignedInt(0, l);

        buf.reset();

        // Read back the unsigned bytes
        assertEquals(0x0000000083838383L, buf.getUnsignedInt());
        assertEquals(0x0000000082828282L, buf.getUnsignedInt());
        assertEquals(0x0000000000008181L, buf.getUnsignedInt());
        assertEquals(0x0000000000000080L, buf.getUnsignedInt());
    }

    /**
     * Test the getSlice method (even if we haven't flipped the buffer)
     */
    @Test
    public void testGetSlice() {
        IoBuffer buf = IoBuffer.allocate(36);

        for (byte i = 0; i < 36; i++) {
            buf.put(i);
        }

        IoBuffer res = buf.getSlice(1, 3);

        // The limit should be 3, the pos should be 0 and the bytes read
        // should be 0x01, 0x02 and 0x03
        assertEquals(0, res.position());
        assertEquals(3, res.limit());
        assertEquals(0x01, res.get());
        assertEquals(0x02, res.get());
        assertEquals(0x03, res.get());

        // Now test after a flip
        buf.flip();

        res = buf.getSlice(1, 3);
        // The limit should be 3, the pos should be 0 and the bytes read
        // should be 0x01, 0x02 and 0x03
        assertEquals(0, res.position());
        assertEquals(3, res.limit());
        assertEquals(0x01, res.get());
        assertEquals(0x02, res.get());
        assertEquals(0x03, res.get());
    }

    /**
     * Test the IoBuffer.shrink() method.
     */
    @Test
    public void testShrink() {
        IoBuffer buf = IoBuffer.allocate(36);
        buf.put( "012345".getBytes());
        buf.flip();
        buf.position(4);
        buf.minimumCapacity(8);

        IoBuffer newBuf = buf.shrink();
        assertEquals(4, newBuf.position());
        assertEquals(6, newBuf.limit());
        assertEquals(9, newBuf.capacity());
        assertEquals(8, newBuf.minimumCapacity());

        buf = IoBuffer.allocate(6);
        buf.put( "012345".getBytes());
        buf.flip();
        buf.position(4);

        newBuf = buf.shrink();
        assertEquals(4, newBuf.position());
        assertEquals(6, newBuf.limit());
        assertEquals(6, newBuf.capacity());
        assertEquals(6, newBuf.minimumCapacity());
    }
    
    
    /**
     * Test the IoBuffer.position(newPosition) method.
     */
    @Test
    public void testSetPosition()
    {
        
    }

    
    @Test
    public void testFillByteSize()
    {
        int length = 1024*1020;
        IoBuffer buffer = IoBuffer.allocate(length);
        buffer.fill((byte)0x80, length);
        
        buffer.flip();
        for (int i=0; i<length; i++) {
            assertEquals((byte)0x80, buffer.get());
        }
    }
}
