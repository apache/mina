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

import static org.easymock.EasyMock.createStrictControl;
import static org.junit.Assert.assertEquals;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.byteaccess.ByteArray.Cursor;
import org.apache.mina.util.byteaccess.CompositeByteArray.CursorListener;
import org.apache.mina.util.byteaccess.CompositeByteArrayRelativeWriter.ChunkedExpander;
import org.apache.mina.util.byteaccess.CompositeByteArrayRelativeWriter.Flusher;
import org.easymock.IMocksControl;
import org.junit.Test;

/**
 * Tests classes in the <code>byteaccess</code> package.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteAccessTest {

    private List<String> operations = new ArrayList<String>();

    private void resetOperations() {
        operations.clear();
    }

    private void assertOperationCountEquals(int expectedCount) {
        assertEquals("Operations: " + operations, expectedCount, operations.size());
    }

    private void addOperation(String description) {
        operations.add(description);
    }

    @Test
    public void testBufferByteArray() throws Exception {
        ByteArray ba = getByteArrayFactory().create(1000);
        testAbsoluteReaderAndWriter(0, 1000, ba, ba);
        testAbsoluteReaderAndWriter(0, 1000, ba, ba);
        Cursor readCursor = ba.cursor();
        Cursor writeCursor = ba.cursor();
        testRelativeReaderAndWriter(1000, readCursor, writeCursor);
    }

    @Test
    public void testCompositeAddAndRemove() throws Exception {
        CompositeByteArray cba = new CompositeByteArray();
        assertEquals(0, cba.first());
        assertEquals(0, cba.last());
        cba.addFirst(getByteArrayFactory().create(100));
        assertEquals(-100, cba.first());
        assertEquals(0, cba.last());
        cba.addFirst(getByteArrayFactory().create(100));
        assertEquals(-200, cba.first());
        assertEquals(0, cba.last());
        cba.addLast(getByteArrayFactory().create(100));
        assertEquals(-200, cba.first());
        assertEquals(100, cba.last());
        cba.removeFirst();
        assertEquals(-100, cba.first());
        assertEquals(100, cba.last());
        cba.addLast(getByteArrayFactory().create(100));
        assertEquals(-100, cba.first());
        assertEquals(200, cba.last());
        cba.removeLast();
        assertEquals(-100, cba.first());
        assertEquals(100, cba.last());
        cba.removeFirst();
        assertEquals(0, cba.first());
        assertEquals(100, cba.last());
        cba.removeFirst();
        assertEquals(100, cba.first());
        assertEquals(100, cba.last());
        cba.addLast(getByteArrayFactory().create(100));
        assertEquals(100, cba.first());
        assertEquals(200, cba.last());
    }

    private BufferByteArray wrapString(String string) {
        byte[] bytes = string.getBytes();
        IoBuffer bb = IoBuffer.wrap(bytes);
        BufferByteArray ba = new BufferByteArray(bb) {

            @Override
            public void free() {
                addOperation(this + ".free()");
                // Nothing to do.
            }

        };
        return ba;
    }

    private String toString(ByteArray ba) {
        IoBuffer bb = IoBuffer.allocate(ba.length());
        ba.get(0, bb);
        byte[] bytes = bb.array();
        String string = new String(bytes);
        return string;
    }

    @Test
    public void testCompositeStringJoin() throws Exception {
        ByteArray ba1 = wrapString("Hello");
        ByteArray ba2 = wrapString("MINA");
        ByteArray ba3 = wrapString("World");

        CompositeByteArray cba = new CompositeByteArray();
        cba.addLast(ba1);
        cba.addLast(ba2);
        cba.addLast(ba3);

        assertEquals("HelloMINAWorld", toString(cba));
    }

    @Test
    public void testCompositeCursor() throws Exception {
        IMocksControl mc = createStrictControl();

        ByteArray ba1 = getByteArrayFactory().create(10);
        ByteArray ba2 = getByteArrayFactory().create(10);
        ByteArray ba3 = getByteArrayFactory().create(10);


        CompositeByteArray cba = new CompositeByteArray();
        cba.addLast(ba1);
        cba.addLast(ba2);
        cba.addLast(ba3);

        CursorListener cl = mc.createMock(CursorListener.class);

        mc.reset();
        mc.replay();
        Cursor cursor = cba.cursor(cl);
        mc.verify();

        mc.reset();
        cl.enteredFirstComponent(0, ba1);
        mc.replay();
        cursor.get();
        mc.verify();

        mc.reset();
        mc.replay();
        cursor.setIndex(10);
        mc.verify();

        mc.reset();
        cl.enteredNextComponent(10, ba2);
        mc.replay();
        cursor.put((byte) 55);
        mc.verify();

        mc.reset();
        mc.replay();
        cursor.setIndex(9);
        mc.verify();

        mc.reset();
        cl.enteredPreviousComponent(0, ba1);
        cl.enteredNextComponent(10, ba2);
        mc.replay();
        cursor.putInt(66);
        mc.verify();

        mc.reset();
        cl.enteredNextComponent(20, ba3);
        mc.replay();
        cursor.setIndex(29);
        cursor.get();
        mc.verify();

        cba.removeLast(); // Force cursor to relocate itself.

        mc.reset();
        cl.enteredLastComponent(10, ba2);
        mc.replay();
        cursor.setIndex(15);
        cursor.get();
        mc.verify();

        mc.reset();
        cl.enteredPreviousComponent(0, ba1);
        mc.replay();
        cursor.setIndex(0);
        cursor.get();
        mc.verify();
    }

    @Test
    public void testCompositeByteArray() throws Exception {
        CompositeByteArray ba = new CompositeByteArray();
        for (int i = 0; i < 1000; i += 100) {
            ba.addLast(getByteArrayFactory().create(100));
        }
        resetOperations();
        testAbsoluteReaderAndWriter(0, 1000, ba, ba);
        testAbsoluteReaderAndWriter(0, 1000, ba, ba);
        assertOperationCountEquals(0);
        Cursor readCursor = ba.cursor();
        Cursor writeCursor = ba.cursor();
        testRelativeReaderAndWriter(1000, readCursor, writeCursor);
        assertOperationCountEquals(0);
    }

    @Test
    public void testCompositeByteArrayRelativeReaderAndWriter() throws Exception {
        CompositeByteArray cba = new CompositeByteArray();
        CompositeByteArrayRelativeReader cbarr = new CompositeByteArrayRelativeReader(cba, true);
        CompositeByteArrayRelativeWriter cbarw = new CompositeByteArrayRelativeWriter(cba, getExpander(100), getFlusher(), false);
        resetOperations();
        testRelativeReaderAndWriter(10, cbarr, cbarw);
        assertOperationCountEquals(2);
        resetOperations();
        testRelativeReaderAndWriter(100, cbarr, cbarw);
        assertOperationCountEquals(3);
        resetOperations();
        testRelativeReaderAndWriter(1000, cbarr, cbarw);
        assertOperationCountEquals(30);
        resetOperations();
        testRelativeReaderAndWriter(10000, cbarr, cbarw);
        assertOperationCountEquals(300);
        resetOperations();
        testRelativeReaderAndWriter(90, cbarr, cbarw);
        assertOperationCountEquals(0); // Last free doesn't occur, since cursor only moves lazily.
    }

    @Test
    public void testCompositeByteArrayRelativeReaderAndWriterWithFlush() throws Exception {
        CompositeByteArray cba = new CompositeByteArray();
        CompositeByteArrayRelativeReader cbarr = new CompositeByteArrayRelativeReader(cba, true);
        CompositeByteArrayRelativeWriter cbarw = new CompositeByteArrayRelativeWriter(cba, getExpander(100), getFlusher(), true);
        resetOperations();
        testRelativeReaderAndWriter(10, cbarr, cbarw);
        assertOperationCountEquals(2);
        resetOperations();
        testRelativeReaderAndWriter(100, cbarr, cbarw);
        assertOperationCountEquals(4);
        resetOperations();
        testRelativeReaderAndWriter(1000, cbarr, cbarw);
        assertOperationCountEquals(40);
        resetOperations();
        testRelativeReaderAndWriter(10000, cbarr, cbarw);
        assertOperationCountEquals(400);
        resetOperations();
        testRelativeReaderAndWriter(90, cbarr, cbarw);
        assertOperationCountEquals(0); // Last free doesn't occur, since cursor only moves lazily.
    }

    @Test
    public void testCompositeRemoveTo() throws Exception {
        CompositeByteArray cba = new CompositeByteArray();
        {
            // Remove nothing.
            resetOperations();
            ByteArray removed = cba.removeTo(0);
            assertEquals(0, removed.first());
            assertEquals(0, removed.last());
            assertEquals(0, cba.first());
            assertEquals(0, cba.last());
            removed.free();
            assertOperationCountEquals(0);
        }
        cba.addLast(getByteArrayFactory().create(100));
        {
            // Remove nothing.
            resetOperations();
            ByteArray removed = cba.removeTo(0);
            assertEquals(0, removed.first());
            assertEquals(0, removed.last());
            assertEquals(0, cba.first());
            assertEquals(100, cba.last());
            removed.free();
            assertOperationCountEquals(0);
        }
        {
            // Remove entire component.
            resetOperations();
            ByteArray removed = cba.removeTo(100);
            assertEquals(0, removed.first());
            assertEquals(100, removed.last());
            assertEquals(100, cba.first());
            assertEquals(100, cba.last());
            removed.free();
            assertOperationCountEquals(1);
        }
        {
            // Remove nothing.
            resetOperations();
            ByteArray removed = cba.removeTo(100);
            assertEquals(0, removed.first());
            assertEquals(0, removed.last());
            assertEquals(100, cba.first());
            assertEquals(100, cba.last());
            removed.free();
            assertOperationCountEquals(0);
        }
        cba.addLast(getByteArrayFactory().create(100));
        {
            // Remove nothing.
            resetOperations();
            ByteArray removed = cba.removeTo(100);
            assertEquals(0, removed.first());
            assertEquals(0, removed.last());
            assertEquals(100, cba.first());
            assertEquals(200, cba.last());
            removed.free();
            assertOperationCountEquals(0);
        }
        {
            // Remove half a component.
            resetOperations();
            ByteArray removed = cba.removeTo(150);
            assertEquals(0, removed.first());
            assertEquals(50, removed.last());
            assertEquals(150, cba.first());
            assertEquals(200, cba.last());
            removed.free();
            assertOperationCountEquals(0); // Doesn't free until component finished.
        }
        {
            // Remove nothing.
            resetOperations();
            ByteArray removed = cba.removeTo(150);
            assertEquals(0, removed.first());
            assertEquals(0, removed.last());
            assertEquals(150, cba.first());
            assertEquals(200, cba.last());
            removed.free();
            assertOperationCountEquals(0);
        }
        {
            // Remove other half.
            resetOperations();
            ByteArray removed = cba.removeTo(200);
            assertEquals(0, removed.first());
            assertEquals(50, removed.last());
            assertEquals(200, cba.first());
            assertEquals(200, cba.last());
            removed.free();
            assertOperationCountEquals(1); // Frees ByteArray behind both buffers.
        }
    }
    
    @Test
    public void testCompositeByteArraySlicing() {
        CompositeByteArray cba = new CompositeByteArray();
        cba.addLast(getByteArrayFactory().create(10));
        cba.addLast(getByteArrayFactory().create(10));
        cba.addLast(getByteArrayFactory().create(10));
        testByteArraySlicing(cba, 0, 30);
        testByteArraySlicing(cba, 5, 10);
        testByteArraySlicing(cba, 10, 20);
        testByteArraySlicing(cba, 1, 28);
        testByteArraySlicing(cba, 19, 2);
    }
    
    @Test
    public void testBufferByteArraySlicing() {
        ByteArray bba = getByteArrayFactory().create(30);
        testByteArraySlicing(bba, 0, 30);
        testByteArraySlicing(bba, 5, 10);
        testByteArraySlicing(bba, 10, 20);
        testByteArraySlicing(bba, 1, 28);
        testByteArraySlicing(bba, 19, 2);
        
    }
    
    private void testByteArraySlicing(ByteArray ba, int start, int length) {
        ByteArray slice = ba.slice(start, length);
        for (int i = 0; i < length; i++) {
            byte b1 = (byte) (i % 67);
            byte b2 = (byte) (i % 36);
            int sourceIndex = i + start;
            int sliceIndex = i;
            ba.put(sourceIndex, b1);
            assertEquals(b1, ba.get(sourceIndex));
            assertEquals(b1, slice.get(sliceIndex));
            slice.put(sliceIndex, b2);
            assertEquals(b2, ba.get(sourceIndex));
            assertEquals(b2, slice.get(sliceIndex));
        }
    }

    private ChunkedExpander getExpander(final int chunkSize) {
        return new ChunkedExpander(getByteArrayFactory(), chunkSize) {
            @Override
            public void expand(CompositeByteArray cba, int minSize) {
                addOperation("ChunkedExpander(" + chunkSize + ").expand(" + cba + "," + minSize + ")");
                super.expand(cba, minSize);
            }
        };
    }

    private Flusher getFlusher() {
        return new CompositeByteArrayRelativeWriter.Flusher() {

            public void flush(ByteArray ba) {
                addOperation("Flusher().flush(" + ba + ")");
                ba.free();
            }

        };
    }

    private SimpleByteArrayFactory getByteArrayFactory() {
        return new SimpleByteArrayFactory() {
            @Override
            public ByteArray create(final int size) {
                if (size < 0) {
                    throw new IllegalArgumentException(
                            "Buffer size must not be negative:" + size);
                }
                IoBuffer bb = IoBuffer.allocate(size);
                ByteArray ba = new BufferByteArray(bb) {

                    @Override
                    public void free() {
                        addOperation(this + ".free()");
                        // Nothing to do.
                    }

                };
                addOperation("SimpleByteArrayFactory().create(" + size + ") = " + ba);
                return ba;
            }
        };
    }

    private void testRelativeReaderAndWriter(int length, IoRelativeReader reader, IoRelativeWriter writer) {
        for (int i = 0; i < length; i++) {
            byte b = (byte) (i % 67);
            writer.put(b);
            assertEquals(b, reader.get());
        }
    }

    private void testAbsoluteReaderAndWriter(int start, int length, IoAbsoluteReader reader, IoAbsoluteWriter writer) {
        for (int i = start; i < length; i++) {
            byte b = (byte) (i % 67);
            writer.put(i, b);
            assertEquals(b, reader.get(i));
        }
    }

    @Test
    public void testByteArrayPrimitiveAccess() {
        ByteArray bbaBig = getByteArrayFactory().create(1000);
        bbaBig.order(ByteOrder.BIG_ENDIAN);
        testPrimitiveAccess(bbaBig.cursor(), bbaBig.cursor());

        ByteArray bbaLittle = getByteArrayFactory().create(1000);
        bbaLittle.order(ByteOrder.LITTLE_ENDIAN);
        testPrimitiveAccess(bbaLittle.cursor(), bbaLittle.cursor());
    }

    @Test
    public void testByteArrayBufferAccess() {
        ByteArray ba = getByteArrayFactory().create(1);
        ba.put(0, (byte) 99);
        IoBuffer bb = IoBuffer.allocate(2);
        
        bb.clear();
        Cursor cursor = ba.cursor();
        assertEquals(0, cursor.getIndex());
        assertEquals(1, cursor.getRemaining());
        assertEquals(0, bb.position());
        assertEquals(2, bb.remaining());
        cursor.get(bb);
        assertEquals(1, cursor.getIndex());
        assertEquals(0, cursor.getRemaining());
        assertEquals(1, bb.position());
        assertEquals(1, bb.remaining());
    }
    
    @Test
    public void testCompositeByteArrayPrimitiveAccess() {
        CompositeByteArray cbaBig = new CompositeByteArray();
        cbaBig.order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 1000; i++) {
            ByteArray component = getByteArrayFactory().create(1);
            component.order(ByteOrder.BIG_ENDIAN);
            cbaBig.addLast(component);
        }
        testPrimitiveAccess(cbaBig.cursor(), cbaBig.cursor());

        CompositeByteArray cbaLittle = new CompositeByteArray();
        cbaLittle.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 1000; i++) {
            ByteArray component = getByteArrayFactory().create(1);
            component.order(ByteOrder.LITTLE_ENDIAN);
            cbaLittle.addLast(component);
        }
        testPrimitiveAccess(cbaLittle.cursor(), cbaLittle.cursor());
    }

    @Test
    public void testCompositeByteArrayWrapperPrimitiveAccess() {
        CompositeByteArray cbaBig = new CompositeByteArray();
        cbaBig.order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 1000; i++) {
            ByteArray component = getByteArrayFactory().create(1);
            component.order(ByteOrder.BIG_ENDIAN);
            cbaBig.addLast(component);
        }
        testPrimitiveAccess(new CompositeByteArrayRelativeWriter(cbaBig, getExpander(10), getFlusher(), false), new CompositeByteArrayRelativeReader(cbaBig, true));

        CompositeByteArray cbaLittle = new CompositeByteArray();
        cbaLittle.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 1000; i++) {
            ByteArray component = getByteArrayFactory().create(1);
            component.order(ByteOrder.LITTLE_ENDIAN);
            cbaLittle.addLast(component);
        }
        testPrimitiveAccess(new CompositeByteArrayRelativeWriter(cbaLittle, getExpander(10), getFlusher(), false), new CompositeByteArrayRelativeReader(cbaLittle, true));
    }

    private void testPrimitiveAccess(IoRelativeWriter write, IoRelativeReader read) {
        byte b = (byte) 0x12;
        write.put(b);
        assertEquals(b, read.get());

        short s = (short) 0x12;
        write.putShort(s);
        assertEquals(s, read.getShort());

        int i = 0x12345678;
        write.putInt(i);
        assertEquals(i, read.getInt());

        long l = 0x1234567890123456L;
        write.putLong(l);
        assertEquals(l, read.getLong());

        float f = Float.intBitsToFloat(i);
        write.putFloat(f);
        assertEquals(f, read.getFloat(), 0);

        double d = Double.longBitsToDouble(l);
        write.putDouble(d);
        assertEquals(d, read.getDouble(), 0);

        char c = (char) 0x1234;
        write.putChar(c);
        assertEquals(c, read.getChar());
    }

}
