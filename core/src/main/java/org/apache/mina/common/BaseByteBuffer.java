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

import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;


/**
 * A base implementation of {@link ByteBuffer}.  This implementation
 * assumes that {@link ByteBuffer#buf()} always returns a correct NIO
 * {@link java.nio.ByteBuffer} instance.  Most implementations could
 * extend this class and implement their own buffer management mechanism.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @noinspection StaticNonFinalField
 * @see ByteBufferAllocator
 */
public abstract class BaseByteBuffer extends ByteBuffer {
    private boolean autoExpand;

    private boolean autoExpandAllowed = true;

    /**
     * We don't have any access to Buffer.markValue(), so we need to track it down,
     * which will cause small extra overhead.
     */
    private int mark = -1;

    protected BaseByteBuffer() {
        this(true);
    }

    protected BaseByteBuffer(boolean autoExpandAllowed) {
        this.autoExpandAllowed = autoExpandAllowed;
    }

    @Override
    public boolean isDirect() {
        return buf().isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return buf().isReadOnly();
    }

    @Override
    public int capacity() {
        return buf().capacity();
    }

    @Override
    public ByteBuffer capacity(int newCapacity) {
        if (newCapacity > capacity()) {
            // Allocate a new buffer and transfer all settings to it.
            int pos = position();
            int limit = limit();
            ByteOrder bo = order();

            capacity0(newCapacity);
            buf().limit(limit);
            if (mark >= 0) {
                buf().position(mark);
                buf().mark();
            }
            buf().position(pos);
            buf().order(bo);
        }

        return this;
    }

    /**
     * Implement this method to increase the capacity of this buffer.
     * <tt>newCapacity</tt> is always greater than the current capacity.
     */
    protected abstract void capacity0(int newCapacity);

    @Override
    public boolean isAutoExpand() {
        return autoExpand && autoExpandAllowed;
    }

    @Override
    public ByteBuffer setAutoExpand(boolean autoExpand) {
        if (!autoExpandAllowed) {
            throw new IllegalStateException(
                    "Derived buffers can't be auto-expandable.");
        }
        this.autoExpand = autoExpand;
        return this;
    }

    @Override
    public ByteBuffer expand(int pos, int expectedRemaining) {
        int end = pos + expectedRemaining;
        if (end > capacity()) {
            // The buffer needs expansion.
            capacity(end);
        }

        if (end > limit()) {
            // We call limit() directly to prevent StackOverflowError
            buf().limit(end);
        }
        return this;
    }

    @Override
    public int position() {
        return buf().position();
    }

    @Override
    public ByteBuffer position(int newPosition) {
        autoExpand(newPosition, 0);
        buf().position(newPosition);
        if (mark > newPosition) {
            mark = -1;
        }
        return this;
    }

    @Override
    public int limit() {
        return buf().limit();
    }

    @Override
    public ByteBuffer limit(int newLimit) {
        autoExpand(newLimit, 0);
        buf().limit(newLimit);
        if (mark > newLimit) {
            mark = -1;
        }
        return this;
    }

    @Override
    public ByteBuffer mark() {
        buf().mark();
        mark = position();
        return this;
    }

    @Override
    public int markValue() {
        return mark;
    }

    @Override
    public ByteBuffer reset() {
        buf().reset();
        return this;
    }

    @Override
    public ByteBuffer clear() {
        buf().clear();
        mark = -1;
        return this;
    }

    @Override
    public ByteBuffer flip() {
        buf().flip();
        mark = -1;
        return this;
    }

    @Override
    public ByteBuffer rewind() {
        buf().rewind();
        mark = -1;
        return this;
    }

    @Override
    public byte get() {
        return buf().get();
    }

    @Override
    public ByteBuffer put(byte b) {
        autoExpand(1);
        buf().put(b);
        return this;
    }

    @Override
    public byte get(int index) {
        return buf().get(index);
    }

    @Override
    public ByteBuffer put(int index, byte b) {
        autoExpand(index, 1);
        buf().put(index, b);
        return this;
    }

    @Override
    public ByteBuffer get(byte[] dst, int offset, int length) {
        buf().get(dst, offset, length);
        return this;
    }

    @Override
    public ByteBuffer put(java.nio.ByteBuffer src) {
        autoExpand(src.remaining());
        buf().put(src);
        return this;
    }

