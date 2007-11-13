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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.EnumSet;
import java.util.Set;


/**
 * A byte buffer used by MINA applications.
 * <p>
 * This is a replacement for {@link ByteBuffer}. Please refer to
 * {@link ByteBuffer} documentation for preliminary usage.  MINA does
 * not use NIO {@link ByteBuffer} directly for two reasons:
 * <ul>
 * <li>It doesn't provide useful getters and putters such as
 * <code>fill</code>, <code>get/putString</code>, and
 * <code>get/putAsciiInt()</code> enough.</li>
 * <li>It is difficult to write variable-length data due to its fixed
 * capacity</li>
 * </ul>
 * </p>
 *
 * <h2>Allocation</h2>
 * <p>
 * You can allocate a new heap buffer.
 * <pre>
 * IoBuffer buf = IoBuffer.allocate(1024, false);
 * </pre>
 * you can also allocate a new direct buffer:
 * <pre>
 * IoBuffer buf = IoBuffer.allocate(1024, true);
 * </pre>
 * or you can set the default buffer type.
 * <pre>
 * // Allocate heap buffer by default.
 * IoBuffer.setUseDirectBuffer(false);
 * // A new heap buffer is returned.
 * IoBuffer buf = IoBuffer.allocate(1024);
 * </pre>
 * </p>
 *
 * <h2>Wrapping existing NIO buffers and arrays</h2>
 * <p>
 * This class provides a few <tt>wrap(...)</tt> methods that wraps
 * any NIO buffers and byte arrays.
 *
 * <h2>AutoExpand</h2>
 * <p>
 * Writing variable-length data using NIO <tt>ByteBuffers</tt> is not really
 * easy, and it is because its size is fixed.  MINA {@link IoBuffer}
 * introduces <tt>autoExpand</tt> property.  If <tt>autoExpand</tt> property
 * is true, you never get {@link BufferOverflowException} or
 * {@link IndexOutOfBoundsException} (except when index is negative).
 * It automatically expands its capacity and limit value.  For example:
 * <pre>
 * String greeting = messageBundle.getMessage( "hello" );
 * IoBuffer buf = IoBuffer.allocate( 16 );
 * // Turn on autoExpand (it is off by default)
 * buf.setAutoExpand( true );
 * buf.putString( greeting, utf8encoder );
 * </pre>
 * NIO <tt>ByteBuffer</tt> is reallocated by MINA {@link IoBuffer} behind
 * the scene if the encoded data is larger than 16 bytes.  Its capacity will
 * increase by two times, and its limit will increase to the last position
 * the string is written.
 * </p>
 * 
 * <h2>AutoShrink</h2>
 * <p>
 * You might also want to decrease the capacity of the buffer when most
 * of the allocated memory area is not being used.  MINA {@link IoBuffer}
 * provides <tt>autoShrink</tt> property to take care of this issue.
 * If <tt>autoShrink</tt> is turned on, {@link IoBuffer} halves the capacity
 * of the buffer when {@link #compact()} is invoked and only 1/4 or less of
 * the current capacity is being used. NIO <tt>ByteBuffer</tt> is reallocated
 * by MINA {@link IoBuffer} behind the scene, and therefore {@link #buf()}
 * will return a different {@link ByteBuffer} instance once capacity is
 * changed.  Please also note {@link #compact()} will not decrease the
 * capacity if the new capacity is less than the initial capacity of the
 * buffer. 
 *
 * <h2>Derived Buffers</h2>
 * <p>
 * Derived buffers are the buffers which were created by
 * {@link #duplicate()}, {@link #slice()}, or {@link #asReadOnlyBuffer()}.
 * They are useful especially when you broadcast the same messages to
 * multiple {@link IoSession}s.  Please note that the buffer derived from and
 * its derived buffers are not both auto-expandable neither auto-shrinkable.
 * Trying to call {@link #setAutoExpand(boolean)} or {@link #setAutoShrink(boolean)}
 * with <tt>true</tt> parameter will raise an {@link IllegalStateException}.
 * </p>
 *
 * <h2>Changing Buffer Allocation Policy</h2>
 * <p>
 * MINA provides a {@link IoBufferAllocator} interface to let you override
 * the default buffer management behavior.  There's only one allocator
 * provided out-of-the-box:
 * <ul>
 * <li>{@link SimpleBufferAllocator}</li>
 * </ul>
 * You can implement your own allocator and use it by calling
 * {@link #setAllocator(IoBufferAllocator)}.
 * </p>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoBufferAllocator
 */
public abstract class IoBuffer implements Comparable<IoBuffer> {
    private static IoBufferAllocator allocator = new SimpleBufferAllocator();
    private static final IoBuffer EMPTY_DIRECT_BUFFER = allocator.allocate(0, true);
    private static final IoBuffer EMPTY_HEAP_BUFFER = allocator.allocate(0, false);
    private static boolean useDirectBuffer = false;

    /**
     * An immutable empty buffer.
     */
    public static final IoBuffer EMPTY_BUFFER = wrap(new byte[0]);

    /**
     * Returns the allocator used by existing and new buffers
     */
    public static IoBufferAllocator getAllocator() {
        return allocator;
    }

    /**
     * Sets the allocator used by existing and new buffers
     */
    public static void setAllocator(IoBufferAllocator newAllocator) {
        if (newAllocator == null) {
            throw new NullPointerException("allocator");
        }

        IoBufferAllocator oldAllocator = allocator;

        allocator = newAllocator;

        if (null != oldAllocator) {
            oldAllocator.dispose();
        }
    }

    /**
     * Returns <tt>true</tt> if and only if a direct buffer is allocated
     * by default when the type of the new buffer is not specified.  The
     * default value is <tt>false</tt>.
     */
    public static boolean isUseDirectBuffer() {
        return useDirectBuffer;
    }

    /**
     * Sets if a direct buffer should be allocated by default when the
     * type of the new buffer is not specified.  The default value is
     * <tt>false</tt>.
     */
    public static void setUseDirectBuffer(boolean useDirectBuffer) {
        IoBuffer.useDirectBuffer = useDirectBuffer;
    }

    /**
     * Returns the direct or heap buffer which is capable to store the
     * specified amount of bytes.
     *
     * @param capacity the capacity of the buffer
     *
     * @see #setUseDirectBuffer(boolean)
     */
    public static IoBuffer allocate(int capacity) {
        return allocate(capacity, useDirectBuffer);
    }

    /**
     * Returns the buffer which is capable of the specified size.
     *
     * @param capacity the capacity of the buffer
     * @param direct   <tt>true</tt> to get a direct buffer,
     *                 <tt>false</tt> to get a heap buffer.
     */
    public static IoBuffer allocate(int capacity, boolean direct) {
        if (capacity == 0) {
            return direct? EMPTY_DIRECT_BUFFER : EMPTY_HEAP_BUFFER;
        }
        
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity: " + capacity);
        }
        
