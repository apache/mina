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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.byteaccess.ByteArrayList.Node;

/**
 * A ByteArray composed of other ByteArrays. Optimized for fast relative access
 * via cursors. Absolute access methods are provided, but may perform poorly.
 *
 * TODO: Write about laziness of cursor implementation - how movement doesn't
 * happen until actual get/put.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class CompositeByteArray extends AbstractByteArray {

    /**
     * Allows for efficient detection of component boundaries when using a cursor.
     *
     * TODO: Is this interface right?
     */
    public interface CursorListener {
        /**
         * Called when the first component in the composite is entered by the cursor.
         * 
         * @param componentIndex The component position
         * @param component The component to use
         */
        void enteredFirstComponent(int componentIndex, ByteArray component);

        /**
         * Called when the next component in the composite is entered by the cursor.
         * 
         * @param componentIndex The component position
         * @param component The component to use
         */
        void enteredNextComponent(int componentIndex, ByteArray component);

        /**
         * Called when the previous component in the composite is entered by the cursor.
         * 
         * @param componentIndex The component position
         * @param component The component to use
         */
        void enteredPreviousComponent(int componentIndex, ByteArray component);

        /**
         * Called when the last component in the composite is entered by the cursor.
         * 
         * @param componentIndex The component position
         * @param component The component to use
         */
        void enteredLastComponent(int componentIndex, ByteArray component);
    }

    /**
     * Stores the underlying <code>ByteArray</code>s.
     */
    private final ByteArrayList bas = new ByteArrayList();

    /**
     * The byte order for data in the buffer
     */
    private ByteOrder order;

    /**
     * May be used in <code>getSingleIoBuffer</code>. Optional.
     */
    private final ByteArrayFactory byteArrayFactory;

    /**
     * Creates a new instance of CompositeByteArray.
     */
    public CompositeByteArray() {
        this(null);
    }

    /**
     * 
     * Creates a new instance of CompositeByteArray.
     *
     * @param byteArrayFactory
     *  The factory used to create the ByteArray objects
     */
    public CompositeByteArray(ByteArrayFactory byteArrayFactory) {
        this.byteArrayFactory = byteArrayFactory;
    }

    /**
     * @return the first {@link ByteArray} in the list
     */
    public ByteArray getFirst() {
        if (bas.isEmpty()) {
            return null;
        }

        return bas.getFirst().getByteArray();
    }

    /**
     * Adds the specified {@link ByteArray} to the first
     * position in the list
     *
     * @param ba The ByteArray to add to the list
     */
    public void addFirst(ByteArray ba) {
        addHook(ba);
        bas.addFirst(ba);
    }

    /**
     * Remove the first {@link ByteArray} in the list
     *
     * @return The first ByteArray in the list
     */
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
     * 
     * @param index The index from where we will remove bytes
     * @return The resulting byte aaay
     */
    public ByteArray removeTo(int index) {
        if (index < first() || index > last()) {
            throw new IndexOutOfBoundsException();
        }

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
                // component then readding remaining bytes.
                // TODO: Consider using getIoBuffers(), as would avoid
                // performance problems for nested ComponentByteArrays.
                IoBuffer bb = component.getSingleIoBuffer();
                
                // get the limit of the buffer
                int originalLimit = bb.limit();
                
                // set the position to the beginning of the buffer
                bb.position(0);
                
                // set the limit of the buffer to what is remaining
                bb.limit(remaining);
                
                // create a new IoBuffer, sharing the data with 'bb'
                IoBuffer bb1 = bb.slice();
                
                // set the position at the end of the buffer
                bb.position(remaining);
                
                // gets the limit of the buffer
                bb.limit(originalLimit);
                
                // create a new IoBuffer, sharing teh data with 'bb'
                IoBuffer bb2 = bb.slice();
                
                // create a new ByteArray with 'bb1'
                ByteArray ba1 = new BufferByteArray(bb1) {
                    @Override
                    public void free() {
                        // Do not free.  This will get freed 
                    }
                };

                // add the new ByteArray to the CompositeByteArray
                prefix.addLast(ba1);
                remaining -= ba1.last();

                // final for anonymous inner class
                final ByteArray componentFinal = component;
                ByteArray ba2 = new BufferByteArray(bb2) {
                    @Override
                    public void free() {
                        componentFinal.free();
                    }
                };
                
                // add the new ByteArray to the CompositeByteArray
                addFirst(ba2);
            }
        }

        // return the CompositeByteArray
        return prefix;
    }

    /**
     * Adds the specified {@link ByteArray} to the end of the list
     *
     * @param ba The ByteArray to add to the end of the list
     */
    public void addLast(ByteArray ba) {
        addHook(ba);
        bas.addLast(ba);
    }

    /**
     * Removes the last {@link ByteArray} in the list
     *
     * @return The ByteArray that was removed
     */
    public ByteArray removeLast() {
        Node node = bas.removeLast();
        
        return node == null ? null : node.getByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void free() {
        while (!bas.isEmpty()) {
            Node node = bas.getLast();
            node.getByteArray().free();
            bas.removeLast();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<IoBuffer> getIoBuffers() {
        if (bas.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<IoBuffer> result = new ArrayList<>();
        Node node = bas.getFirst();

        for (IoBuffer bb : node.getByteArray().getIoBuffers()) {
            result.add(bb);
        }

        while (node.hasNextNode()) {
            node = node.getNextNode();

            for (IoBuffer bb : node.getByteArray().getIoBuffers()) {
                result.add(bb);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer getSingleIoBuffer() {
        if (byteArrayFactory == null) {
            throw new IllegalStateException(
                    "Can't get single buffer from CompositeByteArray unless it has a ByteArrayFactory.");
        }

        if (bas.isEmpty()) {
            ByteArray ba = byteArrayFactory.create(1);
            return ba.getSingleIoBuffer();
        }

        int actualLength = last() - first();

        Node firstNode = bas.getFirst();
        ByteArray ba = firstNode.getByteArray();

        if (ba.last() == actualLength) {
            return ba.getSingleIoBuffer();
        }

        // Replace all nodes with a single node.
        ByteArray target = byteArrayFactory.create(actualLength);
        IoBuffer bb = target.getSingleIoBuffer();
        Cursor cursor = cursor();
        cursor.put(bb); // Copy all existing data into target IoBuffer.

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
     * Get a cursor starting at index 0 (which may not be the start of the
     * array) and with the given listener.
     * 
     * @param listener The listener to use
     * @return a new {@link ByteArray.Cursor} instance
     */
    public Cursor cursor(CursorListener listener) {
        return new CursorImpl(listener);
    }

    /**
     * Get a cursor starting at the given index and with the given listener.
     * 
     * @param index The position of the array to start the Cursor at
     * @param listener The listener for the Cursor that is returned
     * @return The created Cursor
     */
    public Cursor cursor(int index, CursorListener listener) {
        return new CursorImpl(index, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteArray slice(int index, int length) {
        return cursor(index).slice(length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte get(int index) {
        return cursor(index).get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(int index, byte b) {
        cursor(index).put(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get(int index, IoBuffer bb) {
        cursor(index).get(bb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(int index, IoBuffer bb) {
        cursor(index).put(bb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int first() {
        return bas.firstByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int last() {
        return bas.lastByte();
    }

    /**
     * This method should be called prior to adding any component
     * <code>ByteArray</code> to a composite.
     *
     * @param ba The component to add.
     */
    private void addHook(ByteArray ba) {
        // Check first() is zero, otherwise cursor might not work.
        // TODO: Remove this restriction?
        if (ba.first() != 0) {
            throw new IllegalArgumentException("Cannot add byte array that doesn't start from 0: " + ba.first());
        }
        
        // Check order.
        if (order == null) {
            order = ba.order();
        } else if (!order.equals(ba.order())) {
            throw new IllegalArgumentException("Cannot add byte array with different byte order: " + ba.order());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteOrder order() {
        if (order == null) {
            throw new IllegalStateException("Byte order not yet set.");
        }
        return order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void order(ByteOrder order) {
        if (order == null || !order.equals(this.order)) {
            this.order = order;

            if (!bas.isEmpty()) {
                for (Node node = bas.getFirst(); node.hasNextNode(); node = node.getNextNode()) {
                    node.getByteArray().order(order);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort(int index) {
        return cursor(index).getShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putShort(int index, short s) {
        cursor(index).putShort(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(int index) {
        return cursor(index).getInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putInt(int index, int i) {
        cursor(index).putInt(i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(int index) {
        return cursor(index).getLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putLong(int index, long l) {
        cursor(index).putLong(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(int index) {
        return cursor(index).getFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putFloat(int index, float f) {
        cursor(index).putFloat(f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(int index) {
        return cursor(index).getDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putDouble(int index, double d) {
        cursor(index).putDouble(d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getChar(int index) {
        return cursor(index).getChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putChar(int index, char c) {
        cursor(index).putChar(c);
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
            checkBounds(index, 0);
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
            CompositeByteArray slice = new CompositeByteArray(byteArrayFactory);
            int remaining = length;

            while (remaining > 0) {
                prepareForAccess(remaining);
                int componentSliceSize = Math.min(remaining, componentCursor.getRemaining());
                ByteArray componentSlice = componentCursor.slice(componentSliceSize);
                slice.addLast(componentSlice);
                index += componentSliceSize;
                remaining -= componentSliceSize;
            }
            
            return slice;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
                        listener.enteredFirstComponent(componentIndex, componentNode.getByteArray());
                    }
                } else {
                    // Search from the end.
                    componentNode = bas.getLast();
                    componentIndex = last() - componentNode.getByteArray().last();
                    
                    if (listener != null) {
                        listener.enteredLastComponent(componentIndex, componentNode.getByteArray());
                    }
                }
            }

            // Go back, if necessary.
            while (index < componentIndex) {
                componentNode = componentNode.getPreviousNode();
                componentIndex -= componentNode.getByteArray().last();
                
                if (listener != null) {
                    listener.enteredPreviousComponent(componentIndex, componentNode.getByteArray());
                }
            }

            // Go forward, if necessary.
            while (index >= componentIndex + componentNode.getByteArray().length()) {
                componentIndex += componentNode.getByteArray().last();
                componentNode = componentNode.getNextNode();
                
                if (listener != null) {
                    listener.enteredNextComponent(componentIndex, componentNode.getByteArray());
                }
            }

            // Update the cursor.
            int internalComponentIndex = index - componentIndex;
            
            if (componentNode == oldComponentNode) {
                // Move existing cursor.
                componentCursor.setIndex(internalComponentIndex);
            } else {
                // Create new cursor.
                componentCursor = componentNode.getByteArray().cursor(internalComponentIndex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRemaining() {
            return last() - index + 1;
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
        public byte get() {
            prepareForAccess(1);
            byte b = componentCursor.get();
            index += 1;
            
            return b;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void put(byte b) {
            prepareForAccess(1);
            componentCursor.put(b);
            index += 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void get(IoBuffer bb) {
            while (bb.hasRemaining()) {
                int remainingBefore = bb.remaining();
                prepareForAccess(remainingBefore);
                componentCursor.get(bb);
                int remainingAfter = bb.remaining();
                
                // Advance index by actual amount got.
                int chunkSize = remainingBefore - remainingAfter;
                index += chunkSize;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void put(IoBuffer bb) {
            while (bb.hasRemaining()) {
                int remainingBefore = bb.remaining();
                prepareForAccess(remainingBefore);
                componentCursor.put(bb);
                int remainingAfter = bb.remaining();
                
                // Advance index by actual amount put.
                int chunkSize = remainingBefore - remainingAfter;
                index += chunkSize;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public short getShort() {
            prepareForAccess(2);

            if (componentCursor.getRemaining() >= 4) {
                short s = componentCursor.getShort();
                index += 2;
                
                return s;
            } else {
                byte b0 = get();
                byte b1 = get();
                
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    return (short) ((b0 << 8) | (b1 & 0xFF));
                } else {
                    return (short) ((b1 << 8) | (b0 & 0xFF));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putShort(short s) {
            prepareForAccess(2);
            
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putShort(s);
                index += 2;
            } else {
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    put((byte) ((s >> 8) & 0xff));
                    put((byte) (s & 0xff));
                } else {
                    put((byte) (s & 0xff));
                    put((byte) ((s >> 8) & 0xff));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInt() {
            prepareForAccess(4);
            
            if (componentCursor.getRemaining() >= 4) {
                int i = componentCursor.getInt();
                index += 4;
                
                return i;
            } else {
                byte b0 = get();
                byte b1 = get();
                byte b2 = get();
                byte b3 = get();
                
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    return (b0 << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
                } else {
                    return (b3 << 24) | ((b2 & 0xFF) << 16) | ((b1 & 0xFF) << 8) | (b0 & 0xFF);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putInt(int i) {
            prepareForAccess(4);
            
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putInt(i);
                index += 4;
            } else {
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    put((byte) ((i >> 24) & 0xff));
                    put((byte) ((i >> 16) & 0xff));
                    put((byte) ((i >> 8) & 0xff));
                    put((byte) (i & 0xff));
                } else {
                    put((byte) (i & 0xff));
                    put((byte) ((i >> 8) & 0xff));
                    put((byte) ((i >> 16) & 0xff));
                    put((byte) ((i >> 24) & 0xff));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getLong() {
            prepareForAccess(8);
            
            if (componentCursor.getRemaining() >= 4) {
                long l = componentCursor.getLong();
                index += 8;
                
                return l;
            } else {
                byte b0 = get();
                byte b1 = get();
                byte b2 = get();
                byte b3 = get();
                byte b4 = get();
                byte b5 = get();
                byte b6 = get();
                byte b7 = get();
                
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    return ((b0 & 0xFFL) << 56) | ((b1 & 0xFFL) << 48) | ((b2 & 0xFFL) << 40) | ((b3 & 0xFFL) << 32)
                            | ((b4 & 0xFFL) << 24) | ((b5 & 0xFFL) << 16) | ((b6 & 0xFFL) << 8) | (b7 & 0xFFL);
                } else {
                    return ((b7 & 0xFFL) << 56) | ((b6 & 0xFFL) << 48) | ((b5 & 0xFFL) << 40) | ((b4 & 0xFFL) << 32)
                            | ((b3 & 0xFFL) << 24) | ((b2 & 0xFFL) << 16) | ((b1 & 0xFFL) << 8) | (b0 & 0xFFL);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putLong(long l) {
            prepareForAccess(8);
            
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putLong(l);
                index += 8;
            } else {
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    put((byte) ((l >> 56) & 0xff));
                    put((byte) ((l >> 48) & 0xff));
                    put((byte) ((l >> 40) & 0xff));
                    put((byte) ((l >> 32) & 0xff));
                    put((byte) ((l >> 24) & 0xff));
                    put((byte) ((l >> 16) & 0xff));
                    put((byte) ((l >> 8) & 0xff));
                    put((byte) (l & 0xff));
                } else {
                    put((byte) (l & 0xff));
                    put((byte) ((l >> 8) & 0xff));
                    put((byte) ((l >> 16) & 0xff));
                    put((byte) ((l >> 24) & 0xff));
                    put((byte) ((l >> 32) & 0xff));
                    put((byte) ((l >> 40) & 0xff));
                    put((byte) ((l >> 48) & 0xff));
                    put((byte) ((l >> 56) & 0xff));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float getFloat() {
            prepareForAccess(4);
            
            if (componentCursor.getRemaining() >= 4) {
                float f = componentCursor.getFloat();
                index += 4;
                return f;
            } else {
                int i = getInt();
                
                return Float.intBitsToFloat(i);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putFloat(float f) {
            prepareForAccess(4);
            
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putFloat(f);
                index += 4;
            } else {
                int i = Float.floatToIntBits(f);
                putInt(i);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getDouble() {
            prepareForAccess(8);
            
            if (componentCursor.getRemaining() >= 4) {
                double d = componentCursor.getDouble();
                index += 8;
                
                return d;
            } else {
                long l = getLong();
                
                return Double.longBitsToDouble(l);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putDouble(double d) {
            prepareForAccess(8);
            
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putDouble(d);
                index += 8;
            } else {
                long l = Double.doubleToLongBits(d);
                putLong(l);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public char getChar() {
            prepareForAccess(2);
            
            if (componentCursor.getRemaining() >= 4) {
                char c = componentCursor.getChar();
                index += 2;
                
                return c;
            } else {
                byte b0 = get();
                byte b1 = get();
                
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    return (char)((b0 << 8) | (b1 & 0xFF));
                } else {
                    return (char)((b1 << 8) | (b0 & 0xFF));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putChar(char c) {
            prepareForAccess(2);
            
            
            if (componentCursor.getRemaining() >= 4) {
                componentCursor.putChar(c);
                index += 2;
            } else {
                byte b0;
                byte b1;
                
                if (order.equals(ByteOrder.BIG_ENDIAN)) {
                    b0 = (byte) ((c >> 8) & 0xff);
                    b1 = (byte) (c & 0xff);
                } else {
                    b0 = (byte) (c & 0xff);
                    b1 = (byte) ((c >> 8) & 0xff);
                }
                
                put(b0);
                put(b1);
            }
        }
        
        private void checkBounds(int index, int accessSize) {
            int lower = index;
            int upper = index + accessSize;

            if (lower < first()) {
                throw new IndexOutOfBoundsException("Index " + lower + " less than start " + first() + ".");
            }

            if (upper > last()) {
                throw new IndexOutOfBoundsException("Index " + upper + " greater than length " + last() + ".");
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int h = 17;

        h = h*37 + bas.hashCode();
        
        return h;
    }
}
