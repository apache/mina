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
package org.apache.mina.filter.executor;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that verify the functionality provided by the implementation of
 * {@link PriorityThreadPoolExecutor}.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class PriorityThreadPoolExecutorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriorityThreadPoolExecutorTest.class);

    /**
     * Tests that verify the functionality provided by the implementation of
     * {@link org.apache.mina.filter.executor.PriorityThreadPoolExecutor.SessionEntry}
     * .
     *
     * This test asserts that, without a provided comparator, entries are
     * considered equal, when they reference the same session.
     * 
     * @exception Exception If the test throw an exception
     */
    @Test
    public void fifoEntryTestNoComparatorSameSession() throws Exception {
        // Set up fixture.
        IoSession session = new DummySession();
        PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(session, null);
        PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(session, null);
    
        // Execute system under test.
        int result = first.compareTo(last);
    
        // Verify results.
        assertEquals("Without a comparator, entries of the same session are expected to be equal.", 0, result);
    }

    /**
     * Tests that verify the functionality provided by the implementation of
     * {@link org.apache.mina.filter.executor.PriorityThreadPoolExecutor.SessionEntry}
     * .
     *
     * This test asserts that, without a provided comparator, the first entry
     * created is 'less than' an entry that is created later.
     * 
     * @exception Exception If the test throw an exception
     */
    @Test
    public void fifoEntryTestNoComparatorDifferentSession() throws Exception {
        // Set up fixture (the order in which the entries are created is
        // relevant here!)
        PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), null);
        PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), null);
        
        // Execute system under test.
        int result = first.compareTo(last);
        
        // Verify results.
        assertTrue("Without a comparator, the first entry created should be the first entry out. Expected a negative result, instead, got: " + result, result < 0);
    }

    /**
     * Tests that verify the functionality provided by the implementation of
     * {@link org.apache.mina.filter.executor.PriorityThreadPoolExecutor.SessionEntry}
     * .
     *
     * This test asserts that, with a provided comparator, entries are
     * considered equal, when they reference the same session (the provided
     * comparator is ignored).
     * 
     * @exception Exception If the test throw an exception
     */
    @Test
    public void fifoEntryTestWithComparatorSameSession() throws Exception {
        // Set up fixture.
        IoSession session = new DummySession();
        final int predeterminedResult = 3853;
        
        Comparator<IoSession> comparator = (o1, o2) -> predeterminedResult;
        
        PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(session, comparator);
        PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(session, comparator);
        
        // Execute system under test.
        int result = first.compareTo(last);
        
        // Verify results.
        assertEquals("With a comparator, entries of the same session are expected to be equal.", 0, result);
    }

    /**
     * Tests that verify the functionality provided by the implementation of
     * {@link org.apache.mina.filter.executor.PriorityThreadPoolExecutor.SessionEntry}
     * .
     *
     * This test asserts that a provided comparator is used instead of the
     * (fallback) default behavior (when entries are referring different
     * sessions).
     * 
     * @exception Exception If the test throw an exception
     */
    @Test
    public void fifoEntryTestComparatorDifferentSession() throws Exception {
        // Set up fixture (the order in which the entries are created is
        // relevant here!)
        final int predeterminedResult = 3853;
        
        Comparator<IoSession> comparator = (o1, o2) -> predeterminedResult;
        
        PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), comparator);
        PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), comparator);
        
        // Execute system under test.
        int result = first.compareTo(last);
        
        // Verify results.
        assertEquals("With a comparator, comparing entries of different sessions is expected to yield the comparator result.", predeterminedResult, result);
    }

    /**
     * Asserts that, when enough work is being submitted to the executor for it
     * to start queuing work, prioritisation of work starts to occur.
     *
     * This implementation starts a number of sessions, and evenly distributes a
     * number of messages to them. Processing each message is artificially made
     * 'expensive', while the executor pool is kept small. This causes work to
     * be queued in the executor.
     *
     * The executor that is used is configured to prefer one specific session.
     * Each session records the timestamp of its last activity. After all work
     * has been processed, the test asserts that the last activity of all
     * sessions was later than the last activity of the preferred session.
     * 
     * @exception Throwable If the test throw an exception
     */
    @Test
    public void testPrioritisation() throws Throwable {
        IoSession blockerSession = new DummySession();
        CountDownLatch blockerQueued = new CountDownLatch(1);
        CountDownLatch sessionQueued = new CountDownLatch(1);

        MockWorkFilter nextFilter = new MockWorkFilter(blockerSession, blockerQueued, sessionQueued);
        List<LastActivityTracker> sessions = new ArrayList<>();

        int maximumPoolSize = 1; // keep this low, to force resource contention.
        int sessionCount = 10;
        int taskCount = 400;
        int tasksPerSessionCount = taskCount / sessionCount;
        
        for (int i = 0; i < 10; i++) {
            sessions.add(new LastActivityTracker());
        }
        
        LastActivityTracker preferredSession = sessions.get(4); // prefer an arbitrary session
                                                                // (but not the first or last
                                                                // session, for good measure).

        Comparator<IoSession> comparator = new UnfairComparator(preferredSession);

        
        ExecutorService executor = new PriorityThreadPoolExecutor(maximumPoolSize, comparator);
        ExecutorFilter filter = new ExecutorFilter(executor);

        filter.messageReceived(nextFilter, blockerSession, null);
        assertTrue("Worker did not start", blockerQueued.await(4L, TimeUnit.SECONDS));
        
        // Execute system under test.
        for (int i = 0; i < taskCount; i++) {
            int sessionIndex = i % sessions.size();
            
            LastActivityTracker currentSession = sessions.get(sessionIndex);
            filter.messageReceived(nextFilter, currentSession, null);

            Throwable throwable = nextFilter.throwable.get();

            if (throwable != null) {
                throw throwable;
            }
        }

        sessionQueued.countDown();

        // we have to wait before shutdown as shutdown events as prioritized
        Thread.sleep(4L);

        executor.shutdown();
        assertTrue(executor.awaitTermination(16L, TimeUnit.SECONDS));

        for (LastActivityTracker session : sessions) {
            LOGGER.error("Session {} data [lastActivity={},messageCount={}]", session, session.lastActivity.get(),
                    session.messageCount.get());
        }

        for (LastActivityTracker session : sessions) {
            assertEquals(tasksPerSessionCount, session.messageCount.get());

            if (session != preferredSession) {
                long lastActivity = session.lastActivity.get();
                long preferredActivity = preferredSession.lastActivity.get();

                if (lastActivity <= preferredActivity) {
                    LOGGER.error("Session {} finished at {}, before preferred session {} finished at {}", session,
                            lastActivity, preferredSession, preferredActivity);
                }

                assertTrue("All other sessions should have finished later than the preferred session (but at least one did not).",
                    lastActivity > preferredActivity);
            }
        }
    }

    /**
     * A comparator that prefers a particular session.
     */
    private static class UnfairComparator implements Comparator<IoSession> {

        private final IoSession preferred;

        public UnfairComparator(IoSession preferred) {
            this.preferred = preferred;
        }

        @Override
        public int compare(IoSession o1, IoSession o2) {
            if (o1 == preferred) {
                return -1;
            }

            if (o2 == preferred) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * A session that tracks the timestamp of the last activity.
     */
    private static class LastActivityTracker extends DummySession {

        private final AtomicLong lastActivity = new AtomicLong();
        private final AtomicInteger messageCount = new AtomicInteger();

        public void markMessageReceived() {
            lastActivity.set(System.nanoTime());
            messageCount.incrementAndGet();
        }
    }

    /**
     * A filter that simulates a non-negligible amount of work.
     */
    private static class MockWorkFilter extends IoFilterAdapter.NextFilterAdapter {

        private final IoSession blockerSession;
        private final CountDownLatch blockerQueued;
        private final CountDownLatch sessionsQueued;
        private final AtomicReference<Throwable> throwable = new AtomicReference<>();

        public MockWorkFilter(IoSession blockerSession, CountDownLatch blockerQueued, CountDownLatch sessionsQueued) {
            this.blockerSession = blockerSession;
            this.blockerQueued = blockerQueued;
            this.sessionsQueued = sessionsQueued;
        }

        public void exceptionCaught(IoSession session, Throwable cause) {
            throwable.compareAndSet(null, cause);
        }

        public void messageReceived(IoSession session, Object message) {
            try {
                if (session == blockerSession) {
                    // notify that the blocker session has been queued and await for other sessions to queue
                    blockerQueued.countDown();
                    sessionsQueued.await();
                } else {
                    ((LastActivityTracker) session).markMessageReceived();
                }
            } catch (InterruptedException e) {
                throwable.compareAndSet(null, e);
            }
        }
    }
}
