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
 * buffer, allocates buffers whose capacity is power of 2 only, and doesn't
 * provide any caching for direct buffers.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class CachedBufferAllocator implements IoBufferAllocator {
    private static final int MAX_POOL_SIZE = 8;
    
    private final ThreadLocal<Map<Integer, Queue<ByteBuffer>>> localRecyclables =
        new ThreadLocal<Map<Integer, Queue<ByteBuffer>>>() {
            @Override
            protected Map<Integer, Queue<ByteBuffer>> initialValue() {
                Map<Integer, Queue<ByteBuffer>> queues =
                    new HashMap<Integer, Queue<ByteBuffer>>();
                for (int i = 0; i < 31; i ++) {
                    queues.put(1 << i, new CircularQueue<ByteBuffer>(MAX_POOL_SIZE));
                }
                queues.put(Integer.MAX_VALUE, new CircularQueue<ByteBuffer>(MAX_POOL_SIZE));
                return queues;
            }
        };

    public IoBuffer allocate(int capacity, boolean direct) {
        return wrap(allocate0(capacity, direct));
    }
    
    private ByteBuffer allocate0(int capacity, boolean direct) {
        capacity = normalizeCapacity(capacity);
        ByteBuffer buf;
        if (direct) {
            buf = ByteBuffer.allocateDirect(capacity);
        } else {
            // Recycle if possible.
            Queue<ByteBuffer> pool = localRecyclables.get().get(capacity);
            buf = pool.poll();
            if (buf != null) {
                buf.clear();
            } else {
                buf = ByteBuffer.allocate(capacity);
            }
        }
        return buf;
    }

    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new DirtyBuffer(nioBuffer, true);
    }

    public void dispose() {
    }
    
    private static int normalizeCapacity(int requestedCapacity) {
        switch (requestedCapacity) {
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

    private class DirtyBuffer extends AbstractIoBuffer {
        private ByteBuffer buf;

        protected DirtyBuffer(ByteBuffer buf, boolean autoExpandAllowed) {
            super(autoExpandAllowed);
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        @Override
        public ByteBuffer buf() {
            return buf;
        }

        @Override
        protected void capacity0(int requestedCapacity) {
            int newCapacity = normalizeCapacity(requestedCapacity);

            ByteBuffer oldBuf = this.buf;
            ByteBuffer newBuf = allocate0(newCapacity, isDirect());
            oldBuf.clear();
            newBuf.put(oldBuf);
            this.buf = newBuf;
            
            free(oldBuf);
            free(buf);
        }

        @Override
        protected IoBuffer duplicate0() {
            return new DirtyBuffer(this.buf.duplicate(), false);
        }

        @Override
        protected IoBuffer slice0() {
            return new DirtyBuffer(this.buf.slice(), false);
        }

        @Override
        protected IoBuffer asReadOnlyBuffer0() {
            return new DirtyBuffer(this.buf.asReadOnlyBuffer(), false);
        }

        @Override
        public byte[] array() {
            return buf.array();
        }

        @Override
        public int arrayOffset() {
            return buf.arrayOffset();
        }

        @Override
        public boolean hasArray() {
            return buf.hasArray();
        }

        @Override
        public void free() {
            free(buf);
            buf = null; // FIXME better sanity check scheme.
        }
        
        private void free(ByteBuffer buf) {
            // Add to the cache.
            if (!buf.isDirect() && !buf.isReadOnly() && !isDerived()) {
                Queue<ByteBuffer> pool = localRecyclables.get().get(buf.capacity());
                // Restrict the size of the pool to prevent OOM.
                if (pool.size() < MAX_POOL_SIZE) {
                    pool.offer(buf);
                }
            }
        }
    }
}
