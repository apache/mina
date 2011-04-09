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

import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
import java.util.EnumSet;
import java.util.Set;

/**
 * A {@link IoBuffer} that wraps a buffer and proxies any operations to it.
 * <p>
 * You can think this class like a {@link FilterOutputStream}.  All operations
 * are proxied by default so that you can extend this class and override existing
 * operations selectively.  You can introduce new operations, too.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoBufferWrapper extends IoBuffer {

    /**
     * The buffer proxied by this proxy.
     */
    private final IoBuffer buf;

    /**
     * Create a new instance.
     * @param buf the buffer to be proxied
     */
    protected IoBufferWrapper(IoBuffer buf) {
        if (buf == null) {
            throw new IllegalArgumentException("buf");
        }
        this.buf = buf;
    }
    
    /**
     * Returns the parent buffer that this buffer wrapped.
     */
    public IoBuffer getParentBuffer() {
        return buf;
    }

    @Override
    public boolean isDirect() {
        return buf.isDirect();
    }

    @Override
    public ByteBuffer buf() {
        return buf.buf();
    }

    @Override
    public int capacity() {
        return buf.capacity();
    }

    @Override
    public int position() {
        return buf.position();
    }

    @Override
    public IoBuffer position(int newPosition) {
        buf.position(newPosition);
        return this;
    }

    @Override
    public int limit() {
        return buf.limit();
    }

    @Override
    public IoBuffer limit(int newLimit) {
        buf.limit(newLimit);
        return this;
    }

    @Override
    public IoBuffer mark() {
        buf.mark();
        return this;
    }

    @Override
    public IoBuffer reset() {
        buf.reset();
        return this;
    }

    @Override
    public IoBuffer clear() {
        buf.clear();
        return this;
    }

    @Override
    public IoBuffer sweep() {
        buf.sweep();
        return this;
    }

    @Override
    public IoBuffer sweep(byte value) {
        buf.sweep(value);
        return this;
    }

    @Override
    public IoBuffer flip() {
        buf.flip();
        return this;
    }

    @Override
    public IoBuffer rewind() {
        buf.rewind();
        return this;
    }

    @Override
    public int remaining() {
        return buf.remaining();
    }

    @Override
    public boolean hasRemaining() {
        return buf.hasRemaining();
    }

    @Override
    public byte get() {
        return buf.get();
    }

    @Override
    public short getUnsigned() {
        return buf.getUnsigned();
    }

    @Override
    public IoBuffer put(byte b) {
        buf.put(b);
        return this;
    }

    @Override
    public byte get(int index) {
        return buf.get(index);
    }

    @Override
    public short getUnsigned(int index) {
        return buf.getUnsigned(index);
    }

    @Override
    public IoBuffer put(int index, byte b) {
        buf.put(index, b);
        return this;
    }

    @Override
    public IoBuffer get(byte[] dst, int offset, int length) {
        buf.get(dst, offset, length);
        return this;
    }

    @Override
    public IoBuffer getSlice(int index, int length) {
        return buf.getSlice(index, length);
    }

    @Override
    public IoBuffer getSlice(int length) {
        return buf.getSlice(length);
    }

    @Override
    public IoBuffer get(byte[] dst) {
        buf.get(dst);
        return this;
    }

    @Override
    public IoBuffer put(IoBuffer src) {
        buf.put(src);
        return this;
    }

    @Override
    public IoBuffer put(ByteBuffer src) {
        buf.put(src);
        return this;
    }

    @Override
    public IoBuffer put(byte[] src, int offset, int length) {
        buf.put(src, offset, length);
        return this;
    }

    @Override
    public IoBuffer put(byte[] src) {
        buf.put(src);
        return this;
    }

    @Override
    public IoBuffer compact() {
        buf.compact();
        return this;
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    @Override
    public int hashCode() {
        return buf.hashCode();
    }

    @Override
    public boolean equals(Object ob) {
        return buf.equals(ob);
    }

    public int compareTo(IoBuffer that) {
        return buf.compareTo(that);
    }

    @Override
    public ByteOrder order() {
        return buf.order();
    }

    @Override
    public IoBuffer order(ByteOrder bo) {
        buf.order(bo);
        return this;
    }

    @Override
    public char getChar() {
        return buf.getChar();
    }

    @Override
    public IoBuffer putChar(char value) {
        buf.putChar(value);
        return this;
    }

    @Override
    public char getChar(int index) {
        return buf.getChar(index);
    }

    @Override
    public IoBuffer putChar(int index, char value) {
        buf.putChar(index, value);
        return this;
    }

    @Override
    public CharBuffer asCharBuffer() {
        return buf.asCharBuffer();
    }

    @Override
    public short getShort() {
        return buf.getShort();
    }

    @Override
    public int getUnsignedShort() {
        return buf.getUnsignedShort();
    }

    @Override
    public IoBuffer putShort(short value) {
        buf.putShort(value);
        return this;
    }

    @Override
    public short getShort(int index) {
        return buf.getShort(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        return buf.getUnsignedShort(index);
    }

    @Override
    public IoBuffer putShort(int index, short value) {
        buf.putShort(index, value);
        return this;
    }

    @Override
    public ShortBuffer asShortBuffer() {
        return buf.asShortBuffer();
    }

    @Override
    public int getInt() {
        return buf.getInt();
    }

    @Override
    public long getUnsignedInt() {
        return buf.getUnsignedInt();
    }

    @Override
    public IoBuffer putInt(int value) {
        buf.putInt(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(byte value) {
        buf.putUnsignedInt(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(int index, byte value) {
        buf.putUnsignedInt(index, value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(short value) {
        buf.putUnsignedInt(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(int index, short value) {
        buf.putUnsignedInt(index, value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(int value) {
        buf.putUnsignedInt(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(int index, int value) {
        buf.putUnsignedInt(index, value);
        return this;
    }

    @Override
    public IoBuffer putUnsignedInt(long value) {
        buf.putUnsignedInt(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedInt(int index, long value) {
        buf.putUnsignedInt(index, value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(byte value) {
        buf.putUnsignedShort(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(int index, byte value) {
        buf.putUnsignedShort(index, value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(short value) {
        buf.putUnsignedShort(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(int index, short value) {
        buf.putUnsignedShort(index, value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(int value) {
        buf.putUnsignedShort(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(int index, int value) {
        buf.putUnsignedShort(index, value);
        return this;
    }

    @Override
    public IoBuffer putUnsignedShort(long value) {
        buf.putUnsignedShort(value);
        return this;
    }
    
    @Override
    public IoBuffer putUnsignedShort(int index, long value) {
        buf.putUnsignedShort(index, value);
        return this;
    }

    @Override
    public int getInt(int index) {
        return buf.getInt(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        return buf.getUnsignedInt(index);
    }

    @Override
    public IoBuffer putInt(int index, int value) {
        buf.putInt(index, value);
        return this;
    }

    @Override
    public IntBuffer asIntBuffer() {
        return buf.asIntBuffer();
    }

    @Override
    public long getLong() {
        return buf.getLong();
    }

    @Override
    public IoBuffer putLong(long value) {
        buf.putLong(value);
        return this;
    }

    @Override
    public long getLong(int index) {
        return buf.getLong(index);
    }

    @Override
    public IoBuffer putLong(int index, long value) {
        buf.putLong(index, value);
        return this;
    }

    @Override
    public LongBuffer asLongBuffer() {
        return buf.asLongBuffer();
    }

    @Override
    public float getFloat() {
        return buf.getFloat();
    }

    @Override
    public IoBuffer putFloat(float value) {
        buf.putFloat(value);
        return this;
    }

    @Override
    public float getFloat(int index) {
        return buf.getFloat(index);
    }

    @Override
    public IoBuffer putFloat(int index, float value) {
        buf.putFloat(index, value);
        return this;
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        return buf.asFloatBuffer();
    }

    @Override
    public double getDouble() {
        return buf.getDouble();
    }

    @Override
    public IoBuffer putDouble(double value) {
        buf.putDouble(value);
        return this;
    }

    @Override
    public double getDouble(int index) {
        return buf.getDouble(index);
    }

    @Override
    public IoBuffer putDouble(int index, double value) {
        buf.putDouble(index, value);
        return this;
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        return buf.asDoubleBuffer();
    }

    @Override
    public String getHexDump() {
        return buf.getHexDump();
    }

    @Override
    public String getString(int fieldSize, CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getString(fieldSize, decoder);
    }

    @Override
    public String getString(CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getString(decoder);
    }

    @Override
    public String getPrefixedString(CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getPrefixedString(decoder);
    }

    @Override
    public String getPrefixedString(int prefixLength, CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getPrefixedString(prefixLength, decoder);
    }

    @Override
    public IoBuffer putString(CharSequence in, int fieldSize,
            CharsetEncoder encoder) throws CharacterCodingException {
        buf.putString(in, fieldSize, encoder);
        return this;
    }

    @Override
    public IoBuffer putString(CharSequence in, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putString(in, encoder);
        return this;
    }

    @Override
    public IoBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, encoder);
        return this;
    }

    @Override
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
            CharsetEncoder encoder) throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, encoder);
        return this;
    }

    @Override
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
            int padding, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, padding, encoder);
        return this;
    }

    @Override
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength,
            int padding, byte padValue, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, padding, padValue, encoder);
        return this;
    }

    @Override
    public IoBuffer skip(int size) {
        buf.skip(size);
        return this;
    }

    @Override
    public IoBuffer fill(byte value, int size) {
        buf.fill(value, size);
        return this;
    }

    @Override
    public IoBuffer fillAndReset(byte value, int size) {
        buf.fillAndReset(value, size);
        return this;
    }

    @Override
    public IoBuffer fill(int size) {
        buf.fill(size);
        return this;
    }

    @Override
    public IoBuffer fillAndReset(int size) {
        buf.fillAndReset(size);
        return this;
    }

    @Override
    public boolean isAutoExpand() {
        return buf.isAutoExpand();
    }

    @Override
    public IoBuffer setAutoExpand(boolean autoExpand) {
        buf.setAutoExpand(autoExpand);
        return this;
    }

    @Override
    public IoBuffer expand(int pos, int expectedRemaining) {
        buf.expand(pos, expectedRemaining);
        return this;
    }

    @Override
    public IoBuffer expand(int expectedRemaining) {
        buf.expand(expectedRemaining);
        return this;
    }

    @Override
    public Object getObject() throws ClassNotFoundException {
        return buf.getObject();
    }

    @Override
    public Object getObject(ClassLoader classLoader)
            throws ClassNotFoundException {
        return buf.getObject(classLoader);
    }

    @Override
    public IoBuffer putObject(Object o) {
        buf.putObject(o);
        return this;
    }

    @Override
    public InputStream asInputStream() {
        return buf.asInputStream();
    }

    @Override
    public OutputStream asOutputStream() {
        return buf.asOutputStream();
    }

    @Override
    public IoBuffer duplicate() {
        return buf.duplicate();
    }

    @Override
    public IoBuffer slice() {
        return buf.slice();
    }

    @Override
    public IoBuffer asReadOnlyBuffer() {
        return buf.asReadOnlyBuffer();
    }

    @Override
    public byte[] array() {
        return buf.array();
    }

    @Override
    public int arrayOffset() {
        return buf.arrayOffset();
    }

    @Override
    public int minimumCapacity() {
        return buf.minimumCapacity();
    }

    @Override
    public IoBuffer minimumCapacity(int minimumCapacity) {
        buf.minimumCapacity(minimumCapacity);
        return this;
    }

    @Override
    public IoBuffer capacity(int newCapacity) {
        buf.capacity(newCapacity);
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return buf.isReadOnly();
    }

    @Override
    public int markValue() {
        return buf.markValue();
    }

    @Override
    public boolean hasArray() {
        return buf.hasArray();
    }

    @Override
    public void free() {
        buf.free();
    }

    @Override
    public boolean isDerived() {
        return buf.isDerived();
    }

    @Override
    public boolean isAutoShrink() {
        return buf.isAutoShrink();
    }

    @Override
    public IoBuffer setAutoShrink(boolean autoShrink) {
        buf.setAutoShrink(autoShrink);
        return this;
    }

    @Override
    public IoBuffer shrink() {
        buf.shrink();
        return this;
    }

    @Override
    public int getMediumInt() {
        return buf.getMediumInt();
    }

    @Override
    public int getUnsignedMediumInt() {
        return buf.getUnsignedMediumInt();
    }

    @Override
    public int getMediumInt(int index) {
        return buf.getMediumInt(index);
    }

    @Override
    public int getUnsignedMediumInt(int index) {
        return buf.getUnsignedMediumInt(index);
    }

    @Override
    public IoBuffer putMediumInt(int value) {
        buf.putMediumInt(value);
        return this;
    }

    @Override
    public IoBuffer putMediumInt(int index, int value) {
        buf.putMediumInt(index, value);
        return this;
    }

    @Override
    public String getHexDump(int lengthLimit) {
        return buf.getHexDump(lengthLimit);
    }

    @Override
    public boolean prefixedDataAvailable(int prefixLength) {
        return buf.prefixedDataAvailable(prefixLength);
    }

    @Override
    public boolean prefixedDataAvailable(int prefixLength, int maxDataLength) {
        return buf.prefixedDataAvailable(prefixLength, maxDataLength);
    }

    @Override
    public int indexOf(byte b) {
        return buf.indexOf(b);
    }

    @Override
    public <E extends Enum<E>> E getEnum(Class<E> enumClass) {
        return buf.getEnum(enumClass);
    }

    @Override
    public <E extends Enum<E>> E getEnum(int index, Class<E> enumClass) {
        return buf.getEnum(index, enumClass);
    }

    @Override
    public <E extends Enum<E>> E getEnumShort(Class<E> enumClass) {
        return buf.getEnumShort(enumClass);
    }

    @Override
    public <E extends Enum<E>> E getEnumShort(int index, Class<E> enumClass) {
        return buf.getEnumShort(index, enumClass);
    }

    @Override
    public <E extends Enum<E>> E getEnumInt(Class<E> enumClass) {
        return buf.getEnumInt(enumClass);
    }

    @Override
    public <E extends Enum<E>> E getEnumInt(int index, Class<E> enumClass) {
        return buf.getEnumInt(index, enumClass);
    }

    @Override
    public IoBuffer putEnum(Enum<?> e) {
        buf.putEnum(e);
        return this;
    }

    @Override
    public IoBuffer putEnum(int index, Enum<?> e) {
        buf.putEnum(index, e);
        return this;
    }

    @Override
    public IoBuffer putEnumShort(Enum<?> e) {
        buf.putEnumShort(e);
        return this;
    }

    @Override
    public IoBuffer putEnumShort(int index, Enum<?> e) {
        buf.putEnumShort(index, e);
        return this;
    }

    @Override
    public IoBuffer putEnumInt(Enum<?> e) {
        buf.putEnumInt(e);
        return this;
    }

    @Override
    public IoBuffer putEnumInt(int index, Enum<?> e) {
        buf.putEnumInt(index, e);
        return this;
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSet(Class<E> enumClass) {
        return buf.getEnumSet(enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSet(int index, Class<E> enumClass) {
        return buf.getEnumSet(index, enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetShort(Class<E> enumClass) {
        return buf.getEnumSetShort(enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetShort(int index, Class<E> enumClass) {
        return buf.getEnumSetShort(index, enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetInt(Class<E> enumClass) {
        return buf.getEnumSetInt(enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetInt(int index, Class<E> enumClass) {
        return buf.getEnumSetInt(index, enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetLong(Class<E> enumClass) {
        return buf.getEnumSetLong(enumClass);
    }

    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetLong(int index, Class<E> enumClass) {
        return buf.getEnumSetLong(index, enumClass);
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSet(Set<E> set) {
        buf.putEnumSet(set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSet(int index, Set<E> set) {
        buf.putEnumSet(index, set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetShort(Set<E> set) {
        buf.putEnumSetShort(set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetShort(int index, Set<E> set) {
        buf.putEnumSetShort(index, set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetInt(Set<E> set) {
        buf.putEnumSetInt(set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetInt(int index, Set<E> set) {
        buf.putEnumSetInt(index, set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetLong(Set<E> set) {
        buf.putEnumSetLong(set);
        return this;
    }

    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetLong(int index, Set<E> set) {
        buf.putEnumSetLong(index, set);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(byte value) {
        buf.putUnsigned(value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(int index, byte value) {
        buf.putUnsigned(index, value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(short value) {
        buf.putUnsigned(value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(int index, short value) {
        buf.putUnsigned(index, value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(int value) {
        buf.putUnsigned(value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(int index, int value) {
        buf.putUnsigned(index, value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(long value) {
        buf.putUnsigned(value);
        return this;
    }

    @Override
    public IoBuffer putUnsigned(int index, long value) {
        buf.putUnsigned(index, value);
        return this;
    }
}
