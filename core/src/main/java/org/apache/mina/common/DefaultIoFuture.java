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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * A default implementation of {@link IoFuture}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFuture implements IoFuture {

    private static final int DEAD_LOCK_CHECK_INTERVAL = 5000;

    private final IoSession session;
    private final Object lock;
    private IoFutureListener<?> firstListener;
    private List<IoFutureListener<?>> otherListeners;
    private Object result;
    private boolean ready;
    private int waiters;

    /**
     * Creates a new instance.
     *
     * @param session an {@link IoSession} which is associated with this future
     */
    public DefaultIoFuture(IoSession session) {
        this.session = session;
        this.lock = this;
    }

    public IoSession getSession() {
        return session;
    }

    public void join() {
        awaitUninterruptibly();
    }

    public boolean join(long timeoutMillis) {
        return awaitUninterruptibly(timeoutMillis);
    }

    public IoFuture await() throws InterruptedException {
        synchronized (lock) {
            while (!ready) {
                waiters++;
                try {
                    lock.wait(DEAD_LOCK_CHECK_INTERVAL);
                    checkDeadLock();
                } finally {
                    waiters--;
                }
            }
        }
        return this;
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return await(unit.toMillis(timeout));
    }

    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(timeoutMillis, true);
    }

    public IoFuture awaitUninterruptibly() {
        synchronized (lock) {
            while (!ready) {
                waiters++;
                try {
                    lock.wait(DEAD_LOCK_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                } finally {
                    waiters--;
                    if (!ready) {
                        checkDeadLock();
                    }
                }
            }
        }

        return this;
    }

    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return awaitUninterruptibly(unit.toMillis(timeout));
    }

    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        long startTime = timeoutMillis <= 0 ? 0 : System.currentTimeMillis();
        long waitTime = timeoutMillis;

        synchronized (lock) {
            if (ready) {
                return ready;
            } else if (waitTime <= 0) {
                return ready;
            }

            waiters++;
            try {
                for (;;) {
                    try {
                        lock.wait(Math.min(waitTime, DEAD_LOCK_CHECK_INTERVAL));
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        }
                    }

                    if (ready) {
                        return true;
                    } else {
                        waitTime = timeoutMillis
                                - (System.currentTimeMillis() - startTime);
                        if (waitTime <= 0) {
                            return ready;
                        }
                    }
                }
            } finally {
                waiters--;
                if (!ready) {
                    checkDeadLock();
                }
            }
        }
    }

    private void checkDeadLock() {
        IllegalStateException e = new IllegalStateException(
                "DEAD LOCK: " + IoFuture.class.getSimpleName() +
                ".await() was invoked from an I/O processor thread.  " +
                "Please use " + IoFutureListener.class.getSimpleName() +
                " or configure a proper thread model alternatively.");

        StackTraceElement[] stackTrace = e.getStackTrace();

        // Simple and quick check.
        for (StackTraceElement s: stackTrace) {
            if (AbstractIoProcessor.class.getName().equals(s.getClassName())) {
                throw e;
            }
        }

        // And then more precisely.
        for (StackTraceElement s: stackTrace) {
            try {
                Class<?> cls = DefaultIoFuture.class.getClassLoader().loadClass(s.getClassName());
                if (IoProcessor.class.isAssignableFrom(cls)) {
                    throw e;
                }
            } catch (Exception cnfe) {
                // Ignore
            }
        }
    }

    public boolean isReady() {
        synchronized (lock) {
            return ready;
        }
    }

    /**
     * Sets the result of the asynchronous operation, and mark it as finished.
     */
    protected void setValue(Object newValue) {
        synchronized (lock) {
            // Allow only once.
            if (ready) {
                return;
            }

            result = newValue;
            ready = true;
            if (waiters > 0) {
                lock.notifyAll();
            }
        }

        notifyListeners();
    }

    /**
     * Returns the result of the asynchronous operation.
     */
    protected Object getValue() {
        synchronized (lock) {
            return result;
        }
    }

    public IoFuture addListener(IoFutureListener<?> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (ready) {
                notifyNow = true;
            } else {
                if (firstListener == null) {
                    firstListener = listener;
                } else {
                    if (otherListeners == null) {
                        otherListeners = new ArrayList<IoFutureListener<?>>(1);
                    }
                    otherListeners.add(listener);
                }
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
        return this;
    }

    public IoFuture removeListener(IoFutureListener<?> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (lock) {
            if (!ready) {
                if (listener == firstListener) {
                    if (otherListeners != null && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    otherListeners.remove(listener);
                }
            }
        }

        return this;
    }

    private void notifyListeners() {
        // There won't be any visibility problem or concurrent modification
        // because 'ready' flag will be checked against both addListener and
        // removeListener calls.
        if (firstListener != null) {
            notifyListener(firstListener);
            firstListener = null;

            if (otherListeners != null) {
                for (IoFutureListener<?> l : otherListeners) {
                    notifyListener(l);
                }
                otherListeners = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyListener(IoFutureListener l) {
        try {
            l.operationComplete(this);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
        }
    }
}
