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
package org.apache.mina.filter.codec.serialization;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;

/**
 * An {@link ObjectInput} and {@link InputStream} that can read the objects encoded
 * by {@link ObjectSerializationEncoder}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ObjectSerializationInputStream extends InputStream implements ObjectInput {

    private final DataInputStream in;

    private final ClassLoader classLoader;

    private int maxObjectSize = 1048576;

    /**
     * Create a new instance of an ObjectSerializationInputStream
     * @param in The {@link InputStream} to use
     */
    public ObjectSerializationInputStream(InputStream in) {
        this(in, null);
    }

    /**
     * Create a new instance of an ObjectSerializationInputStream
     * @param in The {@link InputStream} to use
     * @param classLoader The class loader to use
     */
    public ObjectSerializationInputStream(InputStream in, ClassLoader classLoader) {
        if (in == null) {
            throw new IllegalArgumentException("in");
        }
        
        if (classLoader == null) {
            this.classLoader = Thread.currentThread().getContextClassLoader();
        } else {
            this.classLoader = classLoader;
        }

        if (in instanceof DataInputStream) {
            this.in = (DataInputStream) in;
        } else {
            this.in = new DataInputStream(in);
        }
    }

    /**
     * @return the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     */
    public int getMaxObjectSize() {
        return maxObjectSize;
    }

    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1048576</tt> (1MB).
     * 
     * @param maxObjectSize The maximum decoded object size
     */
    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize);
        }

        this.maxObjectSize = maxObjectSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        return in.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        int objectSize = in.readInt();
        if (objectSize <= 0) {
            throw new StreamCorruptedException("Invalid objectSize: " + objectSize);
        }
        if (objectSize > maxObjectSize) {
            throw new StreamCorruptedException("ObjectSize too big: " + objectSize + " (expected: <= " + maxObjectSize
                    + ')');
        }

        IoBuffer buf = IoBuffer.allocate(objectSize + 4, false);
        buf.putInt(objectSize);
        in.readFully(buf.array(), 4, objectSize);
        buf.position(0);
        buf.limit(objectSize + 4);

        return buf.getObject(classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() throws IOException {
        return in.readChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() throws IOException {
        return in.readInt();
    }

    /**
     * @see DataInput#readLine()
     * @deprecated Bytes are not properly converted to chars
     */
    @Deprecated
    @Override
    public String readLine() throws IOException {
        return in.readLine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() throws IOException {
        return in.readLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() throws IOException {
        return in.readShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readUTF() throws IOException {
        return in.readUTF();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }
}
