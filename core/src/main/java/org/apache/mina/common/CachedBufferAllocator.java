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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.mina.util.CircularQueue;

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
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
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
     * Returns the maximum number of buffers with the same capacity per thread.
     * <tt>0</tt> means 'no limitation'.
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Returns the maximum capacity of a cached buffer.  A buffer whose
     * capacity is bigger than this value is not pooled.  <tt>0</tt> means
     * 'no limitation'.
     */
    public int getMaxCachedBufferSize() {
        return maxCachedBufferSize;
    }

    private Map<Integer, Queue<CachedBuffer>> newPoolMap() {
        Map<Integer, Queue<CachedBuffer>> poolMap =
            new HashMap<Integer, Queue<CachedBuffer>>();
        int poolSize = maxPoolSize == 0? DEFAULT_MAX_POOL_SIZE : maxPoolSize;
        for (int i = 0; i < 31; i ++) {
            poolMap.put(1 << i, new CircularQueue<CachedBuffer>(poolSize));
        }
        poolMap.put(0, new CircularQueue<CachedBuffer>(poolSize));
        poolMap.put(Integer.MAX_VALUE, new CircularQueue<CachedBuffer>(poolSize));
        return poolMap;
    }

    public IoBuffer allocate(int requestedCapacity, boolean direct) {
        int actualCapacity = normalizeCapacity(requestedCapacity);
        IoBuffer buf ;
        if (maxCachedBufferSize != 0 && actualCapacity > maxCachedBufferSize) {
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
    
    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new CachedBuffer(nioBuffer, true);
    }

    public void dispose() {
    }
    
    private static int normalizeCapacity(int requestedCapacity) {
        switch (requestedCapacity) {
        case 0:
        case 1 <<  0: case 1 <<  1: case 1 <<  2: case 1 <<  3: case 1 <<  4:
        case 1 <<  5: case 1 <<  6: case 1 <<  7: case 1 <<  8: case 1 <<  9:
        case 1 << 10: case 1 << 11: case 1 << 12: case 1 << 13: case 1 << 14:
        case 1 << 15: case 1 << 16: case 1 << 17: case 1 << 18: case 1 << 19:
        case 1 << 21: case 1 << 22: case 1 << 23: case 1 << 24: case 1 << 25:
        case 1 << 26: case 1 << 27: case 1 << 28: case 1 << 29: case 1 << 30:
        case Integer.MAX_VALUE:
            return requestedCapacity;
        }
        
        int newCapacity = 1;
        while (newCapacity < requestedCapacity) {
            newCapacity <<= 1;
            if (newCapacity < 0) {
                return Integer.MAX_VALUE;
            }
        }
        return newCapacity;
    }

    private class CachedBuffer extends AbstractIoBuffer {
        private final Thread ownerThread;
        private ByteBuffer buf;

        protected CachedBuffer(ByteBuffer buf, boolean autoExpandAllowed) {
            super(autoExpandAllowed);
            this.ownerThread = Thread.currentThread();
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        @Override
        public ByteBuffer buf() {
            if (buf == null) {
                throw new IllegalStateException("Buffer has been freed already.");
            }
            return buf;
        }

        @Override
        protected void capacity0(int requestedCapacity) {
            int newCapacity = normalizeCapacity(requestedCapacity);

            ByteBuffer oldBuf = buf();
            ByteBuffer newBuf = allocate(newCapacity, isDirect()).buf();
            oldBuf.clear();
            newBuf.put(oldBuf);
            this.buf = newBuf;
            
            free(oldBuf);
        }

        @Override
        protected IoBuffer duplicate0() {
            return new CachedBuffer(buf().duplicate(), false);
        }

        @Override
        protected IoBuffer slice0() {
            return new CachedBuffer(buf().slice(), false);
        }

        @Override
        protected IoBuffer asReadOnlyBuffer0() {
            return new CachedBuffer(buf().asReadOnlyBuffer(), false);
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
            if (oldBuf == null || oldBuf.capacity() > ' ||
                oldBuf.isReadOnly() || isDerived() ||
                Thread.currentThread() != ownerThread) {
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
            if (maxPoolSize == 0 || pool.size() < maxPoolSize) {
                pool.offer(new CachedBuffer(oldBuf, true));
            }
        }
    }
}
