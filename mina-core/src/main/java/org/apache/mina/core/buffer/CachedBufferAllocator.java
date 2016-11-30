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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An {@link IoBufferAllocator} that caches the buffers which are likely to
 * be reused during auto-expansion of the buffers.
 * <p>
 * In {@link SimpleBufferAllocator}, the underlying {@link ByteBuffer} of
 * the {@link IoBuffer} is reallocated on its capacity change, which means
 * the newly allocated bigger {@link ByteBuffer} replaces the old small
 * {@link ByteBuffer}.  Consequently, the old {@link ByteBuffer} is marked
 * for garbage collection.
 * <p>
 * It's not a problem in most cases as long as the capacity change doesn't
 * happen frequently.  However, once it happens too often, it burdens the
 * VM and the cost of filling the newly allocated {@link ByteBuffer} with
 * {@code NUL} surpass the cost of accessing the cache.  In 2 dual-core
 * Opteron Italy 270 processors, {@link CachedBufferAllocator} outperformed
 * {@link SimpleBufferAllocator} in the following situation:
 * <ul>
 * <li>when a 32 bytes buffer is expanded 4 or more times,</li> 
 * <li>when a 64 bytes buffer is expanded 4 or more times,</li>
 * <li>when a 128 bytes buffer is expanded 2 or more times,</li>
 * <li>and when a 256 bytes or bigger buffer is expanded 1 or more times.</li>
 * </ul>
 * Please note the observation above is subject to change in a different
 * environment.
 * <p>
 * {@link CachedBufferAllocator} uses {@link ThreadLocal} to store the cached
 * buffer, allocates buffers whose capacity is power of 2 only and provides
 * performance advantage if {@link IoBuffer#free()} is called properly.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CachedBufferAllocator implements IoBufferAllocator {

    private static final int DEFAULT_MAX_POOL_SIZE = 8;

    private static final int DEFAULT_MAX_CACHED_BUFFER_SIZE = 1 << 18; // 256KB

    private final int maxPoolSize;

    private final int maxCachedBufferSize;

    private final ThreadLocal<Map<Integer, Queue<CachedBuffer>>> heapBuffers;

    private final ThreadLocal<Map<Integer, Queue<CachedBuffer>>> directBuffers;

    /**
     * Creates a new instance with the default parameters
     * ({@literal #DEFAULT_MAX_POOL_SIZE} and {@literal #DEFAULT_MAX_CACHED_BUFFER_SIZE}). 
     */
    public CachedBufferAllocator() {
        this(DEFAULT_MAX_POOL_SIZE, DEFAULT_MAX_CACHED_BUFFER_SIZE);
    }

    /**
     * Creates a new instance.
     * 
     * @param maxPoolSize the maximum number of buffers with the same capacity per thread.
     *                    <tt>0</tt> disables this limitation.
     * @param maxCachedBufferSize the maximum capacity of a cached buffer.
     *                            A buffer whose capacity is bigger than this value is
     *                            not pooled. <tt>0</tt> disables this limitation.
     */
    public CachedBufferAllocator(int maxPoolSize, int maxCachedBufferSize) {
        if (maxPoolSize < 0) {
            throw new IllegalArgumentException("maxPoolSize: " + maxPoolSize);
        }

        if (maxCachedBufferSize < 0) {
            throw new IllegalArgumentException("maxCachedBufferSize: " + maxCachedBufferSize);
        }

        this.maxPoolSize = maxPoolSize;
        this.maxCachedBufferSize = maxCachedBufferSize;

        this.heapBuffers = new ThreadLocal<Map<Integer, Queue<CachedBuffer>>>() {
            @Override
            protected Map<Integer, Queue<CachedBuffer>> initialValue() {
                return newPoolMap();
            }
        };

        this.directBuffers = new ThreadLocal<Map<Integer, Queue<CachedBuffer>>>() {
            @Override
            protected Map<Integer, Queue<CachedBuffer>> initialValue() {
                return newPoolMap();
            }
        };
    }

    /**
     * @return the maximum number of buffers with the same capacity per thread.
     * <tt>0</tt> means 'no limitation'.
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * @return the maximum capacity of a cached buffer.  A buffer whose
     * capacity is bigger than this value is not pooled.  <tt>0</tt> means
     * 'no limitation'.
     */
    public int getMaxCachedBufferSize() {
        return maxCachedBufferSize;
    }

    Map<Integer, Queue<CachedBuffer>> newPoolMap() {
        Map<Integer, Queue<CachedBuffer>> poolMap = new HashMap<>();

        for (int i = 0; i < 31; i++) {
            poolMap.put(1 << i, new ConcurrentLinkedQueue<CachedBuffer>());
        }

        poolMap.put(0, new ConcurrentLinkedQueue<CachedBuffer>());
        poolMap.put(Integer.MAX_VALUE, new ConcurrentLinkedQueue<CachedBuffer>());

        return poolMap;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public IoBuffer allocate(int requestedCapacity, boolean direct) {
        int actualCapacity = IoBuffer.normalizeCapacity(requestedCapacity);
        IoBuffer buf;

        if ((maxCachedBufferSize != 0) && (actualCapacity > maxCachedBufferSize)) {
            if (direct) {
                buf = wrap(ByteBuffer.allocateDirect(actualCapacity));
            } else {
                buf = wrap(ByteBuffer.allocate(actualCapacity));
            }
        } else {
            Queue<CachedBuffer> pool;

            if (direct) {
                pool = directBuffers.get().get(actualCapacity);
            } else {
                pool = heapBuffers.get().get(actualCapacity);
            }

            // Recycle if possible.
            buf = pool.poll();

            if (buf != null) {
                buf.clear();
                buf.setAutoExpand(false);
                buf.order(ByteOrder.BIG_ENDIAN);
            } else {
                if (direct) {
                    buf = wrap(ByteBuffer.allocateDirect(actualCapacity));
                } else {
                    buf = wrap(ByteBuffer.allocate(actualCapacity));
                }
            }
        }

        buf.limit(requestedCapacity);
        return buf;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public ByteBuffer allocateNioBuffer(int capacity, boolean direct) {
        return allocate(capacity, direct).buf();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new CachedBuffer(nioBuffer);
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public void dispose() {
        // Do nothing
    }

    private class CachedBuffer extends AbstractIoBuffer {
        private final Thread ownerThread;

        private ByteBuffer buf;

        protected CachedBuffer(ByteBuffer buf) {
            super(CachedBufferAllocator.this, buf.capacity());
            this.ownerThread = Thread.currentThread();
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        protected CachedBuffer(CachedBuffer parent, ByteBuffer buf) {
            super(parent);
            this.ownerThread = Thread.currentThread();
            this.buf = buf;
        }

        @Override
        public ByteBuffer buf() {
            if (buf == null) {
                throw new IllegalStateException("Buffer has been freed already.");
            }
            return buf;
        }

        @Override
        protected void buf(ByteBuffer buf) {
            ByteBuffer oldBuf = this.buf;
            this.buf = buf;
            free(oldBuf);
        }

        @Override
        protected IoBuffer duplicate0() {
            return new CachedBuffer(this, buf().duplicate());
        }

        @Override
        protected IoBuffer slice0() {
            return new CachedBuffer(this, buf().slice());
        }

        @Override
        protected IoBuffer asReadOnlyBuffer0() {
            return new CachedBuffer(this, buf().asReadOnlyBuffer());
        }

        @Override
        public byte[] array() {
            return buf().array();
        }

        @Override
        public int arrayOffset() {
            return buf().arrayOffset();
        }

        @Override
        public boolean hasArray() {
            return buf().hasArray();
        }

        @Override
        public void free() {
            free(buf);
            buf = null;
        }

        private void free(ByteBuffer oldBuf) {
            if ((oldBuf == null) || ((maxCachedBufferSize != 0) && (oldBuf.capacity() > maxCachedBufferSize))
                    || oldBuf.isReadOnly() || isDerived() || (Thread.currentThread() != ownerThread)) {
                return;
            }

            // Add to the cache.
            Queue<CachedBuffer> pool;

            if (oldBuf.isDirect()) {
                pool = directBuffers.get().get(oldBuf.capacity());
            } else {
                pool = heapBuffers.get().get(oldBuf.capacity());
            }

            if (pool == null) {
                return;
            }

            // Restrict the size of the pool to prevent OOM.
            if ((maxPoolSize == 0) || (pool.size() < maxPoolSize)) {
                pool.offer(new CachedBuffer(oldBuf));
            }
        }
    }
}
