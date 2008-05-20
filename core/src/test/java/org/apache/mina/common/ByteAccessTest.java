package org.apache.mina.common;

import static org.easymock.EasyMock.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.util.byteaccess.BufferByteArray;
import org.apache.mina.util.byteaccess.ByteArray;
import org.apache.mina.util.byteaccess.CompositeByteArray;
import org.apache.mina.util.byteaccess.CompositeByteArrayRelativeReader;
import org.apache.mina.util.byteaccess.CompositeByteArrayRelativeWriter;
import org.apache.mina.util.byteaccess.IoAbsoluteReader;
import org.apache.mina.util.byteaccess.IoAbsoluteWriter;
import org.apache.mina.util.byteaccess.IoRelativeReader;
import org.apache.mina.util.byteaccess.IoRelativeWriter;
import org.apache.mina.util.byteaccess.SimpleByteArrayFactory;
import org.apache.mina.util.byteaccess.ByteArray.Cursor;
import org.apache.mina.util.byteaccess.CompositeByteArray.CursorListener;
import org.apache.mina.util.byteaccess.CompositeByteArrayRelativeWriter.ChunkedExpander;
import org.apache.mina.util.byteaccess.CompositeByteArrayRelativeWriter.Flusher;
import org.easymock.IMocksControl;

/**
 * Tests classes in the <code>byteaccess</code> package.
 */
public class ByteAccessTest extends TestCase {

    private final List<String> operations = new ArrayList<String>();

    private void resetOperations() {
        operations.clear();
    }

    private void assertOperationCountEquals(int expectedCount) {
        assertEquals("Operations: " + operations, expectedCount, operations.size());
    }

    private void addOperation(String description) {
        operations.add(description);
    }

    public void testBufferByteArray() throws Exception {
        ByteArray ba = getByteArrayFactory().create(1000);
        testAbsoluteReaderAndWriter(0, 1000, ba, ba);
        testAbsoluteReaderAndWriter(0, 1000, ba, ba);
        Cursor readCursor = ba.cursor();
        Cursor writeCursor = ba.cursor();
        testRelativeReaderAndWriter(1000, readCursor, writeCursor);
    }

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
                ByteBuffer bb = ByteBuffer.allocate(size);
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

}
