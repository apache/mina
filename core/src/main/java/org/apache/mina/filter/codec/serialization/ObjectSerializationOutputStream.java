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

import org.apache.mina.common.ByteBuffer;

/**
 * An {@link ObjectOutput} and {@link OutputStream} that can write the objects as
 * the serialized form that {@link ObjectSerializationDecoder} can decode.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ObjectSerializationOutputStream extends OutputStream implements
        ObjectOutput {

    private final DataOutputStream out;

    private int maxObjectSize = Integer.MAX_VALUE;

    public ObjectSerializationOutputStream(OutputStream out) {
        if (out == null) {
            throw new NullPointerException("out");
        }

        if (out instanceof DataOutputStream) {
            this.out = (DataOutputStream) out;
        } else {
            this.out = new DataOutputStream(out);
        }
    }

    /**
     * Returns the allowed maximum size of the encoded object.
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
     */
    public void setMaxObjectSize(int maxObjectSize) {
        if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: "
                    + maxObjectSize);
        }

        this.maxObjectSize = maxObjectSize;
    }

    public void close() throws IOException {
        out.close();
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    public void writeObject(Object obj) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(64, false);
        buf.setAutoExpand(true);
        buf.putObject(obj);

        int objectSize = buf.position() - 4;
        if (objectSize > maxObjectSize) {
            buf.release();
            throw new IllegalArgumentException(
                    "The encoded object is too big: " + objectSize + " (> "
                            + maxObjectSize + ')');
        }

        out.write(buf.array(), 0, buf.position());
        buf.release();
    }

    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    public void writeUTF(String str) throws IOException {
        out.writeUTF(str);
    }
}
