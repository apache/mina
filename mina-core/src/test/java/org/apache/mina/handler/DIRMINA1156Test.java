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
package org.apache.mina.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.filter.executor.PriorityThreadPoolExecutor;
import org.apache.mina.filter.executor.UnorderedThreadPoolExecutor;
import org.junit.Test;

/**
 * Tests that reproduces a bug as described in issue DIRMINA-1156
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://issues.apache.org/jira/browse/DIRMINA-1156">DIRMINA-1156</a>
 */
public class DIRMINA1156Test
{
    /**
     * Tests the state of {@link OrderedThreadPoolExecutor#idleWorkers} and {@link OrderedThreadPoolExecutor#workers}
     * after an {@link Error} is thrown by a session's handler that was invoked through an OrderedThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testOrderedThreadPoolExecutorSessionHandlerThrowingError() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new Error("An Error thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
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
     * after a {@link RuntimeException} is thrown by a session's handler that was invoked through an
     * OrderedThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testOrderedThreadPoolExecutorSessionHandlerThrowingRuntimeException() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new RuntimeException("A RuntimeException thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
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
     * after a (checked) {@link Exception} is thrown by a session's handler that was invoked through an
     * OrderedThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testOrderedThreadPoolExecutorSessionHandlerThrowingCheckedException() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new Exception("A (checked) Exception thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
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
     * Tests the state of {@link UnorderedThreadPoolExecutor#idleWorkers} and {@link UnorderedThreadPoolExecutor#workers}
     * after an {@link Error} is thrown by a session's handler that was invoked through an UnorderedThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testUnorderedThreadPoolExecutorSessionHandlerThrowingError() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        UnorderedThreadPoolExecutor executor = new UnorderedThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new Error("An Error thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
            }

            // Verify results.
            final Field idleWorkersField = UnorderedThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
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
     * Tests the state of {@link UnorderedThreadPoolExecutor#idleWorkers} and {@link UnorderedThreadPoolExecutor#workers}
     * after a {@link RuntimeException} is thrown by a session's handler that was invoked through an
     * UnorderedThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testUnorderedThreadPoolExecutorSessionHandlerThrowingRuntimeException() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        UnorderedThreadPoolExecutor executor = new UnorderedThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new RuntimeException("A RuntimeException thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
            }

            // Verify results.
            final Field idleWorkersField = UnorderedThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
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
     * Tests the state of {@link UnorderedThreadPoolExecutor#idleWorkers} and {@link UnorderedThreadPoolExecutor#workers}
     * after a (checked) {@link Exception} is thrown by a session's handler that was invoked through an
     * UnorderedThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testUnorderedThreadPoolExecutorSessionHandlerThrowingCheckedException() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        UnorderedThreadPoolExecutor executor = new UnorderedThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new Exception("A (checked) Exception thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
            }

            // Verify results.
            final Field idleWorkersField = UnorderedThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
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
     * Tests the state of {@link PriorityThreadPoolExecutor#idleWorkers} and {@link PriorityThreadPoolExecutor#workers}
     * after an {@link Error} is thrown by a session's handler that was invoked through an PriorityThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testPriorityThreadPoolExecutorSessionHandlerThrowingError() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new Error("An Error thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
            }

            // Verify results.
            final Field idleWorkersField = PriorityThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
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
     * Tests the state of {@link PriorityThreadPoolExecutor#idleWorkers} and {@link PriorityThreadPoolExecutor#workers}
     * after a {@link RuntimeException} is thrown by a session's handler that was invoked through an
     * PriorityThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testPriorityThreadPoolExecutorSessionHandlerThrowingRuntimeException() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new RuntimeException("A RuntimeException thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
            }

            // Verify results.
            final Field idleWorkersField = PriorityThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
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
     * Tests the state of {@link PriorityThreadPoolExecutor#idleWorkers} and {@link PriorityThreadPoolExecutor#workers}
     * after a (checked) {@link Exception} is thrown by a session's handler that was invoked through an
     * PriorityThreadPoolExecutor.
     * 
     * @throws Exception exception
     */
    @Test
    public void testPriorityThreadPoolExecutorSessionHandlerThrowingCheckedException() throws Exception
    {
        // Set up test fixture.
        final boolean[] filterTriggered = {false}; // Used to verify the implementation of this test (to see if the Handler is invoked at all).
        int corePoolSize = 1; // Prevent an idle worker from being cleaned up, which would skew the results of this test.
        DummySession session = new DummySession();
        IoFilterChain chain = session.getFilterChain();
        PriorityThreadPoolExecutor executor = new PriorityThreadPoolExecutor(corePoolSize,1);
        chain.addLast("executor", new ExecutorFilter(executor));
        session.setHandler( new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                filterTriggered[0] = true;
                throw new Exception("A (checked) Exception thrown during unit testing.");
            }
        });

        // Execute system under test.
        try {
            chain.fireMessageReceived("foo");

            // Shutting down and awaiting termination ensures that test execution blocks until Handler invocation has happened.
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!filterTriggered[0]) {
                throw new IllegalStateException("Bug in test implementation: the session handler was never invoked.");
            }

            // Verify results.
            final Field idleWorkersField = PriorityThreadPoolExecutor.class.getDeclaredField("idleWorkers"); // Using reflection as the field is not accessible. It might be nicer to make the field package-protected for testing.
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
}
