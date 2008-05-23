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
 * A skeletal decorator which wraps an existing {@link Queue} instance.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class QueueWrapper<E> implements Queue<E> {

    protected final Queue<E> q;

    /**
     * Creates a new instance.
     */
    public QueueWrapper(Queue<E> queue) {
        if (queue == null) {
            throw new NullPointerException("queue");
        }
        this.q = queue;
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(E e) {
        return q.add(e);
    }

    /**
     * {@inheritDoc}
     */
    public E element() {
        return q.element();
    }

    /**
     * {@inheritDoc}
     */
    public boolean offer(E e) {
        return q.offer(e);
    }

    /**
     * {@inheritDoc}
     */
    public E peek() {
        return q.peek();
    }

    /**
     * {@inheritDoc}
     */
    public E poll() {
        return q.poll();
    }

    /**
     * {@inheritDoc}
     */
    public E remove() {
        return q.remove();
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAll(Collection<? extends E> c) {
        return q.addAll(c);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        q.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        return q.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(Collection<?> c) {
        return q.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return q.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<E> iterator() {
        return q.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        return q.remove(o);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(Collection<?> c) {
        return q.removeAll(c);
    }

    /**
     * {@inheritDoc}
     */
    public boolean retainAll(Collection<?> c) {
        return q.retainAll(c);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return q.size();
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray() {
        return q.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public <T> T[] toArray(T[] a) {
        return q.toArray(a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return q.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return q.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return q.toString();
    }
}