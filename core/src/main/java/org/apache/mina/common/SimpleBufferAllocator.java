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


/**
 * A simplistic {@link IoBufferAllocator} which simply allocates a new
 * buffer every time.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class SimpleBufferAllocator implements IoBufferAllocator {
    private static final int MINIMUM_CAPACITY = 1;

    public SimpleBufferAllocator() {
    }

    public IoBuffer allocate(int capacity, boolean direct) {
        java.nio.ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = java.nio.ByteBuffer.allocateDirect(capacity);
        } else {
            nioBuffer = java.nio.ByteBuffer.allocate(capacity);
        }
        return new SimpleBuffer(nioBuffer, true);
    }

    public IoBuffer wrap(java.nio.ByteBuffer nioBuffer) {
        return new SimpleBuffer(nioBuffer, true);
    }

    public void dispose() {
    }

    private static class SimpleBuffer extends AbstractIoBuffer {
        private java.nio.ByteBuffer buf;

        protected SimpleBuffer(java.nio.ByteBuffer buf,
                boolean autoExpandAllowed) {
            super(autoExpandAllowed);
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        @Override
        public java.nio.ByteBuffer buf() {
            return buf;
        }

        @Override
        protected void capacity0(int requestedCapacity) {
            int newCapacity = MINIMUM_CAPACITY;
            while (newCapacity < requestedCapacity) {
                newCapacity <<= 1;
            }

            java.nio.ByteBuffer oldBuf = this.buf;
            java.nio.ByteBuffer newBuf;
            if (isDirect()) {
                newBuf = java.nio.ByteBuffer.allocateDirect(newCapacity);
            } else {
                newBuf = java.nio.ByteBuffer.allocate(newCapacity);
            }

            newBuf.clear();
            oldBuf.clear();
            newBuf.put(oldBuf);
            this.buf = newBuf;
        }

        @Override
        protected IoBuffer duplicate0() {
            return new SimpleBuffer(this.buf.duplicate(), false);
        }

        @Override
        protected IoBuffer slice0() {
            return new SimpleBuffer(this.buf.slice(), false);
        }

        @Override
        protected IoBuffer asReadOnlyBuffer0() {
            return new SimpleBuffer(this.buf.asReadOnlyBuffer(), false);
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
    }
}
