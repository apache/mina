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

/**
 * A skeletal decorator which wraps an existing {@link IoQueue} instance.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoQueueWrapper<E> implements IoQueue<E> {

    protected final IoQueue<E> q;

    /**
     * Creates a new instance.
     */
    public IoQueueWrapper(IoQueue<E> q) {
        if (q == null) {
            throw new NullPointerException("queue");
        }
        this.q = q;
    }

    public boolean add(E e) {
        return q.add(e);
    }

    public E element() {
        return q.element();
    }

    public boolean offer(E e) {
        return q.offer(e);
    }

    public E peek() {
        return q.peek();
    }

    public E poll() {
        return q.poll();
    }

    public E remove() {
        return q.remove();
    }

    public boolean addAll(Collection<? extends E> c) {
        return q.addAll(c);
    }

    public void clear() {
        q.clear();
    }

    public boolean contains(Object o) {
        return q.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return q.containsAll(c);
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public Iterator<E> iterator() {
        return q.iterator();
    }

    public boolean remove(Object o) {
        return q.remove(o);
    }

    public boolean removeAll(Collection<?> c) {
        return q.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return q.retainAll(c);
    }

    public int size() {
        return q.size();
    }

    public Object[] toArray() {
        return q.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return q.toArray(a);
    }

    @Override
    public boolean equals(Object obj) {
        return q.equals(obj);
    }

    @Override
    public int hashCode() {
        return q.hashCode();
    }

    @Override
    public String toString() {
        return q.toString();
    }

    public void addListener(IoQueueListener<? super E> listener) {
        q.addListener(listener);
    }

    public void removeListener(IoQueueListener<? super E> listener) {
        q.removeListener(listener);
    }
}