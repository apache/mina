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
package org.apache.mina.util;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * {@link OutputStream} wrapper for {@link ByteBuffer}
 * 
 * <p>
 * <i>Currently this class is only used and available in MINA's codec module</i>
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer buffer;

    private boolean elastic = false;

    public ByteBufferOutputStream() {
        this(1024);
    }

    private ByteBufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBufferOutputStream(int initialSize) {
        this(ByteBuffer.allocate(initialSize));
    }

    public ByteBuffer getByteBuffer() {
        ByteBuffer out = buffer.asReadOnlyBuffer();
        out.limit(out.position());
        out.position(0);

        return out;
    }

    public boolean isElastic() {
        return elastic;
    }

    private void needSpace(int len) {
        if (elastic && buffer.capacity() - buffer.position() < len) {
            ByteBuffer newBuffer = ByteBuffer.allocate(Math.max(buffer.capacity() * 2, buffer.position() + len));
            buffer.limit(buffer.position());
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
    }

    public void setElastic(boolean elastic) {
        this.elastic = elastic;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        needSpace(len);
        buffer.put(b, off, len);
    }

    @Override
    public void write(int b) {
        needSpace(1);
        buffer.put((byte) b);
    }
}
