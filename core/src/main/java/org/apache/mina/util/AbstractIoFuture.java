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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract implementation of {@link IoFuture}. Owners of this future must implement {@link #cancelOwner(boolean)} to
 * receive notifications of when the future should be canceled.
 * <p>
 * Concrete implementations of this abstract class should consider overriding the two methods
 * {@link #scheduleResult(org.apache.mina.api.IoFutureListener, Object)} and
 * {@link #scheduleException(org.apache.mina.api.IoFutureListener, Throwable)} so that listeners are called in a
 * separate thread. The default implementations may end up calling the listener in the same thread that is registering
 * the listener, before the registration has completed.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoFuture<V> implements IoFuture<V> {

    static final Logger LOG = LoggerFactory.getLogger(AbstractIoFuture.class);

    private final CountDownLatch latch = new CountDownLatch(1);

    private final List<IoFutureListener<V>> listeners = new ArrayList<IoFutureListener<V>>();

    private final AtomicReference<Object> result = new AtomicReference<Object>();

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public IoFuture<V> register(IoFutureListener<V> listener) {

        LOG.debug("registering listener {}", listener);

        synchronized (latch) {
            if (!isDone()) {
                LOG.debug("future is not done, adding listener to listener set");
                listeners.add(listener);
                listener = null;
            }
        }

        if (listener != null) {
            LOG.debug("future is done calling listener");
            Object object = result.get();

            if (object instanceof Throwable) {
                scheduleException(listener, (Throwable) object);
            } else {
                scheduleResult(listener, (V) object);
            }
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {

        LOG.debug("Attempting to cancel");

        CancellationException ce = null;
        synchronized (latch) {
            if (!isCancelled() && !isDone() && cancelOwner(mayInterruptIfRunning)) {

                LOG.debug("Successfully cancelled");

                ce = new CancellationException();
                result.set(ce);
            } else {
                LOG.debug("Unable to cancel");
            }

            latch.countDown();
        }

        if (ce != null) {
            LOG.debug("Calling listeners");

            for (IoFutureListener<V> listener : listeners) {
                scheduleException(listener, ce);
            }
        }

        return ce != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return result.get() instanceof CancellationException;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public V get() throws InterruptedException, ExecutionException {

        LOG.trace("Entering wait");
        latch.await();
        LOG.trace("Wait completed");

        if (isCancelled()) {
            throw new CancellationException();
        }

        Object object = result.get();

        if (object instanceof ExecutionException) {
            throw (ExecutionException) object;
        } else {
            return (V) object;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        LOG.trace("Entering wait");

        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }

        LOG.trace("Wait completed");

        if (isCancelled()) {
            throw new CancellationException();
        }

        Object object = result.get();

        if (object instanceof ExecutionException) {
            throw (ExecutionException) object;
        } else {
            return (V) object;
        }
    }

    /**
     * Notify the owner of this future that a client is attempting to cancel. This attempt will fail if the task has
     * already completed, has already been cancelled, or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called, this task should never run. If the task has already
     * started, then the <tt>mayInterruptIfRunning</tt> parameter determines whether the thread executing this task
     * should be interrupted in an attempt to stop the task.
     * <p/>
     * <p>
     * After this method returns, subsequent calls to {@link #isDone} will always return <tt>true</tt>. Subsequent calls
     * to {@link #isCancelled} will always return <tt>true</tt> if this method returned <tt>true</tt>.
     * <p/>
     * <b>Note:</b> implementations must never throw an exception.
     * 
     * @param mayInterruptIfRunning <tt>true</tt> if the owner executing this task should be interrupted; otherwise,
     *        in-progress tasks are allowed to complete
     * @return <tt>false</tt> if the task could not be cancelled, typically because it has already completed normally;
     *         <tt>true</tt> otherwise
     */
    protected abstract boolean cancelOwner(boolean mayInterruptIfRunning);

    /**
     * Default implementation to call a listener's {@link IoFutureListener#completed(Object)} method. Owners may
     * override this method so that the listener is called from a thread pool.
     * 
     * @param listener the listener to call
     * @param result the result to pass to the listener
     */
    protected void scheduleResult(IoFutureListener<V> listener, V result) {
        LOG.debug("Calling the default result scheduler");

        try {
            listener.completed(result);
        } catch (Exception e) {
            LOG.warn("Listener threw an exception", e);
        }
    }

    /**
     * Default implementation to call a listener's {@link IoFutureListener#exception(Throwable)} method. Owners may
     * override this method so that the listener is called from a thread pool.
     * 
     * @param listener the listener to call
     * @param throwable the exception to pass to the listener
     */
    protected void scheduleException(IoFutureListener<V> listener, Throwable throwable) {
        LOG.debug("Calling the default exception scheduler");

        try {
            listener.exception(throwable);
        } catch (Exception e) {
            LOG.warn("Listener threw an exception", e);
        }
    }

    /**
     * Set the future result of the executing task. Any {@link IoFutureListener}s are notified of the
     * 
     * @param value the value returned by the executing task.
     */
    protected final void setResult(V value) {
        assert !isDone();

        synchronized (latch) {
            result.set(value);
            latch.countDown();
        }

        for (IoFutureListener<V> listener : listeners) {
            scheduleResult(listener, value);
        }

        listeners.clear();
    }

    /**
     * Set the future result as a {@link Throwable}, indicating that a throwable was thrown while executing the task.
     * This value is usually set by the future result owner.
     * <p/>
     * Any {@link IoFutureListener}s are notified of the exception.
     * 
     * @param t the throwable that was thrown while executing the task.
     */
    protected final void setException(Throwable t) {
        assert !isDone();

        ExecutionException ee = new ExecutionException(t);

        synchronized (latch) {
            result.set(ee);
            latch.countDown();
        }

        for (IoFutureListener<V> listener : listeners) {
            scheduleException(listener, ee);
        }

        listeners.clear();
    }
}