    @Override
    public ByteBuffer put(byte[] src, int offset, int length) {
        autoExpand(length);
        buf().put(src, offset, length);
        return this;
    }

    @Override
    public ByteBuffer compact() {
        buf().compact();
        mark = -1;
        return this;
    }

    @Override
    public ByteOrder order() {
        return buf().order();
    }

    @Override
    public ByteBuffer order(ByteOrder bo) {
        buf().order(bo);
        return this;
    }

    @Override
    public char getChar() {
        return buf().getChar();
    }

    @Override
    public ByteBuffer putChar(char value) {
        autoExpand(2);
        buf().putChar(value);
        return this;
    }

    @Override
    public char getChar(int index) {
        return buf().getChar(index);
    }

    @Override
    public ByteBuffer putChar(int index, char value) {
        autoExpand(index, 2);
        buf().putChar(index, value);
        return this;
    }

    @Override
    public CharBuffer asCharBuffer() {
        return buf().asCharBuffer();
    }

    @Override
    public short getShort() {
        return buf().getShort();
    }

    @Override
    public ByteBuffer putShort(short value) {
        autoExpand(2);
        buf().putShort(value);
        return this;
    }

    @Override
    public short getShort(int index) {
        return buf().getShort(index);
    }

    @Override
    public ByteBuffer putShort(int index, short value) {
        autoExpand(index, 2);
        buf().putShort(index, value);
        return this;
    }

    @Override
    public ShortBuffer asShortBuffer() {
        return buf().asShortBuffer();
    }

    @Override
    public int getInt() {
        return buf().getInt();
    }

    @Override
    public ByteBuffer putInt(int value) {
        autoExpand(4);
        buf().putInt(value);
        return this;
    }

    @Override
    public int getInt(int index) {
        return buf().getInt(index);
    }

    @Override
    public ByteBuffer putInt(int index, int value) {
        autoExpand(index, 4);
        buf().putInt(index, value);
        return this;
    }

    @Override
    public IntBuffer asIntBuffer() {
        return buf().asIntBuffer();
    }

    @Override
    public long getLong() {
        return buf().getLong();
    }

    @Override
    public ByteBuffer putLong(long value) {
        autoExpand(8);
        buf().putLong(value);
        return this;
    }

    @Override
    public long getLong(int index) {
        return buf().getLong(index);
    }

    @Override
    public ByteBuffer putLong(int index, long value) {
        autoExpand(index, 8);
        buf().putLong(index, value);
        return this;
    }

    @Override
    public LongBuffer asLongBuffer() {
        return buf().asLongBuffer();
    }

    @Override
    public float getFloat() {
        return buf().getFloat();
    }

    @Override
    public ByteBuffer putFloat(float value) {
        autoExpand(4);
        buf().putFloat(value);
        return this;
    }

    @Override
    public float getFloat(int index) {
        return buf().getFloat(index);
    }

    @Override
    public ByteBuffer putFloat(int index, float value) {
        autoExpand(index, 4);
        buf().putFloat(index, value);
        return this;
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        return buf().asFloatBuffer();
    }

    @Override
    public double getDouble() {
        return buf().getDouble();
    }

    @Override
    public ByteBuffer putDouble(double value) {
        autoExpand(8);
        buf().putDouble(value);
        return this;
    }

    @Override
    public double getDouble(int index) {
        return buf().getDouble(index);
    }

    @Override
    public ByteBuffer putDouble(int index, double value) {
        autoExpand(index, 8);
        buf().putDouble(index, value);
        return this;
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        return buf().asDoubleBuffer();
    }

    @Override
    public final ByteBuffer asReadOnlyBuffer() {
        autoExpandAllowed = false;
        return asReadOnlyBuffer0();
    }

    /**
     * Implement this method to return the unexpandable read only version of
     * this buffer.
     */
    protected abstract ByteBuffer asReadOnlyBuffer0();

    @Override
    public final ByteBuffer duplicate() {
        autoExpandAllowed = false;
        return duplicate0();
    }

    /**
     * Implement this method to return the unexpandable duplicate of this
     * buffer.
     */
    protected abstract ByteBuffer duplicate0();

    @Override
    public final ByteBuffer slice() {
        autoExpandAllowed = false;
        return slice0();
    }

    /**
     * Implement this method to return the unexpandable slice of this
     * buffer.
     */
    protected abstract ByteBuffer slice0();
}
