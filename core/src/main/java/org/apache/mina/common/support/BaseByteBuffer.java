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
package org.apache.mina.common.support;

import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ByteBufferAllocator;

/**
 * A base implementation of {@link ByteBuffer}.  This implementation
 * assumes that {@link ByteBuffer#buf()} always returns a correct NIO
 * {@link java.nio.ByteBuffer} instance.  Most implementations could
 * extend this class and implement their own buffer management mechanism.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * @noinspection StaticNonFinalField
 * @see ByteBufferAllocator
 */
public abstract class BaseByteBuffer extends ByteBuffer {
    private boolean autoExpand;

    /**
     * We don't have any access to Buffer.markValue(), so we need to track it down,
     * which will cause small extra overhead.
     */
    private int mark = -1;

    protected BaseByteBuffer() {
    }

    public boolean isDirect() {
        return buf().isDirect();
    }

    public boolean isReadOnly() {
        return buf().isReadOnly();
    }

    public int capacity() {
        return buf().capacity();
    }

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

    public boolean isAutoExpand() {
        return autoExpand;
    }

    public ByteBuffer setAutoExpand(boolean autoExpand) {
        this.autoExpand = autoExpand;
        return this;
    }

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

    public int position() {
        return buf().position();
    }

    public ByteBuffer position(int newPosition) {
        autoExpand(newPosition, 0);
        buf().position(newPosition);
        if (mark > newPosition) {
            mark = -1;
        }
        return this;
    }

    public int limit() {
        return buf().limit();
    }

    public ByteBuffer limit(int newLimit) {
        autoExpand(newLimit, 0);
        buf().limit(newLimit);
        if (mark > newLimit) {
            mark = -1;
        }
        return this;
    }

    public ByteBuffer mark() {
        buf().mark();
        mark = position();
        return this;
    }

    public int markValue() {
        return mark;
    }

    public ByteBuffer reset() {
        buf().reset();
        return this;
    }

    public ByteBuffer clear() {
        buf().clear();
        mark = -1;
        return this;
    }

    public ByteBuffer flip() {
        buf().flip();
        mark = -1;
        return this;
    }

    public ByteBuffer rewind() {
        buf().rewind();
        mark = -1;
        return this;
    }

    public byte get() {
        return buf().get();
    }

    public ByteBuffer put(byte b) {
        autoExpand(1);
        buf().put(b);
        return this;
    }

    public byte get(int index) {
        return buf().get(index);
    }

    public ByteBuffer put(int index, byte b) {
        autoExpand(index, 1);
        buf().put(index, b);
        return this;
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {
        buf().get(dst, offset, length);
        return this;
    }

    public ByteBuffer put(java.nio.ByteBuffer src) {
        autoExpand(src.remaining());
        buf().put(src);
        return this;
    }

    public ByteBuffer put(byte[] src, int offset, int length) {
        autoExpand(length);
        buf().put(src, offset, length);
        return this;
    }

    public ByteBuffer compact() {
        buf().compact();
        mark = -1;
        return this;
    }

    public ByteOrder order() {
        return buf().order();
    }

    public ByteBuffer order(ByteOrder bo) {
        buf().order(bo);
        return this;
    }

    public char getChar() {
        return buf().getChar();
    }

    public ByteBuffer putChar(char value) {
        autoExpand(2);
        buf().putChar(value);
        return this;
    }

    public char getChar(int index) {
        return buf().getChar(index);
    }

    public ByteBuffer putChar(int index, char value) {
        autoExpand(index, 2);
        buf().putChar(index, value);
        return this;
    }

    public CharBuffer asCharBuffer() {
        return buf().asCharBuffer();
    }

    public short getShort() {
        return buf().getShort();
    }

    public ByteBuffer putShort(short value) {
        autoExpand(2);
        buf().putShort(value);
        return this;
    }

    public short getShort(int index) {
        return buf().getShort(index);
    }

    public ByteBuffer putShort(int index, short value) {
        autoExpand(index, 2);
        buf().putShort(index, value);
        return this;
    }

    public ShortBuffer asShortBuffer() {
        return buf().asShortBuffer();
    }

    public int getInt() {
        return buf().getInt();
    }

    public ByteBuffer putInt(int value) {
        autoExpand(4);
        buf().putInt(value);
        return this;
    }

    public int getInt(int index) {
        return buf().getInt(index);
    }

    public ByteBuffer putInt(int index, int value) {
        autoExpand(index, 4);
        buf().putInt(index, value);
        return this;
    }

    public IntBuffer asIntBuffer() {
        return buf().asIntBuffer();
    }

    public long getLong() {
        return buf().getLong();
    }

    public ByteBuffer putLong(long value) {
        autoExpand(8);
        buf().putLong(value);
        return this;
    }

    public long getLong(int index) {
        return buf().getLong(index);
    }

    public ByteBuffer putLong(int index, long value) {
        autoExpand(index, 8);
        buf().putLong(index, value);
        return this;
    }

    public LongBuffer asLongBuffer() {
        return buf().asLongBuffer();
    }

    public float getFloat() {
        return buf().getFloat();
    }

    public ByteBuffer putFloat(float value) {
        autoExpand(4);
        buf().putFloat(value);
        return this;
    }

    public float getFloat(int index) {
        return buf().getFloat(index);
    }

    public ByteBuffer putFloat(int index, float value) {
        autoExpand(index, 4);
        buf().putFloat(index, value);
        return this;
    }

    public FloatBuffer asFloatBuffer() {
        return buf().asFloatBuffer();
    }

    public double getDouble() {
        return buf().getDouble();
    }

    public ByteBuffer putDouble(double value) {
        autoExpand(8);
        buf().putDouble(value);
        return this;
    }

    public double getDouble(int index) {
        return buf().getDouble(index);
    }

    public ByteBuffer putDouble(int index, double value) {
        autoExpand(index, 8);
        buf().putDouble(index, value);
        return this;
    }

    public DoubleBuffer asDoubleBuffer() {
        return buf().asDoubleBuffer();
    }
}
