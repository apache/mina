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
package org.apache.mina.queue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * The default {@link ByteBufferQueue} implementation.
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultByteBufferQueue extends AbstractIoQueue<ByteBuffer> implements
        ByteBufferQueue {

    private static final int DEFAULT_CAPACITY_INCREMENT = 512;

    private final Queue<ByteBuffer> queue;
    private final ByteOrder order;
    private final ByteBufferFactory bufferFactory;
    private final int capacityIncrement;
    private ByteBuffer tail;
    private int length;

    public DefaultByteBufferQueue() {
        this(
                new CircularQueue<ByteBuffer>(), ByteOrder.BIG_ENDIAN,
                HeapByteBufferFactory.INSTANCE, DEFAULT_CAPACITY_INCREMENT);
    }

    public DefaultByteBufferQueue(int capacityIncrement) {
        this(
                new CircularQueue<ByteBuffer>(), ByteOrder.BIG_ENDIAN,
                HeapByteBufferFactory.INSTANCE, capacityIncrement);
    }

    public DefaultByteBufferQueue(ByteOrder order) {
        this(
                new CircularQueue<ByteBuffer>(), order,
                HeapByteBufferFactory.INSTANCE, DEFAULT_CAPACITY_INCREMENT);
    }

    public DefaultByteBufferQueue(ByteOrder order, int capacityIncrement) {
        this(
                new CircularQueue<ByteBuffer>(), order,
                HeapByteBufferFactory.INSTANCE, capacityIncrement);
    }

    public DefaultByteBufferQueue(
            Queue<ByteBuffer> queue, ByteOrder order,
            ByteBufferFactory bufferFactory, int capacityIncrement) {

        if (queue == null) {
            throw new NullPointerException("queue");
        }
        if (order == null) {
            throw new NullPointerException("order");
        }
        if (bufferFactory == null) {
            throw new NullPointerException("bufferFactory");
        }
        if (capacityIncrement < 8) {
            throw new IllegalArgumentException(
                    "capacityIncrement: " + capacityIncrement +
                    " (expected: an integer greater than or equals to 8)");
        }

        this.queue = queue;
        this.order = order;
        this.bufferFactory = bufferFactory;
        this.capacityIncrement = capacityIncrement;
    }

    @Override
    protected boolean doOffer(ByteBuffer e) {
        e = e.duplicate();

        // Refuse to add an empty buffer.
        if (!e.hasRemaining()) {
            return false;
        }

        tail = null;
        e.order(order);
        return queue.offer(e);
    }

    @Override
    protected ByteBuffer doPoll() {
        ByteBuffer buf = queue.poll();
        if (buf == tail) {
            tail = null;
        }
        return buf;
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
        final Iterator<ByteBuffer> i = queue.iterator();
        return new Iterator<ByteBuffer>() {
            public boolean hasNext() {
                return i.hasNext();
            }

            public ByteBuffer next() {
                return i.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return queue.size();
    }

    public ByteBuffer peek() {
        return queue.peek();
    }

    public ByteOrder order() {
        return order;
    }

    public int length() {
        return length;
    }

    public void addByte(byte value) {
        if (!offerByte(value)) {
            throw new IllegalStateException();
        }
    }

    public void addShort(short value) {
        if (!offerShort(value)) {
            throw new IllegalStateException();
        }
    }

    public void addInt(int value) {
        if (!offerInt(value)) {
            throw new IllegalStateException();
        }
    }

    public void addLong(long value) {
        if (!offerLong(value)) {
            throw new IllegalStateException();
        }
    }

    public void addFloat(float value) {
        if (!offerFloat(value)) {
            throw new IllegalStateException();
        }
    }

    public void addDouble(double value) {
        if (!offerDouble(value)) {
            throw new IllegalStateException();
        }
    }

    public boolean offerByte(byte value) {
        ByteBuffer tail = tail(1);
        if (tail == null) {
            return false;
        }
        int index = tail.limit();
        tail.limit(index + 1);
        tail.put(index, value);
        length ++;
        return true;
    }

    public boolean offerShort(short value) {
        ByteBuffer tail = tail(2);
        if (tail == null) {
            return false;
        }
        int index = tail.limit();
        tail.limit(index + 2);
        tail.putShort(index, value);
        length += 2;
        return true;
    }

    public boolean offerInt(int value) {
        ByteBuffer tail = tail(4);
        if (tail == null) {
            return false;
        }
        int index = tail.limit();
        tail.limit(index + 4);
        tail.putInt(index, value);
        length += 4;
        return true;
    }

    public boolean offerLong(long value) {
        ByteBuffer tail = tail(8);
        if (tail == null) {
            return false;
        }
        int index = tail.limit();
        tail.limit(index + 8);
        tail.putLong(index, value);
        length += 8;
        return true;
    }

    public boolean offerFloat(float value) {
        return offerInt(Float.floatToIntBits(value));
    }

    public boolean offerDouble(double value) {
        return offerLong(Double.doubleToLongBits(value));
    }

    public boolean pollSlice(Queue<ByteBuffer> destination, int length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "length: " + length +
                    " (expected: zero or a positive integer)");
        }

        if (length > this.length) {
            return false;
        }

        if (length == 0) {
            return true;
        }

        int bytesToRead = length;
        for (;;) {
            ByteBuffer element = queue.peek();
            if (element == null) {
                // element shouldn't be null unless it's accessed concurrently.
                throw new ConcurrentModificationException();
            }

            int remaining = element.remaining();
            if (remaining == 0) {
                removeAndAssert(element);
                continue;
            }

            if (remaining >= bytesToRead) {
                int position = element.position();
                ByteBuffer lastElement = element.duplicate();
                lastElement.limit(position + bytesToRead);
                if (destination.offer(lastElement)) {
                    element.position(position + bytesToRead);
                    this.length -= bytesToRead;
                    return true;
                } else {
                    return false;
                }
            }

            if (destination.offer(element.duplicate())) {
                removeAndAssert(element);
                element.limit(element.limit());
                this.length -= remaining;
                bytesToRead -= remaining;
            } else {
                return false;
            }
        }
    }

    public boolean peekSlice(Queue<ByteBuffer> destination, int length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "length: " + length +
                    " (expected: zero or a positive integer)");
        }

        if (length > this.length) {
            return false;
        }

        // No need to fetch anything if the specified length is zero.
        if (length == 0) {
            return true;
        }

        // Optimize when it's enough with one slice.
        ByteBuffer element = safeElement();
        if (element.remaining() >= length) {
            ByteBuffer lastElement = element.duplicate();
            lastElement.limit(element.position() + length);
            return destination.offer(lastElement);
        }

        // Otherwise we have to use an iterator.
        int bytesToRead = length;
        for (ByteBuffer e: this) {
            int remaining = e.remaining();
            if (remaining == 0) {
                continue;
            }

            if (remaining >= bytesToRead) {
                ByteBuffer lastElement = element.duplicate();
                lastElement.limit(e.position() + bytesToRead);
                if (!destination.offer(lastElement)) {
                    return false;
                } else {
                    return true;
                }
            }

            if (!destination.offer(element)) {
                return false;
            }
            bytesToRead -= remaining;
        }

        // The only case that we reach here is concurrent access.
        throw new ConcurrentModificationException();
    }

    public boolean elementAsSlice(Queue<ByteBuffer> destination, int length) {
        checkSequentialAccess(length);
        return peekSlice(destination, length);
    }

    public boolean removeSlice(Queue<ByteBuffer> destination, int length) {
        checkSequentialAccess(length);
        return pollSlice(destination, length);
    }

    public boolean getSlice(Queue<ByteBuffer> destination, int byteIndex, int length) {
        checkRandomAccess(byteIndex, length);

        // No need to fetch anything if the specified length is zero.
        if (length == 0) {
            return true;
        }

        if (byteIndex == 0) {
            return elementAsSlice(destination, length);
        }

        // Optimize when it's enough with one slice.
        ByteBuffer element = safeElement();
        if (element.remaining() >= byteIndex + length) {
            ByteBuffer lastElement = element.duplicate();
            lastElement.position(lastElement.position() + byteIndex);
            lastElement.limit(lastElement.position() + length);
            return destination.offer(lastElement);
        }

        // Otherwise we have to use an iterator.
        int bytesToRead = length;
        for (ByteBuffer b: this) {
            int remaining = b.remaining();

            // Skip until we find the element which matches to the offset.
            if (remaining <= byteIndex) {
                byteIndex -= remaining;
                continue;
            }

            if (remaining > byteIndex + length) {
                ByteBuffer lastElement = b.duplicate();
                lastElement.position(lastElement.position() + byteIndex);
                lastElement.limit(lastElement.position() + length);
                return destination.offer(lastElement);
            }

            b = b.duplicate();
            b.position(b.position() + byteIndex);
            destination.offer(b);
            bytesToRead -= remaining - byteIndex;
            byteIndex -= remaining - byteIndex;
        }

        throw new ConcurrentModificationException();
    }

    public byte   removeByte() {
        checkSequentialAccess(1);
        ByteBuffer e = safeElement();
        length --;
        byte value = e.get();

        if (!e.hasRemaining()) {
            trim();
        }
        return value;
    }

    public short  removeShort() {
        checkSequentialAccess(2);
        // Try to read in one shot.
        ByteBuffer e = safeElement();
        int remaining = e.remaining();
        switch (remaining) {
        case 0: case 1:
            break;
        case 2:
            length -= 2;
            short value = e.getShort();
            trim();
            return value;
        default:
            length -= 2;
            return e.getShort();
        }

        // Otherwise, read byte by byte. (inefficient!)
        return applyByteOrder((short) readByteByByte(2));
    }

    public int    removeInt() {
        checkSequentialAccess(4);
        // Try to read in one shot.
        ByteBuffer e = safeElement();
        int remaining = e.remaining();
        switch (remaining) {
        case 0: case 1: case 2: case 3:
            break;
        case 4:
            length -= 4;
            int value = e.getInt();
            trim();
            return value;
        default:
            length -= 4;
            return e.getInt();
        }

        // Otherwise, read byte by byte. (inefficient!)
        return applyByteOrder((int) readByteByByte(4));
    }

    public long   removeLong() {
        checkSequentialAccess(8);
        // Try to read in one shot.
        ByteBuffer e = safeElement();
        int remaining = e.remaining();
        switch (remaining) {
        case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            break;
        case 8:
            length -= 8;
            long value = e.getLong();
            trim();
            return value;
        default:
            length -= 8;
            return e.getLong();
        }

        // Otherwise, read byte by byte. (inefficient!)
        return applyByteOrder(readByteByByte(8));
    }

    public float  removeFloat() {
        return Float.intBitsToFloat(removeInt());
    }

    public double removeDouble() {
        return Double.longBitsToDouble(removeLong());
    }

    public void   discard(int length) {
        checkSequentialAccess(length);

        int bytesToDiscard = length;
        while (bytesToDiscard > 0) {
            ByteBuffer element = queue.peek();
            int remaining = element.remaining();
            if (remaining == 0) {
                removeAndAssert(element);
                continue;
            }

            if (remaining >= bytesToDiscard) {
                element.position(element.position() + bytesToDiscard);
                this.length -= bytesToDiscard;
                break;
            }

            removeAndAssert(element);
            element.limit(element.limit());
            bytesToDiscard -= remaining;
            this.length -= remaining;
        }
    }

    public byte   elementAsByte  () {
        checkSequentialAccess(1);
        ByteBuffer e = safeElement();
        return e.get(e.position());
    }

    public short  elementAsShort () {
        checkSequentialAccess(2);
        // Try to read in one shot.
        ByteBuffer e = safeElement();
        if (e.remaining() >= 2) {
            return e.getShort(e.position());
        }

        // Otherwise, read byte by byte. (inefficient!)
        return applyByteOrder((short) getByteByByte(2));
    }

    public int    elementAsInt   () {
        checkSequentialAccess(4);
        // Try to read in one shot.
        ByteBuffer e = safeElement();
        if (e.remaining() >= 4) {
            return e.getInt(e.position());
        }

        // Otherwise, read byte by byte. (inefficient!)
        return applyByteOrder((int) getByteByByte(4));
    }

    public long   elementAsLong  () {
        checkSequentialAccess(8);
        // Try to read in one shot.
        ByteBuffer e = safeElement();
        if (e.remaining() >= 8) {
            return e.getLong(e.position());
        }

        // Otherwise, read byte by byte. (inefficient!)
        return applyByteOrder(getByteByByte(8));
    }

    public float  elementAsFloat () {
        return Float.intBitsToFloat(elementAsInt());
    }

    public double elementAsDouble() {
        return Double.longBitsToDouble(elementAsLong());
    }

    public byte   getByte  (int byteIndex) {
        checkRandomAccess(byteIndex, 1);
        if (byteIndex == 0) {
            return elementAsByte();
        }

        // Get the value from the first element if possible.
        ByteBuffer e = safeElement();
        if (e.remaining() > byteIndex) {
            return e.get(e.position() + byteIndex);
        }

        // Otherwise, start expensive traversal.
        for (ByteBuffer b: this) {
            if (b.remaining() > byteIndex) {
                return e.get(e.position() + byteIndex);
            } else {
                byteIndex -= e.remaining();
            }
        }

        // Should never reach here unless concurrent modification happened.
        throw new ConcurrentModificationException();
    }

    public short  getShort (int byteIndex) {
        checkRandomAccess(byteIndex, 2);
        if (byteIndex == 0) {
            return elementAsByte();
        }

        // Get the value from the first element if possible.
        ByteBuffer e = safeElement();
        if (e.remaining() >= byteIndex + 2) {
            return e.getShort(e.position() + byteIndex);
        }

        // Otherwise, start expensive traversal.
        return applyByteOrder((short) getByteByByte(byteIndex, 2));
    }

    public int    getInt   (int byteIndex) {
        checkRandomAccess(byteIndex, 4);
        if (byteIndex == 0) {
            return elementAsByte();
        }

        // Get the value from the first element if possible.
        ByteBuffer e = safeElement();
        if (e.remaining() >= byteIndex + 4) {
            return e.getInt(e.position() + byteIndex);
        }

        // Otherwise, start expensive traversal.
        return applyByteOrder((int) getByteByByte(byteIndex, 4));
    }

    public long   getLong  (int byteIndex) {
        checkRandomAccess(byteIndex, 8);
        if (byteIndex == 0) {
            return elementAsByte();
        }

        // Get the value from the first element if possible.
        ByteBuffer e = safeElement();
        if (e.remaining() >= byteIndex + 8) {
            return e.getLong(e.position() + byteIndex);
        }

        // Otherwise, start expensive traversal.
        return applyByteOrder(getByteByByte(byteIndex, 8));
    }

    public float  getFloat (int byteIndex) {
        return Float.intBitsToFloat(getInt(byteIndex));
    }

    public double getDouble(int byteIndex) {
        return Double.longBitsToDouble(getLong(byteIndex));
    }

    public ByteBuffer merge() {
        ByteBuffer buf = bufferFactory.newByteBuffer(length);
        buf.order(order);
        for (ByteBuffer e: queue) {
            buf.put(e.duplicate());
        }
        buf.clear();
        return buf;
    }

    private ByteBuffer tail(int length) {
        ByteBuffer oldTail = tail;
        if (oldTail == null || oldTail.capacity() - oldTail.limit() < length) {
            ByteBuffer newTail = bufferFactory.newByteBuffer(capacityIncrement);
            newTail.order(order);
            newTail.limit(0);
            if (!queue.offer(newTail)) {
                return null;
            }
            tail = newTail;
            return newTail;
        } else {
            return oldTail;
        }
    }

    /**
     * The same operation with {@link #element()} except that it never returns
     * an empty buffer.
     */
    private ByteBuffer safeElement() {
        for (;;) {
            ByteBuffer e = queue.element();
            if (e.hasRemaining()) {
                return e;
            } else {
                removeAndAssert(e);
            }
        }
    }

    /**
     * Removes any empty buffers in the head of this queue.
     */
    private void trim() {
        for (;;) {
            ByteBuffer e = queue.peek();
            if (e == null || e.hasRemaining()) {
                break;
            }
            removeAndAssert(e);
        }
    }

    /**
     * Removes the first element and make sure it is same with the specified
     * buffer.
     */
    private void removeAndAssert(ByteBuffer e) {
        ByteBuffer removedElement = queue.remove();
        assert removedElement == e;
    }

    /**
     * Throws an exception if the specified length is illegal or the length of
     * this queue is less than the specified integer.
     */
    private void checkSequentialAccess(int length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                    "length: " + length +
                    " (expected: zero or a positive integer)");
        }

        if (this.length < length) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Throws an exception if the byteIndex is incorrect or the length of this
     * queue is less than the specified integer + byteIndex.
     */
    private void checkRandomAccess(int byteIndex, int length) {
        if (byteIndex < 0) {
            throw new IllegalArgumentException(
                    "byteIndex: " + byteIndex +
                    " (expected: 0 or a positive integer)");
        }

        if (this.length < byteIndex + length) {
            throw new IndexOutOfBoundsException();
        }
    }

    private long readByteByByte(int bytesToRead) {
        long value = 0;
        for (ByteBuffer b: this) {
            int remaining = b.remaining();
            for (int i = 0; i < remaining; i ++) {
                value = value << 8 | b.get(b.position() + i);
                bytesToRead --;
                if (bytesToRead == 0) {
                    return value;
                }
            }
        }

        throw new ConcurrentModificationException();
    }

    private long getByteByByte(int bytesToRead) {
        long value = 0;
        for (ByteBuffer b: this) {
            int remaining = b.remaining();
            if (remaining == 0) {
                continue;
            }

            for (int i = 0; i < remaining; i ++) {
                value = value << 8 | b.get(b.position() + i);
                bytesToRead --;
                if (bytesToRead == 0) {
                    return value;
                }
            }
        }
        throw new ConcurrentModificationException();
    }

    private long getByteByByte(int byteIndex, int bytesToRead) {
        long value = 0;
        for (ByteBuffer b: this) {
            int remaining = b.remaining();

            // Skip until we find the element which matches to the offset.
            if (remaining <= byteIndex) {
                byteIndex -= remaining;
                continue;
            }

            for (int i = 0; i < remaining; i ++) {
                value = value << 8 | b.get(b.position() + byteIndex);
                bytesToRead --;
                if (bytesToRead == 0) {
                    return value;
                }
                byteIndex ++;
            }
            byteIndex -= remaining;
        }
        throw new ConcurrentModificationException();
    }

    private short applyByteOrder(short value) {
        // Reverse the bytes if necessary.
        if (order == ByteOrder.LITTLE_ENDIAN) {
            int newValue = value >>> 8 & 0xFF | (value & 0xFF) << 8;
            value = (short) newValue;
        }
        return value;
    }

    private int applyByteOrder(int value) {
        // Reverse the bytes if necessary.
        if (order == ByteOrder.LITTLE_ENDIAN) {
            value = (value >>> 24 & 0xFF) <<  0 |
                    (value >>> 16 & 0xFF) <<  8 |
                    (value >>>  8 & 0xFF) << 16 |
                    (value >>>  0 & 0xFF) << 24;
        }
        return value;
    }

    private long applyByteOrder(long value) {
        // Reverse the bytes if necessary.
        if (order == ByteOrder.LITTLE_ENDIAN) {
            value = (value >>> 56 & 0xFFL) <<  0 |
                    (value >>> 48 & 0xFFL) <<  8 |
                    (value >>> 40 & 0xFFL) << 16 |
                    (value >>> 32 & 0xFFL) << 24 |
                    (value >>> 24 & 0xFFL) << 32 |
                    (value >>> 16 & 0xFFL) << 40 |
                    (value >>>  8 & 0xFFL) << 48 |
                    (value >>>  0 & 0xFFL) << 56;
        }
        return value;
    }
}
