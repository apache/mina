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

import java.nio.Buffer;
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
public class IoBuffer {
    /** The list of ByteBuffers were we store the data */
    private BufferList buffers = new BufferList();

    /** The maximal position in the IoBuffer */
    private int limit;

    /** The current position in the buffer */
    private int position;

    /** The marked position, for the next reset() */
    private int mark;

    /** Tells if the stored buffers are direct or heap */
    private BufferType type;

    /** Tells if the IoBuffer is readonly */
    private boolean readOnly;

    /** The bytes order (BIG_INDIAN or LITTLE_INDIAN) */
    private ByteOrder order = ByteOrder.BIG_ENDIAN;

    /** The two types of buffer we handle */
    public enum BufferType {
        HEAP, DIRECT;
    }

    /** A empty bytes array */
    private static final byte[] EMPTY_BYTES = new byte[] {};

    /** <code>UNSET_MARK</code> means the mark has not been set. */
    private static final int UNSET_MARK = -1;

    /**
     * Construct a IoBuffer, with no buffer in it
     */
    public IoBuffer() {
        position = 0;
        mark = 0;
        limit = 0;
        type = null;
        order = null;
    }

    /**
     * Construct an empty IoBuffer with a defined type (either HEAP or DIRECT)
     * 
     * @param bufferType the type of buffer to use : BufferType.HEAP or BufferType.DIRECT
     */
    public IoBuffer(BufferType bufferType) {
        position = 0;
        mark = 0;
        limit = 0;
        type = bufferType;
    }

    /**
     * Construct a IoBuffer with some ByteBuffers. The IoBuffer type will be selected from the first ByteBuffer type, so
     * will the order.
     * 
     * @param byteBuffers the ByteBuffers added to the IoBuffer list
     */
    public IoBuffer(ByteBuffer... byteBuffers) {
        if ((byteBuffers == null) || (byteBuffers.length == 0)) {
            position = 0;
            mark = 0;
            limit = 0;
            type = null;
            order = null;
        } else {
            for (ByteBuffer byteBuffer : byteBuffers) {
                if (type == null) {
                    type = byteBuffer.isDirect() ? BufferType.DIRECT : BufferType.HEAP;
                }

                if (byteBuffer.limit() > 0) {
                    buffers.add(byteBuffer);
                }
            }
        }
    }

    /**
     * Construct a IoBuffer from an existing IoBuffer.
     * 
     * @param ioBuffer the IoBuffer we want to copy
     */
    public IoBuffer(IoBuffer ioBuffer) {
        // Find the position to start with
        BufferNode node = ioBuffer.buffers.getFirst();
        int pos = 0;

        while (node != null) {
            if (node.offset + node.buffer.limit() < position) {
                node = buffers.getNext();
                pos = node.offset + node.buffer.limit();
            } else {
                buffers.add(node.buffer);
            }
        }

        position = position - pos;
        mark = 0;
        limit = ioBuffer.limit() - pos;
        type = ioBuffer.type;
        order = ioBuffer.order();
    }

    /**
     * Adds a new ByteBuffer at the end of the list of buffers.
     * 
     * @param byteBuffer The added ByteBuffer
     * @return The modified IoBuffer
     */
    public IoBuffer add(ByteBuffer... byteBuffers) {
        for (ByteBuffer byteBuffer : byteBuffers) {
            if (byteBuffer.limit() > 0) {
                buffers.add(byteBuffer);
            }
        }

        return this;
    }

