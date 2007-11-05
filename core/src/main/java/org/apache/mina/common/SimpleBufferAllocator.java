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


/**
 * A simplistic {@link IoBufferAllocator} which simply allocates a new
 * buffer every time.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class SimpleBufferAllocator implements IoBufferAllocator {

    public IoBuffer allocate(int capacity, boolean direct) {
        ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = ByteBuffer.allocateDirect(capacity);
        } else {
            nioBuffer = ByteBuffer.allocate(capacity);
        }
        return new SimpleBuffer(nioBuffer, true);
    }

    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return new SimpleBuffer(nioBuffer, true);
    }

    public void dispose() {
    }

    private static class SimpleBuffer extends AbstractIoBuffer {
        private ByteBuffer buf;

        protected SimpleBuffer(ByteBuffer buf,
                boolean autoExpandAllowed) {
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
            ByteBuffer newBuf;
            if (isDirect()) {
                newBuf = ByteBuffer.allocateDirect(newCapacity);
            } else {
                newBuf = ByteBuffer.allocate(newCapacity);
            }

            newBuf.clear();
            oldBuf.clear();
            newBuf.put(oldBuf);
            this.buf = newBuf;
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

        @Override
        public void free() {
        }
    }
}
