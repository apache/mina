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
package org.apache.mina.core.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the ready {@link org.apache.mina.core.future.ReadFuture} queue
 * used when <tt>useReadOperation</tt> is enabled is bounded: when the
 * application does not drain {@link AbstractIoSession#read()} fast enough,
 * the session must suspend reads instead of queueing every received message
 * until the heap is exhausted.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractIoSessionReadFutureLimitTest {
    @Test
    public void testReadFutureQueueIsBoundedByDefault() {
        DummySession session = new DummySession();
        assertEquals(AbstractIoSession.DEFAULT_MAX_QUEUED_READ_FUTURES, session.getMaxQueuedReadFutures());
        assertTrue(session.getMaxQueuedReadFutures() > 0);
    }

    @Test
    public void testReachingTheLimitSuspendsReadsAndDrainingResumesThem() throws Exception {
        DummySession session = new DummySession();
        session.getConfig().setUseReadOperation(true);
        session.setMaxQueuedReadFutures(4);

        for (int i = 0; i < 4; i++) {
            assertFalse(session.isReadSuspended());
            session.offerReadFuture(Integer.valueOf(i));
        }

        // The limit was reached: read interest must have been suspended
        assertTrue(session.isReadSuspended());

        // Messages already in flight are still queued, and reads stay suspended
        session.offerReadFuture(Integer.valueOf(4));
        assertTrue(session.isReadSuspended());

        // Draining below half of the limit resumes reads
        assertEquals(0, session.read().getMessage());
        assertEquals(1, session.read().getMessage());
        assertTrue(session.isReadSuspended());
        assertEquals(2, session.read().getMessage());
        assertFalse(session.isReadSuspended());

        assertEquals(3, session.read().getMessage());
        assertEquals(4, session.read().getMessage());
    }
}
