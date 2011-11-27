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
package org.apache.mina.util;

import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import java.lang.UnsupportedOperationException;

/**
 * A proxy class used to manage ByteBuffers as if they were just a big ByteBuffer. We can
 * add as many buffers as needed, when accumulating data. From the user PoV, the methods
 * are the very same than what we can get from ByteBuffer.
 * <br/>
 * IoBuffer instances are *not* thred safe.
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
    
    /** The two types of buffer we handle */
    public enum BufferType {
        HEAP, DIRECT;
    }
    
    /**
     * Construct a IoBuffer, with no buffer in it
     */
    public IoBuffer() {
        position = 0;
        mark = 0;
        limit = 0;
        type = null;
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
     * Construct a IoBuffer with no ByteBuffer. The IoBuffer type is not direct.
     * (ie direct or heap will be deduce from this first ByteBuffer characteristic.
     * @param byteBuffer the first ByteBuffer added to the IoBuffer list
     */
    public IoBuffer(ByteBuffer byteBuffer) {
        buffers.add(byteBuffer);
        position = 0;
        mark = 0;
        limit = byteBuffer.limit();
        type = byteBuffer.isDirect() ? BufferType.DIRECT : BufferType.HEAP;
    }
    
    /**
     * Adds a new ByteBuffer at the end of the list of buffers.
     * 
     * @param byteBuffer The added ByteBuffer
     * @return The modified IoBuffer
     */
    public IoBuffer add(ByteBuffer... byteBuffers) {
        for (ByteBuffer byteBuffer:byteBuffers) {
            if (byteBuffer.limit() > 0) {
                buffers.add(byteBuffer);
            }
        }
        
        return this;
    }
    
    /**
     * Allocate a Heap IoBuffer with a defined capacity
     * @param capacity The number of bytes to store
     * @return The allocated IoBuffer
     */
    public static IoBuffer allocate(int capacity) {
        if (capacity >= 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);

            return new IoBuffer(byteBuffer);
        } else {
            return new IoBuffer(BufferType.HEAP);
        }
    }
    
    /**
     * Allocate a Direct IoBuffer with a defined capacity
     * @param capacity The number of bytes to store
     * @return The allocated IoBuffer
     */
    public static IoBuffer allocateDirect(int capacity) {
        if (capacity >= 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity);

            return new IoBuffer(byteBuffer);
        } else {
            return new IoBuffer(BufferType.DIRECT);
        }
    }

    /**
     * @see ByteBuffer#array()
     */
    public byte[] array() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#array()
     */
    public int arrayOffset() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#asCharBuffer()
     */
    public CharBuffer asCharBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#asDoubleBuffer()
     */
    public DoubleBuffer asDoubleBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#asFloatBuffer()
     */
    public FloatBuffer asFloatBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#asIntBuffer()
     */
    public IntBuffer asIntBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#asLongBuffer()
     */
    public LongBuffer asLongBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#asReadOnlyBuffer()
     */
    public IoBuffer asReadOnlyBuffer() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#asShortBuffer()
     */
    public ShortBuffer asShortBuffer() {
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
     * @see Buffer#clear()
     */
    public IoBuffer clear() {
        // TODO code me !
        throw new UnsupportedOperationException();
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
    public boolean equals(Object object) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see Buffer#flip()
     */
    public IoBuffer flip() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * Get a single byte for the IoBuffer at the current position. Increment the current position.
     * @return The byte found a the current position.
     */
    public byte get() {
        if (position>limit) {
            // No more byte to read
            throw new BufferUnderflowException();
        }
        
        // find the byte from the current buffer now
        BufferNode currentNode = buffers.getCurrent();
        
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
                
                return currentNode.buffer.get();
            }
        }
    }
    
    /**
     * @see ByteBuffer#get(byte[])
     */
    public IoBuffer get(byte[] dst) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#get(byte[],int,int)
     */
    public IoBuffer get(byte[] dst, int offset, int length) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#get(int)
     */
    public IoBuffer get(int index) {
        // TODO code me !
        throw new UnsupportedOperationException();
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
     * @see ByteBuffer#getInt()
     */
    public IoBuffer getInt() {
        // TODO code me !
        throw new UnsupportedOperationException();
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
     * @see ByteBuffer#hasArray()
     */
    public boolean hasArray() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#hashCode()
     */
    public int hashCode() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see Buffer#hasRemaining()
     */
    public boolean hasRemaining() {
        return position < limit;
    }
    
    /**
     * @see ByteBuffer#isDirect()
     * Tells if the stored ByteBuffers are Direct buffers or Heap Buffers
     * @return <code>true</code> if we are storing Direct buffers, <code>false</code> otherwise.
     */
    public boolean isDirect() {
        return type == BufferType.DIRECT;
    }

    /**
     * @see Buffer#isReadOnly()
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
     * @see Buffer#limit(int)
     */
    public IoBuffer limit(int newLimit) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see Buffer#mark()
     */
    public IoBuffer mark() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }

    /**
     * @see ByteBuffer#order()
     */
    public IoBuffer order() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#order(ByteOrder)
     */
    public IoBuffer order(ByteOrder bo) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @return The current position across all the ByteBuffers
     */
    public int position() {
        return position;
    }

    /**
     * @see Buffer#position(int)
     */
    public IoBuffer position(int newPosition) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#put(byte)
     */
    public IoBuffer put(byte b) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#put(byte[])
     */
    public IoBuffer put(byte[] src) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    public IoBuffer put(byte[] src, int offset, int length) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#put(int, byte)
     */
    public IoBuffer put(int index, byte b) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * Add a new ByteBuffer into the IoBuffer. Note that the added ByteBuffer is *not* copied.
     * The argument is also supposed to have a position set to 0.
     * @param byteBuffer The added ByteBuffer.
     * @return The IoBuffer instance
     */
    public IoBuffer put(ByteBuffer byteBuffer) {
        assert(byteBuffer != null);
        
        if (byteBuffer.isDirect() && (type != BufferType.DIRECT)) {
            throw new RuntimeException();
        }
        
        limit += byteBuffer.limit();
        buffers.add(byteBuffer);
        
        return this;
    }
    
    /**
     * @see ByteBuffer#putChar(char)
     */
    public IoBuffer putChar(char value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putChar(int, char)
     */
    public IoBuffer putChar(int index, char value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putDouble(double)
     */
    public IoBuffer putDouble(double value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putDouble(int, double)
     */
    public IoBuffer putDouble(int index, double value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putFloat(float)
     */
    public IoBuffer putFloat(float value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putFloat(int, float)
     */
    public IoBuffer putFloat(int index, float value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putInt(int)
     */
    public IoBuffer putInt(int value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putInt(int, int)
     */
    public IoBuffer putInt(int index, int value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putLong(long)
     */
    public IoBuffer putLong(long value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putLong(int, long)
     */
    public IoBuffer putLong(int index, long value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putShort(short)
     */
    public IoBuffer putShort(short value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#putShort(int, short)
     */
    public IoBuffer putShort(int index, short value) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see Buffer#remaining()
     */
    public int remaining() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see Buffer#reset()
     */
    public IoBuffer reset() {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see Buffer#rewind()
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
    public IoBuffer wrap(byte[] array) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see ByteBuffer#wrap(byte[], int, int)
     */
    public IoBuffer wrap(byte[] array, int offset, int length) {
        // TODO code me !
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns a string representing the IoBuffer.
     * @return a String representation of the IoBuffer
     */
    public String toString(){
        return "IoBuffer[pos=" + position + " lim=" + limit + " mrk=" + mark + "]";
    }
    
    
    //------------------------------------------------------------------------------------------------
    // private inner data structure
    //------------------------------------------------------------------------------------------------
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
         * @param entry The added ByteBuffer
         */
        private BufferNode(ByteBuffer byteBuffer) {
            this.buffer = byteBuffer;
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
         * @param byteBuffer The added ByteBuffer
         */
        private void add(ByteBuffer byteBuffer) {
            assert(byteBuffer != null);
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
         * Get the first BufferNode in the list. The current pointer will move
         * forward, after having be reset to the beginning of the list
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
         * Get the next BufferNode from the list. If this is the first time this method
         * is called, it will return the same value than a getFirst().
         * @return The next BufferNode in the list, moving forward in the list at the same time
         */
        private BufferNode getNext() {
            if (current == null) {
                return null;
            }
            
            if (current == tail) {
                pastTail = true;
                
                return null;
            } else {
                current = current.next;
                
                return current;
            }
        }
        
        /**
         * Gets the current BufferNode from the list, if we aren't already past the tail.
         * @return The current BufferNode
         */
        private BufferNode getCurrent() {
            if (pastTail) {
                return null;
            }
            
            return current;
        }
    }
}
