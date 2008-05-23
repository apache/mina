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
package org.apache.mina.queue;

import static org.junit.Assert.*;

import java.nio.ByteOrder;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Tests {@link DefaultByteBufferQueue}.
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultByteBufferQueueTest {

    private static final int CAPACITY_INCREMENT = 9;

    @Test
    public void defaultState() {
        ByteBufferQueue queue = new DefaultByteBufferQueue();
        assertSame(ByteOrder.BIG_ENDIAN, queue.order());
        assertEmpty(queue);
    }

    @Test
    public void alternativeState() {
        ByteBufferQueue queue = new DefaultByteBufferQueue(ByteOrder.LITTLE_ENDIAN);
        assertSame(ByteOrder.LITTLE_ENDIAN, queue.order());
        assertEmpty(queue);
    }

    @Test
    public void sequentialByteAccess() {
        ByteBufferQueue queue = new DefaultByteBufferQueue(CAPACITY_INCREMENT);
        for (int i = 0; i < 256; i ++) {
            assertTrue(queue.offerByte((byte) i));
        }

        assertLength(256, queue);

        for (int i = 0; i < 256; i ++) {
            assertEquals(i, queue.removeByte() & 0xFF);
            assertLength(256 - i - 1, queue);
        }

        assertEmpty(queue);
    }

    @Test
    public void sequentialShortAccess() {
        ByteBufferQueue queue = new DefaultByteBufferQueue(CAPACITY_INCREMENT);
        for (int i = 0; i < 65536; i ++) {
            assertTrue(queue.offerShort((short) i));
        }

        assertLength(65536 * 2, queue);
        assertEquals(65536 / (CAPACITY_INCREMENT / 2), queue.size());

        for (int i = 0; i < 65536; i ++) {
            assertEquals(i, queue.removeShort() & 0xFFFF);
            assertLength(65536 * 2 - (i + 1) * 2, queue);
        }

        assertEmpty(queue);
    }

    @Test
    public void sequentialIntAccess() {
        ByteBufferQueue queue = new DefaultByteBufferQueue(CAPACITY_INCREMENT);
        for (int i = 0; i < 65536; i ++) {
            assertTrue(queue.offerInt(i));
        }

        assertLength(65536 * 4, queue);
        assertEquals(65536 / (CAPACITY_INCREMENT / 4), queue.size());

        for (int i = 0; i < 65536; i ++) {
            assertEquals(i, queue.removeInt());
            assertLength(65536 * 4 - (i + 1) * 4, queue);
        }

        assertEmpty(queue);
    }

    @Test
    public void sequentialLongAccess() {
        ByteBufferQueue queue = new DefaultByteBufferQueue(CAPACITY_INCREMENT);
        for (int i = 0; i < 65536; i ++) {
            assertTrue(queue.offerLong(i));
        }

        assertLength(65536 * 8, queue);
        assertEquals(65536 / (CAPACITY_INCREMENT / 8), queue.size());

        for (int i = 0; i < 65536; i ++) {
            assertEquals(i, queue.removeLong());
            assertLength(65536 * 8 - (i + 1) * 8, queue);
        }

        assertEmpty(queue);
    }

    private void assertLength(int expected, ByteBufferQueue queue) {
        assertEquals(expected, queue.length());
        assertEquals(expected == 0, queue.isEmpty());
    }

    private void assertEmpty(ByteBufferQueue queue) {
        assertEquals(0, queue.size());
        assertEquals(0, queue.length());
        assertTrue(queue.isEmpty());

        try {
            queue.removeByte();
            fail();
        } catch (NoSuchElementException e) {
            // Expected.
        }

        assertEquals(0, queue.size());
        assertEquals(0, queue.length());
        assertTrue(queue.isEmpty());

        try {
            queue.removeShort();
            fail();
        } catch (NoSuchElementException e) {
            // Expected.
        }

        assertEquals(0, queue.size());
        assertEquals(0, queue.length());
        assertTrue(queue.isEmpty());

        try {
            queue.removeInt();
            fail();
        } catch (NoSuchElementException e) {
            // Expected.
        }

        assertEquals(0, queue.size());
        assertEquals(0, queue.length());
        assertTrue(queue.isEmpty());

        try {
            queue.removeLong();
            fail();
        } catch (NoSuchElementException e) {
            // Expected.
        }

        assertEquals(0, queue.size());
        assertEquals(0, queue.length());
        assertTrue(queue.isEmpty());
    }
}
