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

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.FilterEvent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that verify the functionality provided by the implementation of
 * {@link PriorityThreadPoolExecutor}.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class PriorityThreadPoolExecutorTest {
    /**
     * Tests that verify the functionality provided by the implementation of
     * {@link org.apache.mina.filter.executor.PriorityThreadPoolExecutor.SessionEntry}
     * .
     *
     * This test asserts that, without a provided comparator, entries are
     * considered equal, when they reference the same session.
     */
    @Test
    public void fifoEntryTestNoComparatorSameSession() throws Exception {
	// Set up fixture.
	final IoSession session = new DummySession();
	final PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(session, null);
	final PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(session, null);

	// Execute system under test.
	final int result = first.compareTo(last);

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
     */
    @Test
    public void fifoEntryTestNoComparatorDifferentSession() throws Exception {
	// Set up fixture (the order in which the entries are created is
	// relevant here!)
	final PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), null);
	final PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), null);

	// Execute system under test.
	final int result = first.compareTo(last);

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
     */
    @Test
    public void fifoEntryTestWithComparatorSameSession() throws Exception {
	// Set up fixture.
	final IoSession session = new DummySession();
	final int predeterminedResult = 3853;
	final Comparator<IoSession> comparator = new Comparator<IoSession>() {
	    @Override
	    public int compare(IoSession o1, IoSession o2) {
		return predeterminedResult;
	    }
	};

	final PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(session, comparator);
	final PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(session, comparator);

	// Execute system under test.
	final int result = first.compareTo(last);

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
     */
    @Test
    public void fifoEntryTestComparatorDifferentSession() throws Exception {
	// Set up fixture (the order in which the entries are created is
	// relevant here!)
	final int predeterminedResult = 3853;
	final Comparator<IoSession> comparator = new Comparator<IoSession>() {
	    @Override
	    public int compare(IoSession o1, IoSession o2) {
		return predeterminedResult;
	    }
	};
	final PriorityThreadPoolExecutor.SessionEntry first = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), comparator);
	final PriorityThreadPoolExecutor.SessionEntry last = new PriorityThreadPoolExecutor.SessionEntry(new DummySession(), comparator);

	// Execute system under test.
	final int result = first.compareTo(last);

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
     */
    @Test
    public void testPrioritisation() throws Throwable {
	// Set up fixture.
	final MockWorkFilter nextFilter = new MockWorkFilter();
	final List<LastActivityTracker> sessions = new ArrayList<>();
	for (int i = 0; i < 10; i++) {
	    sessions.add(new LastActivityTracker());
	}
	final LastActivityTracker preferredSession = sessions.get(4); // prefer
								      // an
								      // arbitrary
								      // session
								      // (but
								      // not the
								      // first
								      // or last
								      // session,
								      // for
								      // good
								      // measure).
	final Comparator<IoSession> comparator = new UnfairComparator(preferredSession);
	final int maximumPoolSize = 1; // keep this low, to force resource
				       // contention.
	final int amountOfTasks = 400;

	final ExecutorService executor = new PriorityThreadPoolExecutor(maximumPoolSize, comparator);
	final ExecutorFilter filter = new ExecutorFilter(executor);

	// Execute system under test.
	int sessionIndex = 0;
	for (int i = 0; i < amountOfTasks; i++) {
	    if (++sessionIndex >= sessions.size()) {
		sessionIndex = 0;
	    }

	    filter.messageReceived(nextFilter, sessions.get(sessionIndex), null);

	    if (nextFilter.throwable != null) {
		throw nextFilter.throwable;
	    }
	}

	executor.shutdown();

	// Verify results.
	executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

	for (final LastActivityTracker session : sessions) {
	    if (session != preferredSession) {
		assertTrue("All other sessions should have finished later than the preferred session (but at least one did not).", session.lastActivity > preferredSession.lastActivity);
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
     * A session that tracks the timestamp of last activity.
     */
    private static class LastActivityTracker extends DummySession {
	long lastActivity = System.currentTimeMillis();

	public synchronized void setLastActivity() {
	    lastActivity = System.currentTimeMillis();
	}
    }

    /**
     * A filter that simulates a non-negligible amount of work.
     */
    private static class MockWorkFilter implements IoFilter.NextFilter {
	Throwable throwable;

	public void sessionOpened(IoSession session) {
	    // Do nothing
	}

	public void sessionClosed(IoSession session) {
	    // Do nothing
	}

	public void sessionIdle(IoSession session, IdleStatus status) {
	    // Do nothing
	}

	public void exceptionCaught(IoSession session, Throwable cause) {
	    // Do nothing
	}

	public void inputClosed(IoSession session) {
	    // Do nothing
	}

	public void messageReceived(IoSession session, Object message) {
	    try {
		Thread.sleep(20); // mimic work.
		((LastActivityTracker) session).setLastActivity();
	    } catch (Exception e) {
		if (this.throwable == null) {
		    this.throwable = e;
		}
	    }
	}

	public void messageSent(IoSession session, WriteRequest writeRequest) {
	    // Do nothing
	}

	public void filterWrite(IoSession session, WriteRequest writeRequest) {
	    // Do nothing
	}

	public void filterClose(IoSession session) {
	    // Do nothing
	}

	public void sessionCreated(IoSession session) {
	    // Do nothing
	}

	@Override
	public void event(IoSession session, FilterEvent event) {
	    // TODO Auto-generated method stub

	}
    }
}
