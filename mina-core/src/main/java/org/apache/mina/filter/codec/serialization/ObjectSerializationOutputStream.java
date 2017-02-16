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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * An {@link ObjectOutput} and {@link OutputStream} that can write the objects as
 * the serialized form that {@link ObjectSerializationDecoder} can decode.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ObjectSerializationOutputStream extends OutputStream implements ObjectOutput {

    private final DataOutputStream out;

    private int maxObjectSize = Integer.MAX_VALUE;

    /**
     * Create a new instance of an ObjectSerializationOutputStream
     * @param out The {@link OutputStream} to use
     */
    public ObjectSerializationOutputStream(OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("out");
        }

        if (out instanceof DataOutputStream) {
            this.out = (DataOutputStream) out;
        } else {
            this.out = new DataOutputStream(out);
        }
    }

    /**
     * @return the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, this encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public int getMaxObjectSize() {
        return maxObjectSize;
    }

    /**
     * Sets the allowed maximum size of the encoded object.
     * If the size of the encoded object exceeds this value, this encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * 
     * @param maxObjectSize The maximum object size
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
    public void close() throws IOException {
        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object obj) throws IOException {
        IoBuffer buf = IoBuffer.allocate(64, false);
        buf.setAutoExpand(true);
        buf.putObject(obj);

        int objectSize = buf.position() - 4;
        if (objectSize > maxObjectSize) {
            throw new IllegalArgumentException("The encoded object is too big: " + objectSize + " (> " + maxObjectSize
                    + ')');
        }

        out.write(buf.array(), 0, buf.position());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTF(String str) throws IOException {
        out.writeUTF(str);
    }
}
