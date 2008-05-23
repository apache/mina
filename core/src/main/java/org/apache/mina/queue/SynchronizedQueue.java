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

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * A decorator that makes the specified {@link Queue} thread-safe.
 * Like any other synchronizing wrappers, its iteration is not thread-safe.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SynchronizedQueue<E> extends QueueWrapper<E> {

    /**
     * Creates a new instance.
     */
    public SynchronizedQueue(Queue<E> queue) {
        super(queue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean add(E e) {
        return super.add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E element() {
        return super.element();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean offer(E e) {
        return super.offer(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E peek() {
        return super.peek();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E poll() {
        return super.poll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized E remove() {
        return super.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        return super.addAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        super.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean contains(Object o) {
        return super.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Iterator<E> iterator() {
        return super.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean remove(Object o) {
        return super.remove(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int size() {
        return super.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object[] toArray() {
        return super.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return super.toArray(a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString() {
        return super.toString();
    }
}