/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.mina.api.IoFutureListener;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.Matchers;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractIoFutureTest {

    @Test
    public void testSet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setResult(true);

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testSetListeners() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        final IoFutureListener<Boolean> listener = mock(IoFutureListener.class);

        future.register(listener);

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setResult(true);

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        verify(listener).completed(true);
        verify(listener, never()).exception(Matchers.<Throwable> any());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testSetListenersAlreadySet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        final IoFutureListener<Boolean> listener = mock(IoFutureListener.class);

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setResult(true);
        future.register(listener);

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        verify(listener).completed(true);
        verify(listener, never()).exception(Matchers.<Throwable> any());
    }

    @Test
    public void testTimedGet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    future.setResult(true);
                } catch (final InterruptedException ignored) {
                }
            }
        }).start();

        try {
            assertTrue(future.get(1, TimeUnit.DAYS));
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());
        } catch (final InterruptedException e) {
            fail("This future was not interrupted");
        } catch (final ExecutionException ee) {
            fail("This future did not have an execution exception");
        } catch (final TimeoutException e) {
            fail("This future was not interrupted");
        }
    }

    @Test
    public void testException() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setException(new NullPointerException());

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        try {
            future.get();
            fail("This future had an execution exception");
        } catch (final InterruptedException e) {
            fail("This future was not interrupted");
        } catch (final ExecutionException ee) {
            assertTrue(ee.getCause() instanceof NullPointerException);
        }

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testExceptionListeners() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        final IoFutureListener<Boolean> listener = mock(IoFutureListener.class);

        future.register(listener);

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setException(new NullPointerException());

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        verify(listener).exception(argThat(matchesExecutionException()));
        verify(listener, never()).completed(Matchers.<Boolean> any());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testExceptionListenersExceptionAlreadySet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        final IoFutureListener<Boolean> listener = mock(IoFutureListener.class);

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setException(new NullPointerException());
        future.register(listener);

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        verify(listener).exception(argThat(matchesExecutionException()));
        verify(listener, never()).completed(Matchers.<Boolean> any());
    }

    @Test
    public void testImmediateExceptionForTimedGet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.setException(new NullPointerException());

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        try {
            future.get(1, TimeUnit.DAYS);
            fail("This future had an execution exception");
        } catch (final InterruptedException e) {
            fail("This future was not interrupted");
        } catch (final ExecutionException ee) {
            assertTrue(ee.getCause() instanceof NullPointerException);
        } catch (final TimeoutException e) {
            fail("This future was not interrupted");
        }

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testTimedExceptionForTimedGet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    future.setException(new NullPointerException());
                } catch (final InterruptedException ignored) {
                }
            }
        }).start();

        try {
            assertTrue(future.get(1, TimeUnit.DAYS));
        } catch (final InterruptedException e) {
            fail("This future was not interrupted");
        } catch (final ExecutionException ee) {
        } catch (final TimeoutException e) {
            fail("This future was not interrupted");
        }

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testCancel() throws Exception {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        doReturn(true).when(future).cancelOwner(anyBoolean());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());
        assertTrue(future.cancel(false));
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        assertFalse(future.cancel(false));
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        try {
            future.get();
            fail("This future was canceled");
        } catch (final CancellationException ignore) {
        }
    }

    @Test
    public void testCancelUncancelableOwner() throws Exception {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        doReturn(false).when(future).cancelOwner(anyBoolean());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());
        assertFalse(future.cancel(false));
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testCancelFinishedFuture() throws Exception {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        doReturn(true).when(future).cancelOwner(anyBoolean());

        future.setResult(true);

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertFalse(future.cancel(false));
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        assertTrue(future.get());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testCanceledListeners() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        doReturn(true).when(future).cancelOwner(anyBoolean());
        final IoFutureListener<Boolean> listener = mock(IoFutureListener.class);

        future.register(listener);

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.cancel(true);

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        verify(listener).exception(Matchers.<CancellationException> any());
        verify(listener, never()).completed(Matchers.<Boolean> any());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testCanceledListenersAlreadySet() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());
        doReturn(true).when(future).cancelOwner(anyBoolean());
        final IoFutureListener<Boolean> listener = mock(IoFutureListener.class);

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        future.cancel(true);
        future.register(listener);

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        verify(listener).exception(Matchers.<CancellationException> any());
        verify(listener, never()).completed(Matchers.<Boolean> any());
    }

    @Test
    public void testTimeout() {
        final MockAbstractIoFuture<Boolean> future = spy(new MockAbstractIoFuture<Boolean>());

        assertFalse(future.isCancelled());
        assertFalse(future.isDone());

        try {
            future.get(10, TimeUnit.MILLISECONDS);
            fail("This future has timed out");
        } catch (final InterruptedException e) {
            fail("This future was not interrupted");
        } catch (final ExecutionException ee) {
            fail("This future did not have an execution exception");
        } catch (final TimeoutException e) {
        }
    }

    private static Matcher<Throwable> matchesExecutionException() {
        return new BaseMatcher<Throwable>() {
            @Override
            public boolean matches(final Object item) {
                return item instanceof ExecutionException
                        && ((ExecutionException) item).getCause() instanceof NullPointerException;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("ExecutionException(NullPointerException)");
            }
        };
    }

    public static class MockAbstractIoFuture<V> extends AbstractIoFuture<V> {

        @Override
        protected boolean cancelOwner(final boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }
    }
}
