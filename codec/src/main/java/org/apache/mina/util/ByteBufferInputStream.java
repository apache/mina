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

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * {@link InputStream} wrapper for {@link ByteBuffer}
 * 
 * <p>
 * <i>Currently this class is only used and available in MINA's codec module</i>
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class ByteBufferInputStream extends InputStream {

    private final ByteBuffer buffer;

    /**
     * 
     * @param buffer
     *            a buffer to be wrapped
     */
    public ByteBufferInputStream(ByteBuffer buffer) {
        super();
        this.buffer = buffer;
    }

    /**
     * @see InputStream#available()
     */
    @Override
    public int available() {
        return buffer.remaining();
    }

    /**
     * @see InputStream#mark(int)
     */
    @Override
    public synchronized void mark(int readlimit) {
        buffer.mark();
    }

    /**
     * @see InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * @see InputStream#read()
     */
    @Override
    public int read() {
        if (buffer.hasRemaining()) {
            return buffer.get() & 0xff;
        }

        return -1;
    }

    /**
     * @see InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) {
        int remaining = buffer.remaining();
        if (remaining > 0) {
            int readBytes = Math.min(remaining, len);
            buffer.get(b, off, readBytes);
            return readBytes;
        }

        return -1;
    }

    /**
     * @see InputStream#reset()
     */
    @Override
    public synchronized void reset() {
        buffer.reset();
    }

    /**
     * 
     * @param n
     *            the number of bytes to skip (values bigger than
     *            {@link Integer#MAX_VALUE} push the reading head to the end).
     * 
     * @see InputStream#skip(long)
     */
    @Override
    public long skip(long n) {
        int bytes;
        if (n > Integer.MAX_VALUE) {
            bytes = buffer.remaining();
        } else {
            bytes = Math.min(buffer.remaining(), (int) n);
        }
        buffer.position(buffer.position() + bytes);

        return bytes;
    }

}