        return allocator.allocate(capacity, direct);
    }

    /**
     * Wraps the specified NIO {@link ByteBuffer} into MINA buffer.
     */
    public static IoBuffer wrap(ByteBuffer nioBuffer) {
        return allocator.wrap(nioBuffer);
    }

    /**
     * Wraps the specified byte array into MINA heap buffer.
     */
    public static IoBuffer wrap(byte[] byteArray) {
        return wrap(ByteBuffer.wrap(byteArray));
    }

    /**
     * Wraps the specified byte array into MINA heap buffer.
     */
    public static IoBuffer wrap(byte[] byteArray, int offset, int length) {
        return wrap(ByteBuffer.wrap(byteArray, offset, length));
    }
    
    /**
     * Normalizes the specified capacity of the buffer to power of 2,
     * which is often helpful for optimal memory usage and performance. 
     * If it is greater than or equal to {@link Integer#MAX_VALUE}, it
     * returns {@link Integer#MAX_VALUE}.  If it is zero, it returns zero.
     */
    protected static int normalizeCapacity(int requestedCapacity) {
        switch (requestedCapacity) {
        case 0:
        case 1 <<  0: case 1 <<  1: case 1 <<  2: case 1 <<  3: case 1 <<  4:
        case 1 <<  5: case 1 <<  6: case 1 <<  7: case 1 <<  8: case 1 <<  9:
        case 1 << 10: case 1 << 11: case 1 << 12: case 1 << 13: case 1 << 14:
        case 1 << 15: case 1 << 16: case 1 << 17: case 1 << 18: case 1 << 19:
        case 1 << 21: case 1 << 22: case 1 << 23: case 1 << 24: case 1 << 25:
        case 1 << 26: case 1 << 27: case 1 << 28: case 1 << 29: case 1 << 30:
        case Integer.MAX_VALUE:
            return requestedCapacity;
        }
        
        int newCapacity = 1;
        while (newCapacity < requestedCapacity) {
            newCapacity <<= 1;
            if (newCapacity < 0) {
                return Integer.MAX_VALUE;
            }
        }
        return newCapacity;
    }


    /**
     * Creates a new instance.  This is an empty constructor.
     *
     */
    protected IoBuffer() {
    }
    
    /**
     * Declares this buffer and all its derived buffers are not used anymore
     * so that it can be reused by some {@link IoBufferAllocator} implementations.
     * It is not mandatory to call this method, but you might want to invoke this
     * method for maximum performance. 
     */
    public abstract void free();

    /**
     * Returns the underlying NIO buffer instance.
     */
    public abstract ByteBuffer buf();
    
    /**
     * @see ByteBuffer#isDirect()
     */
    public abstract boolean isDirect();
    
    /**
     * returns <tt>true</tt> if and only if this buffer is derived from other buffer
     * via {@link #duplicate()}, {@link #slice()} or {@link #asReadOnlyBuffer()}.
     */
    public abstract boolean isDerived();

    /**
     * @see ByteBuffer#isReadOnly()
     */
    public abstract boolean isReadOnly();

    /**
     * @see ByteBuffer#capacity()
     */
    public abstract int capacity();

    /**
     * Increases the capacity of this buffer.  If the new capacity is less than
     * or equal to the current capacity, this method returns silently.  If the
     * new capacity is greater than the current capacity, the buffer is
     * reallocated while retaining the position, limit, mark and the content
     * of the buffer.
     */
    public abstract IoBuffer capacity(int newCapacity);

    /**
     * Returns <tt>true</tt> if and only if <tt>autoExpand</tt> is turned on.
     */
    public abstract boolean isAutoExpand();

    /**
     * Turns on or off <tt>autoExpand</tt>.
     */
    public abstract IoBuffer setAutoExpand(boolean autoExpand);
    
    /**
     * Returns <tt>true</tt> if and only if <tt>autoShrink</tt> is turned on.
     */
    public abstract boolean isAutoShrink();

    /**
     * Turns on or off <tt>autoShrink</tt>.
     */
    public abstract IoBuffer setAutoShrink(boolean autoShrink);

    /**
     * Changes the capacity and limit of this buffer so this buffer get
     * the specified <tt>expectedRemaining</tt> room from the current position.
     * This method works even if you didn't set <tt>autoExpand</tt> to
     * <tt>true</tt>.
     */
    public IoBuffer expand(int expectedRemaining) {
        return expand(position(), expectedRemaining);
    }

    /**
     * Changes the capacity and limit of this buffer so this buffer get
     * the specified <tt>expectedRemaining</tt> room from the specified
     * <tt>position</tt>.
     * This method works even if you didn't set <tt>autoExpand</tt> to
     * <tt>true</tt>.
     */
    public abstract IoBuffer expand(int position, int expectedRemaining);

    /**
     * @see java.nio.Buffer#position()
     */
    public abstract int position();

    /**
     * @see java.nio.Buffer#position(int)
     */
    public abstract IoBuffer position(int newPosition);

    /**
     * @see java.nio.Buffer#limit()
     */
    public abstract int limit();

    /**
     * @see java.nio.Buffer#limit(int)
     */
    public abstract IoBuffer limit(int newLimit);

    /**
     * @see java.nio.Buffer#mark()
     */
    public abstract IoBuffer mark();

    /**
     * Returns the position of the current mark.  This method returns <tt>-1</tt> if no
     * mark is set.
     */
    public abstract int markValue();

    /**
     * @see java.nio.Buffer#reset()
     */
    public abstract IoBuffer reset();

    /**
     * @see java.nio.Buffer#clear()
     */
    public abstract IoBuffer clear();

    /**
     * Clears this buffer and fills its content with <tt>NUL</tt>.
     * The position is set to zero, the limit is set to the capacity,
     * and the mark is discarded.
     */
    public IoBuffer sweep() {
        clear();
        return fillAndReset(remaining());
    }

    /**
     * Clears this buffer and fills its content with <tt>value</tt>.
     * The position is set to zero, the limit is set to the capacity,
     * and the mark is discarded.
     */
    public IoBuffer sweep(byte value) {
        clear();
        return fillAndReset(value, remaining());
    }

    /**
     * @see java.nio.Buffer#flip()
     */
    public abstract IoBuffer flip();

    /**
     * @see java.nio.Buffer#rewind()
     */
    public abstract IoBuffer rewind();

    /**
     * @see java.nio.Buffer#remaining()
     */
    public int remaining() {
        return limit() - position();
    }

    /**
     * @see java.nio.Buffer#hasRemaining()
     */
    public boolean hasRemaining() {
        return limit() > position();
    }

    /**
     * @see ByteBuffer#duplicate()
     */
    public abstract IoBuffer duplicate();

    /**
     * @see ByteBuffer#slice()
     */
    public abstract IoBuffer slice();

    /**
     * @see ByteBuffer#asReadOnlyBuffer()
     */
    public abstract IoBuffer asReadOnlyBuffer();

    /**
     * @see ByteBuffer#hasArray()
     */
    public abstract boolean hasArray();

    /**
     * @see ByteBuffer#array()
     */
    public abstract byte[] array();

    /**
     * @see ByteBuffer#arrayOffset()
     */
    public abstract int arrayOffset();

    /**
     * @see ByteBuffer#get()
     */
    public abstract byte get();

    /**
     * Reads one unsigned byte as a short integer.
     */
    public short getUnsigned() {
        return (short) (get() & 0xff);
    }

    /**
     * @see ByteBuffer#put(byte)
     */
    public abstract IoBuffer put(byte b);

    /**
     * @see ByteBuffer#get(int)
     */
    public abstract byte get(int index);

    /**
     * Reads one byte as an unsigned short integer.
     */
    public short getUnsigned(int index) {
        return (short) (get(index) & 0xff);
    }

    /**
     * @see ByteBuffer#put(int, byte)
     */
    public abstract IoBuffer put(int index, byte b);

    /**
     * @see ByteBuffer#get(byte[], int, int)
     */
    public abstract IoBuffer get(byte[] dst, int offset, int length);

    /**
     * @see ByteBuffer#get(byte[])
     */
    public IoBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    public abstract IoBuffer put(ByteBuffer src);

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    public IoBuffer put(IoBuffer src) {
        return put(src.buf());
    }

    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    public abstract IoBuffer put(byte[] src, int offset, int length);

    /**
     * @see ByteBuffer#put(byte[])
     */
    public IoBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    /**
     * @see ByteBuffer#compact()
     */
    public abstract IoBuffer compact();

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (isDirect()) {
            buf.append("DirectBuffer");
        } else {
            buf.append("HeapBuffer");
        }
        buf.append("[pos=");
        buf.append(position());
        buf.append(" lim=");
        buf.append(limit());
        buf.append(" cap=");
        buf.append(capacity());
        buf.append(": ");
        buf.append(getHexDump(16));
        buf.append(']');
        return buf.toString();
    }

    @Override
    public int hashCode() {
        int h = 1;
        int p = position();
        for (int i = limit() - 1; i >= p; i--) {
            h = 31 * h + get(i);
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IoBuffer)) {
            return false;
        }

        IoBuffer that = (IoBuffer) o;
        if (this.remaining() != that.remaining()) {
            return false;
        }

        int p = this.position();
        for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--) {
            byte v1 = this.get(i);
            byte v2 = that.get(j);
            if (v1 != v2) {
                return false;
            }
        }
        return true;
    }

    public int compareTo(IoBuffer that) {
        int n = this.position() + Math.min(this.remaining(), that.remaining());
        for (int i = this.position(), j = that.position(); i < n; i++, j++) {
            byte v1 = this.get(i);
            byte v2 = that.get(j);
            if (v1 == v2) {
                continue;
            }
            if (v1 < v2) {
                return -1;
            }

            return +1;
        }
        return this.remaining() - that.remaining();
    }

    /**
     * @see ByteBuffer#order()
     */
    public abstract ByteOrder order();

    /**
     * @see ByteBuffer#order(ByteOrder)
     */
    public abstract IoBuffer order(ByteOrder bo);

    /**
     * @see ByteBuffer#getChar()
     */
    public abstract char getChar();

    /**
     * @see ByteBuffer#putChar(char)
     */
    public abstract IoBuffer putChar(char value);

    /**
     * @see ByteBuffer#getChar(int)
     */
    public abstract char getChar(int index);

    /**
     * @see ByteBuffer#putChar(int, char)
     */
    public abstract IoBuffer putChar(int index, char value);

    /**
     * @see ByteBuffer#asCharBuffer()
     */
    public abstract CharBuffer asCharBuffer();

    /**
     * @see ByteBuffer#getShort()
     */
    public abstract short getShort();

    /**
     * Reads two bytes unsigned integer.
     */
    public int getUnsignedShort() {
        return getShort() & 0xffff;
    }

    /**
     * @see ByteBuffer#putShort(short)
     */
    public abstract IoBuffer putShort(short value);

    /**
     * @see ByteBuffer#getShort()
     */
    public abstract short getShort(int index);

    /**
     * Reads two bytes unsigned integer.
     */
    public int getUnsignedShort(int index) {
        return getShort(index) & 0xffff;
    }

    /**
     * @see ByteBuffer#putShort(int, short)
     */
    public abstract IoBuffer putShort(int index, short value);

    /**
     * @see ByteBuffer#asShortBuffer()
     */
    public abstract ShortBuffer asShortBuffer();

    /**
     * @see ByteBuffer#getInt()
     */
    public abstract int getInt();

    /**
     * Reads four bytes unsigned integer.
     */
    public long getUnsignedInt() {
        return getInt() & 0xffffffffL;
    }

    /**
     * Relative <i>get</i> method for reading a medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order,
     * and then increments the position by three.</p>
     *
     * @return  The medium int value at the buffer's current position
     */
    public int getMediumInt() {
        byte b1 = get();
        byte b2 = get();
        byte b3 = get();
        if (ByteOrder.BIG_ENDIAN.equals(order())) {
            return getMediumInt(b1, b2, b3);
        } else {
            return getMediumInt(b3, b2, b1);
        }
    }

    /**
     * Relative <i>get</i> method for reading an unsigned medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order,
     * and then increments the position by three.</p>
     *
     * @return  The unsigned medium int value at the buffer's current position
     */
    public int getUnsignedMediumInt() {
        int b1 = getUnsigned();
        int b2 = getUnsigned();
        int b3 = getUnsigned();
        if (ByteOrder.BIG_ENDIAN.equals(order())) {
            return b1 << 16 | b2 << 8 | b3;
        } else {
            return b3 << 16 | b2 << 8 | b1;
        }
    }

    /**
     * Absolute <i>get</i> method for reading a medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order.</p>
     *
     * @param index  The index from which the medium int will be read
     * @return  The medium int value at the given index
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     */
    public int getMediumInt(int index) {
        byte b1 = get(index);
        byte b2 = get(index + 1);
        byte b3 = get(index + 2);
        if (ByteOrder.BIG_ENDIAN.equals(order())) {
            return getMediumInt(b1, b2, b3);
        } else {
            return getMediumInt(b3, b2, b1);
        }
    }

    /**
     * Absolute <i>get</i> method for reading an unsigned medium int value.
     *
     * <p> Reads the next three bytes at this buffer's current position,
     * composing them into an int value according to the current byte order.</p>
     *
     * @param index  The index from which the unsigned medium int will be read
     * @return  The unsigned medium int value at the given index
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit
     */
    public int getUnsignedMediumInt(int index) {
        int b1 = getUnsigned(index);
        int b2 = getUnsigned(index + 1);
        int b3 = getUnsigned(index + 2);
        if (ByteOrder.BIG_ENDIAN.equals(order())) {
            return b1 << 16 | b2 << 8 | b3;
        } else {
            return b3 << 16 | b2 << 8 | b1;
        }
    }

    private int getMediumInt(byte b1, byte b2, byte b3) {
        int ret = b1 << 16 & 0xff0000 | b2 << 8 & 0xff00 | b3 & 0xff;
        // Check to see if the medium int is negative (high bit in b1 set)
        if ((b1 & 0x80) == 0x80) {
            // Make the the whole int negative
            ret |= 0xff000000;
        }
        return ret;
    }

    /**
     * Relative <i>put</i> method for writing a medium int
     * value.
     *
     * <p> Writes three bytes containing the given int value, in the
     * current byte order, into this buffer at the current position, and then
     * increments the position by three.</p>
     *
     * @param  value
     *         The medium int value to be written
     *
     * @return  This buffer
     *
     * @throws  BufferOverflowException
     *          If there are fewer than three bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public IoBuffer putMediumInt(int value) {
        byte b1 = (byte) (value >> 16);
        byte b2 = (byte) (value >> 8);
        byte b3 = (byte) value;

        if (ByteOrder.BIG_ENDIAN.equals(order())) {
            put(b1).put(b2).put(b3);
        } else {
            put(b3).put(b2).put(b1);
        }

        return this;
    }

    /**
     * Absolute <i>put</i> method for writing a medium int
     * value.
     *
     * <p> Writes three bytes containing the given int value, in the
     * current byte order, into this buffer at the given index.</p>
     *
     * @param  index
     *         The index at which the bytes will be written
     *
     * @param  value
     *         The medium int value to be written
     *
     * @return  This buffer
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>index</tt> is negative
     *          or not smaller than the buffer's limit,
     *          minus three
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public IoBuffer putMediumInt(int index, int value) {
        byte b1 = (byte) (value >> 16);
        byte b2 = (byte) (value >> 8);
        byte b3 = (byte) value;

        if (ByteOrder.BIG_ENDIAN.equals(order())) {
            put(index, b1).put(index + 1, b2).put(index + 2, b3);
        } else {
            put(index, b3).put(index + 1, b2).put(index + 2, b1);
        }

        return this;
    }

    /**
     * @see ByteBuffer#putInt(int)
     */
    public abstract IoBuffer putInt(int value);

    /**
     * @see ByteBuffer#getInt(int)
     */
    public abstract int getInt(int index);

    /**
     * Reads four bytes unsigned integer.
     */
    public long getUnsignedInt(int index) {
        return getInt(index) & 0xffffffffL;
    }

    /**
     * @see ByteBuffer#putInt(int, int)
     */
    public abstract IoBuffer putInt(int index, int value);

    /**
     * @see ByteBuffer#asIntBuffer()
     */
    public abstract IntBuffer asIntBuffer();

    /**
     * @see ByteBuffer#getLong()
     */
    public abstract long getLong();

    /**
     * @see ByteBuffer#putLong(int, long)
     */
    public abstract IoBuffer putLong(long value);

    /**
     * @see ByteBuffer#getLong(int)
     */
    public abstract long getLong(int index);

    /**
     * @see ByteBuffer#putLong(int, long)
     */
    public abstract IoBuffer putLong(int index, long value);

    /**
     * @see ByteBuffer#asLongBuffer()
     */
    public abstract LongBuffer asLongBuffer();

    /**
     * @see ByteBuffer#getFloat()
     */
    public abstract float getFloat();

    /**
     * @see ByteBuffer#putFloat(float)
     */
    public abstract IoBuffer putFloat(float value);

    /**
     * @see ByteBuffer#getFloat(int)
     */
    public abstract float getFloat(int index);

    /**
     * @see ByteBuffer#putFloat(int, float)
     */
    public abstract IoBuffer putFloat(int index, float value);

    /**
     * @see ByteBuffer#asFloatBuffer()
     */
    public abstract FloatBuffer asFloatBuffer();

    /**
     * @see ByteBuffer#getDouble()
     */
    public abstract double getDouble();

    /**
     * @see ByteBuffer#putDouble(double)
     */
    public abstract IoBuffer putDouble(double value);

    /**
     * @see ByteBuffer#getDouble(int)
     */
    public abstract double getDouble(int index);

    /**
     * @see ByteBuffer#putDouble(int, double)
     */
    public abstract IoBuffer putDouble(int index, double value);

    /**
     * @see ByteBuffer#asDoubleBuffer()
     */
    public abstract DoubleBuffer asDoubleBuffer();

    /**
     * Returns an {@link InputStream} that reads the data from this buffer.
     * {@link InputStream#read()} returns <tt>-1</tt> if the buffer position
     * reaches to the limit.
     */
    public InputStream asInputStream() {
        return new InputStream() {
            @Override
            public int available() {
                return IoBuffer.this.remaining();
            }

            @Override
            public synchronized void mark(int readlimit) {
                IoBuffer.this.mark();
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int read() {
                if (IoBuffer.this.hasRemaining()) {
                    return IoBuffer.this.get() & 0xff;
                } else {
                    return -1;
                }
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int remaining = IoBuffer.this.remaining();
                if (remaining > 0) {
                    int readBytes = Math.min(remaining, len);
                    IoBuffer.this.get(b, off, readBytes);
                    return readBytes;
                } else {
                    return -1;
                }
            }

            @Override
            public synchronized void reset() {
                IoBuffer.this.reset();
            }

            @Override
            public long skip(long n) {
                int bytes;
                if (n > Integer.MAX_VALUE) {
                    bytes = IoBuffer.this.remaining();
                } else {
                    bytes = Math.min(IoBuffer.this.remaining(), (int) n);
                }
                IoBuffer.this.skip(bytes);
                return bytes;
            }
        };
    }

    /**
     * Returns an {@link OutputStream} that appends the data into this buffer.
     * Please note that the {@link OutputStream#write(int)} will throw a
     * {@link BufferOverflowException} instead of an {@link IOException}
     * in case of buffer overflow.  Please set <tt>autoExpand</tt> property by
     * calling {@link #setAutoExpand(boolean)} to prevent the unexpected runtime
     * exception.
     */
    public OutputStream asOutputStream() {
        return new OutputStream() {
            @Override
            public void write(byte[] b, int off, int len) {
                IoBuffer.this.put(b, off, len);
            }

            @Override
            public void write(int b) {
                IoBuffer.this.put((byte) b);
            }
        };
    }

    /**
     * Returns hexdump of this buffer.  The data and pointer are
     * not changed as a result of this method call.
     *
     * @return
     *  hexidecimal representation of this buffer
     */
    public String getHexDump() {
        return this.getHexDump(Integer.MAX_VALUE);
    }

    /**
     * Return hexdump of this buffer with limited length.
     *
     * @param lengthLimit The maximum number of bytes to dump from
     *                    the current buffer position.
     * @return
     *  hexidecimal representation of this buffer
     */
    public String getHexDump(int lengthLimit) {
        return IoBufferHexDumper.getHexdump(this, lengthLimit);
    }

    ////////////////////////////////
    // String getters and putters //
    ////////////////////////////////

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.  This method reads
     * until the limit of this buffer if no <tt>NUL</tt> is found.
     */
    public String getString(CharsetDecoder decoder)
            throws CharacterCodingException {
        if (!hasRemaining()) {
            return "";
        }

        boolean utf16 = decoder.charset().name().startsWith("UTF-16");

        int oldPos = position();
        int oldLimit = limit();
        int end = -1;
        int newPos;

        if (!utf16) {
            end = indexOf((byte) 0x00);
            if (end < 0) {
                newPos = end = oldLimit;
            } else {
                newPos = end + 1;
            }
        } else {
            int i = oldPos;
            for (;;) {
                boolean wasZero = get(i) == 0;
                i++;

                if (i >= oldLimit) {
                    break;
                }

                if (get(i) != 0) {
                    i++;
                    if (i >= oldLimit) {
                        break;
                    } else {
                        continue;
                    }
                }

                if (wasZero) {
                    end = i - 1;
                    break;
                }
            }

            if (end < 0) {
                newPos = end = oldPos + (oldLimit - oldPos & 0xFFFFFFFE);
            } else {
                if (end + 2 <= oldLimit) {
                    newPos = end + 2;
                } else {
                    newPos = end;
                }
            }
        }

        if (oldPos == end) {
            position(newPos);
            return "";
        }

        limit(end);
        decoder.reset();

        int expectedLength = (int) (remaining() * decoder.averageCharsPerByte()) + 1;
        CharBuffer out = CharBuffer.allocate(expectedLength);
        for (;;) {
            CoderResult cr;
            if (hasRemaining()) {
                cr = decoder.decode(buf(), out, true);
            } else {
                cr = decoder.flush(out);
            }

            if (cr.isUnderflow()) {
                break;
            }

            if (cr.isOverflow()) {
                CharBuffer o = CharBuffer.allocate(out.capacity()
                        + expectedLength);
                out.flip();
                o.put(out);
                out = o;
                continue;
            }

            if (cr.isError()) {
                // Revert the buffer back to the previous state.
                limit(oldLimit);
                position(oldPos);
                cr.throwException();
            }
        }

        limit(oldLimit);
        position(newPos);
        return out.flip().toString();
    }

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     *
     * @param fieldSize the maximum number of bytes to read
     */
    public String getString(int fieldSize, CharsetDecoder decoder)
            throws CharacterCodingException {
        checkFieldSize(fieldSize);

        if (fieldSize == 0) {
            return "";
        }

        if (!hasRemaining()) {
            return "";
        }

        boolean utf16 = decoder.charset().name().startsWith("UTF-16");

        if (utf16 && (fieldSize & 1) != 0) {
            throw new IllegalArgumentException("fieldSize is not even.");
        }

        int oldPos = position();
        int oldLimit = limit();
        int end = oldPos + fieldSize;

        if (oldLimit < end) {
            throw new BufferUnderflowException();
        }

        int i;

        if (!utf16) {
            for (i = oldPos; i < end; i++) {
                if (get(i) == 0) {
                    break;
                }
            }

            if (i == end) {
                limit(end);
            } else {
                limit(i);
            }
        } else {
            for (i = oldPos; i < end; i += 2) {
                if (get(i) == 0 && get(i + 1) == 0) {
                    break;
                }
            }

            if (i == end) {
                limit(end);
            } else {
                limit(i);
            }
        }

        if (!hasRemaining()) {
            limit(oldLimit);
            position(end);
            return "";
        }
        decoder.reset();

        int expectedLength = (int) (remaining() * decoder.averageCharsPerByte()) + 1;
        CharBuffer out = CharBuffer.allocate(expectedLength);
        for (;;) {
            CoderResult cr;
            if (hasRemaining()) {
                cr = decoder.decode(buf(), out, true);
            } else {
                cr = decoder.flush(out);
            }

            if (cr.isUnderflow()) {
                break;
            }

            if (cr.isOverflow()) {
                CharBuffer o = CharBuffer.allocate(out.capacity()
                        + expectedLength);
                out.flip();
                o.put(out);
                out = o;
                continue;
            }

            if (cr.isError()) {
                // Revert the buffer back to the previous state.
                limit(oldLimit);
                position(oldPos);
                cr.throwException();
            }
        }

        limit(oldLimit);
        position(end);
        return out.flip().toString();
    }

    /**
     * Writes the content of <code>in</code> into this buffer using the
     * specified <code>encoder</code>.  This method doesn't terminate
     * string with <tt>NUL</tt>.  You have to do it by yourself.
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public IoBuffer putString(CharSequence val, CharsetEncoder encoder)
            throws CharacterCodingException {
        if (val.length() == 0) {
            return this;
        }

        CharBuffer in = CharBuffer.wrap(val);
        encoder.reset();

        int expandedState = 0;

        for (;;) {
            CoderResult cr;
            if (in.hasRemaining()) {
                cr = encoder.encode(in, buf(), true);
            } else {
                cr = encoder.flush(buf());
            }

            if (cr.isUnderflow()) {
                break;
            }
            if (cr.isOverflow()) {
                if (isAutoExpand()) {
                    switch (expandedState) {
                    case 0:
                        autoExpand((int) Math.ceil(in.remaining()
                                * encoder.averageBytesPerChar()));
                        expandedState++;
                        break;
                    case 1:
                        autoExpand((int) Math.ceil(in.remaining()
                                * encoder.maxBytesPerChar()));
                        expandedState++;
                        break;
                    default:
                        throw new RuntimeException("Expanded by "
                                + (int) Math.ceil(in.remaining()
                                        * encoder.maxBytesPerChar())
                                + " but that wasn't enough for '" + val + "'");
                    }
                    continue;
                }
            } else {
                expandedState = 0;
            }
            cr.throwException();
        }
        return this;
    }

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * <code>NUL</code>-terminated string using the specified
     * <code>encoder</code>.
     * <p>
     * If the charset name of the encoder is UTF-16, you cannot specify
     * odd <code>fieldSize</code>, and this method will append two
     * <code>NUL</code>s as a terminator.
     * <p>
     * Please note that this method doesn't terminate with <code>NUL</code>
     * if the input string is longer than <tt>fieldSize</tt>.
     *
     * @param fieldSize the maximum number of bytes to write
     */
    public IoBuffer putString(CharSequence val, int fieldSize,
            CharsetEncoder encoder) throws CharacterCodingException {
        checkFieldSize(fieldSize);

        if (fieldSize == 0) {
            return this;
        }

        autoExpand(fieldSize);

        boolean utf16 = encoder.charset().name().startsWith("UTF-16");

        if (utf16 && (fieldSize & 1) != 0) {
            throw new IllegalArgumentException("fieldSize is not even.");
        }

        int oldLimit = limit();
        int end = position() + fieldSize;

        if (oldLimit < end) {
            throw new BufferOverflowException();
        }

        if (val.length() == 0) {
            if (!utf16) {
                put((byte) 0x00);
            } else {
                put((byte) 0x00);
                put((byte) 0x00);
            }
            position(end);
            return this;
        }

        CharBuffer in = CharBuffer.wrap(val);
        limit(end);
        encoder.reset();

        for (;;) {
            CoderResult cr;
            if (in.hasRemaining()) {
                cr = encoder.encode(in, buf(), true);
            } else {
                cr = encoder.flush(buf());
            }

            if (cr.isUnderflow() || cr.isOverflow()) {
                break;
            }
            cr.throwException();
        }

        limit(oldLimit);

        if (position() < end) {
            if (!utf16) {
                put((byte) 0x00);
            } else {
                put((byte) 0x00);
                put((byte) 0x00);
            }
        }

        position(end);
        return this;
    }

    /**
     * Reads a string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>decoder</code> and returns it.
     * This method is a shortcut for <tt>getPrefixedString(2, decoder)</tt>.
     */
    public String getPrefixedString(CharsetDecoder decoder)
            throws CharacterCodingException {
        return getPrefixedString(2, decoder);
    }

    /**
     * Reads a string which has a length field before the actual
     * encoded string, using the specified <code>decoder</code> and returns it.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     */
    public String getPrefixedString(int prefixLength, CharsetDecoder decoder)
            throws CharacterCodingException {
        if (!prefixedDataAvailable(prefixLength)) {
            throw new BufferUnderflowException();
        }

        int fieldSize = 0;

        switch (prefixLength) {
        case 1:
            fieldSize = getUnsigned();
            break;
        case 2:
            fieldSize = getUnsignedShort();
            break;
        case 4:
            fieldSize = getInt();
            break;
        }

        if (fieldSize == 0) {
            return "";
        }

        boolean utf16 = decoder.charset().name().startsWith("UTF-16");

        if (utf16 && (fieldSize & 1) != 0) {
            throw new BufferDataException(
                    "fieldSize is not even for a UTF-16 string.");
        }

        int oldLimit = limit();
        int end = position() + fieldSize;

        if (oldLimit < end) {
            throw new BufferUnderflowException();
        }

        limit(end);
        decoder.reset();

        int expectedLength = (int) (remaining() * decoder.averageCharsPerByte()) + 1;
        CharBuffer out = CharBuffer.allocate(expectedLength);
        for (;;) {
            CoderResult cr;
            if (hasRemaining()) {
                cr = decoder.decode(buf(), out, true);
            } else {
                cr = decoder.flush(out);
            }

            if (cr.isUnderflow()) {
                break;
            }

            if (cr.isOverflow()) {
                CharBuffer o = CharBuffer.allocate(out.capacity()
                        + expectedLength);
                out.flip();
                o.put(out);
                out = o;
                continue;
            }

            cr.throwException();
        }

        limit(oldLimit);
        position(end);
        return out.flip().toString();
    }

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     * This method is a shortcut for <tt>putPrefixedString(in, 2, 0, encoder)</tt>.
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public IoBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder)
            throws CharacterCodingException {
        return putPrefixedString(in, 2, 0, encoder);
    }

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     * This method is a shortcut for <tt>putPrefixedString(in, prefixLength, 0, encoder)</tt>.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
            CharsetEncoder encoder) throws CharacterCodingException {
        return putPrefixedString(in, prefixLength, 0, encoder);
    }

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     * This method is a shortcut for <tt>putPrefixedString(in, prefixLength, padding, ( byte ) 0, encoder)</tt>.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     * @param padding      the number of padded <tt>NUL</tt>s (1 (or 0), 2, or 4)
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
            int padding, CharsetEncoder encoder)
            throws CharacterCodingException {
        return putPrefixedString(in, prefixLength, padding, (byte) 0, encoder);
    }

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * string which has a 16-bit length field before the actual
     * encoded string, using the specified <code>encoder</code>.
     *
     * @param prefixLength the length of the length field (1, 2, or 4)
     * @param padding      the number of padded bytes (1 (or 0), 2, or 4)
     * @param padValue     the value of padded bytes
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public IoBuffer putPrefixedString(CharSequence val, int prefixLength,
            int padding, byte padValue, CharsetEncoder encoder)
            throws CharacterCodingException {
        int maxLength;
        switch (prefixLength) {
        case 1:
            maxLength = 255;
            break;
        case 2:
            maxLength = 65535;
            break;
        case 4:
            maxLength = Integer.MAX_VALUE;
            break;
        default:
            throw new IllegalArgumentException("prefixLength: " + prefixLength);
        }

        if (val.length() > maxLength) {
            throw new IllegalArgumentException(
                    "The specified string is too long.");
        }
        if (val.length() == 0) {
            switch (prefixLength) {
            case 1:
                put((byte) 0);
                break;
            case 2:
                putShort((short) 0);
                break;
            case 4:
                putInt(0);
                break;
            }
            return this;
        }

        int padMask;
        switch (padding) {
        case 0:
        case 1:
            padMask = 0;
            break;
        case 2:
            padMask = 1;
            break;
        case 4:
            padMask = 3;
            break;
        default:
            throw new IllegalArgumentException("padding: " + padding);
        }

        CharBuffer in = CharBuffer.wrap(val);
        int expectedLength = (int) (in.remaining() * encoder
                .averageBytesPerChar()) + 1;

        skip(prefixLength); // make a room for the length field
        int oldPos = position();
        encoder.reset();

        for (;;) {
            CoderResult cr;
            if (in.hasRemaining()) {
                cr = encoder.encode(in, buf(), true);
            } else {
                cr = encoder.flush(buf());
            }

            if (position() - oldPos > maxLength) {
                throw new IllegalArgumentException(
                        "The specified string is too long.");
            }

            if (cr.isUnderflow()) {
                break;
            }
            if (cr.isOverflow() && isAutoExpand()) {
                autoExpand(expectedLength);
                continue;
            }
            cr.throwException();
        }

        // Write the length field
        fill(padValue, padding - (position() - oldPos & padMask));
        int length = position() - oldPos;
        switch (prefixLength) {
        case 1:
            put(oldPos - 1, (byte) length);
            break;
        case 2:
            putShort(oldPos - 2, (short) length);
            break;
        case 4:
            putInt(oldPos - 4, length);
            break;
        }
        return this;
    }

    /**
     * Reads a Java object from the buffer using the context {@link ClassLoader}
     * of the current thread.
     */
    public Object getObject() throws ClassNotFoundException {
        return getObject(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Reads a Java object from the buffer using the specified <tt>classLoader</tt>.
     */
    public Object getObject(final ClassLoader classLoader)
            throws ClassNotFoundException {
        if (!prefixedDataAvailable(4)) {
            throw new BufferUnderflowException();
        }

        int length = getInt();
        if (length <= 4) {
            throw new BufferDataException(
                    "Object length should be greater than 4: " + length);
        }

        int oldLimit = limit();
        limit(position() + length);
        try {
            ObjectInputStream in = new ObjectInputStream(asInputStream()) {
                @Override
                protected ObjectStreamClass readClassDescriptor()
                        throws IOException, ClassNotFoundException {
                    String className = readUTF();
                    Class<?> clazz = Class
                            .forName(className, true, classLoader);
                    return ObjectStreamClass.lookup(clazz);
                }
            };
            return in.readObject();
        } catch (IOException e) {
            throw new BufferDataException(e);
        } finally {
            limit(oldLimit);
        }
    }

    /**
     * Writes the specified Java object to the buffer.
     */
    public IoBuffer putObject(Object o) {
        int oldPos = position();
        skip(4); // Make a room for the length field.
        try {
            ObjectOutputStream out = new ObjectOutputStream(asOutputStream()) {
                @Override
                protected void writeClassDescriptor(ObjectStreamClass desc)
                        throws IOException {
                    writeUTF(desc.getName());
                }
            };
            out.writeObject(o);
            out.flush();
        } catch (IOException e) {
            throw new BufferDataException(e);
        }

        // Fill the length field
        int newPos = position();
        position(oldPos);
        putInt(newPos - oldPos - 4);
        position(newPos);
        return this;
    }

    /**
     * Returns <tt>true</tt> if this buffer contains a data which has a data
     * length as a prefix and the buffer has remaining data as enough as
     * specified in the data length field.  This method is identical with
     * <tt>prefixedDataAvailable( prefixLength, Integer.MAX_VALUE )</tt>.
     * Please not that using this method can allow DoS (Denial of Service)
     * attack in case the remote peer sends too big data length value.
     * It is recommended to use {@link #prefixedDataAvailable(int, int)}
     * instead.
     *
     * @param prefixLength the length of the prefix field (1, 2, or 4)
     *
     * @throws IllegalArgumentException if prefixLength is wrong
     * @throws BufferDataException      if data length is negative
     */
    public boolean prefixedDataAvailable(int prefixLength) {
        return prefixedDataAvailable(prefixLength, Integer.MAX_VALUE);
    }

    /**
     * Returns <tt>true</tt> if this buffer contains a data which has a data
     * length as a prefix and the buffer has remaining data as enough as
     * specified in the data length field.
     *
     * @param prefixLength  the length of the prefix field (1, 2, or 4)
     * @param maxDataLength the allowed maximum of the read data length
     *
     * @throws IllegalArgumentException if prefixLength is wrong
     * @throws BufferDataException      if data length is negative or greater then <tt>maxDataLength</tt>
     */
    public boolean prefixedDataAvailable(int prefixLength, int maxDataLength) {
        if (remaining() < prefixLength) {
            return false;
        }

        int dataLength;
        switch (prefixLength) {
        case 1:
            dataLength = getUnsigned(position());
            break;
        case 2:
            dataLength = getUnsignedShort(position());
            break;
        case 4:
            dataLength = getInt(position());
            break;
        default:
            throw new IllegalArgumentException("prefixLength: " + prefixLength);
        }

        if (dataLength < 0 || dataLength > maxDataLength) {
            throw new BufferDataException("dataLength: " + dataLength);
        }

        return remaining() - prefixLength >= dataLength;
    }

    /////////////////////
    // IndexOf methods //
    /////////////////////

    /**
     * Returns the first occurence position of the specified byte from the current position to
     * the current limit.
     *
     * @return <tt>-1</tt> if the specified byte is not found
     */
    public int indexOf(byte b) {
        if (hasArray()) {
            int arrayOffset = arrayOffset();
            int beginPos = arrayOffset + position();
            int limit = arrayOffset + limit();
            byte[] array = array();

            for (int i = beginPos; i < limit; i++) {
                if (array[i] == b) {
                    return i - arrayOffset;
                }
            }
        } else {
            int beginPos = position();
            int limit = limit();

            for (int i = beginPos; i < limit; i++) {
                if (get(i) == b) {
                    return i;
                }
            }
        }

        return -1;
    }

    //////////////////////////
    // Skip or fill methods //
    //////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    public IoBuffer skip(int size) {
        autoExpand(size);
        return position(position() + size);
    }

    /**
     * Fills this buffer with the specified value.
     * This method moves buffer position forward.
     */
    public IoBuffer fill(byte value, int size) {
        autoExpand(size);
        int q = size >>> 3;
        int r = size & 7;

        if (q > 0) {
            int intValue = value | value << 8 | value << 16 | value << 24;
            long longValue = intValue;
            longValue <<= 32;
            longValue |= intValue;

            for (int i = q; i > 0; i--) {
                putLong(longValue);
            }
        }

        q = r >>> 2;
        r = r & 3;

        if (q > 0) {
            int intValue = value | value << 8 | value << 16 | value << 24;
            putInt(intValue);
        }

        q = r >> 1;
        r = r & 1;

        if (q > 0) {
            short shortValue = (short) (value | value << 8);
            putShort(shortValue);
        }

        if (r > 0) {
            put(value);
        }

        return this;
    }

    /**
     * Fills this buffer with the specified value.
     * This method does not change buffer position.
     */
    public IoBuffer fillAndReset(byte value, int size) {
        autoExpand(size);
        int pos = position();
        try {
            fill(value, size);
        } finally {
            position(pos);
        }
        return this;
    }

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method moves buffer position forward.
     */
    public IoBuffer fill(int size) {
        autoExpand(size);
        int q = size >>> 3;
        int r = size & 7;

        for (int i = q; i > 0; i--) {
            putLong(0L);
        }

        q = r >>> 2;
        r = r & 3;

        if (q > 0) {
            putInt(0);
        }

        q = r >> 1;
        r = r & 1;

        if (q > 0) {
            putShort((short) 0);
        }

        if (r > 0) {
            put((byte) 0);
        }

        return this;
    }

    /**
     * Fills this buffer with <code>NUL (0x00)</code>.
     * This method does not change buffer position.
     */
    public IoBuffer fillAndReset(int size) {
        autoExpand(size);
        int pos = position();
        try {
            fill(size);
        } finally {
            position(pos);
        }

        return this;
    }

    //////////////////////////
    // Enum methods         //
    //////////////////////////

    /**
     * Reads a byte from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param enumClass  The enum's class object
     */
    public <E extends Enum<E>> E getEnum(Class<E> enumClass) {
        return toEnum(enumClass, get());
    }

    /**
     * Reads a byte from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param index  the index from which the byte will be read
     * @param enumClass  The enum's class object
     */
    public <E extends Enum<E>> E getEnum(int index, Class<E> enumClass) {
        return toEnum(enumClass, get(index));
    }

    /**
     * Reads a short from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param enumClass  The enum's class object
     */
    public <E extends Enum<E>> E getEnumShort(Class<E> enumClass) {
        return toEnum(enumClass, getShort());
    }

    /**
     * Reads a short from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param index  the index from which the bytes will be read
     * @param enumClass  The enum's class object
     */
    public <E extends Enum<E>> E getEnumShort(int index, Class<E> enumClass) {
        return toEnum(enumClass, getShort(index));
    }

    /**
     * Reads an int from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param enumClass  The enum's class object
     */
    public <E extends Enum<E>> E getEnumInt(Class<E> enumClass) {
        return toEnum(enumClass, getInt());
    }

    /**
     * Reads an int from the buffer and returns the correlating enum constant defined
     * by the specified enum type.
     *
     * @param <E> The enum type to return
     * @param index  the index from which the bytes will be read
     * @param enumClass  The enum's class object
     */
    public <E extends Enum<E>> E getEnumInt(int index, Class<E> enumClass) {
        return toEnum(enumClass, getInt(index));
    }

    /**
     * Writes an enum's ordinal value to the buffer as a byte.
     *
     * @param e  The enum to write to the buffer
     */
    public IoBuffer putEnum(Enum<?> e) {
        if (e.ordinal() > Byte.MAX_VALUE) {
            throw new IllegalArgumentException(enumConversionErrorMessage(e,
                    "byte"));
        }
        return put((byte) e.ordinal());
    }

    /**
     * Writes an enum's ordinal value to the buffer as a byte.
     *
     * @param index The index at which the byte will be written
     * @param e  The enum to write to the buffer
     */
    public IoBuffer putEnum(int index, Enum<?> e) {
        if (e.ordinal() > Byte.MAX_VALUE) {
            throw new IllegalArgumentException(enumConversionErrorMessage(e,
                    "byte"));
        }
        return put(index, (byte) e.ordinal());
    }

    /**
     * Writes an enum's ordinal value to the buffer as a short.
     *
     * @param e  The enum to write to the buffer
     */
    public IoBuffer putEnumShort(Enum<?> e) {
        if (e.ordinal() > Short.MAX_VALUE) {
            throw new IllegalArgumentException(enumConversionErrorMessage(e,
                    "short"));
        }
        return putShort((short) e.ordinal());
    }

    /**
     * Writes an enum's ordinal value to the buffer as a short.
     *
     * @param index The index at which the bytes will be written
     * @param e  The enum to write to the buffer
     */
    public IoBuffer putEnumShort(int index, Enum<?> e) {
        if (e.ordinal() > Short.MAX_VALUE) {
            throw new IllegalArgumentException(enumConversionErrorMessage(e,
                    "short"));
        }
        return putShort(index, (short) e.ordinal());
    }

    /**
     * Writes an enum's ordinal value to the buffer as an integer.
     *
     * @param e  The enum to write to the buffer
     */
    public IoBuffer putEnumInt(Enum<?> e) {
        return putInt(e.ordinal());
    }

    /**
     * Writes an enum's ordinal value to the buffer as an integer.
     *
     * @param index The index at which the bytes will be written
     * @param e  The enum to write to the buffer
     */
    public IoBuffer putEnumInt(int index, Enum<?> e) {
        return putInt(index, e.ordinal());
    }

    private <E> E toEnum(Class<E> enumClass, int i) {
        E[] enumConstants = enumClass.getEnumConstants();
        if (i > enumConstants.length) {
            throw new IndexOutOfBoundsException(String.format(
                    "%d is too large of an ordinal to convert to the enum %s",
                    i, enumClass.getName()));
        }
        return enumConstants[i];
    }

    private String enumConversionErrorMessage(Enum<?> e, String type) {
        return String.format("%s.%s has an ordinal value too large for a %s", e
                .getClass().getName(), e.name(), type);
    }

    //////////////////////////
    // EnumSet methods      //
    //////////////////////////

    private static final long BYTE_MASK = 0xFFL;

    private static final long SHORT_MASK = 0xFFFFL;

    private static final long INT_MASK = 0xFFFFFFFFL;

    /**
     * Reads a byte sized bit vector and converts it to an {@link EnumSet}.
     *
     * <p>Each bit is mapped to a value in the specified enum.  The least significant
     * bit maps to the first entry in the specified enum and each subsequent bit maps
     * to each subsequent bit as mapped to the subsequent enum value.</p>
     *
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSet(Class<E> enumClass) {
        return toEnumSet(enumClass, get() & BYTE_MASK);
    }

    /**
     * Reads a byte sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the byte will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSet(int index,
            Class<E> enumClass) {
        return toEnumSet(enumClass, get(index) & BYTE_MASK);
    }

    /**
     * Reads a short sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSetShort(Class<E> enumClass) {
        return toEnumSet(enumClass, getShort() & SHORT_MASK);
    }

    /**
     * Reads a short sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the bytes will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSetShort(int index,
            Class<E> enumClass) {
        return toEnumSet(enumClass, getShort(index) & SHORT_MASK);
    }

    /**
     * Reads an int sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSetInt(Class<E> enumClass) {
        return toEnumSet(enumClass, getInt() & INT_MASK);
    }

    /**
     * Reads an int sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the bytes will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSetInt(int index,
            Class<E> enumClass) {
        return toEnumSet(enumClass, getInt(index) & INT_MASK);
    }

    /**
     * Reads a long sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSetLong(Class<E> enumClass) {
        return toEnumSet(enumClass, getLong());
    }

    /**
     * Reads a long sized bit vector and converts it to an {@link EnumSet}.
     *
     * @see #getEnumSet(Class)
     * @param <E>  the enum type
     * @param index  the index from which the bytes will be read
     * @param enumClass  the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public <E extends Enum<E>> EnumSet<E> getEnumSetLong(int index,
            Class<E> enumClass) {
        return toEnumSet(enumClass, getLong(index));
    }

    /**
     * Utility method for converting a bit vector to an EnumSet.
     */
    private <E extends Enum<E>> EnumSet<E> toEnumSet(Class<E> clazz, long vector) {
        EnumSet<E> set = EnumSet.noneOf(clazz);
        long mask = 1;
        for (E e : clazz.getEnumConstants()) {
            if ((mask & vector) == mask) {
                set.add(e);
            }
            mask <<= 1;
        }
        return set;
    }

    /**
     * Writes the specified {@link Set} to the buffer as a byte sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSet(Set<E> set) {
        long vector = toLong(set);
        if ((vector & ~BYTE_MASK) != 0) {
            throw new IllegalArgumentException(
                    "The enum set is too large to fit in a byte: " + set);
        }
        return put((byte) vector);
    }

    /**
     * Writes the specified {@link Set} to the buffer as a byte sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the byte will be written
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSet(int index, Set<E> set) {
        long vector = toLong(set);
        if ((vector & ~BYTE_MASK) != 0) {
            throw new IllegalArgumentException(
                    "The enum set is too large to fit in a byte: " + set);
        }
        return put(index, (byte) vector);
    }

    /**
     * Writes the specified {@link Set} to the buffer as a short sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSetShort(Set<E> set) {
        long vector = toLong(set);
        if ((vector & ~SHORT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "The enum set is too large to fit in a short: " + set);
        }
        return putShort((short) vector);
    }

    /**
     * Writes the specified {@link Set} to the buffer as a short sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the bytes will be written
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSetShort(int index, Set<E> set) {
        long vector = toLong(set);
        if ((vector & ~SHORT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "The enum set is too large to fit in a short: " + set);
        }
        return putShort(index, (short) vector);
    }

    /**
     * Writes the specified {@link Set} to the buffer as an int sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSetInt(Set<E> set) {
        long vector = toLong(set);
        if ((vector & ~INT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "The enum set is too large to fit in an int: " + set);
        }
        return putInt((int) vector);
    }

    /**
     * Writes the specified {@link Set} to the buffer as an int sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the bytes will be written
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSetInt(int index, Set<E> set) {
        long vector = toLong(set);
        if ((vector & ~INT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "The enum set is too large to fit in an int: " + set);
        }
        return putInt(index, (int) vector);
    }

    /**
     * Writes the specified {@link Set} to the buffer as a long sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSetLong(Set<E> set) {
        return putLong(toLong(set));
    }

    /**
     * Writes the specified {@link Set} to the buffer as a long sized bit vector.
     *
     * @param <E> the enum type of the Set
     * @param index  the index at which the bytes will be written
     * @param set  the enum set to write to the buffer
     */
    public <E extends Enum<E>> IoBuffer putEnumSetLong(int index, Set<E> set) {
        return putLong(index, toLong(set));
    }

    /**
     * Utility method for converting an Set to a bit vector.
     */
    private <E extends Enum<E>> long toLong(Set<E> set) {
        long vector = 0;
        for (E e : set) {
            if (e.ordinal() >= Long.SIZE) {
                throw new IllegalArgumentException(
                        "The enum set is too large to fit in a bit vector: "
                                + set);
            }
            vector |= 1L << e.ordinal();
        }
        return vector;
    }

    /**
     * This method forwards the call to {@link #expand(int)} only when
     * <tt>autoExpand</tt> property is <tt>true</tt>.
     */
    protected IoBuffer autoExpand(int expectedRemaining) {
        if (isAutoExpand()) {
            expand(expectedRemaining);
        }
        return this;
    }

    /**
     * This method forwards the call to {@link #expand(int)} only when
     * <tt>autoExpand</tt> property is <tt>true</tt>.
     */
    protected IoBuffer autoExpand(int pos, int expectedRemaining) {
        if (isAutoExpand()) {
            expand(pos, expectedRemaining);
        }
        return this;
    }

    private static void checkFieldSize(int fieldSize) {
        if (fieldSize < 0) {
            throw new IllegalArgumentException("fieldSize cannot be negative: "
                    + fieldSize);
        }
    }
}
