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

import java.nio.ByteOrder;

import org.apache.mina.common.support.BaseByteBuffer;
import org.apache.mina.util.ExpiringStack;

/**
 * A {@link ByteBufferAllocator} which pools allocated buffers. <p> All buffers are allocated with the size of power of
 * 2 (e.g. 16, 32, 64, ...) This means that you cannot simply assume that the actual capacity of the buffer and the
 * capacity you requested are same. </p> <p> This allocator releases the buffers which have not been in use for a
 * certain period.  You can adjust the period by calling {@link #setTimeout(int)}. The default timeout is 1 minute (60
 * seconds).  To release these buffers periodically, a daemon thread is started when a new instance of the allocator is
 * created.  You can stop the thread by calling {@link #dispose()}. </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class PooledByteBufferAllocator implements ByteBufferAllocator {
    private static final int MINIMUM_CAPACITY = 1;

    private static int threadId = 0;

    private final Expirer expirer;

    private final ExpiringStack[] heapBufferStacks = new ExpiringStack[] {
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), };

    private final ExpiringStack[] directBufferStacks = new ExpiringStack[] {
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), new ExpiringStack(),
            new ExpiringStack(), new ExpiringStack(), };

    private int timeout;

    private boolean disposed;

    /**
     * Creates a new instance with the default timeout.
     */
    public PooledByteBufferAllocator() {
        this(60);
    }

    /**
     * Creates a new instance with the specified <tt>timeout</tt>.
     */
    public PooledByteBufferAllocator(int timeout) {
        setTimeout(timeout);
        expirer = new Expirer();
        expirer.start();
    }

    /**
     * Stops the thread which releases unused buffers and make this allocator unusable from now on.
     */
    public void dispose() {
        if (this == ByteBuffer.getAllocator()) {
            throw new IllegalStateException("This allocator is in use.");
        }

        expirer.shutdown();

        for (int i = directBufferStacks.length - 1; i >= 0; i--) {
            ExpiringStack stack = directBufferStacks[i];
            synchronized (stack) {
                stack.clear();
            }
        }
        for (int i = heapBufferStacks.length - 1; i >= 0; i--) {
            ExpiringStack stack = heapBufferStacks[i];
            synchronized (stack) {
                stack.clear();
            }
        }
        disposed = true;
    }

    /**
     * Returns the timeout value of this allocator in seconds.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Returns the timeout value of this allocator in milliseconds.
     */
    public long getTimeoutMillis() {
        return timeout * 1000L;
    }

    /**
     * Sets the timeout value of this allocator in seconds.
     *
     * @param timeout <tt>0</tt> or negative value to disable timeout.
     */
    public void setTimeout(int timeout) {
        if (timeout < 0) {
            timeout = 0;
        }

        this.timeout = timeout;

        if (timeout > 0) {

        }
    }

    public ByteBuffer allocate(int capacity, boolean direct) {
        ensureNotDisposed();
        UnexpandableByteBuffer ubb = allocate0(capacity, direct);
        PooledByteBuffer buf = allocateContainer();
        buf.init(ubb, true);
        return buf;
    }

    private PooledByteBuffer allocateContainer() {
        return new PooledByteBuffer();
    }

    private UnexpandableByteBuffer allocate0(int capacity, boolean direct) {
        ExpiringStack[] bufferStacks = direct ? directBufferStacks
                : heapBufferStacks;
        int idx = getBufferStackIndex(bufferStacks, capacity);
        ExpiringStack stack = bufferStacks[idx];

        UnexpandableByteBuffer buf;
        synchronized (stack) {
            buf = (UnexpandableByteBuffer) stack.pop();
        }

        if (buf == null) {
            java.nio.ByteBuffer nioBuf = direct ? java.nio.ByteBuffer
                    .allocateDirect(MINIMUM_CAPACITY << idx)
                    : java.nio.ByteBuffer.allocate(MINIMUM_CAPACITY << idx);
            buf = new UnexpandableByteBuffer(nioBuf);
        } else {
            //Fix for DIRMINA-622
            java.nio.ByteBuffer b = buf.buf();
            b.clear();
            for (int i=0,max=b.remaining();i<max;i++)
                b.put((byte) 0);
        }

        buf.init();

        return buf;
    }

    private void release0(UnexpandableByteBuffer buf) {
        ExpiringStack[] bufferStacks = buf.buf().isDirect() ? directBufferStacks
                : heapBufferStacks;
        ExpiringStack stack = bufferStacks[getBufferStackIndex(bufferStacks,
                buf.buf().capacity())];

        synchronized (stack) {
            // push back
            stack.push(buf);
        }
    }

    public ByteBuffer wrap(java.nio.ByteBuffer nioBuffer) {
        ensureNotDisposed();
        PooledByteBuffer buf = allocateContainer();
        buf.init(new UnexpandableByteBuffer(nioBuffer), false);
        buf.buf.init();
        buf.setPooled(false);
        return buf;
    }

    private int getBufferStackIndex(ExpiringStack[] bufferStacks, int size) {
        int targetSize = MINIMUM_CAPACITY;
        int stackIdx = 0;
        while (size > targetSize) {
            targetSize <<= 1;
            stackIdx++;
            if (stackIdx >= bufferStacks.length) {
                throw new IllegalArgumentException("Buffer size is too big: "
                        + size);
            }
        }

        return stackIdx;
    }

    private void ensureNotDisposed() {
        if (disposed) {
            throw new IllegalStateException(
                    "This allocator is disposed already.");
        }
    }

    private class Expirer extends Thread {
        private boolean timeToStop;

        Expirer() {
            super("PooledByteBufferExpirer-" + threadId++);
            setDaemon(true);
        }

        public void shutdown() {
            timeToStop = true;
            interrupt();
            while (isAlive()) {
                try {
                    join();
                } catch (InterruptedException e) {
                    //ignore since this is shutdown time
                }
            }
        }

        public void run() {
            // Expire unused buffers every seconds
            while (!timeToStop) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }

                // Check if expiration is disabled.
                long timeout = getTimeoutMillis();
                if (timeout <= 0L) {
                    continue;
                }

                // Expire old buffers
                long expirationTime = System.currentTimeMillis() - timeout;

                for (int i = directBufferStacks.length - 1; i >= 0; i--) {
                    ExpiringStack stack = directBufferStacks[i];
                    synchronized (stack) {
                        stack.expireBefore(expirationTime);
                    }
                }

                for (int i = heapBufferStacks.length - 1; i >= 0; i--) {
                    ExpiringStack stack = heapBufferStacks[i];
                    synchronized (stack) {
                        stack.expireBefore(expirationTime);
                    }
                }
            }
        }
    }

    private class PooledByteBuffer extends BaseByteBuffer {
        private UnexpandableByteBuffer buf;

        private int refCount = 1;

        protected PooledByteBuffer() {
        }

        public synchronized void init(UnexpandableByteBuffer buf, boolean clear) {
            this.buf = buf;
            if (clear) {
                buf.buf().clear();
            }
            buf.buf().order(ByteOrder.BIG_ENDIAN);
            setAutoExpand(false);
            refCount = 1;
        }

        public synchronized void acquire() {
            if (refCount <= 0) {
                throw new IllegalStateException("Already released buffer.");
            }

            refCount++;
        }

        public void release() {
            synchronized (this) {
                if (refCount <= 0) {
                    refCount = 0;
                    throw new IllegalStateException(
                            "Already released buffer.  You released the buffer too many times.");
                }

                refCount--;
                if (refCount > 0) {
                    return;
                }
            }

            // No need to return buffers to the pool if it is disposed already.
            if (disposed) {
                return;
            }

            buf.release();
        }

        public java.nio.ByteBuffer buf() {
            return buf.buf();
        }

        public boolean isPooled() {
            return buf.isPooled();
        }

        public void setPooled(boolean pooled) {
            buf.setPooled(pooled);
        }

        public ByteBuffer duplicate() {
            PooledByteBuffer newBuf = allocateContainer();
            newBuf.init(new UnexpandableByteBuffer(buf().duplicate(), buf),
                    false);
            return newBuf;
        }

        public ByteBuffer slice() {
            PooledByteBuffer newBuf = allocateContainer();
            newBuf.init(new UnexpandableByteBuffer(buf().slice(), buf), false);
            return newBuf;
        }

        public ByteBuffer asReadOnlyBuffer() {
            PooledByteBuffer newBuf = allocateContainer();
            newBuf.init(new UnexpandableByteBuffer(buf().asReadOnlyBuffer(),
                    buf), false);
            return newBuf;
        }

        public byte[] array() {
            return buf().array();
        }

        public int arrayOffset() {
            return buf().arrayOffset();
        }

        protected void capacity0(int requestedCapacity) {
            if (buf.isDerived()) {
                throw new IllegalStateException(
                        "Derived buffers cannot be expanded.");
            }

            int newCapacity = MINIMUM_CAPACITY;
            while (newCapacity < requestedCapacity) {
                newCapacity <<= 1;
            }

            UnexpandableByteBuffer oldBuf = this.buf;
            boolean direct = isDirect();
            UnexpandableByteBuffer newBuf;

            try {
                newBuf = allocate0(newCapacity, direct);
            } catch (OutOfMemoryError e) {
                if (direct) {
                    newBuf = allocate0(newCapacity, false);
                } else {
                    throw e;
                }
            }

            newBuf.buf().clear();
            oldBuf.buf().clear();
            newBuf.buf().put(oldBuf.buf());
            this.buf = newBuf;
            oldBuf.release();
        }
    }

    private class UnexpandableByteBuffer {
        private final java.nio.ByteBuffer buf;

        private final UnexpandableByteBuffer parentBuf;

        private int refCount;

        private boolean pooled;

        protected UnexpandableByteBuffer(java.nio.ByteBuffer buf) {
            this.buf = buf;
            this.parentBuf = null;
        }

        protected UnexpandableByteBuffer(java.nio.ByteBuffer buf,
                UnexpandableByteBuffer parentBuf) {
            parentBuf.acquire();
            this.buf = buf;
            this.parentBuf = parentBuf;
        }

        public void init() {
            refCount = 1;
            pooled = true;
        }

        public synchronized void acquire() {
            if (isDerived()) {
                parentBuf.acquire();
                return;
            }

            if (refCount <= 0) {
                throw new IllegalStateException("Already released buffer.");
            }

            refCount++;
        }

        public void release() {
            if (isDerived()) {
                parentBuf.release();
                return;
            }

            synchronized (this) {
                if (refCount <= 0) {
                    refCount = 0;
                    throw new IllegalStateException(
                            "Already released buffer.  You released the buffer too many times.");
                }

                refCount--;
                if (refCount > 0) {
                    return;
                }
            }

            // No need to return buffers to the pool if it is disposed already.
            if (disposed) {
                return;
            }

            if (pooled) {
                if (parentBuf != null) {
                    release0(parentBuf);
                } else {
                    release0(this);
                }
            }
        }

        public java.nio.ByteBuffer buf() {
            return buf;
        }

        public boolean isPooled() {
            return pooled;
        }

        public void setPooled(boolean pooled) {
            this.pooled = pooled;
        }

        public boolean isDerived() {
            return parentBuf != null;
        }
    }
}
