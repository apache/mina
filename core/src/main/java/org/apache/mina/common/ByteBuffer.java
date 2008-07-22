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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.support.ByteBufferHexDumper;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A byte buffer used by MINA applications.
 * <p>
 * This is a replacement for {@link java.nio.ByteBuffer}. Please refer to
 * {@link java.nio.ByteBuffer} and {@link java.nio.Buffer} documentation for
 * usage.  MINA does not use NIO {@link java.nio.ByteBuffer} directly for two
 * reasons:
 * <ul>
 * <li>It doesn't provide useful getters and putters such as
 * <code>fill</code>, <code>get/putString</code>, and
 * <code>get/putAsciiInt()</code> enough.</li>
 * <li>It is hard to distinguish if the buffer is created from MINA buffer
 * pool or not.  MINA have to return used buffers back to pool.</li>
 * <li>It is difficult to write variable-length data due to its fixed
 * capacity</li>
 * </ul>
 * </p>
 *
 * <h2>Allocation</h2>
 * <p>
 * You can get a heap buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024, false);
 * </pre>
 * you can also get a direct buffer from buffer pool:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024, true);
 * </pre>
 * or you can let MINA choose:
 * <pre>
 * ByteBuffer buf = ByteBuffer.allocate(1024);
 * </pre>
 * </p>
 *
 * <h2>Acquire/Release</h2>
 * <p>
 * <b>Please note that you never need to release the allocated buffer</b>
 * because MINA will release it automatically when:
 * <ul>
 * <li>You pass the buffer by calling {@link IoSession#write(Object)}.</li>
 * <li>You pass the buffer by calling {@link IoFilter.NextFilter#filterWrite(IoSession,IoFilter.WriteRequest)}.</li>
 * <li>You pass the buffer by calling {@link ProtocolEncoderOutput#write(ByteBuffer)}.</li>
 * </ul>
 * And, you don't need to release any {@link ByteBuffer} which is passed as a parameter
 * of {@link IoHandler#messageReceived(IoSession, Object)} method.  They are released
 * automatically when the method returns.
 * <p>
 * You have to release buffers manually by calling {@link #release()} when:
 * <ul>
 * <li>You allocated a buffer, but didn't pass the buffer to any of two methods above.</li>
 * <li>You called {@link #acquire()} to prevent the buffer from being released.</li>
 * </ul>
 * </p>
 *
 * <h2>Wrapping existing NIO buffers and arrays</h2>
 * <p>
 * This class provides a few <tt>wrap(...)</tt> methods that wraps
 * any NIO buffers and byte arrays.  Wrapped MINA buffers are not returned
 * to the buffer pool by default to prevent unexpected memory leakage by default.
 * In case you want to make it pooled, you can call {@link #setPooled(boolean)}
 * with <tt>true</tt> flag to enable pooling.
 *
 * <h2>AutoExpand</h2>
 * <p>
 * Writing variable-length data using NIO <tt>ByteBuffers</tt> is not really
 * easy, and it is because its size is fixed.  MINA <tt>ByteBuffer</tt>
 * introduces <tt>autoExpand</tt> property.  If <tt>autoExpand</tt> property
 * is true, you never get {@link BufferOverflowException} or
 * {@link IndexOutOfBoundsException} (except when index is negative).
 * It automatically expands its capacity and limit value.  For example:
 * <pre>
 * String greeting = messageBundle.getMessage( "hello" );
 * ByteBuffer buf = ByteBuffer.allocate( 16 );
 * // Turn on autoExpand (it is off by default)
 * buf.setAutoExpand( true );
 * buf.putString( greeting, utf8encoder );
 * </pre>
 * NIO <tt>ByteBuffer</tt> is reallocated by MINA <tt>ByteBuffer</tt> behind
 * the scene if the encoded data is larger than 16 bytes.  Its capacity and
 * its limit will increase to the last position the string is written.
 * </p>
 *
 * <h2>Derived Buffers</h2>
 * <p>
 * Derived buffers are the buffers which were created by
 * {@link #duplicate()}, {@link #slice()}, or {@link #asReadOnlyBuffer()}.
 * They are useful especially when you broadcast the same messages to
 * multiple {@link IoSession}s.  Please note that the derived buffers are
 * neither pooled nor auto-expandable.  Trying to expand a derived buffer will
 * raise {@link IllegalStateException}.
 * </p>
 *
 * <h2>Changing Buffer Allocation and Management Policy</h2>
 * <p>
 * MINA provides a {@link ByteBufferAllocator} interface to let you override
 * the default buffer management behavior.  There are two allocators provided
 * out-of-the-box:
 * <ul>
 * <li>{@link PooledByteBufferAllocator} (Default)</li>
 * <li>{@link SimpleByteBufferAllocator}</li>
 * </ul>
 * You can change the allocator by calling {@link #setAllocator(ByteBufferAllocator)}.
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * @noinspection StaticNonFinalField
 * @see ByteBufferAllocator
 */
public abstract class ByteBuffer implements Comparable<ByteBuffer> {
    private static ByteBufferAllocator allocator = new PooledByteBufferAllocator();

    private static boolean useDirectBuffers = false;

    /**
     * Returns the current allocator which manages the allocated buffers.
     */
    public static ByteBufferAllocator getAllocator() {
        return allocator;
    }

    /**
     * Changes the current allocator with the specified one to manage
     * the allocated buffers from now.
     */
    public static void setAllocator(ByteBufferAllocator newAllocator) {
        if (newAllocator == null) {
            throw new NullPointerException("allocator");
        }

        ByteBufferAllocator oldAllocator = allocator;

        allocator = newAllocator;

        if (null != oldAllocator) {
            oldAllocator.dispose();
        }
    }

    public static boolean isUseDirectBuffers() {
        return useDirectBuffers;
    }

    public static void setUseDirectBuffers(boolean useDirectBuffers) {
        ByteBuffer.useDirectBuffers = useDirectBuffers;
    }

    /**
     * Returns the direct or heap buffer which is capable of the specified
     * size.  This method tries to allocate direct buffer first, and then
     * tries heap buffer if direct buffer memory is exhausted.  Please use
     * {@link #allocate(int, boolean)} to allocate buffers of specific type.
     *
     * @param capacity the capacity of the buffer
     */
    public static ByteBuffer allocate(int capacity) {
        if (useDirectBuffers) {
            try {
                // first try to allocate direct buffer
                return allocate(capacity, true);
            } catch (OutOfMemoryError e) {
                // fall through to heap buffer
            }
        }

        return allocate(capacity, false);
    }

    /**
     * Returns the buffer which is capable of the specified size.
     *
     * @param capacity the capacity of the buffer
     * @param direct   <tt>true</tt> to get a direct buffer,
     *                 <tt>false</tt> to get a heap buffer.
     */
    public static ByteBuffer allocate(int capacity, boolean direct) {
        return allocator.allocate(capacity, direct);
    }

    /**
     * Wraps the specified NIO {@link java.nio.ByteBuffer} into MINA buffer.
     */
    public static ByteBuffer wrap(java.nio.ByteBuffer nioBuffer) {
        return allocator.wrap(nioBuffer);
    }

    /**
     * Wraps the specified byte array into MINA heap buffer.
     */
    public static ByteBuffer wrap(byte[] byteArray) {
        return wrap(java.nio.ByteBuffer.wrap(byteArray));
    }

    /**
     * Wraps the specified byte array into MINA heap buffer.
     * Please note that MINA buffers are going to be pooled, and
     * therefore there can be waste of memory if you wrap
     * your byte array specifying <tt>offset</tt> and <tt>length</tt>.
     */
    public static ByteBuffer wrap(byte[] byteArray, int offset, int length) {
        return wrap(java.nio.ByteBuffer.wrap(byteArray, offset, length));
    }

    private static final Set<String> primitiveTypeNames = new HashSet<String>();
    
    static {
        primitiveTypeNames.add("void");
        primitiveTypeNames.add("boolean");
        primitiveTypeNames.add("byte");
        primitiveTypeNames.add("char");
        primitiveTypeNames.add("short");
        primitiveTypeNames.add("int");
        primitiveTypeNames.add("long");
        primitiveTypeNames.add("float");
        primitiveTypeNames.add("double");
    }

    protected ByteBuffer() {
    }

    /**
     * Increases the internal reference count of this buffer to defer
     * automatic release.  You have to invoke {@link #release()} as many
     * as you invoked this method to release this buffer.
     *
     * @throws IllegalStateException if you attempt to acquire already
     *                               released buffer.
     */
    public abstract void acquire();

    /**
     * Releases the specified buffer to buffer pool.
     *
     * @throws IllegalStateException if you attempt to release already
     *                               released buffer.
     */
    public abstract void release();

    /**
     * Returns the underlying NIO buffer instance.
     */
    public abstract java.nio.ByteBuffer buf();

    /**
     * @see java.nio.ByteBuffer#isDirect()
     */
    public abstract boolean isDirect();

    /**
     * @see java.nio.ByteBuffer#isReadOnly()
     */
    public abstract boolean isReadOnly();

    /**
     * @see java.nio.ByteBuffer#capacity()
     */
    public abstract int capacity();

    /**
     * Changes the capacity of this buffer.
     */
    public abstract ByteBuffer capacity(int newCapacity);

    /**
     * Returns <tt>true</tt> if and only if <tt>autoExpand</tt> is turned on.
     */
    public abstract boolean isAutoExpand();

    /**
     * Turns on or off <tt>autoExpand</tt>.
     */
    public abstract ByteBuffer setAutoExpand(boolean autoExpand);

    /**
     * Changes the capacity and limit of this buffer so this buffer get
     * the specified <tt>expectedRemaining</tt> room from the current position.
     * This method works even if you didn't set <tt>autoExpand</tt> to
     * <tt>true</tt>.
     */
    public ByteBuffer expand(int expectedRemaining) {
        return expand(position(), expectedRemaining);
    }

    /**
     * Changes the capacity and limit of this buffer so this buffer get
     * the specified <tt>expectedRemaining</tt> room from the specified
     * <tt>pos</tt>.
     * This method works even if you didn't set <tt>autoExpand</tt> to
     * <tt>true</tt>.
     */
    public abstract ByteBuffer expand(int pos, int expectedRemaining);

    /**
     * Returns <tt>true</tt> if and only if this buffer is returned back
     * to the buffer pool when released.
     * <p>
     * The default value of this property is <tt>true</tt> if and only if you
     * allocated this buffer using {@link #allocate(int)} or {@link #allocate(int, boolean)},
     * or <tt>false</tt> otherwise. (i.e. {@link #wrap(byte[])}, {@link #wrap(byte[], int, int)},
     * and {@link #wrap(java.nio.ByteBuffer)})
     */
    public abstract boolean isPooled();

    /**
     * Sets whether this buffer is returned back to the buffer pool when released.
     * <p>
     * The default value of this property is <tt>true</tt> if and only if you
     * allocated this buffer using {@link #allocate(int)} or {@link #allocate(int, boolean)},
     * or <tt>false</tt> otherwise. (i.e. {@link #wrap(byte[])}, {@link #wrap(byte[], int, int)},
     * and {@link #wrap(java.nio.ByteBuffer)})
     */
    public abstract void setPooled(boolean pooled);

    /**
     * @see java.nio.Buffer#position()
     */
    public abstract int position();

    /**
     * @see java.nio.Buffer#position(int)
     */
    public abstract ByteBuffer position(int newPosition);

    /**
     * @see java.nio.Buffer#limit()
     */
    public abstract int limit();

    /**
     * @see java.nio.Buffer#limit(int)
     */
    public abstract ByteBuffer limit(int newLimit);

    /**
     * @see java.nio.Buffer#mark()
     */
    public abstract ByteBuffer mark();

    /**
     * Returns the position of the current mark.  This method returns <tt>-1</tt> if no
     * mark is set.
     */
    public abstract int markValue();

    /**
     * @see java.nio.Buffer#reset()
     */
    public abstract ByteBuffer reset();

    /**
     * @see java.nio.Buffer#clear()
     */
    public abstract ByteBuffer clear();

    /**
     * Clears this buffer and fills its content with <tt>NUL</tt>.
     * The position is set to zero, the limit is set to the capacity,
     * and the mark is discarded.
     */
    public ByteBuffer sweep() {
        clear();
        return fillAndReset(remaining());
    }

    /**
     * Clears this buffer and fills its content with <tt>value</tt>.
     * The position is set to zero, the limit is set to the capacity,
     * and the mark is discarded.
     */
    public ByteBuffer sweep(byte value) {
        clear();
        return fillAndReset(value, remaining());
    }

    /**
     * @see java.nio.Buffer#flip()
     */
    public abstract ByteBuffer flip();

    /**
     * @see java.nio.Buffer#rewind()
     */
    public abstract ByteBuffer rewind();

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
        return remaining() > 0;
    }

    /**
     * @see java.nio.ByteBuffer#duplicate()
     */
    public abstract ByteBuffer duplicate();

    /**
     * @see java.nio.ByteBuffer#slice()
     */
    public abstract ByteBuffer slice();

    /**
     * @see java.nio.ByteBuffer#asReadOnlyBuffer()
     */
    public abstract ByteBuffer asReadOnlyBuffer();

    /**
     * @see java.nio.ByteBuffer#array()
     */
    public abstract byte[] array();

    /**
     * @see java.nio.ByteBuffer#arrayOffset()
     */
    public abstract int arrayOffset();

    /**
     * @see java.nio.ByteBuffer#get()
     */
    public abstract byte get();

    /**
     * Reads one unsigned byte as a short integer.
     */
    public short getUnsigned() {
        return (short) (get() & 0xff);
    }

    /**
     * @see java.nio.ByteBuffer#put(byte)
     */
    public abstract ByteBuffer put(byte b);

    /**
     * @see java.nio.ByteBuffer#get(int)
     */
    public abstract byte get(int index);

    /**
     * Reads one byte as an unsigned short integer.
     */
    public short getUnsigned(int index) {
        return (short) (get(index) & 0xff);
    }

    /**
     * @see java.nio.ByteBuffer#put(int, byte)
     */
    public abstract ByteBuffer put(int index, byte b);

    /**
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     */
    public abstract ByteBuffer get(byte[] dst, int offset, int length);

    /**
     * @see java.nio.ByteBuffer#get(byte[])
     */
    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    public abstract ByteBuffer put(java.nio.ByteBuffer src);

    /**
     * Writes the content of the specified <tt>src</tt> into this buffer.
     */
    public ByteBuffer put(ByteBuffer src) {
        return put(src.buf());
    }

    /**
     * @see java.nio.ByteBuffer#put(byte[], int, int)
     */
    public abstract ByteBuffer put(byte[] src, int offset, int length);

    /**
     * @see java.nio.ByteBuffer#put(byte[])
     */
    public ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    /**
     * @see java.nio.ByteBuffer#compact()
     */
    public abstract ByteBuffer compact();

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
        buf.append(getHexDump());
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
        if (!(o instanceof ByteBuffer)) {
            return false;
        }

        ByteBuffer that = (ByteBuffer) o;
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

    public int compareTo(ByteBuffer that) {
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
     * @see java.nio.ByteBuffer#order()
     */
    public abstract ByteOrder order();

    /**
     * @see java.nio.ByteBuffer#order(ByteOrder)
     */
    public abstract ByteBuffer order(ByteOrder bo);

    /**
     * @see java.nio.ByteBuffer#getChar()
     */
    public abstract char getChar();

    /**
     * @see java.nio.ByteBuffer#putChar(char)
     */
    public abstract ByteBuffer putChar(char value);

    /**
     * @see java.nio.ByteBuffer#getChar(int)
     */
    public abstract char getChar(int index);

    /**
     * @see java.nio.ByteBuffer#putChar(int, char)
     */
    public abstract ByteBuffer putChar(int index, char value);

    /**
     * @see java.nio.ByteBuffer#asCharBuffer()
     */
    public abstract CharBuffer asCharBuffer();

    /**
     * @see java.nio.ByteBuffer#getShort()
     */
    public abstract short getShort();

    /**
     * Reads two bytes unsigned integer.
     */
    public int getUnsignedShort() {
        return getShort() & 0xffff;
    }

    /**
     * @see java.nio.ByteBuffer#putShort(short)
     */
    public abstract ByteBuffer putShort(short value);

    /**
     * @see java.nio.ByteBuffer#getShort()
     */
    public abstract short getShort(int index);

    /**
     * Reads two bytes unsigned integer.
     */
    public int getUnsignedShort(int index) {
        return getShort(index) & 0xffff;
    }

    /**
     * @see java.nio.ByteBuffer#putShort(int, short)
     */
    public abstract ByteBuffer putShort(int index, short value);

    /**
     * @see java.nio.ByteBuffer#asShortBuffer()
     */
    public abstract ShortBuffer asShortBuffer();

    /**
     * @see java.nio.ByteBuffer#getInt()
     */
    public abstract int getInt();

    /**
     * Reads four bytes unsigned integer.
     */
    public long getUnsignedInt() {
        return getInt() & 0xffffffffL;
    }

    /**
     * @see java.nio.ByteBuffer#putInt(int)
     */
    public abstract ByteBuffer putInt(int value);

    /**
     * @see java.nio.ByteBuffer#getInt(int)
     */
    public abstract int getInt(int index);

    /**
     * Reads four bytes unsigned integer.
     */
    public long getUnsignedInt(int index) {
        return getInt(index) & 0xffffffffL;
    }

    /**
     * @see java.nio.ByteBuffer#putInt(int, int)
     */
    public abstract ByteBuffer putInt(int index, int value);

    /**
     * @see java.nio.ByteBuffer#asIntBuffer()
     */
    public abstract IntBuffer asIntBuffer();

    /**
     * @see java.nio.ByteBuffer#getLong()
     */
    public abstract long getLong();

    /**
     * @see java.nio.ByteBuffer#putLong(int, long)
     */
    public abstract ByteBuffer putLong(long value);

    /**
     * @see java.nio.ByteBuffer#getLong(int)
     */
    public abstract long getLong(int index);

    /**
     * @see java.nio.ByteBuffer#putLong(int, long)
     */
    public abstract ByteBuffer putLong(int index, long value);

    /**
     * @see java.nio.ByteBuffer#asLongBuffer()
     */
    public abstract LongBuffer asLongBuffer();

    /**
     * @see java.nio.ByteBuffer#getFloat()
     */
    public abstract float getFloat();

    /**
     * @see java.nio.ByteBuffer#putFloat(float)
     */
    public abstract ByteBuffer putFloat(float value);

    /**
     * @see java.nio.ByteBuffer#getFloat(int)
     */
    public abstract float getFloat(int index);

    /**
     * @see java.nio.ByteBuffer#putFloat(int, float)
     */
    public abstract ByteBuffer putFloat(int index, float value);

    /**
     * @see java.nio.ByteBuffer#asFloatBuffer()
     */
    public abstract FloatBuffer asFloatBuffer();

    /**
     * @see java.nio.ByteBuffer#getDouble()
     */
    public abstract double getDouble();

    /**
     * @see java.nio.ByteBuffer#putDouble(double)
     */
    public abstract ByteBuffer putDouble(double value);

    /**
     * @see java.nio.ByteBuffer#getDouble(int)
     */
    public abstract double getDouble(int index);

    /**
     * @see java.nio.ByteBuffer#putDouble(int, double)
     */
    public abstract ByteBuffer putDouble(int index, double value);

    /**
     * @see java.nio.ByteBuffer#asDoubleBuffer()
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
                return ByteBuffer.this.remaining();
            }

            @Override
            public synchronized void mark(int readlimit) {
                ByteBuffer.this.mark();
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int read() {
                if (ByteBuffer.this.hasRemaining()) {
                    return ByteBuffer.this.get() & 0xff;
                } else {
                    return -1;
                }
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int remaining = ByteBuffer.this.remaining();
                if (remaining > 0) {
                    int readBytes = Math.min(remaining, len);
                    ByteBuffer.this.get(b, off, readBytes);
                    return readBytes;
                } else {
                    return -1;
                }
            }

            @Override
            public synchronized void reset() {
                ByteBuffer.this.reset();
            }

            @Override
            public long skip(long n) {
                int bytes;
                if (n > Integer.MAX_VALUE) {
                    bytes = ByteBuffer.this.remaining();
                } else {
                    bytes = Math.min(ByteBuffer.this.remaining(), (int) n);
                }
                ByteBuffer.this.skip(bytes);
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
                ByteBuffer.this.put(b, off, len);
            }

            @Override
            public void write(int b) {
                ByteBuffer.this.put((byte) b);
            }
        };
    }

    /**
     * Returns hexdump of this buffer.
     */
    public String getHexDump() {
        return ByteBufferHexDumper.getHexdump(this);
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
            end = indexOf((byte)0x00);
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

        if (utf16 && ((fieldSize & 1) != 0)) {
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

    private int indexOf(byte b) {
        if (buf().hasArray()) {
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
    
    /**
     * Writes the content of <code>in</code> into this buffer using the
     * specified <code>encoder</code>.  This method doesn't terminate
     * string with <tt>NUL</tt>.  You have to do it by yourself.
     *
     * @throws BufferOverflowException if the specified string doesn't fit
     */
    public ByteBuffer putString(CharSequence val, CharsetEncoder encoder)
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
    public ByteBuffer putString(CharSequence val, int fieldSize,
            CharsetEncoder encoder) throws CharacterCodingException {
        checkFieldSize(fieldSize);

        if (fieldSize == 0)
            return this;

        autoExpand(fieldSize);

        boolean utf16 = encoder.charset().name().startsWith("UTF-16");

        if (utf16 && ((fieldSize & 1) != 0)) {
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

        if (utf16 && ((fieldSize & 1) != 0)) {
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
    public ByteBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder)
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
    public ByteBuffer putPrefixedString(CharSequence in, int prefixLength,
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
    public ByteBuffer putPrefixedString(CharSequence in, int prefixLength,
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
    public ByteBuffer putPrefixedString(CharSequence val, int prefixLength,
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
        fill(padValue, padding - ((position() - oldPos) & padMask));
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
                    int type = read();
                    if (type < 0) {
                        throw new EOFException();
                    }
                    switch (type) {
                    case 0: // Primitive types
                        return super.readClassDescriptor();
                    case 1: // Non-primitive types
                        String className = readUTF();
                        Class<?> clazz =
                            Class.forName(className, true, classLoader);
                        return ObjectStreamClass.lookup(clazz);
                    default:
                        throw new StreamCorruptedException(
                                "Unexpected class descriptor type: " + type);
                    }
                }
                
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    String name = desc.getName();
                    try {
                        return Class.forName(name, false, classLoader);
                    } catch (ClassNotFoundException ex) {
                        return super.resolveClass(desc);
                    }
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
    public ByteBuffer putObject(Object o) {
        int oldPos = position();
        skip(4); // Make a room for the length field.
        try {
            ObjectOutputStream out = new ObjectOutputStream(asOutputStream()) {
                @Override
                protected void writeClassDescriptor(ObjectStreamClass desc)
                        throws IOException {
                    String className = desc.getName();
                    if (primitiveTypeNames.contains(className)) {
                        write(0);
                        super.writeClassDescriptor(desc);
                    } else {
                        write(1);
                        writeUTF(desc.getName());
                    }
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

    //////////////////////////
    // Skip or fill methods //
    //////////////////////////

    /**
     * Forwards the position of this buffer as the specified <code>size</code>
     * bytes.
     */
    public ByteBuffer skip(int size) {
        autoExpand(size);
        return position(position() + size);
    }

    /**
     * Fills this buffer with the specified value.
     * This method moves buffer position forward.
     */
    public ByteBuffer fill(byte value, int size) {
        autoExpand(size);
        int q = size >>> 3;
        int r = size & 7;

        if (q > 0) {
            int intValue = value | (value << 8) | (value << 16) | (value << 24);
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
            int intValue = value | (value << 8) | (value << 16) | (value << 24);
            putInt(intValue);
        }

        q = r >> 1;
        r = r & 1;

        if (q > 0) {
            short shortValue = (short) (value | (value << 8));
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
    public ByteBuffer fillAndReset(byte value, int size) {
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
    public ByteBuffer fill(int size) {
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
    public ByteBuffer fillAndReset(int size) {
        autoExpand(size);
        int pos = position();
        try {
            fill(size);
        } finally {
            position(pos);
        }

        return this;
    }

    /**
     * This method forwards the call to {@link #expand(int)} only when
     * <tt>autoExpand</tt> property is <tt>true</tt>.
     */
    protected ByteBuffer autoExpand(int expectedRemaining) {
        if (isAutoExpand()) {
            expand(expectedRemaining);
        }
        return this;
    }

    /**
     * This method forwards the call to {@link #expand(int)} only when
     * <tt>autoExpand</tt> property is <tt>true</tt>.
     */
    protected ByteBuffer autoExpand(int pos, int expectedRemaining) {
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
