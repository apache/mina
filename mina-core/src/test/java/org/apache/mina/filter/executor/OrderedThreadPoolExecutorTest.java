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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.junit.Test;

/**
 * Tests that verify the functionality provided by the implementation of
 * {@link OrderedThreadPoolExecutor}.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class OrderedThreadPoolExecutorTest
{
    /**
     * Tests the state of {@link OrderedThreadPoolExecutor#idleWorkers} and {@link OrderedThreadPoolExecutor#workers}
     * after a RuntimeException is thrown when the {@link OrderedThreadPoolExecutor.Worker} is running.
     *
     * Note that the implementation of this test is <em>not representative</em> of how tasks are normally executed, as
     * tasks would ordinarily be 'wrapped' in a FilterChain. Most FilterChain implementations would catch the
     * RuntimeException that is being used in the implementation of this test. The purpose of this test is to verify
     * Worker's behavior when a RuntimeException is thrown during execution occurs (even if that RuntimeException cannot
     * occur in the way that this test simulates it). A test that implements the execution in a more realistic manner is
     * provided in {@link org.apache.mina.transport.socket.nio.DIRMINA1156Test}.
     *
     * @see org.apache.mina.transport.socket.nio.DIRMINA1156Test
     * @see <a href="https://issues.apache.org/jira/browse/DIRMINA-1132">Issue DIRMINA-1156: Inconsistent worker / idleWorker in ThreadPoolExecutors</a>
     */
    @Test
    public void testRuntimeExceptionInWorkerRun() throws Throwable
    {
        // Set up test fixture.
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(corePoolSize,1);
        IoFilter.NextFilter nextFilter = new NextFilterAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) {
                throw new RuntimeException("A RuntimeException thrown during unit testing.");
            }
        };
        DummySession session = new DummySession();
        ExecutorFilter filter = new ExecutorFilter(executor);

        try {
            // Execute system under test.
            filter.messageReceived(nextFilter, session, null);

            // Shutting down and awaiting termination ensures that test execution blocks until Worker execution has happened.
            executor.shutdown();
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Bug in test implementation.");
            }

            // Verify results.
            final Field idleWorkersField = OrderedThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
            idleWorkersField.setAccessible(true);
            final AtomicInteger idleWorkers = (AtomicInteger) idleWorkersField.get(executor);
            assertEquals("After all tasks have finished, the amount of workers that are idle should equal the amount of workers, but did not.", executor.getPoolSize(), idleWorkers.get());
        } finally {
            // Clean up test fixture.
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Tests the state of {@link OrderedThreadPoolExecutor#idleWorkers} and {@link OrderedThreadPoolExecutor#workers}
     * after an Error is thrown when the {@link OrderedThreadPoolExecutor.Worker} is running.
     *
     * Note that the implementation of this test is <em>not representative</em> of how tasks are normally executed, as
     * tasks would ordinarily be 'wrapped' in a FilterChain. Most FilterChain implementations would catch the Error that
     * is being used in the implementation of this test. The purpose of this test is to verify Worker's behavior when an
     * Error is thrown during execution occurs (even if that Error cannot occur in the way that this test simulates it).
     * A test that implements the execution in a more realistic manner is provided in
     * {@link org.apache.mina.transport.socket.nio.DIRMINA1156Test}.
     *
     * @see org.apache.mina.transport.socket.nio.DIRMINA1156Test
     * @see <a href="https://issues.apache.org/jira/browse/DIRMINA-1132">Issue DIRMINA-1156: Inconsistent worker / idleWorker in ThreadPoolExecutors</a>
     */
    @Test
    public void testErrorInWorkerRun() throws Throwable
    {
        // Set up test fixture.
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(corePoolSize,1);
        IoFilter.NextFilter nextFilter = new NextFilterAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) {
                throw new Error("An Error thrown during unit testing.");
            }
        };
        DummySession session = new DummySession();
        ExecutorFilter filter = new ExecutorFilter(executor);

        try {
            // Execute system under test.
            filter.messageReceived(nextFilter, session, null);

            // Ensure that the task has been executed in the executor.
            executor.shutdown(); // Shutting down and awaiting termination ensures that test execution blocks until Worker execution has happened.
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Bug in test implementation.");
            }

            // Verify results.
            final Field idleWorkersField = OrderedThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
            idleWorkersField.setAccessible(true);
            final AtomicInteger idleWorkers = (AtomicInteger) idleWorkersField.get(executor);
            assertEquals("After all tasks have finished, the amount of workers that are idle should equal the amount of workers, but did not.", executor.getPoolSize(), idleWorkers.get());
        } finally {
            // Clean up test fixture.
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Empty implementation of IoFilter.NextFilterAdapter, intended to facilitate easy subclassing.
     */
    private abstract static class NextFilterAdapter implements IoFilter.NextFilter {
        public void sessionOpened(IoSession session) {}
        public void sessionClosed(IoSession session) {}
        public void sessionIdle(IoSession session, IdleStatus status) {}
        public void exceptionCaught(IoSession session, Throwable cause) {}
        public void inputClosed(IoSession session) {}
        public void messageReceived(IoSession session, Object message) {}
        public void messageSent(IoSession session, WriteRequest writeRequest) {}
        public void filterWrite(IoSession session, WriteRequest writeRequest) {}
        public void filterClose(IoSession session) {}
        public void sessionCreated(IoSession session) {}
    }
}
