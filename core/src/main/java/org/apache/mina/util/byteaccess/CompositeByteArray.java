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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.mina.util.byteaccess.ByteArrayList.Node;

/**
 * A ByteArray composed of other ByteArrays. Optimised for fast relative access
 * via cursors. Absolute access methods are provided, but may perform poorly.
 */
public final class CompositeByteArray implements ByteArray {

    /**
     * Allows for efficient detection of component boundaries when using a cursor.
     *
     * TODO: Is this interface right?
     */
    public interface CursorListener {

        /**
         * Called when the first component in the composite is entered by the cursor.
         */
        public void enteredFirstComponent(int componentIndex, ByteArray component);

        /**
         * Called when the next component in the composite is entered by the cursor.
         */
        public void enteredNextComponent(int componentIndex, ByteArray component);

        /**
         * Called when the previous component in the composite is entered by the cursor.
         */
        public void enteredPreviousComponent(int componentIndex, ByteArray component);

        /**
         * Called when the last component in the composite is entered by the cursor.
         */
        public void enteredLastComponent(int componentIndex, ByteArray component);

    }

    /**
     * Stores the underlying <code>ByteArray</code>s.
     */
    private final ByteArrayList bas = new ByteArrayList();

    private ByteOrder order;

    /**
     * May be used in <code>getSingleByteBuffer</code>. Optional.
     */
    private final ByteArrayFactory byteArrayFactory;

    public CompositeByteArray() {
        this(null);
    }

    public CompositeByteArray(ByteArrayFactory byteArrayFactory) {
        this.byteArrayFactory = byteArrayFactory;
    }

    public ByteArray getFirst() {
        if (bas.isEmpty()) {
            return null;
        } else {
            return bas.getFirst().getByteArray();
        }
    }

    public void addFirst(ByteArray ba) {
        handleAdd(ba);
        bas.addFirst(ba);
    }

    public ByteArray removeFirst() {
        Node node = bas.removeFirst();
        return node == null ? null : node.getByteArray();
    }

    /**
     * Remove component <code>ByteArray</code>s to the given index (splitting
     * them if necessary) and returning them in a single <code>ByteArray</code>.
     * The caller is responsible for freeing the returned object.
     *
     * TODO: Document free behaviour more thoroughly.
     */
    public ByteArray removeTo(int index) {
        if (index < first() || index > last()) {
            throw new IndexOutOfBoundsException();
        }
        // Optimisation when removing exactly one component.
//        if (index == start() + getFirst().length()) {
//            ByteArray component = getFirst();
//            removeFirst();
//            return component;
//        }
        // Removing
        CompositeByteArray prefix = new CompositeByteArray(byteArrayFactory);
        int remaining = index - first();
        while (remaining > 0) {
            ByteArray component = removeFirst();
            if (component.last() <= remaining) {
                // Remove entire component.
                prefix.addLast(component);
                remaining -= component.last();
            } else {
                // Remove part of component. Do this by removing entire
                // component
                // then readding remaining bytes.
                // TODO: Consider using getByteBuffers(), as more generic.
                ByteBuffer bb = component.getSingleByteBuffer();
                int originalLimit = bb.limit();
                bb.position(0);
                bb.limit(remaining);
                ByteBuffer bb1 = bb.slice();
                bb.position(remaining);
                bb.limit(originalLimit);
                ByteBuffer bb2 = bb.slice();
                ByteArray ba1 = new BufferByteArray(bb1) {
                    @Override
                    public void free() {
                        // Do not free.
                    }
                };
                prefix.addLast(ba1);
                remaining -= ba1.last();
                final ByteArray componentFinal = component; // final for
                // anonymous inner
                // class
                ByteArray ba2 = new BufferByteArray(bb2) {
                    @Override
                    public void free() {
                        componentFinal.free();
                    }
                };
                addFirst(ba2);
            }
        }
        return prefix;
    }

    public void addLast(ByteArray ba) {
        handleAdd(ba);
        bas.addLast(ba);
    }

    public ByteArray removeLast() {
        Node node = bas.removeLast();
        return node == null ? null : node.getByteArray();
    }

    /**
     * @inheritDoc
     */
    public void free() {
        while (!bas.isEmpty()) {
            Node node = bas.getLast();
            node.getByteArray().free();
            bas.removeLast();
        }
    }

    private void checkBounds(int index, int accessSize) {
        int lower = index;
        int upper = index + accessSize;
        if (lower < first()) {
            throw new IndexOutOfBoundsException("Index " + lower
                    + " less than start " + first() + ".");
        }
        if (upper > last()) {
            throw new IndexOutOfBoundsException("Index " + upper
                    + " greater than length " + last() + ".");
        }
    }