    /**
     * Allocate a Heap IoBuffer with a defined capacity
     * 
     * @param capacity The number of bytes to store
     * @return The allocated IoBuffer
     */
    public static IoBuffer allocate(int capacity) {
        if (capacity >= 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);

            return new IoBuffer(byteBuffer);
        } else {
            throw new IllegalArgumentException("Cannot allocate an IoBuffer with a negative value : " + capacity);
        }
    }

    /**
     * Allocate a Direct IoBuffer with a defined capacity
     * 
     * @param capacity The number of bytes to store
     * @return The allocated IoBuffer
     */
    public static IoBuffer allocateDirect(int capacity) {
        if (capacity >= 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity);

            return new IoBuffer(byteBuffer);
        } else {
            throw new IllegalArgumentException("Cannot allocate an IoBuffer with a negative value : " + capacity);
        }
    }

    /**
     * @see ByteBuffer#array() Returns the byte array which this IoBuffer is based on, up to the sum of each contained
     *      ByteBuffer's limit().<br/>
     *      This array can be modified, but this won't modify the content of the underlying ByteBuffer instances,
     *      contrary to the ByteBuffer.array() method.
     * 
     * @return the byte array which this IoBuffer is based on.
     * @exception ReadOnlyBufferException if this IoBuffer is based on a read-only array.
     * @exception UnsupportedOperationException if this IoBuffer is not based on an array.
     */
    public byte[] array() {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        if (buffers.size == 0) {
            return EMPTY_BYTES;
        }

        byte[] array = new byte[buffers.length];
        BufferNode node = buffers.getFirst();
        int pos = 0;

        while (node != null) {
            ByteBuffer buffer = node.buffer;
            byte[] src = buffer.array();
            int length = buffer.limit();

            System.arraycopy(src, 0, array, pos, length);
            pos += length;

            node = buffers.getNext();
        }

        return array;
    }

    /**
     * @see ByteBuffer#arrayOffset() Returns the offset of the byte array which this IoBuffer is based on, if there is
     *      one.
     *      <p>
     *      The offset is the index of the array which corresponds to the zero position of the IoBuffer.
     * 
     * @return the offset of the byte array which this IoBuffer is based on.
     * @exception ReadOnlyBufferException if this IoBuffer is based on a read-only array.
     * @exception UnsupportedOperationException if this IoBuffer is not based on an array.
     */
    public int arrayOffset() {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }

        // The offset is always 0
        return 0;
    }

    /**
     * @see ByteBuffer#asReadOnlyBuffer()
     */
    public IoBuffer asReadOnlyBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @return the IoBuffer total capacity
     */
    public int capacity() {
        return limit;
    }

    /**
     * @see Buffer#clear() Clears this IoBuffer.
     *      <p>
     *      the following internal changes take place:
     *      <ul>
     *      <li>the current position is reset back to the start of the buffer</li>
     *      <li>the value of the buffer limit is made equal to the capacity</li>
     *      <li>and mark is cleared</li>
     *      </ul>
     *      Note that the resulting IoBuffer might be wider than the original one, simply because we will extent the
     *      ByteBuffers limit to their capacity.
     * 
     * @return this buffer.
     */
    public IoBuffer clear() {
        position = 0;
        mark = UNSET_MARK;

        BufferNode node = buffers.head;
        int offset = 0;

        while (node != null) {
            node.buffer.clear();
            node.offset = offset;
            offset += node.buffer.limit();
            node = node.next;
        }

        limit = offset;
        buffers.length = 0;
        buffers.current = buffers.head;

        return this;
    }

    /**
     * @see ByteBuffer#compact()
     */
    public IoBuffer compact() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#compareTo(ByteBuffer)
     */
    public int compareTo(IoBuffer buffer) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#duplicate()
     */
    public IoBuffer duplicate() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see Buffer#flip() Flips this buffer.
     *      <p>
     *      The limit is set to the current position, then the position is set to zero, and the mark is cleared.
     *      <p>
     *      The content of this IoBuffer is not changed.
     * 
     * @return this IoBuffer.
     */
    public IoBuffer flip() {
        limit = position;
        position = 0;
        mark = UNSET_MARK;

        return this;
    }

    /**
     * Get a single byte for the IoBuffer at the current position. Increment the current position.
     * 
     * @return The byte found a the current position.
     */
    public byte get() {
        if (position >= limit) {
            // No more byte to read
            throw new BufferUnderflowException();
        }

        // find the byte from the current buffer now
        BufferNode currentNode = buffers.getCurrent();

        // If the position is within the current buffer, then get the data from it
        int bufferPosition = position - currentNode.offset;

        if (bufferPosition < currentNode.buffer.limit()) {
            position++;

            return currentNode.buffer.get();
        } else {
            // We have exhausted the current buffer, let's see if we have one more
            currentNode = buffers.getNext();

            if (currentNode == null) {
                // No more buffers
                throw new BufferUnderflowException();
            } else {
                position++;
                currentNode.buffer.position(0);

                return currentNode.buffer.get();
            }
        }
    }

    /**
     * @see ByteBuffer#get(byte[]) Reads bytes from the current position into the specified byte array and increases the
     *      position by the number of bytes read.
     *      <p>
     *      Calling this method has the same effect as {@code get(dest, 0, dest.length)}.
     * 
     * @param dest the destination byte array.
     * @return this IoBuffer.
     * @exception BufferUnderflowException if {@code dest.length} is greater than {@code remaining()}.
     */
    public IoBuffer get(byte[] dst) {
        if (dst.length > remaining()) {
            throw new BufferUnderflowException();
        }

        int size = dst.length;
        int destPos = 0;
        BufferNode node = buffers.current;

        while (size > 0) {
            int length = node.buffer.limit() - node.buffer.position();
            System.arraycopy(node.buffer.array(), node.buffer.position(), dst, destPos, length);
            destPos += length;
            node = buffers.getNext();
            size -= length;
        }

        return this;
    }

    /**
     * @see ByteBuffer#get(byte[],int,int)
     */
    public IoBuffer get(byte[] dst, int offset, int length) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#get(int) Returns the byte at the specified index and does not change the position.
     * 
     * @param index the index, must not be negative and less than limit.
     * @return the byte at the specified index.
     * @exception IndexOutOfBoundsException if index is invalid.
     */
    public byte get(int index) {
        if ((index < 0) || (index >= limit)) {
            throw new IndexOutOfBoundsException();
        }

        BufferNode currentNode = buffers.current;
        BufferNode node = buffers.getFirst();

        while (node != null) {
            if (node.offset + node.buffer.limit() > index) {
                byte result = node.buffer.get(index - node.offset);

                // Reset the initial position before returning
                buffers.current = currentNode;

                return result;
            } else {
                node = buffers.getNext();
            }
        }

        // Reset the initial position before returning
        buffers.current = currentNode;

        // Unlikely to happen
        throw new IndexOutOfBoundsException();
    }

    /**
     * @see ByteBuffer#getChar()
     */
    public IoBuffer getChar() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getChar(int)
     */
    public IoBuffer getChar(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getDouble()
     */
    public IoBuffer getDouble() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getDouble(int)
     */
    public IoBuffer getDouble(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getFloat()
     */
    public IoBuffer getFloat() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getFloat(int)
     */
    public IoBuffer getFloat(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getInt() Returns the int at the current position and increases the position by 4.
     *      <p>
     *      The 4 bytes starting at the current position are composed into a int according to the current byte order and
     *      returned.
     * 
     * @return the int at the current position.
     * @exception BufferUnderflowException if the position is greater than {@code limit - 4}.
     */
    public int getInt() {
        int newPosition = position + 4;

        if (newPosition > limit) {
            throw new BufferUnderflowException();
        }

        int result = loadInt(position);
        position = newPosition;

        return result;
    }

    /**
     * Load an int from the underlying byteBuffers, taking the order into account.
     */
    private int loadInt(int index) {
        int bytes = 0;

        if (order == ByteOrder.BIG_ENDIAN) {
            for (int i = 0; i < 4; i++) {
                bytes = bytes << 8;
                bytes = bytes | (get() & 0xFF);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                int val = get() & 0xFF;
                bytes = bytes | (val << (i << 3));
            }
        }

        return bytes;
    }

    /**
     * @see ByteBuffer#getInt(int)
     */
    public IoBuffer getInt(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getLong()
     */
    public IoBuffer getLong() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getLong(int)
     */
    public IoBuffer getLong(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getShort()
     */
    public IoBuffer getShort() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#getShort(int)
     */
    public IoBuffer getShort(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#hashCode()
     */
    @Override
    public int hashCode() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see Buffer#hasRemaining() Indicates if there are elements remaining in this IoBuffer, that is if
     *      {@code position < limit}.
     * 
     * @return {@code true} if there are elements remaining in this IoBuffer, {@code false} otherwise.
     */
    public boolean hasRemaining() {
        return position < limit;
    }

    /**
     * @see ByteBuffer#isDirect() Tells if the stored ByteBuffers are Direct buffers or Heap Buffers
     * @return <code>true</code> if we are storing Direct buffers, <code>false</code> otherwise.
     */
    public boolean isDirect() {
        return type == BufferType.DIRECT;
    }

    /**
     * @see Buffer#isReadOnly() Indicates whether this IoBuffer is read-only.
     * 
     * @return {@code true} if this IoBuffer is read-only, {@code false} otherwise.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @return the IoBuffer limit
     */
    public int limit() {
        return limit;
    }

    /**
     * @see Buffer#mark() Marks the current position, so that the position may return to this point later by calling
     *      <code>reset()</code>.
     * 
     * @return this IoBuffer.
     */
    public IoBuffer mark() {
        mark = position;

        return this;
    }

    /**
     * @see ByteBuffer#order() Returns the byte order used by this Iouffer when converting bytes from/to other primitive
     *      types.
     *      <p>
     *      The default byte order of byte buffer is always {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}
     * 
     * @return the byte order used by this IoBuffer when converting bytes from/to other primitive types.
     */
    public ByteOrder order() {
        return order;
    }

    /**
     * @see ByteBuffer#order(ByteOrder) Sets the byte order of this IoBuffer.
     * 
     * @param byteOrder the byte order to set. If {@code null} then the order will be {@link ByteOrder#LITTLE_ENDIAN
     *        LITTLE_ENDIAN}.
     * @return this IoBuffer.
     * @see ByteOrder
     */
    public IoBuffer order(ByteOrder bo) {
        if (bo == null) {
            order = ByteOrder.LITTLE_ENDIAN;
        } else {
            order = bo;
        }

        return this;
    }

    /**
     * @see Buffer#position()
     * @return The current position across all the ByteBuffers contained in the IoBuffer
     */
    public int position() {
        return position;
    }

    /**
     * @see Buffer#position(int) Sets the position in the IoBuffer.
     *      <p>
     *      If the mark is set and it is greater than the new position, then it is cleared.
     * 
     * @param newPosition the new position, must be not negative and not greater than limit.
     * @return this IoBuffer.
     * @exception IllegalArgumentException if <code>newPosition</code> is invalid.
     */
    public IoBuffer position(int newPosition) {
        if (newPosition < 0) {
            throw new IllegalArgumentException("The new position(" + newPosition + ") is negative");
        }

        if (newPosition >= limit) {
            throw new IllegalArgumentException("The new position(" + newPosition
                    + ") is larger than this buffer limit (" + limit());
        }

        if (buffers.head == null) {
            throw new IllegalArgumentException("Cannot set a position over an empty buffer");
        }

        // Find the right current buffer
        BufferNode currentNode = buffers.getCurrent();

        // The new position might not be on the current buffer.
        if ((newPosition < currentNode.offset) || (newPosition >= currentNode.offset + currentNode.buffer.limit())) {
            // Ok, we aren't on the current buffer. Find the new current buffer
            BufferNode node = buffers.head;
            int counter = 0;

            while (node != null) {
                counter += node.buffer.limit();

                if (counter >= newPosition) {
                    // Found
                    currentNode = node;
                    break;
                } else {
                    node = node.next;
                }
            }
        }

        position = newPosition;
        currentNode.buffer.position(position - currentNode.offset);
        buffers.current = currentNode;

        return this;
    }

    /**
     * @see Buffer#remaining() Returns the number of remaining elements in this IoBuffer, that is
     *      {@code limit - position}.
     * 
     * @return the number of remaining elements in this IoBuffer.
     */
    public int remaining() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see Buffer#reset() Resets the position of this IoBuffer to the <code>mark</code>.
     * 
     * @return this IoBuffer.
     * @exception InvalidMarkException if the mark is not set.
     */
    public IoBuffer reset() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see Buffer#rewind() Rewinds this IoBuffer.
     *      <p>
     *      The position is set to zero, and the mark is cleared. The content of this IoBuffer is not changed.
     * 
     * @return this IoBuffer.
     */
    public IoBuffer rewind() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#slice()
     */
    public IoBuffer slice() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#wrap(byte[])
     */
    static public IoBuffer wrap(byte[] array) {
        return new IoBuffer(ByteBuffer.wrap(array));
    }

    /**
     * @see ByteBuffer#wrap(byte[], int, int)
     */
    public IoBuffer wrap(byte[] array, int offset, int length) {
        return new IoBuffer(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Returns a string representing the IoBuffer.
     * 
     * @return a String representation of the IoBuffer
     */
    @Override
    public String toString() {
        return "IoBuffer[pos=" + position + " lim=" + limit + " mrk=" + mark + "]";
    }

    // ------------------------------------------------------------------------------------------------
    // private inner data structure
    // ------------------------------------------------------------------------------------------------
    /**
     * A container for ByterBuffers stored in the buffers list
     */
    private class BufferNode {
        /** The stored buffer */
        private ByteBuffer buffer;

        /** The next buffer in the list */
        private BufferNode next;

        /** The position of this buffer in the IoBuffer list of bytes */
        private int offset;

        /**
         * Creates a new entry in the list
         * 
         * @param entry The added ByteBuffer
         */
        private BufferNode(ByteBuffer byteBuffer) {
            this.buffer = byteBuffer;
        }

        @Override
        public String toString() {
            return buffer.toString() + ", Offset:" + offset + (next != null ? " --> \n  " : "");
        }
    }

    /**
     * A LinkedList storing all the ByteBuffers. It can only be browsed forward.
     */
    private class BufferList {
        /** The first ByteBuffer in the list */
        private BufferNode head;

        /** The last ByteBuffer in the list */
        private BufferNode tail;

        /** The current ByteBuffer in the list */
        private BufferNode current;

        /** The number of nodes in the list */
        private int size;

        /** The number of bytes in the list */
        private int length;

        /** A flag used to indicate that we already have navigated past the tail of the list. */
        private boolean pastTail;

        /**
         * Creates an empty list
         */
        private BufferList() {
            head = tail = current = null;
            size = 0;
            length = 0;
            pastTail = false;
        }

        /**
         * Creates a list with one ByteBuffer
         * 
         * @param byteBuffer The added ByteBuffer
         */
        private BufferList(ByteBuffer byteBuffer) {
            BufferNode node = new BufferNode(byteBuffer);
            head = tail = current = node;
            size = 1;
            length = byteBuffer.limit();
            pastTail = false;
        }

        /**
         * Adds a new ByteBuffer in the list
         * 
         * @param byteBuffer The added ByteBuffer
         */
        private void add(ByteBuffer byteBuffer) {
            assert (byteBuffer != null);

            // Check the buffer type
            if (type == null) {
                if (byteBuffer.isDirect()) {
                    type = BufferType.DIRECT;
                } else {
                    type = BufferType.HEAP;
                }
            } else {
                if (isDirect() != byteBuffer.isDirect()) {
                    throw new RuntimeException();
                }
            }

            // Check the ByteOrder
            if (size == 0) {
                order = byteBuffer.order();
            } else if (order != byteBuffer.order()) {
                throw new RuntimeException();
            }

            BufferNode newNode = new BufferNode(byteBuffer);
            newNode.offset = length;

            if (size == 0) {
                head = tail = current = newNode;
            } else {
                tail.next = newNode;
                tail = newNode;
            }

            size++;
            length += byteBuffer.limit();
            limit = length;
            pastTail = false;
        }

        /**
         * Get the first BufferNode in the list. The current pointer will move forward, after having be reset to the
         * beginning of the list
         * 
         * @return The first BufferNode in the list
         */
        private BufferNode getFirst() {
            if (head == null) {
                return null;
            }

            current = head.next;
            pastTail = false;

            return head;
        }

        /**
         * Get the next BufferNode from the list. If this is the first time this method is called, it will return the
         * same value than a getFirst().
         * 
         * @return The next BufferNode in the list, moving forward in the list at the same time
         */
        private BufferNode getNext() {
            if (current == null) {
                return null;
            }

            if (current == tail) {
                if (pastTail) {
                    return null;
                } else {
                    pastTail = true;

                    return current;
                }
            } else {
                current = current.next;

                return current;
            }
        }

        /**
         * Gets the current BufferNode from the list, if we aren't already past the tail.
         * 
         * @return The current BufferNode
         */
        private BufferNode getCurrent() {
            if (pastTail) {
                return null;
            }

            return current;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            BufferNode node = head;

            while (node != null) {
                if (node == current) {
                    sb.append("**");
                }

                sb.append(node);
                node = node.next;
            }

            return sb.toString();
        }
    }
}
