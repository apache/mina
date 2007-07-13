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
package org.apache.mina.handler.support;

import java.io.IOException;
import java.io.InputStream;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;

/**
 * An {@link InputStream} that buffers data read from
 * {@link IoHandler#messageReceived(IoSession, Object)} events.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoSessionInputStream extends InputStream {
    private final Object mutex = new Object();

    private final ByteBuffer buf;

    private volatile boolean closed;

    private volatile boolean released;

    private IOException exception;

    public IoSessionInputStream() {
        buf = ByteBuffer.allocate(16);
        buf.setAutoExpand(true);
        buf.limit(0);
    }

    public int available() {
        if (released) {
            return 0;
        } else {
            synchronized (mutex) {
                return buf.remaining();
            }
        }
    }

    public void close() {
        if (closed) {
            return;
        }

        synchronized (mutex) {
            closed = true;
            releaseBuffer();

            mutex.notifyAll();
        }
    }

    public int read() throws IOException {
        synchronized (mutex) {
            if (!waitForData()) {
                return -1;
            }

            return buf.get() & 0xff;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (mutex) {
            if (!waitForData()) {
                return -1;
            }

            int readBytes;

            if (len > buf.remaining()) {
                readBytes = buf.remaining();
            } else {
                readBytes = len;
            }

            buf.get(b, off, readBytes);

            return readBytes;
        }
    }

    private boolean waitForData() throws IOException {
        if (released) {
            return false;
        }

        synchronized (mutex) {
            while (!released && buf.remaining() == 0 && exception == null) {
                try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    IOException ioe = new IOException(
                            "Interrupted while waiting for more data");
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }

        if (exception != null) {
            releaseBuffer();
            throw exception;
        }

        if (closed && buf.remaining() == 0) {
            releaseBuffer();

            return false;
        }

        return true;
    }

    private void releaseBuffer() {
        if (released) {
            return;
        }

        released = true;
        buf.release();
    }

    public void write(ByteBuffer src) {
        synchronized (mutex) {
            if (closed) {
                return;
            }

            if (buf.hasRemaining()) {
                this.buf.compact();
                this.buf.put(src);
                this.buf.flip();
            } else {
                this.buf.clear();
                this.buf.put(src);
                this.buf.flip();
                mutex.notifyAll();
            }
        }
    }

    public void throwException(IOException e) {
        synchronized (mutex) {
            if (exception == null) {
                exception = e;

                mutex.notifyAll();
            }
        }
    }
}