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

import java.util.NoSuchElementException;

/**
 * A {@link IoQueue} which is based on
 * <a href="http://en.wikipedia.org/wiki/Circular_buffer">an array-backed
 * circular buffer</a>.  This queue is unbound - it automatically expands
 * or shrinks its capacity as needed.
 * <p>
 * This queue is not thread safe.  {@link SynchronizedIoQueue} should be used
 * together to guarantee thread safety.
 *
 * @param <E> the type of the queue's elements
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class CircularIoQueue<E> extends AbstractIoQueue<E> {

    private static final int DEFAULT_CAPACITY = 4;

    private final int initialCapacity;
    // XXX: This volatile keyword here is a workaround for SUN Java Compiler
    //      bug, which produces buggy byte code.  I don't even know why adding
    //      a volatile fixes the problem.  Eclipse Java Compiler seems to
    //      produce correct byte code.
    private volatile E[] items;
    private int mask;
    private int first;
    private int last;
    private boolean full;
    private int shrinkThreshold;

    /**
     * Creates a new empty queue.
     */
    public CircularIoQueue() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new empty queue with the specified initial capacity.
     */
    public CircularIoQueue(int initialCapacity) {
        int actualCapacity = normalizeCapacity(initialCapacity);
        items = newElementArray(actualCapacity);
        mask = actualCapacity - 1;
        this.initialCapacity = actualCapacity;
        this.shrinkThreshold = 0;
    }

    private static int normalizeCapacity(int initialCapacity) {
        int actualCapacity = 1;
        while (actualCapacity < initialCapacity) {
            actualCapacity <<= 1;
            if (actualCapacity < 0) {
                actualCapacity = 1 << 30;
                break;
            }
        }
        return actualCapacity;
    }

    @Override
    protected final void doOffer(E e) {
        if (e == null) {
            throw new NullPointerException("element");
        }

        expandIfNeeded();
        items[last] = e;
        increaseSize();
    }

    @Override
    protected final E doPoll() {
        if (isEmpty()) {
            return null;
        }

        E ret = items[first];
        items[first] = null;
        decreaseSize();

        if (first == last) {
            first = last = 0;
        }

        shrinkIfNeeded();
        return ret;
    }

    public final E element(int index) {
        checkIndex(index);
        return items[getRealIndex(index)];
    }

    @Override
    public int size() {
        if (full) {
            return items.length;
        }

        if (last >= first) {
            return last - first;
        } else {
            return last - first + items.length;
        }
    }

    public E peek() {
        if (isEmpty()) {
            return null;
        }

        return items[first];
    }

    private void checkIndex(int idx) {
        if (idx < 0 || idx >= size()) {
            throw new NoSuchElementException(String.valueOf(idx));
        }
    }

    private int getRealIndex(int idx) {
        return first + idx & mask;
    }

    private void increaseSize() {
        last = last + 1 & mask;
        full = first == last;
    }

    private void decreaseSize() {
        first = first + 1 & mask;
        full = false;
    }

    private void expandIfNeeded() {
        if (full) {
            // expand queue
            final int oldLen = items.length;
            final int newLen = oldLen << 1;
            E[] tmp = newElementArray(newLen);

            if (first < last) {
                System.arraycopy(items, first, tmp, 0, last - first);
            } else {
                System.arraycopy(items, first, tmp, 0, oldLen - first);
                System.arraycopy(items, 0, tmp, oldLen - first, last);
            }

            first = 0;
            last = oldLen;
            items = tmp;
            mask = tmp.length - 1;
            if (newLen >>> 3 > initialCapacity) {
                shrinkThreshold = newLen >>> 3;
            }
        }
    }

    private void shrinkIfNeeded() {
        int size = size();
        if (size <= shrinkThreshold) {
            // shrink queue
            final int oldLen = items.length;
            int newLen = normalizeCapacity(size);
            if (size == newLen) {
                newLen <<= 1;
            }

            if (newLen >= oldLen) {
                return;
            }

            if (newLen < initialCapacity) {
                if (oldLen == initialCapacity) {
                    return;
                } else {
                    newLen = initialCapacity;
                }
            }

            E[] tmp = newElementArray(newLen);

            // Copy only when there's something to copy.
            if (size > 0) {
                if (first < last) {
                    System.arraycopy(items, first, tmp, 0, last - first);
                } else {
                    System.arraycopy(items, first, tmp, 0, oldLen - first);
                    System.arraycopy(items, 0, tmp, oldLen - first, last);
                }
            }

            first = 0;
            last = size;
            items = tmp;
            mask = tmp.length - 1;
            shrinkThreshold = 0;
        }
    }

    @SuppressWarnings("unchecked")
    private E[] newElementArray(int length) {
        return (E[]) new Object[length];
    }
}
