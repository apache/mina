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
     * @return the parent buffer that this buffer wrapped.
     */
    public IoBuffer getParentBuffer() {
        return buf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirect() {
        return buf.isDirect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer buf() {
        return buf.buf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {
        return buf.capacity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int position() {
        return buf.position();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer position(int newPosition) {
        buf.position(newPosition);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int limit() {
        return buf.limit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer limit(int newLimit) {
        buf.limit(newLimit);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer mark() {
        buf.mark();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer reset() {
        buf.reset();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer clear() {
        buf.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer sweep() {
        buf.sweep();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer sweep(byte value) {
        buf.sweep(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer flip() {
        buf.flip();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer rewind() {
        buf.rewind();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remaining() {
        return buf.remaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRemaining() {
        return buf.hasRemaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte get() {
        return buf.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getUnsigned() {
        return buf.getUnsigned();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer put(byte b) {
        buf.put(b);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte get(int index) {
        return buf.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getUnsigned(int index) {
        return buf.getUnsigned(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer put(int index, byte b) {
        buf.put(index, b);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer get(byte[] dst, int offset, int length) {
        buf.get(dst, offset, length);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer getSlice(int index, int length) {
        return buf.getSlice(index, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer getSlice(int length) {
        return buf.getSlice(length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer get(byte[] dst) {
        buf.get(dst);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer put(IoBuffer src) {
        buf.put(src);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer put(ByteBuffer src) {
        buf.put(src);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer put(byte[] src, int offset, int length) {
        buf.put(src, offset, length);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer put(byte[] src) {
        buf.put(src);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer compact() {
        buf.compact();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return buf.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object ob) {
        return buf.equals(ob);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(IoBuffer that) {
        return buf.compareTo(that);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteOrder order() {
        return buf.order();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer order(ByteOrder bo) {
        buf.order(bo);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getChar() {
        return buf.getChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putChar(char value) {
        buf.putChar(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getChar(int index) {
        return buf.getChar(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putChar(int index, char value) {
        buf.putChar(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharBuffer asCharBuffer() {
        return buf.asCharBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort() {
        return buf.getShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnsignedShort() {
        return buf.getUnsignedShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putShort(short value) {
        buf.putShort(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort(int index) {
        return buf.getShort(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnsignedShort(int index) {
        return buf.getUnsignedShort(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putShort(int index, short value) {
        buf.putShort(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShortBuffer asShortBuffer() {
        return buf.asShortBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt() {
        return buf.getInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUnsignedInt() {
        return buf.getUnsignedInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putInt(int value) {
        buf.putInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(byte value) {
        buf.putUnsignedInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(int index, byte value) {
        buf.putUnsignedInt(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(short value) {
        buf.putUnsignedInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(int index, short value) {
        buf.putUnsignedInt(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(int value) {
        buf.putUnsignedInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(int index, int value) {
        buf.putUnsignedInt(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(long value) {
        buf.putUnsignedInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedInt(int index, long value) {
        buf.putUnsignedInt(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(byte value) {
        buf.putUnsignedShort(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(int index, byte value) {
        buf.putUnsignedShort(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(short value) {
        buf.putUnsignedShort(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(int index, short value) {
        buf.putUnsignedShort(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(int value) {
        buf.putUnsignedShort(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(int index, int value) {
        buf.putUnsignedShort(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(long value) {
        buf.putUnsignedShort(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsignedShort(int index, long value) {
        buf.putUnsignedShort(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(int index) {
        return buf.getInt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUnsignedInt(int index) {
        return buf.getUnsignedInt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putInt(int index, int value) {
        buf.putInt(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntBuffer asIntBuffer() {
        return buf.asIntBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong() {
        return buf.getLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putLong(long value) {
        buf.putLong(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(int index) {
        return buf.getLong(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putLong(int index, long value) {
        buf.putLong(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LongBuffer asLongBuffer() {
        return buf.asLongBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat() {
        return buf.getFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putFloat(float value) {
        buf.putFloat(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(int index) {
        return buf.getFloat(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putFloat(int index, float value) {
        buf.putFloat(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FloatBuffer asFloatBuffer() {
        return buf.asFloatBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble() {
        return buf.getDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putDouble(double value) {
        buf.putDouble(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(int index) {
        return buf.getDouble(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putDouble(int index, double value) {
        buf.putDouble(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DoubleBuffer asDoubleBuffer() {
        return buf.asDoubleBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHexDump() {
        return buf.getHexDump();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(int fieldSize, CharsetDecoder decoder) throws CharacterCodingException {
        return buf.getString(fieldSize, decoder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(CharsetDecoder decoder) throws CharacterCodingException {
        return buf.getString(decoder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefixedString(CharsetDecoder decoder) throws CharacterCodingException {
        return buf.getPrefixedString(decoder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefixedString(int prefixLength, CharsetDecoder decoder) throws CharacterCodingException {
        return buf.getPrefixedString(prefixLength, decoder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putString(CharSequence in, int fieldSize, CharsetEncoder encoder) throws CharacterCodingException {
        buf.putString(in, fieldSize, encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putString(CharSequence in, CharsetEncoder encoder) throws CharacterCodingException {
        buf.putString(in, encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder) throws CharacterCodingException {
        buf.putPrefixedString(in, encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength, int padding, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, padding, encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putPrefixedString(CharSequence in, int prefixLength, int padding, byte padValue,
            CharsetEncoder encoder) throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, padding, padValue, encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer skip(int size) {
        buf.skip(size);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer fill(byte value, int size) {
        buf.fill(value, size);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer fillAndReset(byte value, int size) {
        buf.fillAndReset(value, size);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer fill(int size) {
        buf.fill(size);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer fillAndReset(int size) {
        buf.fillAndReset(size);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutoExpand() {
        return buf.isAutoExpand();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer setAutoExpand(boolean autoExpand) {
        buf.setAutoExpand(autoExpand);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer expand(int pos, int expectedRemaining) {
        buf.expand(pos, expectedRemaining);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer expand(int expectedRemaining) {
        buf.expand(expectedRemaining);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject() throws ClassNotFoundException {
        return buf.getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject(ClassLoader classLoader) throws ClassNotFoundException {
        return buf.getObject(classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putObject(Object o) {
        buf.putObject(o);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream asInputStream() {
        return buf.asInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream asOutputStream() {
        return buf.asOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer duplicate() {
        return buf.duplicate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer slice() {
        return buf.slice();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer asReadOnlyBuffer() {
        return buf.asReadOnlyBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] array() {
        return buf.array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arrayOffset() {
        return buf.arrayOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int minimumCapacity() {
        return buf.minimumCapacity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer minimumCapacity(int minimumCapacity) {
        buf.minimumCapacity(minimumCapacity);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer capacity(int newCapacity) {
        buf.capacity(newCapacity);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return buf.isReadOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int markValue() {
        return buf.markValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasArray() {
        return buf.hasArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void free() {
        buf.free();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDerived() {
        return buf.isDerived();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutoShrink() {
        return buf.isAutoShrink();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer setAutoShrink(boolean autoShrink) {
        buf.setAutoShrink(autoShrink);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer shrink() {
        buf.shrink();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMediumInt() {
        return buf.getMediumInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnsignedMediumInt() {
        return buf.getUnsignedMediumInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMediumInt(int index) {
        return buf.getMediumInt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnsignedMediumInt(int index) {
        return buf.getUnsignedMediumInt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putMediumInt(int value) {
        buf.putMediumInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putMediumInt(int index, int value) {
        buf.putMediumInt(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHexDump(int lengthLimit) {
        return buf.getHexDump(lengthLimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean prefixedDataAvailable(int prefixLength) {
        return buf.prefixedDataAvailable(prefixLength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean prefixedDataAvailable(int prefixLength, int maxDataLength) {
        return buf.prefixedDataAvailable(prefixLength, maxDataLength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(byte b) {
        return buf.indexOf(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> E getEnum(Class<E> enumClass) {
        return buf.getEnum(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> E getEnum(int index, Class<E> enumClass) {
        return buf.getEnum(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> E getEnumShort(Class<E> enumClass) {
        return buf.getEnumShort(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> E getEnumShort(int index, Class<E> enumClass) {
        return buf.getEnumShort(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> E getEnumInt(Class<E> enumClass) {
        return buf.getEnumInt(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> E getEnumInt(int index, Class<E> enumClass) {
        return buf.getEnumInt(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putEnum(Enum<?> e) {
        buf.putEnum(e);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putEnum(int index, Enum<?> e) {
        buf.putEnum(index, e);
        return this;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putEnumInt(Enum<?> e) {
        buf.putEnumInt(e);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putEnumInt(int index, Enum<?> e) {
        buf.putEnumInt(index, e);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSet(Class<E> enumClass) {
        return buf.getEnumSet(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSet(int index, Class<E> enumClass) {
        return buf.getEnumSet(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetShort(Class<E> enumClass) {
        return buf.getEnumSetShort(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetShort(int index, Class<E> enumClass) {
        return buf.getEnumSetShort(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetInt(Class<E> enumClass) {
        return buf.getEnumSetInt(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetInt(int index, Class<E> enumClass) {
        return buf.getEnumSetInt(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetLong(Class<E> enumClass) {
        return buf.getEnumSetLong(enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> EnumSet<E> getEnumSetLong(int index, Class<E> enumClass) {
        return buf.getEnumSetLong(index, enumClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSet(Set<E> set) {
        buf.putEnumSet(set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSet(int index, Set<E> set) {
        buf.putEnumSet(index, set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetShort(Set<E> set) {
        buf.putEnumSetShort(set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetShort(int index, Set<E> set) {
        buf.putEnumSetShort(index, set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetInt(Set<E> set) {
        buf.putEnumSetInt(set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetInt(int index, Set<E> set) {
        buf.putEnumSetInt(index, set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetLong(Set<E> set) {
        buf.putEnumSetLong(set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> IoBuffer putEnumSetLong(int index, Set<E> set) {
        buf.putEnumSetLong(index, set);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(byte value) {
        buf.putUnsigned(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(int index, byte value) {
        buf.putUnsigned(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(short value) {
        buf.putUnsigned(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(int index, short value) {
        buf.putUnsigned(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(int value) {
        buf.putUnsigned(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(int index, int value) {
        buf.putUnsigned(index, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(long value) {
        buf.putUnsigned(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer putUnsigned(int index, long value) {
        buf.putUnsigned(index, value);
        return this;
    }
}