    /**
     * @inheritDoc
     */
    public Iterable<ByteBuffer> getByteBuffers() {
        if (bas.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<ByteBuffer> result = new ArrayList<ByteBuffer>();
        Node node = bas.getFirst();
        for (ByteBuffer bb : node.getByteArray().getByteBuffers()) {
            result.add(bb);
        }
        while (node.hasNextNode()) {
            node = node.getNextNode();
            for (ByteBuffer bb : node.getByteArray().getByteBuffers()) {
                result.add(bb);
            }
        }
        return result;
    }

    /**
     * @inheritDoc
     */
    public ByteBuffer getSingleByteBuffer() {
        if (byteArrayFactory == null) {
            throw new IllegalStateException(
                    "Can't get single buffer from CompositeByteArray unless it has a ByteArrayFactory.");
        }
        if (bas.isEmpty()) {
            ByteArray ba = byteArrayFactory.create(1);
            return ba.getSingleByteBuffer();
        }
        int actualLength = last() - first();
        {
            Node node = bas.getFirst();
            ByteArray ba = node.getByteArray();
            if (ba.last() == actualLength) {
                return ba.getSingleByteBuffer();
            }
        }
        // Replace all nodes with a single node.
        ByteArray target = byteArrayFactory.create(actualLength);
        ByteBuffer bb = target.getSingleByteBuffer();
        Cursor cursor = cursor();
        cursor.put(bb); // Copy all existing data into target ByteBuffer.
        while (!bas.isEmpty()) {
            Node node = bas.getLast();
            ByteArray component = node.getByteArray();
            bas.removeLast();
            component.free();
        }
        bas.addLast(target);
        return bb;
    }

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
     * Get a cursor starting at index 0 (which may not be the start of the
     * array) and with the given listener.
     */
    public Cursor cursor(CursorListener listener) {
        return new CursorImpl(listener);
    }

    /**
     * Get a cursor starting at the given index and with the given listener.
     */
    public Cursor cursor(int index, CursorListener listener) {
        return new CursorImpl(index, listener);
    }

    /**
     * @inheritDoc
     */
    public byte get(int index) {
        return cursor(index).get();
    }

    /**
     * @inheritDoc
     */
    public void put(int index, byte b) {
        cursor(index).put(b);
    }

    /**
     * @inheritDoc
     */
    public void get(int index, ByteBuffer bb) {
        cursor(index).get(bb);
    }

    /**
     * @inheritDoc
     */
    public void put(int index, ByteBuffer bb) {
        cursor(index).put(bb);
    }

    /**
     * @inheritDoc
     */
    public int getInt(int index) {
        return cursor(index).getInt();
    }

    /**
     * @inheritDoc
     */
    public void putInt(int index, int i) {
        cursor(index).putInt(i);
    }

    /**
     * @inheritDoc
     */
    public int first() {
        return bas.firstByte();
    }

    /**
     * @inheritDoc
     */
    public int last() {
        return bas.lastByte();
    }

    /**
     * @inheritDoc
     */
    public int length() {
        return last() - first();
    }

    private void handleAdd(ByteArray ba) {
        // Check first() is zero, otherwise cursor might not work.
        // TODO: Remove this restriction?
        if (ba.first() != 0) {
            throw new IllegalArgumentException(
                    "Cannot add byte array that doesn't start from 0: "
                            + ba.first());
        }
        // Check order.
        if (order == null) {
            order = ba.order();
        } else if (!order.equals(ba.order())) {
            throw new IllegalArgumentException(
                    "Cannot add byte array with different byte order: "
                            + ba.order());
        }
    }

    /**
     * @inheritDoc
     */
    public ByteOrder order() {
        if (order == null) {
            throw new IllegalStateException("Byte order not yet set.");
        }
        return order;
    }

    /**
     * @inheritDoc
     */
    public void order(ByteOrder order) {
        if (order == null || !order.equals(this.order)) {
            this.order = order;
            if (!bas.isEmpty()) {
                for (Node node = bas.getFirst(); node.hasNextNode(); node = node
                        .getNextNode()) {
                    node.getByteArray().order(order);
                }
            }
        }
    }

    private class CursorImpl implements Cursor {

        private int index;

        private final CursorListener listener;

        private Node componentNode;

        // Index of start of current component.
        private int componentIndex;

        // Cursor within current component.
        private ByteArray.Cursor componentCursor;

        public CursorImpl() {
            this(0, null);
        }

        public CursorImpl(int index) {
            this(index, null);
        }

        public CursorImpl(CursorListener listener) {
            this(0, listener);
        }

        public CursorImpl(int index, CursorListener listener) {
            this.index = index;
            this.listener = listener;
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
            checkBounds(index, 0);
            this.index = index;
        }

        /**
         * @inheritDoc
         */
        public ByteOrder order() {
            return CompositeByteArray.this.order();
        }

        private void prepareForAccess(int accessSize) {
            // Handle removed node. Do this first so we can remove the reference
            // even if bounds checking fails.
            if (componentNode != null && componentNode.isRemoved()) {
                componentNode = null;
                componentCursor = null;
            }

            // Bounds checks
            checkBounds(index, accessSize);

            // Remember the current node so we can later tell whether or not we
            // need to create a new cursor.
            Node oldComponentNode = componentNode;

            // Handle missing node.
            if (componentNode == null) {
                int basMidpoint = (last() - first()) / 2 + first();
                if (index <= basMidpoint) {
                    // Search from the start.
                    componentNode = bas.getFirst();
                    componentIndex = first();
                    if (listener != null) {
                        listener.enteredFirstComponent(componentIndex,
                                componentNode.getByteArray());
                    }
                } else {
                    // Search from the end.
                    componentNode = bas.getLast();
                    componentIndex = last()
                            - componentNode.getByteArray().last();
                    if (listener != null) {
                        listener.enteredLastComponent(componentIndex,
                                componentNode.getByteArray());
                    }
                }
            }

            // Go back, if necessary.
            while (index < componentIndex) {
                componentNode = componentNode.getPreviousNode();
                componentIndex -= componentNode.getByteArray().last();
                if (listener != null) {
                    listener.enteredPreviousComponent(componentIndex,
                            componentNode.getByteArray());
                }
            }

            // Go forward, if necessary.
            while (index >= componentIndex + componentNode.getByteArray()
                    .last()) {
                componentIndex += componentNode.getByteArray().last();
                componentNode = componentNode.getNextNode();
                if (listener != null) {
                    listener.enteredNextComponent(componentIndex, componentNode
                            .getByteArray());
                }
            }

            // Update the cursor.
            int internalComponentIndex = index - componentIndex;
            if (componentNode == oldComponentNode) {
                // Move existing cursor.
                componentCursor.setIndex(internalComponentIndex);
            } else {
                // Create new cursor.
                componentCursor = componentNode.getByteArray().cursor(
                        internalComponentIndex);
            }
        }

        /**
         * @inheritDoc
         */
        public int getRemaining() {
            return last() - index + 1;
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
        public byte get() {
            prepareForAccess(1);
            byte b = componentCursor.get();
            index += 1;
            return b;
        }

        /**
         * @inheritDoc
         */
        public void put(byte b) {
            prepareForAccess(1);
            componentCursor.put(b);
            index += 1;
        }

        /**
         * @inheritDoc
         */
        public void get(ByteBuffer bb) {
            prepareForAccess(bb.remaining());
            while (bb.hasRemaining()) {
                int chunkSize = componentCursor.getRemaining();
                prepareForAccess(chunkSize);
                componentCursor.get(bb);
                index += chunkSize;
            }
        }

        /**
         * @inheritDoc
         */
        public void put(ByteBuffer bb) {
            prepareForAccess(bb.remaining());
            while (bb.hasRemaining()) {
                int chunkSize = componentCursor.getRemaining();
                prepareForAccess(chunkSize);
                componentCursor.put(bb);
                index += chunkSize;
            }
        }

        /**
         * @inheritDoc
         */
        public int getInt() {
            prepareForAccess(4);
            if (componentCursor.getRemaining() >= 4) {
                int i = componentCursor.getInt();
                index += 4;
                return i;
            } else {
                byte b1 = get();
                byte b2 = get();
                byte b3 = get();
                byte b4 = get();
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    return b1 << 24 & b2 << 16 & b3 << 8 & b4;
                } else {
                    return b4 << 24 & b3 << 16 & b2 << 8 & b1;
                }
            }
        }

        /**
         * @inheritDoc
         */
        public void putInt(int i) {
            prepareForAccess(4);
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putInt(i);
                index += 4;
            } else {
                byte b1;
                byte b2;
                byte b3;
                byte b4;
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    b4 = (byte) (i & 0xff);
                    i >>= 8;
                    b3 = (byte) (i & 0xff);
                    i >>= 8;
                    b2 = (byte) (i & 0xff);
                    i >>= 8;
                    b1 = (byte) (i & 0xff);
                } else {
                    b1 = (byte) (i & 0xff);
                    i >>= 8;
                    b2 = (byte) (i & 0xff);
                    i >>= 8;
                    b3 = (byte) (i & 0xff);
                    i >>= 8;
                    b4 = (byte) (i & 0xff);
                }
                put(b1);
                put(b2);
                put(b3);
                put(b4);
            }
        }
    }
}
