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

import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

/**
 * A {@link ByteBuffer} that wraps a buffer and proxies any operations to it.
 * <p>
 * You can think this class like a {@link FilterOutputStream}.  All operations
 * are proxied by default so that you can extend this class and override existing
 * operations selectively.  You can introduce new operations, too.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ByteBufferProxy extends ByteBuffer {

    /**
     * The buffer proxied by this proxy.
     */
    protected ByteBuffer buf;

    /**
     * Create a new instance.
     * @param buf the buffer to be proxied
     */
    protected ByteBufferProxy(ByteBuffer buf) {
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        this.buf = buf;
    }

    public void acquire() {
        buf.acquire();
    }

    public void release() {
        buf.release();
    }

    public boolean isDirect() {
        return buf.isDirect();
    }

    public java.nio.ByteBuffer buf() {
        return buf.buf();
    }

    public int capacity() {
        return buf.capacity();
    }

    public int position() {
        return buf.position();
    }

    public ByteBuffer position(int newPosition) {
        buf.position(newPosition);
        return this;
    }

    public int limit() {
        return buf.limit();
    }

    public ByteBuffer limit(int newLimit) {
        buf.limit(newLimit);
        return this;
    }

    public ByteBuffer mark() {
        buf.mark();
        return this;
    }

    public ByteBuffer reset() {
        buf.reset();
        return this;
    }

    public ByteBuffer clear() {
        buf.clear();
        return this;
    }

    public ByteBuffer sweep() {
        buf.sweep();
        return this;
    }

    public ByteBuffer sweep(byte value) {
        buf.sweep(value);
        return this;
    }

    public ByteBuffer flip() {
        buf.flip();
        return this;
    }

    public ByteBuffer rewind() {
        buf.rewind();
        return this;
    }

    public int remaining() {
        return buf.remaining();
    }

    public boolean hasRemaining() {
        return buf.hasRemaining();
    }

    public byte get() {
        return buf.get();
    }

    public short getUnsigned() {
        return buf.getUnsigned();
    }

    public ByteBuffer put(byte b) {
        buf.put(b);
        return this;
    }

    public byte get(int index) {
        return buf.get(index);
    }

    public short getUnsigned(int index) {
        return buf.getUnsigned(index);
    }

    public ByteBuffer put(int index, byte b) {
        buf.put(index, b);
        return this;
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {
        buf.get(dst, offset, length);
        return this;
    }

    public ByteBuffer get(byte[] dst) {
        buf.get(dst);
        return this;
    }

    public ByteBuffer put(ByteBuffer src) {
        buf.put(src);
        return this;
    }

    public ByteBuffer put(java.nio.ByteBuffer src) {
        buf.put(src);
        return this;
    }

    public ByteBuffer put(byte[] src, int offset, int length) {
        buf.put(src, offset, length);
        return this;
    }

    public ByteBuffer put(byte[] src) {
        buf.put(src);
        return this;
    }

    public ByteBuffer compact() {
        buf.compact();
        return this;
    }

    public String toString() {
        return buf.toString();
    }

    public int hashCode() {
        return buf.hashCode();
    }

    public boolean equals(Object ob) {
        return buf.equals(ob);
    }

    public int compareTo(ByteBuffer that) {
        return buf.compareTo(that);
    }

    public ByteOrder order() {
        return buf.order();
    }

    public ByteBuffer order(ByteOrder bo) {
        buf.order(bo);
        return this;
    }

    public char getChar() {
        return buf.getChar();
    }

    public ByteBuffer putChar(char value) {
        buf.putChar(value);
        return this;
    }

    public char getChar(int index) {
        return buf.getChar(index);
    }

    public ByteBuffer putChar(int index, char value) {
        buf.putChar(index, value);
        return this;
    }

    public CharBuffer asCharBuffer() {
        return buf.asCharBuffer();
    }

    public short getShort() {
        return buf.getShort();
    }

    public int getUnsignedShort() {
        return buf.getUnsignedShort();
    }

    public ByteBuffer putShort(short value) {
        buf.putShort(value);
        return this;
    }

    public short getShort(int index) {
        return buf.getShort(index);
    }

    public int getUnsignedShort(int index) {
        return buf.getUnsignedShort(index);
    }

    public ByteBuffer putShort(int index, short value) {
        buf.putShort(index, value);
        return this;
    }

    public ShortBuffer asShortBuffer() {
        return buf.asShortBuffer();
    }

    public int getInt() {
        return buf.getInt();
    }

    public long getUnsignedInt() {
        return buf.getUnsignedInt();
    }

    public ByteBuffer putInt(int value) {
        buf.putInt(value);
        return this;
    }

    public int getInt(int index) {
        return buf.getInt(index);
    }

    public long getUnsignedInt(int index) {
        return buf.getUnsignedInt(index);
    }

    public ByteBuffer putInt(int index, int value) {
        buf.putInt(index, value);
        return this;
    }

    public IntBuffer asIntBuffer() {
        return buf.asIntBuffer();
    }

    public long getLong() {
        return buf.getLong();
    }

    public ByteBuffer putLong(long value) {
        buf.putLong(value);
        return this;
    }

    public long getLong(int index) {
        return buf.getLong(index);
    }

    public ByteBuffer putLong(int index, long value) {
        buf.putLong(index, value);
        return this;
    }

    public LongBuffer asLongBuffer() {
        return buf.asLongBuffer();
    }

    public float getFloat() {
        return buf.getFloat();
    }

    public ByteBuffer putFloat(float value) {
        buf.putFloat(value);
        return this;
    }

    public float getFloat(int index) {
        return buf.getFloat(index);
    }

    public ByteBuffer putFloat(int index, float value) {
        buf.putFloat(index, value);
        return this;
    }

    public FloatBuffer asFloatBuffer() {
        return buf.asFloatBuffer();
    }

    public double getDouble() {
        return buf.getDouble();
    }

    public ByteBuffer putDouble(double value) {
        buf.putDouble(value);
        return this;
    }

    public double getDouble(int index) {
        return buf.getDouble(index);
    }

    public ByteBuffer putDouble(int index, double value) {
        buf.putDouble(index, value);
        return this;
    }

    public DoubleBuffer asDoubleBuffer() {
        return buf.asDoubleBuffer();
    }

    public String getHexDump() {
        return buf.getHexDump();
    }

    public String getString(int fieldSize, CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getString(fieldSize, decoder);
    }

    public String getString(CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getString(decoder);
    }

    public String getPrefixedString(CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getPrefixedString(decoder);
    }

    public String getPrefixedString(int prefixLength, CharsetDecoder decoder)
            throws CharacterCodingException {
        return buf.getPrefixedString(prefixLength, decoder);
    }

    public ByteBuffer putString(CharSequence in, int fieldSize,
            CharsetEncoder encoder) throws CharacterCodingException {
        buf.putString(in, fieldSize, encoder);
        return this;
    }

    public ByteBuffer putString(CharSequence in, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putString(in, encoder);
        return this;
    }

    public ByteBuffer putPrefixedString(CharSequence in, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, encoder);
        return this;
    }

    public ByteBuffer putPrefixedString(CharSequence in, int prefixLength,
            CharsetEncoder encoder) throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, encoder);
        return this;
    }

    public ByteBuffer putPrefixedString(CharSequence in, int prefixLength,
            int padding, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, padding, encoder);
        return this;
    }

    public ByteBuffer putPrefixedString(CharSequence in, int prefixLength,
            int padding, byte padValue, CharsetEncoder encoder)
            throws CharacterCodingException {
        buf.putPrefixedString(in, prefixLength, padding, padValue, encoder);
        return this;
    }

    public ByteBuffer skip(int size) {
        buf.skip(size);
        return this;
    }

    public ByteBuffer fill(byte value, int size) {
        buf.fill(value, size);
        return this;
    }

    public ByteBuffer fillAndReset(byte value, int size) {
        buf.fillAndReset(value, size);
        return this;
    }

    public ByteBuffer fill(int size) {
        buf.fill(size);
        return this;
    }

    public ByteBuffer fillAndReset(int size) {
        buf.fillAndReset(size);
        return this;
    }

    public boolean isAutoExpand() {
        return buf.isAutoExpand();
    }

    public ByteBuffer setAutoExpand(boolean autoExpand) {
        buf.setAutoExpand(autoExpand);
        return this;
    }

    public ByteBuffer expand(int pos, int expectedRemaining) {
        buf.expand(pos, expectedRemaining);
        return this;
    }

    public ByteBuffer expand(int expectedRemaining) {
        buf.expand(expectedRemaining);
        return this;
    }

    public boolean isPooled() {
        return buf.isPooled();
    }

    public void setPooled(boolean pooled) {
        buf.setPooled(pooled);
    }

    public Object getObject() throws ClassNotFoundException {
        return buf.getObject();
    }

    public Object getObject(ClassLoader classLoader)
            throws ClassNotFoundException {
        return buf.getObject(classLoader);
    }

    public ByteBuffer putObject(Object o) {
        buf.putObject(o);
        return this;
    }

    public InputStream asInputStream() {
        return buf.asInputStream();
    }

    public OutputStream asOutputStream() {
        return buf.asOutputStream();
    }

    public ByteBuffer duplicate() {
        return buf.duplicate();
    }

    public ByteBuffer slice() {
        return buf.slice();
    }

    public ByteBuffer asReadOnlyBuffer() {
        return buf.asReadOnlyBuffer();
    }

    public byte[] array() {
        return buf.array();
    }

    public int arrayOffset() {
        return buf.arrayOffset();
    }

    public ByteBuffer capacity(int newCapacity) {
        buf.capacity(newCapacity);
        return this;
    }

    public boolean isReadOnly() {
        return buf.isReadOnly();
    }

    public int markValue() {
        return buf.markValue();
    }
}
