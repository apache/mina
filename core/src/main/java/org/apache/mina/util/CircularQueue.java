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
package org.apache.mina.util;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * A unbounded circular queue based on array.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class CircularQueue<E> extends AbstractList<E> implements List<E>, Queue<E>, Serializable {

    private static final long serialVersionUID = 3993421269224511264L;

    private static final int DEFAULT_CAPACITY = 4;

    private final int initialCapacity;
    private Object[] items;
    private int mask;
    private int first = 0;
    private int last = 0;
    private boolean full;

    /**
     * Construct a new, empty queue.
     */
    public CircularQueue() {
        this(DEFAULT_CAPACITY);
    }
    
    public CircularQueue(int initialCapacity) {
        int actualCapacity = normalizeCapacity(initialCapacity);
        items = new Object[actualCapacity];
        mask = actualCapacity - 1;
        this.initialCapacity = actualCapacity;
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

    /**
     * Returns the capacity of this queue.
     */
    public int capacity() {
        return items.length;
    }

    @Override
    public void clear() {
        if (!isEmpty()) {
            Arrays.fill(items, null);
            first = 0;
            last = 0;
            full = false;
            shrinkIfNeeded();
        }
    }

    @SuppressWarnings("unchecked")
    public E poll() {
        if (isEmpty()) {
            return null;
        }

        Object ret = items[first];
        items[first] = null;
        decreaseSize();
        
        if (first == last) {
            first = last = 0;
        }

        shrinkIfNeeded();
        return (E) ret;
    }

    public boolean offer(E item) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        expandIfNeeded();
        items[last] = item;
        increaseSize();
        return true;
    }

    @SuppressWarnings("unchecked")
    public E peek() {
        if (isEmpty()) {
            return null;
        }

        return (E) items[first];
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int idx) {
        checkIndex(idx);
        return (E) items[getRealIndex(idx)];
    }

    @Override
    public boolean isEmpty() {
        return (first == last) && !full;
    }

    @Override
    public int size() {
        if (full) {
            return capacity();
        }
        
        if (last >= first) {
            return last - first;
        } else {
            return last - first + capacity();
        }
    }
    
    @Override
    public String toString() {
        return "first=" + first + ", last=" + last + ", size=" + size()
                + ", mask = " + mask;
    }

    private void checkIndex(int idx) {
        if (idx < 0 || idx >= size()) {
            throw new IndexOutOfBoundsException(String.valueOf(idx));
        }
    }

    private int getRealIndex(int idx) {
        return (first + idx) & mask;
    }

    private void increaseSize() {
        last = (last + 1) & mask;
        full = first == last;
    }

    private void decreaseSize() {
        first = (first + 1) & mask;
        full = false;
    }

    private void expandIfNeeded() {
        if (full) {
            // expand queue
            final int oldLen = items.length;
            Object[] tmp = new Object[oldLen << 1];
    
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
        }
    }
    
    private void shrinkIfNeeded() {
        int size = size();
        if (size < (capacity() >>> 1)) {
            // shrink queue
            final int oldLen = items.length;
            int newLen = normalizeCapacity(size);
            if (newLen < initialCapacity) {
                if (oldLen == initialCapacity) {
                    return;
                } else {
                    newLen = initialCapacity;
                }
            }
            
            Object[] tmp = new Object[newLen];
    
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
        }
    }

    @Override
    public boolean add(E o) {
        return offer(o);
    }

    @SuppressWarnings("unchecked")
    @Override
    public E set(int idx, E o) {
        checkIndex(idx);

        int realIdx = getRealIndex(idx);
        Object old = items[realIdx];
        items[realIdx] = o;
        return (E) old;
    }

    @Override
    public void add(int idx, E o) {
        if (idx == size()) {
            offer(o);
            return;
        }

        checkIndex(idx);
        expandIfNeeded();

        int realIdx = getRealIndex(idx);

        // Make a room for a new element.
        if (first < last) {
            System
                    .arraycopy(items, realIdx, items, realIdx + 1, last
                            - realIdx);
        } else {
            if (realIdx >= first) {
                System.arraycopy(items, 0, items, 1, last);
                items[0] = items[items.length - 1];
                System.arraycopy(items, realIdx, items, realIdx + 1,
                        items.length - realIdx - 1);
            } else {
                System.arraycopy(items, realIdx, items, realIdx + 1, last
                        - realIdx);
            }
        }

        items[realIdx] = o;
        increaseSize();
    }

    @SuppressWarnings("unchecked")
    @Override
    public E remove(int idx) {
        if (idx == 0) {
            return poll();
        }

        checkIndex(idx);

        int realIdx = getRealIndex(idx);
        Object removed = items[realIdx];

        // Remove a room for the removed element.
        if (first < last) {
            System.arraycopy(items, first, items, first + 1, realIdx - first);
        } else {
            if (realIdx >= first) {
                System.arraycopy(items, first, items, first + 1, realIdx
                        - first);
            } else {
                System.arraycopy(items, 0, items, 1, realIdx);
                items[0] = items[items.length - 1];
                System.arraycopy(items, first, items, first + 1, items.length
                        - first - 1);
            }
        }

        items[first] = null;
        decreaseSize();

        shrinkIfNeeded();
        return (E) removed;
    }

    public E remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return poll();
    }

    public E element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return peek();
    }
}