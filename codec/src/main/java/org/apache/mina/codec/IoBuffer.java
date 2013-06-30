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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;

/**
 * A proxy class used to manage ByteBuffers as if they were just a big ByteBuffer. We can add as many buffers as needed,
 * when accumulating data. From the user PoV, the methods are the very same than what we can get from ByteBuffer. <br/>
 * IoBuffer instances are *not* thread safe.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class IoBuffer {

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
     * Build new IoBuffer containing 
     * 
     * @param buffers
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
    private Pointer limit;

    /** The current position in the buffer */
    private Pointer mark;

    /** The marked position, for the next reset() */
    private Pointer position;

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
     * Further reads on the returned inputstream move the reading head of the {@link IoBuffer}
     * instance used for it's creation</i>
     *
     * @return an input stream
     */
    public InputStream asInputStream() {
        return new InputStream() {

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int toRead = Math.min(remaining(), len);
                get(b, off, toRead);
                return toRead;
            }

            @Override
            public int read() throws IOException {
                return hasRemaining() ? get() & 0xff : -1;
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
     * Returns a copy of the current {@link IoBuffer}, with an independent copy if the postion, limit and mark.
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
     * @param size the number of bytes to extend the current IoBuffer 
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
        if (position.getPosition() >= limit.getPosition()) {
            throw new BufferUnderflowException();
        }

        updatePosition();
        position.setPosition(position.getPosition() + 1);

        return position.getNode().getBuffer().get();
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
            updatePosition();

            ByteBuffer currentBuffer = position.getNode().getBuffer();
            int blocksize = Math.min(remainsToCopy, currentBuffer.remaining());
            position.getNode().getBuffer().get(dst, currentOffset, blocksize);

            currentOffset += blocksize;
            remainsToCopy -= blocksize;

            position.setPosition(position.getPosition() + blocksize);
        }
        return this;
    }

    /**
     * @see ByteBuffer#get(int)
     */
    public byte get(int pos) {
        if (pos >= limit.getPosition()) {
            throw new IndexOutOfBoundsException();
        }
        BufferNode node = getBufferNodeByPosition(pos);
        return node.getBuffer().get(pos - node.getOffset());
    }

    private BufferNode getBufferNodeByPosition(int pos) {
        if (head == null) {
            return null;
        }
        BufferNode currentNode = head;
        int max = currentNode.getBuffer().capacity();

        while (max <= pos && currentNode != null) {
            currentNode = currentNode.getNext();
            if (currentNode != null) {
                max += currentNode.getBuffer().capacity();
            }
        }

        return currentNode;
    }

    /**
     * @see ByteBuffer#getChar()
     */
    public char getChar() {
        if (remaining() < 2) {
            throw new BufferUnderflowException();
        }
        return (char) getShort();
    }

    /**
     * @see ByteBuffer#getChar(int)
     */
    public char getChar(int index) {
        int oldPos = position();
        position(index);
        char out = getChar();
        position(oldPos);
        position.getNode().getBuffer().position(position.getPositionInNode());
        return out;
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
        int oldPos = position();
        position(index);
        double out = getDouble();
        position(oldPos);
        position.getNode().getBuffer().position(position.getPositionInNode());
        return out;
    }

    /**
     * @see ByteBuffer#getFloat()
     */
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    /**
     * @see ByteBuffer#getFloat(int)
     */
    public float getFloat(int index) {
        int oldPos = position();
        position(index);
        float out = getFloat();
        position(oldPos);
        return out;
    }

    /**
     * @see ByteBuffer#getInt()
     */
    public int getInt() {
        if (remaining() < 4) {
            throw new BufferUnderflowException();
        }

        int out = 0;
        for (int i = 0; i < 32; i += 8) {
            out |= (get() & 0xff) << (bo == ByteOrder.BIG_ENDIAN ? 24 - i : i);
        }
        return out;
    }

    /**
     * @see ByteBuffer#getInt(int)
     */
    public int getInt(int index) {
        int oldPos = position();
        position(index);
        int out = getInt();
        position(oldPos);
        return out;
    }

    /**
     * @see ByteBuffer#getLong()
     */
    public long getLong() {
        if (remaining() < 8) {
            throw new BufferUnderflowException();
        }

        long out = 0;
        for (int i = 0; i < 64; i += 8) {
            out |= (get() & 0xffl) << (bo == ByteOrder.BIG_ENDIAN ? 56 - i : i);
        }
        return out;
    }

    /**
     * @see ByteBuffer#getLong(int)
     */
    public long getLong(int index) {
        int oldPos = position();
        position(index);
        long out = getLong();
        position(oldPos);
        return out;
    }

    private Pointer getPointerByPosition(int pos) {
        if (pos == capacity) {
            return new Pointer(tail, pos);
        }

        BufferNode currentNode = getBufferNodeByPosition(pos);
        return new Pointer(currentNode, pos);
    }

    /**
     * @see ByteBuffer#getShort()
     */
    public short getShort() {
        if (remaining() < 2) {
            throw new BufferUnderflowException();
        }

        if (bo == ByteOrder.BIG_ENDIAN) {
            return (short) ((get() & 0xff) << 8 | (get() & 0xff));
        } else {
            return (short) ((get() & 0xff) | (get() & 0xff) << 8);
        }
    }

    /**
     * @see ByteBuffer#getShort(int)
     */
    public long getShort(int index) {
        int oldPos = position();
        position(index);
        short out = getShort();
        position(oldPos);
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 0;
        Pointer oldPos = position.duplicate();
        while (hasRemaining()) {
            hash *= 31;
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
     * @see ByteBuffer#limit()
     */
    public int limit() {
        return limit.getPosition();
    }

    /**
     * @see ByteBuffer#limit(int)
     */
    public void limit(int limit) {
        this.limit = getPointerByPosition(limit);
    }

    /**
     * @see ByteBuffer#mark()
     */
    public void mark() {
        this.limit = position.duplicate();
    }

    /**
     * Returns the byte order used by this Iouffer when converting bytes from/to other primitive
     * types.
     * <p>
     * The default byte order of byte buffer is always {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}
     * 
     * @return the byte order used by this IoBuffer when converting bytes from/to other primitive types.
     * 
     * @see ByteBuffer#order()
     */
    public ByteOrder order() {
        return bo;
    }

    /**
     * Sets the byte order of this IoBuffer.
     * 
     * @param byteOrder the byte order to set. If {@code null} then the order will be {@link ByteOrder#LITTLE_ENDIAN
     *        LITTLE_ENDIAN}.
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
    public void position(int position) {
        if (position > limit() || position < 0) {
            throw new IllegalArgumentException();
        }

        if (mark != null && mark.getPosition() > position) {
            mark = null;
        }

        setPosition(getPointerByPosition(position));
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

        updatePosition();
        position.setPosition(position.getPosition() + 1);

        position.getNode().getBuffer().put(b);
        return this;
    }

    /**
     * @see ByteBuffer#put(byte[])
     */
    public IoBuffer put(byte[] dst) {
        put(dst, 0, dst.length);
        return this;
    }

    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    public IoBuffer put(byte[] dst, int offset, int length) {
        if (readonly) {
            throw new ReadOnlyBufferException();
        }
        if (remaining() < length) {
            throw new BufferUnderflowException();
        }

        int remainsToCopy = length;
        int currentOffset = offset;

        while (remainsToCopy > 0) {
            updatePosition();

            ByteBuffer currentBuffer = position.getNode().getBuffer();
            int blocksize = Math.min(remainsToCopy, currentBuffer.remaining());
            position.getNode().getBuffer().put(dst, currentOffset, blocksize);

            currentOffset += blocksize;
            remainsToCopy -= blocksize;

            position.setPosition(position.getPosition() + blocksize);
        }
        return this;
    }

    /**
     * @see ByteBuffer#put(int, byte)
     */
    public IoBuffer put(int pos, byte value) {
        if (pos >= limit.getPosition()) {
            throw new IndexOutOfBoundsException();
        }
        BufferNode node = getBufferNodeByPosition(pos);
        node.getBuffer().put(pos - node.getOffset(), value);
        return this;
    }

    /**
     * @see ByteBuffer#putChar(char)
     */
    public IoBuffer putChar(char value) {
        putShort((short) value);
        return this;
    }

    /**
     * @see ByteBuffer#putChar(int, char)
     */
    public IoBuffer putChar(int index, char value) {
        Pointer oldPos = position.duplicate();
        position(index);
        putChar(value);
        position = oldPos;
        return this;
    }

    /**
     * @see ByteBuffer#putDouble(double)
     */
    public IoBuffer putDouble(double value) {
        return putLong(Double.doubleToLongBits(value));
    }

    /**
     * @see ByteBuffer#putDouble(int, double)
     */
    public IoBuffer putDouble(int index, double value) {
        int oldPos = position();
        position(index);
        putDouble(value);
        position(oldPos);
        return this;
    }

    /**
     * @see ByteBuffer#putFloat(float)
     */
    public IoBuffer putFloat(float value) {
        return putInt(Float.floatToIntBits(value));
    }

    /**
     * @see ByteBuffer#putFloat(int, float)
     */
    public IoBuffer putFloat(int index, float value) {
        int oldPos = position();
        position(index);
        putFloat(value);
        position(oldPos);
        return this;
    }

    /**
     * @see ByteBuffer#putInt(int)
     */
    public IoBuffer putInt(int value) {
        if (remaining() < 4) {
            throw new BufferUnderflowException();
        }

        for (int i = 0; i < 32; i += 8) {
            put((byte) (value >> (bo == ByteOrder.BIG_ENDIAN ? 24 - i : i)));
        }
        return this;
    }

    /**
     * @see ByteBuffer#putInt(int, int)
     */
    public IoBuffer putInt(int index, int value) {
        int oldPos = position();
        position(index);
        putInt(value);
        position(oldPos);
        return this;
    }

    /**
     * @see ByteBuffer#putLong(int, int)
     */
    public IoBuffer putLong(int index, long value) {
        int oldPos = position();
        position(index);
        putLong(value);
        position(oldPos);
        return this;
    }

    /**
     * @see ByteBuffer#putLong(long)
     */
    public IoBuffer putLong(long value) {
        if (remaining() < 8) {
            throw new BufferUnderflowException();
        }

        for (int i = 0; i < 64; i += 8) {
            put((byte) (value >> (bo == ByteOrder.BIG_ENDIAN ? 56 - i : i)));
        }

        return this;
    }

    /**
     * @see ByteBuffer#putShort(int, short)
     */
    public IoBuffer putShort(int index, short value) {
        int oldPos = position();
        position(index);
        putShort(value);
        position(oldPos);
        return this;
    }

    /**
     * @see ByteBuffer#putShort(short)
     */
    public IoBuffer putShort(short value) {
        if (remaining() < 2) {
            throw new BufferUnderflowException();
        }

        for (int i = 0; i < 16; i += 8) {
            put((byte) (value >> (bo == ByteOrder.BIG_ENDIAN ? 8 - i : i)));
        }
        return this;
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

    private void setPosition(Pointer position) {
        this.position = position;
        if (capacity > 0) {
            position.getNode().getBuffer().position(position.getPositionInNode());
        }
    }

    /**
     * @see ByteBuffer#slice()
     */
    public IoBuffer slice() {
        updatePosition();
        IoBuffer out = new IoBuffer();
        out.order(order());

        if (hasRemaining()) {
            tail.getBuffer().limit(limit.getPositionInNode());
            for (BufferNode node = position.getNode(); node != limit.getNode(); node = node.getNext()) {
                if (node != head) {
                    node.getBuffer().position(0);
                }
                out.add(node.getBuffer());
            }
            if (tail != head) {
                tail.getBuffer().position(0);
            }
            out.add(tail.getBuffer().slice());
            tail.getBuffer().limit(tail.getBuffer().capacity());
        }

        return out;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
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

    private void updatePosition() {
        while (!position.getNode().getBuffer().hasRemaining() && position.getNode().hasNext()) {
            position.setNode(position.getNode().getNext());
            position.getNode().getBuffer().rewind();
        }
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

        public int getOffset() {
            return offset;
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

    private static final class Pointer {
        private BufferNode node;

        private int position;

        public Pointer(BufferNode node, int position) {
            super();
            this.node = node;
            this.position = position;
        }

        public Pointer duplicate() {
            return new Pointer(node, position);
        }

        public BufferNode getNode() {
            return node;
        }

        public int getPosition() {
            return position;
        }

        public int getPositionInNode() {
            return position - node.getOffset();
        }

        private void setNode(BufferNode node) {
            this.node = node;
        }

        private void setPosition(int position) {
            this.position = position;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(getClass().getName());
            sb.append("[node=");
            sb.append(getNode());
            sb.append(", pos=");
            sb.append(getPosition());            
            sb.append("]");
            return sb.toString();           
        }
    }
}