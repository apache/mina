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
package org.apache.mina.core.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
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
import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

/**
 * A byte buffer used by MINA applications.
 * <p>
 * This is a replacement for {@link ByteBuffer}. Please refer to
 * {@link ByteBuffer} documentation for preliminary usage. MINA does not use NIO
 * {@link ByteBuffer} directly for two reasons:
 * <ul>
 * <li>It doesn't provide useful getters and putters such as <code>fill</code>,
 * <code>get/putString</code>, and <code>get/putAsciiInt()</code> enough.</li>
 * <li>It is difficult to write variable-length data due to its fixed capacity</li>
 * </ul>
 * </p>
 * 
 * <h2>Allocation</h2>
 * <p>
 * You can allocate a new heap buffer.
 * 
 * <pre>
 * IoBuffer buf = IoBuffer.allocate(1024, false);
 * </pre>
 * 
 * you can also allocate a new direct buffer:
 * 
 * <pre>
 * IoBuffer buf = IoBuffer.allocate(1024, true);
 * </pre>
 * 
 * or you can set the default buffer type.
 * 
 * <pre>
 * // Allocate heap buffer by default.
 * IoBuffer.setUseDirectBuffer(false);
 * // A new heap buffer is returned.
 * IoBuffer buf = IoBuffer.allocate(1024);
 * </pre>
 * 
 * </p>
 * 
 * <h2>Wrapping existing NIO buffers and arrays</h2>
 * <p>
 * This class provides a few <tt>wrap(...)</tt> methods that wraps any NIO
 * buffers and byte arrays.
 * 
 * <h2>AutoExpand</h2>
 * <p>
 * Writing variable-length data using NIO <tt>ByteBuffers</tt> is not really
 * easy, and it is because its size is fixed. {@link IoBuffer} introduces
 * <tt>autoExpand</tt> property. If <tt>autoExpand</tt> property is true, you
 * never get {@link BufferOverflowException} or
 * {@link IndexOutOfBoundsException} (except when index is negative). It
 * automatically expands its capacity and limit value. For example:
 * 
 * <pre>
 * String greeting = messageBundle.getMessage(&quot;hello&quot;);
 * IoBuffer buf = IoBuffer.allocate(16);
 * // Turn on autoExpand (it is off by default)
 * buf.setAutoExpand(true);
 * buf.putString(greeting, utf8encoder);
 * </pre>
 * 
 * The underlying {@link ByteBuffer} is reallocated by {@link IoBuffer} behind
 * the scene if the encoded data is larger than 16 bytes in the example above.
 * Its capacity will double, and its limit will increase to the last position
 * the string is written.
 * </p>
 * 
 * <h2>AutoShrink</h2>
 * <p>
 * You might also want to decrease the capacity of the buffer when most of the
 * allocated memory area is not being used. {@link IoBuffer} provides
 * <tt>autoShrink</tt> property to take care of this issue. If
 * <tt>autoShrink</tt> is turned on, {@link IoBuffer} halves the capacity of the
 * buffer when {@link #compact()} is invoked and only 1/4 or less of the current
 * capacity is being used.
 * <p>
 * You can also {@link #shrink()} method manually to shrink the capacity of the
 * buffer.
 * <p>
 * The underlying {@link ByteBuffer} is reallocated by {@link IoBuffer} behind
 * the scene, and therefore {@link #buf()} will return a different
 * {@link ByteBuffer} instance once capacity changes. Please also note
 * {@link #compact()} or {@link #shrink()} will not decrease the capacity if the
 * new capacity is less than the {@link #minimumCapacity()} of the buffer.
 * 
 * <h2>Derived Buffers</h2>
 * <p>
 * Derived buffers are the buffers which were created by {@link #duplicate()},
 * {@link #slice()}, or {@link #asReadOnlyBuffer()}. They are useful especially
 * when you broadcast the same messages to multiple {@link IoSession}s. Please
 * note that the buffer derived from and its derived buffers are not both
 * auto-expandable neither auto-shrinkable. Trying to call
 * {@link #setAutoExpand(boolean)} or {@link #setAutoShrink(boolean)} with
 * <tt>true</tt> parameter will raise an {@link IllegalStateException}.
 * </p>
 * 
 * <h2>Changing Buffer Allocation Policy</h2>
 * <p>
 * {@link IoBufferAllocator} interface lets you override the default buffer
 * management behavior. There are two allocators provided out-of-the-box:
 * <ul>
 * <li>{@link SimpleBufferAllocator} (default)</li>
 * <li>{@link CachedBufferAllocator}</li>
 * </ul>
 * You can implement your own allocator and use it by calling
 * {@link #setAllocator(IoBufferAllocator)}.
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class IoBuffer implements Comparable<IoBuffer> {
    /** The allocator used to create new buffers */
    private static IoBufferAllocator allocator = new SimpleBufferAllocator();

    /** A flag indicating which type of buffer we are using : heap or direct */
    private static boolean useDirectBuffer = false;

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
            throw new IllegalArgumentException("allocator");
        }

        IoBufferAllocator oldAllocator = allocator;

        allocator = newAllocator;

        if (null != oldAllocator) {
            oldAllocator.dispose();
        }
    }

    /**
     * Returns <tt>true</tt> if and only if a direct buffer is allocated by
     * default when the type of the new buffer is not specified. The default
     * value is <tt>false</tt>.
     */
    public static boolean isUseDirectBuffer() {
        return useDirectBuffer;
    }

    /**
     * Sets if a direct buffer should be allocated by default when the type of
     * the new buffer is not specified. The default value is <tt>false</tt>.
     */
    public static void setUseDirectBuffer(boolean useDirectBuffer) {
        IoBuffer.useDirectBuffer = useDirectBuffer;
    }

    /**
     * Returns the direct or heap buffer which is capable to store the specified
     * amount of bytes.
     * 
     * @param capacity
     *            the capacity of the buffer
     * 
     * @see #setUseDirectBuffer(boolean)
     */
    public static IoBuffer allocate(int capacity) {
        return allocate(capacity, useDirectBuffer);
    }

    /**
     * Returns the buffer which is capable of the specified size.
     * 
     * @param capacity
     *            the capacity of the buffer
     * @param direct
     *            <tt>true</tt> to get a direct buffer, <tt>false</tt> to get a
     *            heap buffer.
     */
    public static IoBuffer allocate(int capacity, boolean direct) {
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
     * Normalizes the specified capacity of the buffer to power of 2, which is
     * often helpful for optimal memory usage and performance. If it is greater
     * than or equal to {@link Integer#MAX_VALUE}, it returns
     * {@link Integer#MAX_VALUE}. If it is zero, it returns zero.
     */
    protected static int normalizeCapacity(int requestedCapacity) {
        if (requestedCapacity < 0) {
            return Integer.MAX_VALUE;
        }

        int newCapacity = Integer.highestOneBit(requestedCapacity);
        newCapacity <<= (newCapacity < requestedCapacity ? 1 : 0);
        return newCapacity < 0 ? Integer.MAX_VALUE : newCapacity;
    }

    /**
     * Creates a new instance. This is an empty constructor.
     */
    protected IoBuffer() {
        // Do nothing
    }

    /**
     * Declares this buffer and all its derived buffers are not used anymore so
     * that it can be reused by some {@link IoBufferAllocator} implementations.
     * It is not mandatory to call this method, but you might want to invoke
     * this method for maximum performance.
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
     * returns <tt>true</tt> if and only if this buffer is derived from other
     * buffer via {@link #duplicate()}, {@link #slice()} or
     * {@link #asReadOnlyBuffer()}.
     */
    public abstract boolean isDerived();

    /**
     * @see ByteBuffer#isReadOnly()
     */
    public abstract boolean isReadOnly();

    /**
     * Returns the minimum capacity of this buffer which is used to determine
     * the new capacity of the buffer shrunk by {@link #compact()} and
     * {@link #shrink()} operation. The default value is the initial capacity of
     * the buffer.
     */
    public abstract int minimumCapacity();

    /**
     * Sets the minimum capacity of this buffer which is used to determine the
     * new capacity of the buffer shrunk by {@link #compact()} and
     * {@link #shrink()} operation. The default value is the initial capacity of
     * the buffer.
     */
    public abstract IoBuffer minimumCapacity(int minimumCapacity);

    /**
     * @see ByteBuffer#capacity()
     */
    public abstract int capacity();

    /**
     * Increases the capacity of this buffer. If the new capacity is less than
     * or equal to the current capacity, this method returns silently. If the
     * new capacity is greater than the current capacity, the buffer is
     * reallocated while retaining the position, limit, mark and the content of
     * the buffer.
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
     * Changes the capacity and limit of this buffer so this buffer get the
     * specified <tt>expectedRemaining</tt> room from the current position. This
     * method works even if you didn't set <tt>autoExpand</tt> to <tt>true</tt>.
     */
    public abstract IoBuffer expand(int expectedRemaining);

    /**
     * Changes the capacity and limit of this buffer so this buffer get the
     * specified <tt>expectedRemaining</tt> room from the specified
     * <tt>position</tt>. This method works even if you didn't set
     * <tt>autoExpand</tt> to <tt>true</tt>.
     */
    public abstract IoBuffer expand(int position, int expectedRemaining);

    /**
     * Changes the capacity of this buffer so this buffer occupies as less
     * memory as possible while retaining the position, limit and the buffer
     * content between the position and limit. The capacity of the buffer never
     * becomes less than {@link #minimumCapacity()}. The mark is discarded once
     * the capacity changes.
     */
    public abstract IoBuffer shrink();

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
     * Returns the position of the current mark. This method returns <tt>-1</tt>
     * if no mark is set.
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
     * Clears this buffer and fills its content with <tt>NUL</tt>. The position
     * is set to zero, the limit is set to the capacity, and the mark is
     * discarded.
     */
    public abstract IoBuffer sweep();

    /**
     * double Clears this buffer and fills its content with <tt>value</tt>. The
     * position is set to zero, the limit is set to the capacity, and the mark
     * is discarded.
     */
    public abstract IoBuffer sweep(byte value);

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
    public abstract int remaining();

    /**
     * @see java.nio.Buffer#hasRemaining()
     */
    public abstract boolean hasRemaining();

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
    public abstract short getUnsigned();

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
    public abstract short getUnsigned(int index);

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
    public abstract IoBuffer get(byte[] dst);

    /**
     * TODO document me.
     */
    public abstract IoBuffer getSlice(int index, int length);

    /**
     * TODO document me.
     */
    public abstract IoBuffer getSlice(int length);

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    public abstract IoBuffer put(ByteBuffer src);

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    public abstract IoBuffer put(IoBuffer src);

    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    public abstract IoBuffer put(byte[] src, int offset, int length);

    /**
     * @see ByteBuffer#put(byte[])
     */
    public abstract IoBuffer put(byte[] src);

    /**
     * @see ByteBuffer#compact()
     */
    public abstract IoBuffer compact();

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
    public abstract int getUnsignedShort();

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
    public abstract int getUnsignedShort(int index);

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
    public abstract long getUnsignedInt();

    /**
     * Relative <i>get</i> method for reading a medium int value.
     * 
     * <p>
     * Reads the next three bytes at this buffer's current position, composing
     * them into an int value according to the current byte order, and then
     * increments the position by three.
     * </p>
     * 
     * @return The medium int value at the buffer's current position
     */
    public abstract int getMediumInt();

    /**
     * Relative <i>get</i> method for reading an unsigned medium int value.
     * 
     * <p>
     * Reads the next three bytes at this buffer's current position, composing
     * them into an int value according to the current byte order, and then
     * increments the position by three.
     * </p>
     * 
     * @return The unsigned medium int value at the buffer's current position
     */
    public abstract int getUnsignedMediumInt();

    /**
     * Absolute <i>get</i> method for reading a medium int value.
     * 
     * <p>
     * Reads the next three bytes at this buffer's current position, composing
     * them into an int value according to the current byte order.
     * </p>
     * 
     * @param index
     *            The index from which the medium int will be read
     * @return The medium int value at the given index
     * 
     * @throws IndexOutOfBoundsException
     *             If <tt>index</tt> is negative or not smaller than the
     *             buffer's limit
     */
    public abstract int getMediumInt(int index);

    /**
     * Absolute <i>get</i> method for reading an unsigned medium int value.
     * 
     * <p>
     * Reads the next three bytes at this buffer's current position, composing
     * them into an int value according to the current byte order.
     * </p>
     * 
     * @param index
     *            The index from which the unsigned medium int will be read
     * @return The unsigned medium int value at the given index
     * 
     * @throws IndexOutOfBoundsException
     *             If <tt>index</tt> is negative or not smaller than the
     *             buffer's limit
     */
    public abstract int getUnsignedMediumInt(int index);

    /**
     * Relative <i>put</i> method for writing a medium int value.
     * 
     * <p>
     * Writes three bytes containing the given int value, in the current byte
     * order, into this buffer at the current position, and then increments the
     * position by three.
     * </p>
     * 
     * @param value
     *            The medium int value to be written
     * 
     * @return This buffer
     * 
     * @throws BufferOverflowException
     *             If there are fewer than three bytes remaining in this buffer
     * 
     * @throws ReadOnlyBufferException
     *             If this buffer is read-only
     */
    public abstract IoBuffer putMediumInt(int value);

    /**
     * Absolute <i>put</i> method for writing a medium int value.
     * 
     * <p>
     * Writes three bytes containing the given int value, in the current byte
     * order, into this buffer at the given index.
     * </p>
     * 
     * @param index
     *            The index at which the bytes will be written
     * 
     * @param value
     *            The medium int value to be written
     * 
     * @return This buffer
     * 
     * @throws IndexOutOfBoundsException
     *             If <tt>index</tt> is negative or not smaller than the
     *             buffer's limit, minus three
     * 
     * @throws ReadOnlyBufferException
     *             If this buffer is read-only
     */
    public abstract IoBuffer putMediumInt(int index, int value);

    /**
     * @see ByteBuffer#putInt(int)
     */
    public abstract IoBuffer putInt(int value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer
     * @param value the byte to write
     */
    public abstract IoBuffer putUnsigned(byte value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the byte to write
     */
    public abstract IoBuffer putUnsigned(int index, byte value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer
     * @param value the short to write
     */
    public abstract IoBuffer putUnsigned(short value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the short to write
     */
    public abstract IoBuffer putUnsigned(int index, short value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer
     * @param value the int to write
     */
    public abstract IoBuffer putUnsigned(int value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the int to write
     */
    public abstract IoBuffer putUnsigned(int index, int value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer
     * @param value the long to write
     */
    public abstract IoBuffer putUnsigned(long value);
    
    /**
     * Writes an unsigned byte into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the long to write
     */
    public abstract IoBuffer putUnsigned(int index, long value);
    
    /**
     * Writes an unsigned int into the ByteBuffer
     * @param value the byte to write
     */
    public abstract IoBuffer putUnsignedInt(byte value);
    
    /**
     * Writes an unsigned int into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the byte to write
     */
    public abstract IoBuffer putUnsignedInt(int index, byte value);
    
    /**
     * Writes an unsigned int into the ByteBuffer
     * @param value the short to write
     */
    public abstract IoBuffer putUnsignedInt(short value);
    
    /**
     * Writes an unsigned int into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the short to write
     */
    public abstract IoBuffer putUnsignedInt(int index, short value);
    
    /**
     * Writes an unsigned int into the ByteBuffer
     * @param value the int to write
     */
    public abstract IoBuffer putUnsignedInt(int value);
    
    /**
     * Writes an unsigned int into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the int to write
     */
    public abstract IoBuffer putUnsignedInt(int index, int value);
    
    /**
     * Writes an unsigned int into the ByteBuffer
     * @param value the long to write
     */
    public abstract IoBuffer putUnsignedInt(long value);
    
    /**
     * Writes an unsigned int into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the long to write
     */
    public abstract IoBuffer putUnsignedInt(int index, long value);
    
    /**
     * Writes an unsigned short into the ByteBuffer
     * @param value the byte to write
     */
    public abstract IoBuffer putUnsignedShort(byte value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the byte to write
     */
    public abstract IoBuffer putUnsignedShort(int index, byte value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer
     * @param value the short to write
     */
    public abstract IoBuffer putUnsignedShort(short value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the short to write
     */
    public abstract IoBuffer putUnsignedShort(int index, short value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer
     * @param value the int to write
     */
    public abstract IoBuffer putUnsignedShort(int value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the int to write
     */
    public abstract IoBuffer putUnsignedShort(int index, int value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer
     * @param value the long to write
     */
    public abstract IoBuffer putUnsignedShort(long value);
    
    /**
     * Writes an unsigned Short into the ByteBuffer at a specified position
     * @param index the position in the buffer to write the value
     * @param value the long to write
     */
    public abstract IoBuffer putUnsignedShort(int index, long value);

    /**
     * @see ByteBuffer#getInt(int)
     */
    public abstract int getInt(int index);

    /**
     * Reads four bytes unsigned integer.
     * @param index the position in the buffer to write the value
     */
    public abstract long getUnsignedInt(int index);

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
    public abstract InputStream asInputStream();

    /**
     * Returns an {@link OutputStream} that appends the data into this buffer.
     * Please note that the {@link OutputStream#write(int)} will throw a
     * {@link BufferOverflowException} instead of an {@link IOException} in case
     * of buffer overflow. Please set <tt>autoExpand</tt> property by calling
     * {@link #setAutoExpand(boolean)} to prevent the unexpected runtime
     * exception.
     */
    public abstract OutputStream asOutputStream();

    /**
     * Returns hexdump of this buffer. The data and pointer are not changed as a
     * result of this method call.
     * 
     * @return hexidecimal representation of this buffer
     */
    public abstract String getHexDump();

    /**
     * Return hexdump of this buffer with limited length.
     * 
     * @param lengthLimit
     *            The maximum number of bytes to dump from the current buffer
     *            position.
     * @return hexidecimal representation of this buffer
     */
    public abstract String getHexDump(int lengthLimit);

    // //////////////////////////////
    // String getters and putters //
    // //////////////////////////////

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it. This method reads until
     * the limit of this buffer if no <tt>NUL</tt> is found.
     */
    public abstract String getString(CharsetDecoder decoder) throws CharacterCodingException;

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     * 
     * @param fieldSize
     *            the maximum number of bytes to read
     */
    public abstract String getString(int fieldSize, CharsetDecoder decoder) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer using the
     * specified <code>encoder</code>. This method doesn't terminate string with
     * <tt>NUL</tt>. You have to do it by yourself.
     * 
     * @throws BufferOverflowException
     *             if the specified string doesn't fit
     */
    public abstract IoBuffer putString(CharSequence val, CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a
     * <code>NUL</code>-terminated string using the specified
     * <code>encoder</code>.
     * <p>
     * If the charset name of the encoder is UTF-16, you cannot specify odd
     * <code>fieldSize</code>, and this method will append two <code>NUL</code>s
     * as a terminator.
     * <p>
     * Please note that this method doesn't terminate with <code>NUL</code> if
     * the input string is longer than <tt>fieldSize</tt>.
     * 
     * @param fieldSize
     *            the maximum number of bytes to write
     */
    public abstract IoBuffer putString(CharSequence val, int fieldSize, CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * Reads a string which has a 16-bit length field before the actual encoded
     * string, using the specified <code>decoder</code> and returns it. This
     * method is a shortcut for <tt>getPrefixedString(2, decoder)</tt>.
     */
    public abstract String getPrefixedString(CharsetDecoder decoder) throws CharacterCodingException;

    /**
     * Reads a string which has a length field before the actual encoded string,
     * using the specified <code>decoder</code> and returns it.
     * 
     * @param prefixLength
     *            the length of the length field (1, 2, or 4)
     */
    public abstract String getPrefixedString(int prefixLength, CharsetDecoder decoder) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a string which
     * has a 16-bit length field before the actual encoded string, using the
     * specified <code>encoder</code>. This method is a shortcut for
     * <tt>putPrefixedString(in, 2, 0, encoder)</tt>.
     * 
     * @throws BufferOverflowException
     *             if the specified string doesn't fit
     */
    public abstract IoBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a string which
     * has a 16-bit length field before the actual encoded string, using the
     * specified <code>encoder</code>. This method is a shortcut for
     * <tt>putPrefixedString(in, prefixLength, 0, encoder)</tt>.
     * 
     * @param prefixLength
     *            the length of the length field (1, 2, or 4)
     * 
     * @throws BufferOverflowException
     *             if the specified string doesn't fit
     */
    public abstract IoBuffer putPrefixedString(CharSequence in, int prefixLength, CharsetEncoder encoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a string which
     * has a 16-bit length field before the actual encoded string, using the
     * specified <code>encoder</code>. This method is a shortcut for
     * <tt>putPrefixedString(in, prefixLength, padding, ( byte ) 0, encoder)</tt>
     * .
     * 
     * @param prefixLength
     *            the length of the length field (1, 2, or 4)
     * @param padding
     *            the number of padded <tt>NUL</tt>s (1 (or 0), 2, or 4)
     * 
     * @throws BufferOverflowException
     *             if the specified string doesn't fit
     */
    public abstract IoBuffer putPrefixedString(CharSequence in, int prefixLength, int padding, CharsetEncoder encoder)
            throws CharacterCodingException;

    /**
     * Writes the content of <code>in</code> into this buffer as a string which
     * has a 16-bit length field before the actual encoded string, using the
     * specified <code>encoder</code>.
     * 
     * @param prefixLength
     *            the length of the length field (1, 2, or 4)
     * @param padding
     *            the number of padded bytes (1 (or 0), 2, or 4)
     * @param padValue
     *            the value of padded bytes
     * 
     * @throws BufferOverflowException
     *             if the specified string doesn't fit
     */
    public abstract IoBuffer putPrefixedString(CharSequence val, int prefixLength, int padding, byte padValue,
            CharsetEncoder encoder) throws CharacterCodingException;

    /**
     * Reads a Java object from the buffer using the context {@link ClassLoader}
     * of the current thread.
     */
    public abstract Object getObject() throws ClassNotFoundException;

    /**
     * Reads a Java object from the buffer using the specified
     * <tt>classLoader</tt>.
     */
    public abstract Object getObject(final ClassLoader classLoader) throws ClassNotFoundException;

    /**
     * Writes the specified Java object to the buffer.
     */
    public abstract IoBuffer putObject(Object o);

    /**
     * Returns <tt>true</tt> if this buffer contains a data which has a data
     * length as a prefix and the buffer has remaining data as enough as
     * specified in the data length field. This method is identical with
     * <tt>prefixedDataAvailable( prefixLength, Integer.MAX_VALUE )</tt>. Please
     * not that using this method can allow DoS (Denial of Service) attack in
     * case the remote peer sends too big data length value. It is recommended
     * to use {@link #prefixedDataAvailable(int, int)} instead.
     * 
     * @param prefixLength
     *            the length of the prefix field (1, 2, or 4)
     * 
     * @throws IllegalArgumentException
     *             if prefixLength is wrong
     * @throws BufferDataException
     *             if data length is negative
     */
    public abstract boolean prefixedDataAvailable(int prefixLength);

    /**
     * Returns <tt>true</tt> if this buffer contains a data which has a data
     * length as a prefix and the buffer has remaining data as enough as
     * specified in the data length field.
     * 
     * @param prefixLength
     *            the length of the prefix field (1, 2, or 4)
     * @param maxDataLength
     *            the allowed maximum of the read data length
     * 
     * @throws IllegalArgumentException
     *             if prefixLength is wrong
     * @throws BufferDataException
     *             if data length is negative or greater then
     *             <tt>maxDataLength</tt>
     */
    public abstract boolean prefixedDataAvailable(int prefixLength, int maxDataLength);

    // ///////////////////
    // IndexOf methods //
    // ///////////////////

    /**
     * Returns the first occurence position of the specified byte from the
     * current position to the current limit.
     * 
     * @return <tt>-1</tt> if the specified byte is not found
     */
    public abstract int indexOf(byte b);

    // ////////////////////////
    // Skip or fill methods //
    // ////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    public abstract IoBuffer skip(int size);

    /**
     * Fills this buffer with the specified value. This method moves buffer
     * position forward.
     */
    public abstract IoBuffer fill(byte value, int size);

    /**
     * Fills this buffer with the specified value. This method does not change
     * buffer position.
     */
    public abstract IoBuffer fillAndReset(byte value, int size);

    /**
     * Fills this buffer with <code>NUL (0x00)</code>. This method moves buffer
     * position forward.
     */
    public abstract IoBuffer fill(int size);

    /**
     * Fills this buffer with <code>NUL (0x00)</code>. This method does not
     * change buffer position.
     */
    public abstract IoBuffer fillAndReset(int size);

    // ////////////////////////
    // Enum methods //
    // ////////////////////////

    /**
     * Reads a byte from the buffer and returns the correlating enum constant
     * defined by the specified enum type.
     * 
     * @param <E>
     *            The enum type to return
     * @param enumClass
     *            The enum's class object
     */
    public abstract <E extends Enum<E>> E getEnum(Class<E> enumClass);

    /**
     * Reads a byte from the buffer and returns the correlating enum constant
     * defined by the specified enum type.
     * 
     * @param <E>
     *            The enum type to return
     * @param index
     *            the index from which the byte will be read
     * @param enumClass
     *            The enum's class object
     */
    public abstract <E extends Enum<E>> E getEnum(int index, Class<E> enumClass);

    /**
     * Reads a short from the buffer and returns the correlating enum constant
     * defined by the specified enum type.
     * 
     * @param <E>
     *            The enum type to return
     * @param enumClass
     *            The enum's class object
     */
    public abstract <E extends Enum<E>> E getEnumShort(Class<E> enumClass);

    /**
     * Reads a short from the buffer and returns the correlating enum constant
     * defined by the specified enum type.
     * 
     * @param <E>
     *            The enum type to return
     * @param index
     *            the index from which the bytes will be read
     * @param enumClass
     *            The enum's class object
     */
    public abstract <E extends Enum<E>> E getEnumShort(int index, Class<E> enumClass);

    /**
     * Reads an int from the buffer and returns the correlating enum constant
     * defined by the specified enum type.
     * 
     * @param <E>
     *            The enum type to return
     * @param enumClass
     *            The enum's class object
     */
    public abstract <E extends Enum<E>> E getEnumInt(Class<E> enumClass);

    /**
     * Reads an int from the buffer and returns the correlating enum constant
     * defined by the specified enum type.
     * 
     * @param <E>
     *            The enum type to return
     * @param index
     *            the index from which the bytes will be read
     * @param enumClass
     *            The enum's class object
     */
    public abstract <E extends Enum<E>> E getEnumInt(int index, Class<E> enumClass);

    /**
     * Writes an enum's ordinal value to the buffer as a byte.
     * 
     * @param e
     *            The enum to write to the buffer
     */
    public abstract IoBuffer putEnum(Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as a byte.
     * 
     * @param index
     *            The index at which the byte will be written
     * @param e
     *            The enum to write to the buffer
     */
    public abstract IoBuffer putEnum(int index, Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as a short.
     * 
     * @param e
     *            The enum to write to the buffer
     */
    public abstract IoBuffer putEnumShort(Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as a short.
     * 
     * @param index
     *            The index at which the bytes will be written
     * @param e
     *            The enum to write to the buffer
     */
    public abstract IoBuffer putEnumShort(int index, Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as an integer.
     * 
     * @param e
     *            The enum to write to the buffer
     */
    public abstract IoBuffer putEnumInt(Enum<?> e);

    /**
     * Writes an enum's ordinal value to the buffer as an integer.
     * 
     * @param index
     *            The index at which the bytes will be written
     * @param e
     *            The enum to write to the buffer
     */
    public abstract IoBuffer putEnumInt(int index, Enum<?> e);

    // ////////////////////////
    // EnumSet methods //
    // ////////////////////////

    /**
     * Reads a byte sized bit vector and converts it to an {@link EnumSet}.
     * 
     * <p>
     * Each bit is mapped to a value in the specified enum. The least
     * significant bit maps to the first entry in the specified enum and each
     * subsequent bit maps to each subsequent bit as mapped to the subsequent
     * enum value.
     * </p>
     * 
     * @param <E>
     *            the enum type
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSet(Class<E> enumClass);

    /**
     * Reads a byte sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param index
     *            the index from which the byte will be read
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSet(int index, Class<E> enumClass);

    /**
     * Reads a short sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSetShort(Class<E> enumClass);

    /**
     * Reads a short sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param index
     *            the index from which the bytes will be read
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSetShort(int index, Class<E> enumClass);

    /**
     * Reads an int sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSetInt(Class<E> enumClass);

    /**
     * Reads an int sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param index
     *            the index from which the bytes will be read
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSetInt(int index, Class<E> enumClass);

    /**
     * Reads a long sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSetLong(Class<E> enumClass);

    /**
     * Reads a long sized bit vector and converts it to an {@link EnumSet}.
     * 
     * @see #getEnumSet(Class)
     * @param <E>
     *            the enum type
     * @param index
     *            the index from which the bytes will be read
     * @param enumClass
     *            the enum class used to create the EnumSet
     * @return the EnumSet representation of the bit vector
     */
    public abstract <E extends Enum<E>> EnumSet<E> getEnumSetLong(int index, Class<E> enumClass);

    /**
     * Writes the specified {@link Set} to the buffer as a byte sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSet(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a byte sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param index
     *            the index at which the byte will be written
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSet(int index, Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a short sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSetShort(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a short sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param index
     *            the index at which the bytes will be written
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSetShort(int index, Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as an int sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSetInt(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as an int sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param index
     *            the index at which the bytes will be written
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSetInt(int index, Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a long sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSetLong(Set<E> set);

    /**
     * Writes the specified {@link Set} to the buffer as a long sized bit
     * vector.
     * 
     * @param <E>
     *            the enum type of the Set
     * @param index
     *            the index at which the bytes will be written
     * @param set
     *            the enum set to write to the buffer
     */
    public abstract <E extends Enum<E>> IoBuffer putEnumSetLong(int index, Set<E> set);
}
