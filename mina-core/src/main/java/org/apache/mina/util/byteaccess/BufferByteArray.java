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

import java.nio.ByteOrder;
import java.util.Collections;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * A <code>ByteArray</code> backed by a <code>IoBuffer</code>. This class
 * is abstract. Subclasses need to override the <code>free()</code> method. An
 * implementation backed by a heap <code>IoBuffer</code> can be created with
 * a <code>SimpleByteArrayFactory</code>.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class BufferByteArray extends AbstractByteArray {

    /**
     * The backing <code>IoBuffer</code>.
     */
    protected IoBuffer bb;

    /**
     * 
     * Creates a new instance of BufferByteArray and uses the supplied
     * {@link IoBuffer} to back this class
     *
     * @param bb
     *  The backing buffer
     */
    public BufferByteArray(IoBuffer bb) {
        this.bb = bb;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<IoBuffer> getIoBuffers() {
        return Collections.singletonList(bb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer getSingleIoBuffer() {
        return bb;
    }

    /**
     * {@inheritDoc}
     * 
     * Calling <code>free()</code> on the returned slice has no effect.
     */
    @Override
    public ByteArray slice(int index, int length) {
        int oldLimit = bb.limit();
        bb.position(index);
        bb.limit(index + length);
        IoBuffer slice = bb.slice();
        bb.limit(oldLimit);
        return new BufferByteArray(slice) {

            @Override
            public void free() {
                // Do nothing.
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor cursor() {
        return new CursorImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor cursor(int index) {
        return new CursorImpl(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int first() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int last() {
        return bb.limit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteOrder order() {
        return bb.order();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void order(ByteOrder order) {
        bb.order(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte get(int index) {
        return bb.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(int index, byte b) {
        bb.put(index, b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get(int index, IoBuffer other) {
        bb.position(index);
        other.put(bb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(int index, IoBuffer other) {
        bb.position(index);
        bb.put(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort(int index) {
        return bb.getShort(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putShort(int index, short s) {
        bb.putShort(index, s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(int index) {
        return bb.getInt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putInt(int index, int i) {
        bb.putInt(index, i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(int index) {
        return bb.getLong(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putLong(int index, long l) {
        bb.putLong(index, l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(int index) {
        return bb.getFloat(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putFloat(int index, float f) {
        bb.putFloat(index, f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(int index) {
        return bb.getDouble(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putDouble(int index, double d) {
        bb.putDouble(index, d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getChar(int index) {
        return bb.getChar(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putChar(int index, char c) {
        bb.putChar(index, c);
    }

    private class CursorImpl implements Cursor {

        private int index;

        public CursorImpl() {
            // This space intentionally blank.
        }

        public CursorImpl(int index) {
            setIndex(index);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRemaining() {
            return last() - index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasRemaining() {
            return getRemaining() > 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getIndex() {
            return index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setIndex(int index) {
            if (index < 0 || index > last()) {
                throw new IndexOutOfBoundsException();
            }
            this.index = index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void skip(int length) {
            setIndex(index + length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteArray slice(int length) {
            ByteArray slice = BufferByteArray.this.slice(index, length);
            index += length;
            return slice;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ByteOrder order() {
            return BufferByteArray.this.order();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte get() {
            byte b = BufferByteArray.this.get(index);
            index += 1;
            return b;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void put(byte b) {
            BufferByteArray.this.put(index, b);
            index += 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void get(IoBuffer bb) {
            int size = Math.min(getRemaining(), bb.remaining());
            BufferByteArray.this.get(index, bb);
            index += size;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void put(IoBuffer bb) {
            int size = bb.remaining();
            BufferByteArray.this.put(index, bb);
            index += size;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public short getShort() {
            short s = BufferByteArray.this.getShort(index);
            index += 2;
            return s;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putShort(short s) {
            BufferByteArray.this.putShort(index, s);
            index += 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInt() {
            int i = BufferByteArray.this.getInt(index);
            index += 4;
            return i;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putInt(int i) {
            BufferByteArray.this.putInt(index, i);
            index += 4;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getLong() {
            long l = BufferByteArray.this.getLong(index);
            index += 8;
            return l;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putLong(long l) {
            BufferByteArray.this.putLong(index, l);
            index += 8;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float getFloat() {
            float f = BufferByteArray.this.getFloat(index);
            index += 4;
            return f;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putFloat(float f) {
            BufferByteArray.this.putFloat(index, f);
            index += 4;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getDouble() {
            double d = BufferByteArray.this.getDouble(index);
            index += 8;
            return d;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putDouble(double d) {
            BufferByteArray.this.putDouble(index, d);
            index += 8;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public char getChar() {
            char c = BufferByteArray.this.getChar(index);
            index += 2;
            return c;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putChar(char c) {
            BufferByteArray.this.putChar(index, c);
            index += 2;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int h = 17;
        
        if (bb != null) {
            h = h * 37 + bb.hashCode();
        }
        
        return h;
    }
}
