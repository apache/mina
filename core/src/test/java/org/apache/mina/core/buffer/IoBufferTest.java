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

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

/**
 * Tests {@link IoBuffer}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoBufferTest {
    @Test
    public void testNormalizeCapacity() {
        // A few sanity checks
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(-10));
        assertEquals(0, IoBufferImpl.normalizeCapacity(0));
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, IoBufferImpl.normalizeCapacity(Integer.MAX_VALUE - 10));

        // A sanity check test for all the powers of 2
        for (int i = 0; i < 30; i++) {
            int n = 1 << i;

            assertEquals(n, IoBufferImpl.normalizeCapacity(n));

            if (i > 1) {
                // test that n - 1 will be normalized to n (notice that n = 2^i)
                assertEquals(n, IoBufferImpl.normalizeCapacity(n - 1));
            }

            // test that n + 1 will be normalized to 2^(i + 1)
            assertEquals(n << 1, IoBufferImpl.normalizeCapacity(n + 1));
        }

        // The first performance test measures the time to normalize integers
        // from 0 to 2^27 (it tests 2^27 integers)
        long time = System.currentTimeMillis();

        for (int i = 0; i < 1 << 27; i++) {
            int n = IoBufferImpl.normalizeCapacity(i);

            // do a simple superfluous test to prevent possible compiler or JVM
            // optimizations of not executing non used code/variables
            if (n == -1) {
                System.out.println("n should never be -1");
            }
        }

        long time2 = System.currentTimeMillis();
        System.out.println("Time for performance test 1: " + (time2 - time) + "ms");

        // The second performance test measures the time to normalize integers
        // from Integer.MAX_VALUE to Integer.MAX_VALUE - 2^27 (it tests 2^27
        // integers)
        time = System.currentTimeMillis();
        for (int i = Integer.MAX_VALUE; i > Integer.MAX_VALUE - (1 << 27); i--) {
            int n = IoBufferImpl.normalizeCapacity(i);

            // do a simple superfluous test to prevent possible compiler or JVM
            // optimizations of not executing non used code/variables
            if (n == -1) {
                System.out.println("n should never be -1");
            }
        }

        time2 = System.currentTimeMillis();
        System.out.println("Time for performance test 2: " + (time2 - time) + "ms");
    }

    /**
     * This class extends the AbstractIoBuffer class to have direct access to
     * the protected IoBuffer.normalizeCapacity() method and to expose it for
     * the tests.
     */
    private static class IoBufferImpl extends AbstractIoBuffer {

        public static int normalizeCapacity(int requestedCapacity) {
            return IoBuffer.normalizeCapacity(requestedCapacity);
        }

        protected IoBufferImpl(AbstractIoBuffer parent) {
            super(parent);
        }

        protected IoBuffer asReadOnlyBuffer0() {
            return null;
        }

        protected void buf(ByteBuffer newBuf) {
        }

        protected IoBuffer duplicate0() {
            return null;
        }

        protected IoBuffer slice0() {
            return null;
        }

        public byte[] array() {
            return null;
        }

        public int arrayOffset() {
            return 0;
        }

        public ByteBuffer buf() {
            return null;
        }

        public void free() {
        }

        public boolean hasArray() {
            return false;
        }

    }
}
