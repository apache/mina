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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

/**
 * A <code>ByteArray</code> backed by a <code>ByteBuffer</code>. This class
 * is abstract. Subclasses need to override the <code>free()</code> method. An
 * implementation backed by a heap <code>ByteBuffer</code> can be created with
 * a <code>SimpleByteArrayFactory</code>.
 *
 */
public abstract class BufferByteArray implements ByteArray {

    /**
     * The backing <code>ByteBuffer</code>.
     */
    protected ByteBuffer bb;

    public BufferByteArray(ByteBuffer bb) {
        this.bb = bb;
    }

    /**
     * @inheritDoc
     */
    public Iterable<ByteBuffer> getByteBuffers() {
        return Collections.singletonList(bb);
    }

    /**
     * @inheritDoc
     */
    public ByteBuffer getSingleByteBuffer() {
        return bb;
    }

    /**
     * @inheritDoc
     */
    public abstract void free();

    /**
     * @inheritDoc
     */
    public Cursor cursor() {
        return new CursorImpl();
    }

    /**
     * @inheritDoc
     */
    public Cursor cursor(int index) {
        return new CursorImpl(index);
    }

    /**
     * @inheritDoc
     */
    public int first() {
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int last() {
        return bb.limit();
    }

    /**
     * @inheritDoc
     */
    public int length() {
        return last() - first();
    }

    /**
     * @inheritDoc
     */
    public ByteOrder order() {
        return bb.order();
    }

    /**
     * @inheritDoc
     */
    public void order(ByteOrder order) {
        bb.order(order);
    }

    /**
     * @inheritDoc
     */
    public byte get(int index) {
        return bb.get(index);
    }

    /**
     * @inheritDoc
     */
    public void put(int index, byte b) {
        bb.put(index, b);
    }

    /**
     * @inheritDoc
     */
    public void get(int index, ByteBuffer other) {
        bb.position(index);
        other.put(bb);
    }

    /**
     * @inheritDoc
     */
    public void put(int index, ByteBuffer other) {
        bb.position(index);
        bb.put(other);
    }

    /**
     * @inheritDoc
     */
    public int getInt(int index) {
        return bb.getInt(index);
    }

    /**
     * @inheritDoc
     */
    public void putInt(int index, int i) {
        bb.putInt(index, i);
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
         * @inheritDoc
         */
        public int getRemaining() {
            return last() - index;
        }

        /**
         * @inheritDoc
         */
        public boolean hasRemaining() {
            return getRemaining() > 0;
        }

        /**
         * @inheritDoc
         */
        public int getIndex() {
            return index;
        }

        /**
         * @inheritDoc
         */
        public void setIndex(int index) {
            if (index < 0 || index > last()) {
                throw new IndexOutOfBoundsException();
            }
            this.index = index;
        }

        /**
         * @inheritDoc
         */
        public ByteOrder order() {
            return BufferByteArray.this.order();
        }

        /**
         * @inheritDoc
         */
        public byte get() {
            byte b = BufferByteArray.this.get(index);
            index += 1;
            return b;
        }

        /**
         * @inheritDoc
         */
        public void put(byte b) {
            BufferByteArray.this.put(index, b);
            index += 1;
        }

        /**
         * @inheritDoc
         */
        public void get(ByteBuffer bb) {
            int size = bb.remaining();
            BufferByteArray.this.get(index, bb);
            index += size;
        }

        /**
         * @inheritDoc
         */
        public void put(ByteBuffer bb) {
            int size = bb.remaining();
            BufferByteArray.this.put(index, bb);
            index += size;
        }

        /**
         * @inheritDoc
         */
        public int getInt() {
            int i = BufferByteArray.this.getInt(index);
            index += 4;
            return i;
        }

        /**
         * @inheritDoc
         */
        public void putInt(int i) {
            BufferByteArray.this.putInt(index, i);
            index += 4;
        }

    }
}
