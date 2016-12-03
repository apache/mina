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
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * A decorator that makes the specified {@link Queue} thread-safe.
 * Like any other synchronizing wrappers, iteration is not thread-safe.
 * 
 * @param <E> The type of elements stored in the queue
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SynchronizedQueue<E> implements Queue<E>, Serializable {

    private static final long serialVersionUID = -1439242290701194806L;

    private final Queue<E> queue;

    /**
     * Create a new SynchronizedQueue instance
     * 
     * @param queue The queue
     */
    public SynchronizedQueue(Queue<E> queue) {
        this.queue = queue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean add(E e) {
        return queue.add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E element() {
        return queue.element();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean offer(E e) {
        return queue.offer(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E peek() {
        return queue.peek();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E poll() {
        return queue.poll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E remove() {
        return queue.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        return queue.addAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        queue.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean contains(Object o) {
        return queue.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Iterator<E> iterator() {
        return queue.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean remove(Object o) {
        return queue.remove(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int size() {
        return queue.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object[] toArray() {
        return queue.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean equals(Object obj) {
        return queue.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int hashCode() {
        return queue.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString() {
        return queue.toString();
    }
}