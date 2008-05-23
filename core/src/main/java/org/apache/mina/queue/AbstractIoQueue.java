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

import java.util.AbstractQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A skeletal implementation of {@link IoQueue} which lessens the burden of
 * writing a new {@link IoQueue} implementation.
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 *
 * @param <E> the type of the queue's elements
 */
public abstract class AbstractIoQueue<E> extends AbstractQueue<E> implements IoQueue<E> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIoQueue.class);

    private volatile IoQueueListener<? super E>[] listeners = newListenerArray(0);

    public final void addListener(IoQueueListener<? super E> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (this) {
            IoQueueListener<? super E>[] oldListeners = this.listeners;
            IoQueueListener<? super E>[] newListeners =
                newListenerArray(oldListeners.length + 1);
            System.arraycopy(
                    oldListeners, 0, newListeners, 0, oldListeners.length);
            newListeners[oldListeners.length] = listener;
            this.listeners = newListeners;
        }
    }

    public final void removeListener(IoQueueListener<? super E> listener) {
        synchronized (this) {
            int index = -1;
            IoQueueListener<? super E>[] oldListeners = this.listeners;
            for (int i = 0; i < oldListeners.length; i ++) {
                if (oldListeners[i] == listener) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                return;
            }

            IoQueueListener<? super E>[] newListeners =
                newListenerArray(oldListeners.length - 1);

            if (index == 0) {
                System.arraycopy(
                        oldListeners, 1, newListeners, 0, newListeners.length);

            } else if (index == newListeners.length) {
                System.arraycopy(
                        oldListeners, 0, newListeners, 0, newListeners.length);

            } else {
                System.arraycopy(
                        oldListeners, 0, newListeners, 0, index);
                System.arraycopy(
                        oldListeners, index + 1,
                        newListeners, index, newListeners.length - index);
            }

            this.listeners = newListeners;
        }
    }

    public final boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException("element");
        }

        try {
            if (!accept(e)) {
                return false;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Error ex) {
            throw ex;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to accept: " + e, t);
        }

        if (!doOffer(e)) {
            return false;
        }
        offered(e);
        return true;
    }

    public final E poll() {
        E e = doPoll();
        if (e == null) {
            return null;
        }

        polled(e);
        return e;
    }

    /**
     * Performs the actual insertion operation.
     *
     * @param e an element to add to the tail of this queue.
     */
    protected abstract boolean doOffer(E e);

    /**
     * Performs the actual removal operation.
     *
     * @return the removed head object if this queue is not empty.
     *         <tt>null</tt> if this queue is empty.
     */
    protected abstract E doPoll();

    /**
     * Calls {@link IoQueueListener#accept(IoQueue, Object)} for all
     * registered listeners and returns if the element should be accepted to
     * this queue or not.  This operation fails fast, which means that
     * this method will return <tt>false</tt> as soon as any listener
     * returns <tt>false</tt>, not iterating the whole listeners.
     */
    private boolean accept(E element) throws Exception {
        IoQueueListener<? super E>[] listeners = this.listeners;
        for (IoQueueListener<? super E> l: listeners) {
            if (!l.accept(this, element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calls {@link IoQueueListener#offered(IoQueue, Object)} for all
     * registered listeners.
     */
    private void offered(E element) {
        IoQueueListener<? super E>[] listeners = this.listeners;
        for (IoQueueListener<? super E> l: listeners) {
            try {
                l.offered(this, element);
            } catch (Throwable t) {
                LOG.warn("Exception raised from an IoQueueListener.", t);
            }
        }
    }

    /**
     * Calls {@link IoQueueListener#polled(IoQueue, Object)} for all
     * registered listeners.
     */
    private void polled(E element) {
        IoQueueListener<? super E>[] listeners = this.listeners;
        for (IoQueueListener<? super E> l: listeners) {
            try {
                l.offered(this, element);
            } catch (Throwable t) {
                LOG.warn("Exception raised from an IoQueueListener.", t);
            }
        }
    }

    /**
     * Creates a new listener array of the specified length.
     */
    @SuppressWarnings("unchecked")
    private IoQueueListener<? super E>[] newListenerArray(int length) {
        return new IoQueueListener[length];
    }
}
