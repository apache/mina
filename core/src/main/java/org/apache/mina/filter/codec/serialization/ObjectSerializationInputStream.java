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

import org.apache.mina.common.BufferDataException;
import org.apache.mina.common.ByteBuffer;

/**
 * An {@link ObjectInput} and {@link InputStream} that can read the objects encoded
 * by {@link ObjectSerializationEncoder}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ObjectSerializationInputStream extends InputStream implements
        ObjectInput {

    private final DataInputStream in;

    private final ClassLoader classLoader;

    private int maxObjectSize = 1048576;

    public ObjectSerializationInputStream(InputStream in) {
        this(in, null);
    }

    public ObjectSerializationInputStream(InputStream in,
            ClassLoader classLoader) {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        if (in instanceof DataInputStream) {
            this.in = (DataInputStream) in;
        } else {
            this.in = new DataInputStream(in);
        }

        this.classLoader = classLoader;
    }

    /**
     * Returns the allowed maximum size of the object to be decoded.
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
     */
    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: "
                    + maxObjectSize);
        }

        this.maxObjectSize = maxObjectSize;
    }

    public int read() throws IOException {
        return in.read();
    }

    public Object readObject() throws ClassNotFoundException, IOException {
        int objectSize = in.readInt();
        if (objectSize <= 0) {
            throw new StreamCorruptedException("Invalid objectSize: "
                    + objectSize);
        }
        if (objectSize > maxObjectSize) {
            throw new StreamCorruptedException("ObjectSize too big: "
                    + objectSize + " (expected: <= " + maxObjectSize + ')');
        }

        ByteBuffer buf = ByteBuffer.allocate(objectSize + 4, false);
        buf.putInt(objectSize);
        in.readFully(buf.array(), 4, objectSize);
        buf.position(0);
        buf.limit(objectSize + 4);

        Object answer = buf.getObject(classLoader);
        buf.release();
        return answer;
    }

    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public char readChar() throws IOException {
        return in.readChar();
    }

    public double readDouble() throws IOException {
        return in.readDouble();
    }

    public float readFloat() throws IOException {
        return in.readFloat();
    }

    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    /**
     * @see DataInput#readLine()
     * @deprecated
     */
    public String readLine() throws IOException {
        return in.readLine();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public short readShort() throws IOException {
        return in.readShort();
    }

    public String readUTF() throws IOException {
        return in.readUTF();
    }

    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    public int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }
}
