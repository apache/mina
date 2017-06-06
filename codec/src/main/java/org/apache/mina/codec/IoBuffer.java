/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.codec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;

/**
 * A proxy class used to manage ByteBuffers as if they were just a big
 * ByteBuffer. We can add as many buffers as needed when accumulating data. From
 * the user point of view, the methods are the very same as ByteBuffer provides.
 * 
 * <p>
 * IoBuffer instances are *not* thread safe.
 * 
 * <p>
 * The IoBuffer uses a singly linked list to handle the multiple Buffers. Thus
 * sequential access is very efficient and random access is not. It fits well
 * with the common usage patterns of IoBuffer.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class IoBuffer {
    private static final int BYTE_MASK = 0xff;

    private static final long BYTE_MASK_L = 0xffL;

    /**
     * @see ByteBuffer#allocate(int)
     */
    public static IoBuffer allocate(int capacity) {
        return wrap(ByteBuffer.allocate(capacity));
    }

    /**
     * @see ByteBuffer#allocateDirect(int)
     */
    public static IoBuffer allocateDirect(int capacity) {
        return wrap(ByteBuffer.allocateDirect(capacity));
    }

    /**
     * Build a new instance of {@link IoBuffer}
     * 
     * @return a new instance of {@link IoBuffer}
     */
    public static IoBuffer newInstance() {
        return new IoBuffer();
    }

    /**
     * @see ByteBuffer#wrap(byte[])
     */
    public static IoBuffer wrap(byte[]... arrays) {
        IoBuffer ioBuffer = new IoBuffer();
        for (byte[] array : arrays) {
            ioBuffer.add(ByteBuffer.wrap(array));
        }
        return ioBuffer;
    }

    /**
     * @see ByteBuffer#wrap(byte[], int, int)
     */
    public static IoBuffer wrap(byte[] array, int offset, int length) {
        return wrap(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Wraps ByteBuffers into a new IoBuffer
     * 
     * @param buffers
     *            the ByteBuffers to wrap
     * @return the new {@link IoBuffer}
     */
    public static IoBuffer wrap(ByteBuffer... buffers) {
        IoBuffer ioBuffer = new IoBuffer();
        for (ByteBuffer b : buffers) {
            ioBuffer.add(b);
        }
        return ioBuffer;
    }

    private ByteOrder bo = ByteOrder.BIG_ENDIAN;

    private int capacity = 0;

    private boolean direct = true;

    private BufferNode head, tail;

    /** The maximal position in the IoBuffer */
    private Pointer limit = new Pointer();

    /** The current position in the buffer */
    private Pointer mark = new Pointer();

    /** The marked position, for the next reset() */
    private Pointer position = new Pointer();

    /** If the buffer is readonly */
    private boolean readonly = false;

    private IoBuffer() {
        limit(0);
        position(0);
        mark = null;
    }

    /**
     * Add one or more ByteBuffer to the current IoBuffer
     * 
     * @param buffers
     *            the ByteBuffers to add
     * @return the current {@link IoBuffer}
     */
    public IoBuffer add(ByteBuffer... buffers) {
        for (ByteBuffer buffer : buffers) {
            enqueue(buffer.slice());
        }
        return this;
    }

    /**
     * @see ByteBuffer#array()
     */
    public byte[] array() {
        if (capacity == 0) {
            return new byte[0];
        }
        if (head.hasNext()) {
            throw new UnsupportedOperationException();
        }
        return head.getBuffer().array();
    }

    /**
     * @see ByteBuffer#arrayOffset()
     */
    public int arrayOffset() {
        if (capacity == 0) {
            return 0;
        }
        if (head.hasNext()) {
            throw new UnsupportedOperationException();
        }
        return head.getBuffer().arrayOffset();
    }

    /**
     * Provides an input stream which is actually reading the {@link IoBuffer}
     * instance.
     * <p>
     * Further reads on the returned InputStream move the reading head of the
     * {@link IoBuffer} instance used for its creation
     * 
     * @return an input stream
     */
    public InputStream asInputStream() {
        return new InputStream() {

            @Override
            public int read() throws IOException {
                return hasRemaining() ? get() & BYTE_MASK : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (!hasRemaining()) {
                    return -1;
                }

                int toRead = Math.min(remaining(), len);
                get(b, off, toRead);
                return toRead;
            }
        };
    }

    /**
     * @see ByteBuffer#asReadOnlyBuffer()
     */
    public IoBuffer asReadOnlyBuffer() {
        IoBuffer buffer = duplicate();
        buffer.readonly = true;
        return buffer;
    }

    /**
     * @see ByteBuffer#capacity()
     */
    public int capacity() {
        return capacity;
    }

    /**
     * @see ByteBuffer#clear()
     */
    public IoBuffer clear() {
        position = getPointerByPosition(0);
        limit = getPointerByPosition(capacity);
        mark = null;
        return this;
    }

    /**
     * @see ByteBuffer#compact()
     */
    public IoBuffer compact() {
        for (int i = 0; i < remaining(); i++) {
            put(i, get(i + position.getPosition()));
        }
        position(limit() - position());
        limit(capacity);
        mark = null;
        return this;
    }

    /**
     * Returns a copy of the current {@link IoBuffer}, with an independent copy
     * of the position, limit and mark.
     * 
     * @return the copied {@link IoBuffer}
     */
    public IoBuffer duplicate() {
        IoBuffer buffer = new IoBuffer();

        for (BufferNode node = head; node != null; node = node.getNext()) {
            ByteBuffer byteBuffer = node.getBuffer().duplicate();
            byteBuffer.rewind();
            buffer.enqueue(byteBuffer);
        }
        buffer.position(position());
        buffer.limit(limit());
        buffer.mark = mark != null ? getPointerByPosition(mark.getPosition()) : null;

        buffer.readonly = readonly;
        return buffer;
    }

    private void enqueue(ByteBuffer buffer) {

        if (buffer.isReadOnly()) {
            readonly = true;
        }

        if (!buffer.isDirect()) {
            direct = false;
        }
        if (buffer.remaining() > 0) {
            BufferNode newnode = new BufferNode(buffer, capacity);
            capacity += buffer.capacity();

            if (head == null) {
                head = newnode;
                position = getPointerByPosition(0);
            } else {
                tail.setNext(newnode);
            }
            tail = newnode;

            limit = getPointerByPosition(capacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof IoBuffer)) {
            return false;
        }
        IoBuffer that = (IoBuffer) ob;
        if (this.remaining() != that.remaining()) {
            return false;
        }
        int p = this.position();
        int q = that.position();
        while (this.hasRemaining() && that.hasRemaining()) {
            if (this.get() != that.get()) {
                this.position(p);
                that.position(q);
                return false;
            }

        }
        this.position(p);
        that.position(q);
        return true;
    }

    /**
     * Extends the current IoBuffer capacity.
     * 
     * @param size
     *            the number of bytes to extend the current IoBuffer
     * @return the current {@link IoBuffer}
     */
    public IoBuffer extend(int size) {
        ByteBuffer extension = isDirect() ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
        add(extension);
        return this;
    }

    /**
     * @see ByteBuffer#flip()
     */
    public IoBuffer flip() {
        limit = position;
        position = getPointerByPosition(0);
        return this;
    }

    /**
     * @see ByteBuffer#get()
     */
    public byte get() {
        if (!hasRemaining()) {
            throw new BufferUnderflowException();
        }

        return get(position);
    }

    /**
     * @see ByteBuffer#get(byte[])
     */
    public IoBuffer get(byte[] dst) {
        get(dst, 0, dst.length);
        return this;
    }

    /**
     * @see ByteBuffer#get(byte[], int,int)
     */
    public IoBuffer get(byte[] dst, int offset, int length) {
        if (remaining() < length) {
            throw new BufferUnderflowException();
        }
        int remainsToCopy = length;
        int currentOffset = offset;

        while (remainsToCopy > 0) {
            position.updatePos();
            position.getNode().getBuffer().position(position.getPositionInNode());
            ByteBuffer currentBuffer = position.getNode().getBuffer();
            int blocksize = Math.min(remainsToCopy, currentBuffer.remaining());
            position.getNode().getBuffer().get(dst, currentOffset, blocksize);

            currentOffset += blocksize;
            remainsToCopy -= blocksize;

            position.incrementPosition(blocksize);

            position.getNode().getBuffer().position(0);
        }
        return this;
    }

    /**
     * @see ByteBuffer#get(int)
     */
    public byte get(int index) {
        if (index >= limit.getPosition()) {
            throw new IndexOutOfBoundsException();
        }
        return get(getPointerByPosition(index));
    }

    private byte get(Pointer pos) {
        pos.updatePos();
        byte b = pos.getNode().getBuffer().get(pos.getPositionInNode());
        pos.incrPosition();
        return b;
    }

    /**
     * @see ByteBuffer#getChar()
     */
    public char getChar() {
        return getChar(position);
    }

    /**
     * @see ByteBuffer#getChar(int)
     */
    public char getChar(int index) {
        return getChar(getPointerByPosition(index));
    }

    private char getChar(Pointer position) {
        return (char) getShort(position);
    }

    /**
     * @see ByteBuffer#getDouble()
     */
    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    /**
     * @see ByteBuffer#getDouble(int)
     */
    public double getDouble(int index) {
        return getDouble(getPointerByPosition(index));
    }

    private double getDouble(Pointer pos) {
        return Double.longBitsToDouble(getLong(pos));
    }

    /**
     * @see ByteBuffer#getFloat()
     */
    public float getFloat() {
        return getFloat(position);
    }

    /**
     * @see ByteBuffer#getFloat(int)
     */
    public float getFloat(int index) {
        return getFloat(getPointerByPosition(index));
    }

    private float getFloat(Pointer pos) {
        return Float.intBitsToFloat(getInt(pos));
    }

    /**
     * @see ByteBuffer#getInt()
     */
    public int getInt() {
        return getInt(position);
    }

    /**
     * @see ByteBuffer#getInt(int)
     */
    public int getInt(int index) {
        return getInt(getPointerByPosition(index));
    }

    private int getInt(Pointer pos) {
        if (pos.getPosition() > capacity - Integer.SIZE / Byte.SIZE) {
            throw new BufferUnderflowException();
        }

        int out = 0;
        for (int i = 0; i < Integer.SIZE; i += Byte.SIZE) {
            out |= (get(pos) & BYTE_MASK) << (bo == ByteOrder.BIG_ENDIAN ? (Integer.SIZE - Byte.SIZE) - i : i);
        }
        return out;
    }

    /**
     * @see ByteBuffer#getLong()
     */
    public long getLong() {
        return getLong(position);
    }

    /**
     * @see ByteBuffer#getLong(int)
     */
    public long getLong(int index) {
        return getLong(getPointerByPosition(index));
    }

    private long getLong(Pointer pos) {
        if (pos.getPosition() > capacity - Long.SIZE / Byte.SIZE) {
            throw new BufferUnderflowException();
        }

        long out = 0;
        for (int i = 0; i < Long.SIZE; i += Byte.SIZE) {
            out |= (get(pos) & BYTE_MASK_L) << (bo == ByteOrder.BIG_ENDIAN ? (Long.SIZE - Byte.SIZE) - i : i);
        }
        return out;
    }

    private Pointer getPointerByPosition(int pos) {
        return new Pointer(pos);
    }

    /**
     * @see ByteBuffer#getShort()
     */
    public short getShort() {
        return getShort(position);
    }

    /**
     * @see ByteBuffer#getShort(int)
     */
    public long getShort(int index) {
        return getShort(getPointerByPosition(index));
    }

    private short getShort(Pointer pos) {
        if (pos.getPosition() > capacity - Short.SIZE / Byte.SIZE) {
            throw new BufferUnderflowException();
        }
        if (bo == ByteOrder.BIG_ENDIAN) {
            return (short) ((get(pos) & BYTE_MASK) << Byte.SIZE | (get(pos) & BYTE_MASK));
        } else {
            return (short) ((get(pos) & BYTE_MASK) | (get(pos) & BYTE_MASK) << Byte.SIZE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 0;
        Pointer oldPos = position.duplicate();
        while (hasRemaining()) {
            hash *= 31; // NOSONAR, standard way of hashing
            hash += get();
        }
        position = oldPos;
        return hash;
    }

    /**
     * @see ByteBuffer#hasRemaining()
     */
    public boolean hasRemaining() {
        return remaining() > 0;
    }

    /**
     * @see ByteBuffer#isDirect()
     */
    public boolean isDirect() {
        return direct;
    }

    /**
     * @see ByteBuffer#isReadOnly()
     */
    public boolean isReadOnly() {
        return readonly;
    }

    /**
     * @see ByteBuffer#limit()
     */
    public int limit() {
        return limit.getPosition();
    }

    /**
     * @see ByteBuffer#limit(int)
     */
    public void limit(int newLimit) {
        this.limit = getPointerByPosition(newLimit);
    }

    /**
     * @see ByteBuffer#mark()
     */
    public void mark() {
        this.mark = position.duplicate();
    }

    /**
     * Returns the byte order used by this IoBuffer when converting bytes
     * from/to other primitive types.
     * <p>
     * The default byte order of byte buffer is always
     * {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}
     * 
     * @return the byte order used by this IoBuffer when converting bytes
     *         from/to other primitive types.
     * 
     * @see ByteBuffer#order()
     */
    public ByteOrder order() {
        return bo;
    }

    /**
     * Sets the byte order of this IoBuffer.
     * 
     * @param bo
     *            the byte order to set. If {@code null} then the order will be
     *            {@link ByteOrder#LITTLE_ENDIAN LITTLE_ENDIAN}.
     * @return this IoBuffer.
     * @see ByteBuffer#order(ByteOrder)
     */
    public IoBuffer order(ByteOrder bo) {
        this.bo = bo != null ? bo : ByteOrder.LITTLE_ENDIAN;

        return this;
    }

    /**
     * @see ByteBuffer#position()
     */
    public int position() {
        return position.getPosition();
    }

    /**
     * @see ByteBuffer#position(int)
     */
    public void position(int newPosition) {
        if (newPosition > limit() || newPosition < 0) {
            throw new IllegalArgumentException();
        }

        if (mark != null && mark.getPosition() > newPosition) {
            mark = null;
        }

        this.position.setPosition(newPosition);
    }

    /**
     * @see ByteBuffer#put(byte)
     */
    public IoBuffer put(byte b) {
        if (readonly) {
            throw new ReadOnlyBufferException();
        }
        if (position.getPosition() >= limit.getPosition()) {
            throw new BufferUnderflowException();
        }

        put(position, b);
        return this;
    }

    /**
     * @see ByteBuffer#put(byte[])
     */
    public IoBuffer put(byte[] src) {
        put(src, 0, src.length);
        return this;
    }

    /**
     * @see ByteBuffer#put(ByteBuffer)
     */
    public IoBuffer put(ByteBuffer src) {

        if (remaining() < src.remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        while (src.hasRemaining()) {
            put(src.get());
        }

        return this;
    }

    /**
     * @see ByteBuffer#put(ByteBuffer)
     */
    public IoBuffer put(IoBuffer src) {
        if (src == this) { // NOSONAR, checking the instance
            throw new IllegalArgumentException();
        }

        if (remaining() < src.remaining()) {
            throw new BufferOverflowException();
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        while (src.hasRemaining()) {
            put(src.get());
        }

        return this;
    }

    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    public IoBuffer put(byte[] src, int offset, int length) {
        if (readonly) {
            throw new ReadOnlyBufferException();
        }
        if (remaining() < length) {
            throw new BufferUnderflowException();
        }

        int remainsToCopy = length;
        int currentOffset = offset;
        position.getNode().getBuffer().position(position.getPositionInNode());
        while (remainsToCopy > 0) {
            position.updatePos();

            ByteBuffer currentBuffer = position.getNode().getBuffer();
            int blocksize = Math.min(remainsToCopy, currentBuffer.remaining());
            position.getNode().getBuffer().put(src, currentOffset, blocksize);

            currentOffset += blocksize;
            remainsToCopy -= blocksize;

            position.incrementPosition(blocksize);
        }
        position.getNode().getBuffer().position(0);
        return this;
    }

    /**
     * @see ByteBuffer#put(int, byte)
     */
    public IoBuffer put(int index, byte value) {
        if (index >= limit.getPosition()) {
            throw new IndexOutOfBoundsException();
        }
        Pointer p = getPointerByPosition(index);
        put(p, value);
        return this;
    }

    private IoBuffer put(Pointer pos, byte b) {
        pos.updatePos();
        pos.getNode().getBuffer().put(pos.getPositionInNode(), b);
        pos.incrPosition();
        return this;
    }

    /**
     * @see ByteBuffer#putChar(char)
     */
    public IoBuffer putChar(char value) {
        return putChar(position, value);
    }

    /**
     * @see ByteBuffer#putChar(int, char)
     */
    public IoBuffer putChar(int index, char value) {
        return putChar(getPointerByPosition(index), value);
    }

    private IoBuffer putChar(Pointer index, char value) {
        return putShort(index, (short) value);
    }

    /**
     * @see ByteBuffer#putDouble(double)
     */
    public IoBuffer putDouble(double value) {
        return putDouble(position, value);
    }

    /**
     * @see ByteBuffer#putDouble(int, double)
     */
    public IoBuffer putDouble(int index, double value) {
        return putDouble(getPointerByPosition(index), value);
    }

    private IoBuffer putDouble(Pointer pos, double value) {
        return putLong(pos, Double.doubleToLongBits(value));
    }

    /**
     * @see ByteBuffer#putFloat(float)
     */
    public IoBuffer putFloat(float value) {
        return putFloat(position, value);
    }

    /**
     * @see ByteBuffer#putFloat(int, float)
     */
    public IoBuffer putFloat(int index, float value) {
        return putFloat(getPointerByPosition(index), value);
    }

    private IoBuffer putFloat(Pointer pointer, float value) {
        return putInt(pointer, Float.floatToIntBits(value));
    }

    /**
     * @see ByteBuffer#putInt(int)
     */
    public IoBuffer putInt(int value) {
        return putInt(position, value);
    }

    /**
     * @see ByteBuffer#putInt(int, int)
     */
    public IoBuffer putInt(int index, int value) {
        return putInt(getPointerByPosition(index), value);
    }

    private IoBuffer putInt(Pointer pointer, int value) {
        if (position.getPosition() > pointer.getPosition()
                || pointer.getPosition() > limit.getPosition() - Integer.SIZE / Byte.SIZE) {
            throw new BufferUnderflowException();
        }
        for (int i = 0; i < Integer.SIZE; i += Byte.SIZE) {
            put(pointer, (byte) (value >> (bo == ByteOrder.BIG_ENDIAN ? (Integer.SIZE - Byte.SIZE) - i : i)));
        }
        return this;
    }

    /**
     * @see ByteBuffer#putLong(int, long)
     */
    public IoBuffer putLong(int index, long value) {
        return putLong(getPointerByPosition(index), value);
    }

    /**
     * @see ByteBuffer#putLong(long)
     */
    public IoBuffer putLong(long value) {
        return putLong(position, value);
    }

    private IoBuffer putLong(Pointer pointer, long value) {
        if (position.getPosition() > pointer.getPosition()
                || pointer.getPosition() > limit.getPosition() - Long.SIZE / Byte.SIZE) {
            throw new BufferUnderflowException();
        }
        for (int i = 0; i < Long.SIZE; i += Byte.SIZE) {
            put(pointer, (byte) (value >> (bo == ByteOrder.BIG_ENDIAN ? (Long.SIZE - Byte.SIZE) - i : i)));
        }

        return this;
    }

    /**
     * @see ByteBuffer#putShort(int, short)
     */
    public IoBuffer putShort(int index, short value) {
        return putShort(getPointerByPosition(index), value);
    }

    private IoBuffer putShort(Pointer pointer, short value) {
        if (position.getPosition() > pointer.getPosition()
                || pointer.getPosition() > limit.getPosition() - Short.SIZE / Byte.SIZE) {
            throw new BufferUnderflowException();
        }
        for (int i = 0; i < Short.SIZE; i += Byte.SIZE) {
            put(pointer, (byte) (value >> (bo == ByteOrder.BIG_ENDIAN ? Byte.SIZE - i : i)));
        }
        return this;
    }

    /**
     * @see ByteBuffer#putShort(short)
     */
    public IoBuffer putShort(short value) {
        return putShort(position, value);
    }

    /**
     * @see ByteBuffer#remaining()
     */
    public int remaining() {
        return limit() - position();
    }

    /**
     * @see ByteBuffer#reset()
     */
    public IoBuffer reset() {
        if (mark == null) {
            throw new InvalidMarkException();
        }
        position = mark.duplicate();
        return this;
    }

    /**
     * @see ByteBuffer#rewind()
     */
    public IoBuffer rewind() {
        position(0);
        mark = getPointerByPosition(-1);
        return this;
    }

    /**
     * @see ByteBuffer#slice()
     */
    public IoBuffer slice() {
        position.updatePos();
        IoBuffer out = new IoBuffer();
        out.order(order());

        position.getNode().getBuffer().position(position.getPositionInNode());
        if (hasRemaining()) {
            tail.getBuffer().limit(limit.getPositionInNode());
            for (BufferNode node = position.getNode(); node != limit.getNode(); node = node.getNext()) {
                if (node != head) { // NOSONAR, check if instances are the same.
                    node.getBuffer().position(0);
                }
                out.add(node.getBuffer());
            }
            if (tail != head) { // NOSONAR, check if instances are the same.
                tail.getBuffer().position(0);
            }
            out.add(tail.getBuffer().slice());
            tail.getBuffer().limit(tail.getBuffer().capacity());
        }
        position.getNode().getBuffer().position(0);

        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("[pos=");
        sb.append(position());
        sb.append(" lim=");
        sb.append(limit());
        sb.append(" cap=");
        sb.append(capacity());
        sb.append("]");
        return sb.toString();
    }

    private static final class BufferNode {
        private final ByteBuffer buffer;

        private BufferNode next;

        private final int offset;

        public BufferNode(ByteBuffer buffer, int offset) {
            this.buffer = buffer;
            this.offset = offset;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public BufferNode getNext() {
            return next;
        }

        public boolean hasNext() {
            return next != null;
        }

        public void setNext(BufferNode next) {
            this.next = next;
        }

        @Override
        public String toString() {
            return "BufferNode [offset=" + offset + ", buffer=" + buffer + "]";
        }
    }

    private final class Pointer {

        private BufferNode node;

        private int positionInBuffer;

        public Pointer(int position) {
            this();

            setPosition(position);
        }

        public Pointer() {
        }

        public Pointer duplicate() {
            return new Pointer(getPosition());
        }

        public BufferNode getNode() {
            return node;
        }

        public int getPosition() {
            return positionInBuffer + (node == null ? 0 : node.offset);
        }

        public int getPositionInNode() {
            updatePos();
            return positionInBuffer;
        }

        public void incrPosition() {
            incrementPosition(1);
        }

        public void setPosition(int newPosition) {
            if ((node == null) || (newPosition < node.offset)) {
                node = head;
            }

            positionInBuffer = node == null ? 0 : newPosition - node.offset;
        }

        public void incrementPosition(int positionIncrement) {
            positionInBuffer += positionIncrement;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getName());
            sb.append("[pos=");
            sb.append(getPosition());
            sb.append(", node=");
            sb.append(getNode());
            sb.append("]");
            return sb.toString();
        }

        public void updatePos() {
            while (node != null && positionInBuffer >= node.getBuffer().capacity() && node.hasNext()) {
                positionInBuffer -= node.getBuffer().capacity();
                node = node.getNext();
            }
        }
    }
}
