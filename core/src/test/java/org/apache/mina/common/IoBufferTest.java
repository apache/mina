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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests {@link IoBuffer}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoBufferTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IoBufferTest.class);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testAllocate() throws Exception {
        for (int i = 10; i < 1048576 * 2; i = i * 11 / 10) // increase by 10%
        {
            IoBuffer buf = IoBuffer.allocate(i);
            Assert.assertEquals(0, buf.position());
            Assert.assertEquals(buf.capacity(), buf.remaining());
            Assert.assertTrue(buf.capacity() >= i);
            Assert.assertTrue(buf.capacity() < i * 2);
        }
    }

    public void testAutoExpand() throws Exception {
        IoBuffer buf = IoBuffer.allocate(1);

        buf.put((byte) 0);
        try {
            buf.put((byte) 0);
            Assert.fail();
        } catch (BufferOverflowException e) {
            // ignore
        }

        buf.setAutoExpand(true);
        buf.put((byte) 0);
        Assert.assertEquals(2, buf.position());
        Assert.assertEquals(2, buf.limit());
        Assert.assertEquals(2, buf.capacity());

        buf.setAutoExpand(false);
        try {
            buf.put(3, (byte) 0);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
            // ignore
        }

        buf.setAutoExpand(true);
        buf.put(3, (byte) 0);
        Assert.assertEquals(2, buf.position());
        Assert.assertEquals(4, buf.limit());
        Assert.assertEquals(4, buf.capacity());

        // Make sure the buffer is doubled up.
        buf = IoBuffer.allocate(1).setAutoExpand(true);
        int lastCapacity = buf.capacity();
        for (int i = 0; i < 1048576; i ++) {
            buf.put((byte) 0);
            if (lastCapacity != buf.capacity()) {
                Assert.assertEquals(lastCapacity * 2, buf.capacity());
                lastCapacity = buf.capacity();
            }
        }
    }

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

        Assert.assertEquals(5, buf.position());
        buf.reset();
        Assert.assertEquals(3, buf.position());
    }
    
    public void testAutoShrink() throws Exception {
        IoBuffer buf = IoBuffer.allocate(8).setAutoShrink(true);
        
        // Make sure the buffer doesn't shrink too much (less than the initial
        // capacity.)
        buf.sweep((byte) 1);
        buf.fill(7); 
        buf.compact();
        Assert.assertEquals(8, buf.capacity());
        Assert.assertEquals(1, buf.position());
        Assert.assertEquals(8, buf.limit());
        buf.clear();
        Assert.assertEquals(1, buf.get());

        // Expand the buffer.
        buf.capacity(32).clear();
        Assert.assertEquals(32, buf.capacity());
        
        // Make sure the buffer shrinks when only 1/4 is being used.
        buf.sweep((byte) 1);
        buf.fill(24);
        buf.compact();
        Assert.assertEquals(16, buf.capacity());
        Assert.assertEquals(8, buf.position());
        Assert.assertEquals(16, buf.limit());
        buf.clear();
        for (int i = 0; i < 8; i ++) {
            Assert.assertEquals(1, buf.get());
        }

        // Expand the buffer.
        buf.capacity(32).clear();
        Assert.assertEquals(32, buf.capacity());
        
        // Make sure the buffer shrinks when only 1/8 is being used.
        buf.sweep((byte) 1);
        buf.fill(28);
        buf.compact();
        Assert.assertEquals(8, buf.capacity());
        Assert.assertEquals(4, buf.position());
        Assert.assertEquals(8, buf.limit());
        buf.clear();
        for (int i = 0; i < 4; i ++) {
            Assert.assertEquals(1, buf.get());
        }

        // Expand the buffer.
        buf.capacity(32).clear();
        Assert.assertEquals(32, buf.capacity());
        
        // Make sure the buffer shrinks when 0 byte is being used.
        buf.fill(32);
        buf.compact();
        Assert.assertEquals(8, buf.capacity());
        Assert.assertEquals(0, buf.position());
        Assert.assertEquals(8, buf.limit());

        // Expand the buffer.
        buf.capacity(32).clear();
        Assert.assertEquals(32, buf.capacity());
        
        // Make sure the buffer doesn't shrink when more than 1/4 is being used.
        buf.sweep((byte) 1);
        buf.fill(23);
        buf.compact();
        Assert.assertEquals(32, buf.capacity());
        Assert.assertEquals(9, buf.position());
        Assert.assertEquals(32, buf.limit());
        buf.clear();
        for (int i = 0; i < 9; i ++) {
            Assert.assertEquals(1, buf.get());
        }
    }

    public void testGetString() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        CharsetDecoder decoder;

        Charset charset = Charset.forName("UTF-8");
        buf.clear();
        buf.putString("hello", charset.newEncoder());
        buf.put((byte) 0);
        buf.flip();
        Assert.assertEquals("hello", buf.getString(charset.newDecoder()));

        buf.clear();
        buf.putString("hello", charset.newEncoder());
        buf.flip();
        Assert.assertEquals("hello", buf.getString(charset.newDecoder()));

        decoder = Charset.forName("ISO-8859-1").newDecoder();
        buf.clear();
        buf.put((byte) 'A');
        buf.put((byte) 'B');
        buf.put((byte) 'C');
        buf.put((byte) 0);

        buf.position(0);
        Assert.assertEquals("ABC", buf.getString(decoder));
        Assert.assertEquals(4, buf.position());

        buf.position(0);
        buf.limit(1);
        Assert.assertEquals("A", buf.getString(decoder));
        Assert.assertEquals(1, buf.position());

        buf.clear();
        Assert.assertEquals("ABC", buf.getString(10, decoder));
        Assert.assertEquals(10, buf.position());

        buf.clear();
        Assert.assertEquals("A", buf.getString(1, decoder));
        Assert.assertEquals(1, buf.position());

        // Test a trailing garbage
        buf.clear();
        buf.put((byte) 'A');
        buf.put((byte) 'B');
        buf.put((byte) 0);
        buf.put((byte) 'C');
        buf.position(0);
        Assert.assertEquals("AB", buf.getString(4, decoder));
        Assert.assertEquals(4, buf.position());

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
        Assert.assertEquals("ABC", buf.getString(decoder));
        Assert.assertEquals(8, buf.position());

        buf.position(0);
        buf.limit(2);
        Assert.assertEquals("A", buf.getString(decoder));
        Assert.assertEquals(2, buf.position());

        buf.position(0);
        buf.limit(3);
        Assert.assertEquals("A", buf.getString(decoder));
        Assert.assertEquals(2, buf.position());

        buf.clear();
        Assert.assertEquals("ABC", buf.getString(10, decoder));
        Assert.assertEquals(10, buf.position());

        buf.clear();
        Assert.assertEquals("A", buf.getString(2, decoder));
        Assert.assertEquals(2, buf.position());

        buf.clear();
        try {
            buf.getString(1, decoder);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // ignore
        }

        // Test getting strings from an empty buffer.
        buf.clear();
        buf.limit(0);
        Assert.assertEquals("", buf.getString(decoder));
        Assert.assertEquals("", buf.getString(2, decoder));

        // Test getting strings from non-empty buffer which is filled with 0x00
        buf.clear();
        buf.putInt(0);
        buf.clear();
        buf.limit(4);
        Assert.assertEquals("", buf.getString(decoder));
        Assert.assertEquals(2, buf.position());
        Assert.assertEquals(4, buf.limit());

        buf.position(0);
        Assert.assertEquals("", buf.getString(2, decoder));
        Assert.assertEquals(2, buf.position());
        Assert.assertEquals(4, buf.limit());
    }

    public void testGetStringWithFailure() throws Exception {
        String test = "\u30b3\u30e1\u30f3\u30c8\u7de8\u96c6";
        IoBuffer buffer = IoBuffer.wrap(test.getBytes("Shift_JIS"));

        // Make sure the limit doesn't change when an exception arose.
        int oldLimit = buffer.limit();
        int oldPos = buffer.position();
        try {
            buffer.getString(3, Charset.forName("ASCII").newDecoder());
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(oldLimit, buffer.limit());
            Assert.assertEquals(oldPos, buffer.position());
        }

        try {
            buffer.getString(Charset.forName("ASCII").newDecoder());
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(oldLimit, buffer.limit());
            Assert.assertEquals(oldPos, buffer.position());
        }
    }

    public void testPutString() throws Exception {
        CharsetEncoder encoder;
        IoBuffer buf = IoBuffer.allocate(16);
        encoder = Charset.forName("ISO-8859-1").newEncoder();

        buf.putString("ABC", encoder);
        Assert.assertEquals(3, buf.position());
        buf.clear();
        Assert.assertEquals('A', buf.get(0));
        Assert.assertEquals('B', buf.get(1));
        Assert.assertEquals('C', buf.get(2));

        buf.putString("D", 5, encoder);
        Assert.assertEquals(5, buf.position());
        buf.clear();
        Assert.assertEquals('D', buf.get(0));
        Assert.assertEquals(0, buf.get(1));

        buf.putString("EFG", 2, encoder);
        Assert.assertEquals(2, buf.position());
        buf.clear();
        Assert.assertEquals('E', buf.get(0));
        Assert.assertEquals('F', buf.get(1));
        Assert.assertEquals('C', buf.get(2)); // C may not be overwritten

        // UTF-16: We specify byte order to omit BOM.
        encoder = Charset.forName("UTF-16BE").newEncoder();
        buf.clear();

        buf.putString("ABC", encoder);
        Assert.assertEquals(6, buf.position());
        buf.clear();

        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals('A', buf.get(1));
        Assert.assertEquals(0, buf.get(2));
        Assert.assertEquals('B', buf.get(3));
        Assert.assertEquals(0, buf.get(4));
        Assert.assertEquals('C', buf.get(5));

        buf.putString("D", 10, encoder);
        Assert.assertEquals(10, buf.position());
        buf.clear();
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals('D', buf.get(1));
        Assert.assertEquals(0, buf.get(2));
        Assert.assertEquals(0, buf.get(3));

        buf.putString("EFG", 4, encoder);
        Assert.assertEquals(4, buf.position());
        buf.clear();
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals('E', buf.get(1));
        Assert.assertEquals(0, buf.get(2));
        Assert.assertEquals('F', buf.get(3));
        Assert.assertEquals(0, buf.get(4)); // C may not be overwritten
        Assert.assertEquals('C', buf.get(5)); // C may not be overwritten

        // Test putting an emptry string
        buf.putString("", encoder);
        Assert.assertEquals(0, buf.position());
        buf.putString("", 4, encoder);
        Assert.assertEquals(4, buf.position());
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals(0, buf.get(1));
    }

    public void testGetPrefixedString() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        CharsetEncoder encoder;
        CharsetDecoder decoder;
        encoder = Charset.forName("ISO-8859-1").newEncoder();
        decoder = Charset.forName("ISO-8859-1").newDecoder();

        buf.putShort((short) 3);
        buf.putString("ABCD", encoder);
        buf.clear();
        Assert.assertEquals("ABC", buf.getPrefixedString(decoder));
    }

    public void testPutPrefixedString() throws Exception {
        CharsetEncoder encoder;
        IoBuffer buf = IoBuffer.allocate(16);
        buf.fillAndReset(buf.remaining());
        encoder = Charset.forName("ISO-8859-1").newEncoder();

        // Without autoExpand
        buf.putPrefixedString("ABC", encoder);
        Assert.assertEquals(5, buf.position());
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals(3, buf.get(1));
        Assert.assertEquals('A', buf.get(2));
        Assert.assertEquals('B', buf.get(3));
        Assert.assertEquals('C', buf.get(4));

        buf.clear();
        try {
            buf.putPrefixedString("123456789012345", encoder);
            Assert.fail();
        } catch (BufferOverflowException e) {
            // OK
        }

        // With autoExpand
        buf.clear();
        buf.setAutoExpand(true);
        buf.putPrefixedString("123456789012345", encoder);
        Assert.assertEquals(17, buf.position());
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals(15, buf.get(1));
        Assert.assertEquals('1', buf.get(2));
        Assert.assertEquals('2', buf.get(3));
        Assert.assertEquals('3', buf.get(4));
        Assert.assertEquals('4', buf.get(5));
        Assert.assertEquals('5', buf.get(6));
        Assert.assertEquals('6', buf.get(7));
        Assert.assertEquals('7', buf.get(8));
        Assert.assertEquals('8', buf.get(9));
        Assert.assertEquals('9', buf.get(10));
        Assert.assertEquals('0', buf.get(11));
        Assert.assertEquals('1', buf.get(12));
        Assert.assertEquals('2', buf.get(13));
        Assert.assertEquals('3', buf.get(14));
        Assert.assertEquals('4', buf.get(15));
        Assert.assertEquals('5', buf.get(16));
    }

    public void testPutPrefixedStringWithPrefixLength() throws Exception {
        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        IoBuffer buf = IoBuffer.allocate(16).sweep().setAutoExpand(true);

        buf.putPrefixedString("A", 1, encoder);
        Assert.assertEquals(2, buf.position());
        Assert.assertEquals(1, buf.get(0));
        Assert.assertEquals('A', buf.get(1));

        buf.sweep();
        buf.putPrefixedString("A", 2, encoder);
        Assert.assertEquals(3, buf.position());
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals(1, buf.get(1));
        Assert.assertEquals('A', buf.get(2));

        buf.sweep();
        buf.putPrefixedString("A", 4, encoder);
        Assert.assertEquals(5, buf.position());
        Assert.assertEquals(0, buf.get(0));
        Assert.assertEquals(0, buf.get(1));
        Assert.assertEquals(0, buf.get(2));
        Assert.assertEquals(1, buf.get(3));
        Assert.assertEquals('A', buf.get(4));
    }

    public void testPutPrefixedStringWithPadding() throws Exception {
        CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
        IoBuffer buf = IoBuffer.allocate(16).sweep().setAutoExpand(true);

        buf.putPrefixedString("A", 1, 2, (byte) 32, encoder);
        Assert.assertEquals(3, buf.position());
        Assert.assertEquals(2, buf.get(0));
        Assert.assertEquals('A', buf.get(1));
        Assert.assertEquals(' ', buf.get(2));

        buf.sweep();
        buf.putPrefixedString("A", 1, 4, (byte) 32, encoder);
        Assert.assertEquals(5, buf.position());
        Assert.assertEquals(4, buf.get(0));
        Assert.assertEquals('A', buf.get(1));
        Assert.assertEquals(' ', buf.get(2));
        Assert.assertEquals(' ', buf.get(3));
        Assert.assertEquals(' ', buf.get(4));
    }

    public void testWideUtf8Characters() throws Exception {
        Runnable r = new Runnable() {
            public void run() {
                IoBuffer buffer = IoBuffer.allocate(1);
                buffer.setAutoExpand(true);

                Charset charset = Charset.forName("UTF-8");

                CharsetEncoder encoder = charset.newEncoder();

                for (int i = 0; i < 5; i++) {
                    try {
                        buffer.putString("\u89d2", encoder);
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
        Assert.assertEquals(o, o2);

        // This assertion is just to make sure that deserialization occurred.
        Assert.assertNotSame(o, o2);
    }

    public void testSweepWithZeros() throws Exception {
        IoBuffer buf = IoBuffer.allocate(4);
        buf.putInt(0xdeadbeef);
        buf.clear();
        Assert.assertEquals(0xdeadbeef, buf.getInt());
        Assert.assertEquals(4, buf.position());
        Assert.assertEquals(4, buf.limit());

        buf.sweep();
        Assert.assertEquals(0, buf.position());
        Assert.assertEquals(4, buf.limit());
        Assert.assertEquals(0x0, buf.getInt());
    }

    public void testSweepNonZeros() throws Exception {
        IoBuffer buf = IoBuffer.allocate(4);
        buf.putInt(0xdeadbeef);
        buf.clear();
        Assert.assertEquals(0xdeadbeef, buf.getInt());
        Assert.assertEquals(4, buf.position());
        Assert.assertEquals(4, buf.limit());

        buf.sweep((byte) 0x45);
        Assert.assertEquals(0, buf.position());
        Assert.assertEquals(4, buf.limit());
        Assert.assertEquals(0x45454545, buf.getInt());
    }

    public void testWrapNioBuffer() throws Exception {
        ByteBuffer nioBuf = ByteBuffer.allocate(10);
        nioBuf.position(3);
        nioBuf.limit(7);

        IoBuffer buf = IoBuffer.wrap(nioBuf);
        Assert.assertEquals(3, buf.position());
        Assert.assertEquals(7, buf.limit());
        Assert.assertEquals(10, buf.capacity());
    }

    public void testWrapSubArray() throws Exception {
        byte[] array = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        IoBuffer buf = IoBuffer.wrap(array, 3, 4);
        Assert.assertEquals(3, buf.position());
        Assert.assertEquals(7, buf.limit());
        Assert.assertEquals(10, buf.capacity());

        buf.clear();
        Assert.assertEquals(0, buf.position());
        Assert.assertEquals(10, buf.limit());
        Assert.assertEquals(10, buf.capacity());
    }

    public void testDuplicate() throws Exception {
        IoBuffer original;
        IoBuffer duplicate;

        // Test if the buffer is duplicated correctly.
        original = IoBuffer.allocate(16).sweep();
        original.position(4);
        original.limit(10);
        duplicate = original.duplicate();
        original.put(4, (byte) 127);
        Assert.assertEquals(4, duplicate.position());
        Assert.assertEquals(10, duplicate.limit());
        Assert.assertEquals(16, duplicate.capacity());
        Assert.assertNotSame(original.buf(), duplicate.buf());
        Assert.assertSame(original.buf().array(), duplicate.buf().array());
        Assert.assertEquals(127, duplicate.get(4));

        // Test a duplicate of a duplicate.
        original = IoBuffer.allocate(16);
        duplicate = original.duplicate().duplicate();
        Assert.assertNotSame(original.buf(), duplicate.buf());
        Assert.assertSame(original.buf().array(), duplicate.buf().array());

        // Try to expand.
        original = IoBuffer.allocate(16);
        original.setAutoExpand(true);
        duplicate = original.duplicate();
        Assert.assertFalse(original.isAutoExpand());

        try {
            original.setAutoExpand(true);
            Assert.fail();
        } catch (IllegalStateException e) {
            // OK
        }

        try {
            duplicate.setAutoExpand(true);
            Assert.fail();
        } catch (IllegalStateException e) {
            // OK
        }
    }

    public void testSlice() throws Exception {
        IoBuffer original;
        IoBuffer slice;

        // Test if the buffer is sliced correctly.
        original = IoBuffer.allocate(16).sweep();
        original.position(4);
        original.limit(10);
        slice = original.slice();
        original.put(4, (byte) 127);
        Assert.assertEquals(0, slice.position());
        Assert.assertEquals(6, slice.limit());
        Assert.assertEquals(6, slice.capacity());
        Assert.assertNotSame(original.buf(), slice.buf());
        Assert.assertEquals(127, slice.get(0));
    }

    public void testReadOnlyBuffer() throws Exception {
        IoBuffer original;
        IoBuffer duplicate;

        // Test if the buffer is duplicated correctly.
        original = IoBuffer.allocate(16).sweep();
        original.position(4);
        original.limit(10);
        duplicate = original.asReadOnlyBuffer();
        original.put(4, (byte) 127);
        Assert.assertEquals(4, duplicate.position());
        Assert.assertEquals(10, duplicate.limit());
        Assert.assertEquals(16, duplicate.capacity());
        Assert.assertNotSame(original.buf(), duplicate.buf());
        Assert.assertEquals(127, duplicate.get(4));

        // Try to expand.
        try {
            original = IoBuffer.allocate(16);
            duplicate = original.asReadOnlyBuffer();
            duplicate.putString("A very very very very looooooong string",
                    Charset.forName("ISO-8859-1").newEncoder());
            Assert.fail();
        } catch (ReadOnlyBufferException e) {
            // OK
        }
    }

    public void testGetUnsigned() throws Exception {
        IoBuffer buf = IoBuffer.allocate(16);
        buf.put((byte) 0xA4);
        buf.put((byte) 0xD0);
        buf.put((byte) 0xB3);
        buf.put((byte) 0xCD);
        buf.flip();

        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.mark();
        Assert.assertEquals(0xA4, buf.getUnsigned());
        buf.reset();
        Assert.assertEquals(0xD0A4, buf.getUnsignedShort());
        buf.reset();
        Assert.assertEquals(0xCDB3D0A4L, buf.getUnsignedInt());
    }

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

            Assert.assertEquals(4, buf.indexOf((byte) 0x1));
            Assert.assertEquals(-1, buf.indexOf((byte) 0x2));
            Assert.assertEquals(2, buf.indexOf((byte) 0x3));
            Assert.assertEquals(3, buf.indexOf((byte) 0x4));
        }
    }

    // We need an enum with 64 values
    private static enum TestEnum {
        E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17, E18, E19, E20, E21, E22, E23, E24, E25, E26, E27, E28, E29, E30, E31, E32, E33, E34, E35, E36, E37, E38, E39, E40, E41, E42, E43, E44, E45, E46, E77, E48, E49, E50, E51, E52, E53, E54, E55, E56, E57, E58, E59, E60, E61, E62, E63, E64
    }

    private static enum TooBigEnum {
        E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15, E16, E17, E18, E19, E20, E21, E22, E23, E24, E25, E26, E27, E28, E29, E30, E31, E32, E33, E34, E35, E36, E37, E38, E39, E40, E41, E42, E43, E44, E45, E46, E77, E48, E49, E50, E51, E52, E53, E54, E55, E56, E57, E58, E59, E60, E61, E62, E63, E64, E65
    }

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

    public void testGetEnumSet() {
        IoBuffer buf = IoBuffer.allocate(8);

        // Test empty set
        buf.put((byte) 0);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf
                .getEnumSet(TestEnum.class));

        buf.clear();
        buf.putShort((short) 0);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf
                .getEnumSet(TestEnum.class));

        buf.clear();
        buf.putInt(0);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf
                .getEnumSet(TestEnum.class));

        buf.clear();
        buf.putLong(0L);
        buf.flip();
        assertEquals(EnumSet.noneOf(TestEnum.class), buf
                .getEnumSet(TestEnum.class));

        // Test complete set
        buf.clear();
        buf.put((byte) -1);
        buf.flip();
        assertEquals(EnumSet.range(TestEnum.E1, TestEnum.E8), buf
                .getEnumSet(TestEnum.class));

        buf.clear();
        buf.putShort((short) -1);
        buf.flip();
        assertEquals(EnumSet.range(TestEnum.E1, TestEnum.E16), buf
                .getEnumSetShort(TestEnum.class));

        buf.clear();
        buf.putInt(-1);
        buf.flip();
        assertEquals(EnumSet.range(TestEnum.E1, TestEnum.E32), buf
                .getEnumSetInt(TestEnum.class));

        buf.clear();
        buf.putLong(-1L);
        buf.flip();
        assertEquals(EnumSet.allOf(TestEnum.class), buf
                .getEnumSetLong(TestEnum.class));

        // Test high bit set
        buf.clear();
        buf.put(Byte.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E8), buf.getEnumSet(TestEnum.class));

        buf.clear();
        buf.putShort(Short.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E16), buf
                .getEnumSetShort(TestEnum.class));

        buf.clear();
        buf.putInt(Integer.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E32), buf
                .getEnumSetInt(TestEnum.class));

        buf.clear();
        buf.putLong(Long.MIN_VALUE);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E64), buf
                .getEnumSetLong(TestEnum.class));

        // Test high low bits set
        buf.clear();
        byte b = Byte.MIN_VALUE + 1;
        buf.put(b);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E8), buf
                .getEnumSet(TestEnum.class));

        buf.clear();
        short s = Short.MIN_VALUE + 1;
        buf.putShort(s);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E16), buf
                .getEnumSetShort(TestEnum.class));

        buf.clear();
        buf.putInt(Integer.MIN_VALUE + 1);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E32), buf
                .getEnumSetInt(TestEnum.class));

        buf.clear();
        buf.putLong(Long.MIN_VALUE + 1);
        buf.flip();
        assertEquals(EnumSet.of(TestEnum.E1, TestEnum.E64), buf
                .getEnumSetLong(TestEnum.class));
    }

    public void testBitVectorOverFlow() {
        IoBuffer buf = IoBuffer.allocate(8);
        try {
            buf.putEnumSet(EnumSet.of(TestEnum.E9));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }

        try {
            buf.putEnumSetShort(EnumSet.of(TestEnum.E17));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }

        try {
            buf.putEnumSetInt(EnumSet.of(TestEnum.E33));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }

        try {
            buf.putEnumSetLong(EnumSet.of(TooBigEnum.E65));
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

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

}
