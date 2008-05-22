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
 * A decorator that makes the specified {@link IoQueue} thread-safe.
 * Like any other synchronizing wrappers, its iteration is not thread-safe.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SynchronizedIoQueue<E> extends IoQueueWrapper<E> {

    /**
     * Creates a new instance.
     */
    public SynchronizedIoQueue(IoQueue<E> q) {
        super(q);
    }

    @Override
    public synchronized boolean add(E e) {
        return super.add(e);
    }

    @Override
    public synchronized E element() {
        return super.element();
    }

    @Override
    public synchronized boolean offer(E e) {
        return super.offer(e);
    }

    @Override
    public synchronized E peek() {
        return super.peek();
    }

    @Override
    public synchronized E poll() {
        return super.poll();
    }

    @Override
    public synchronized E remove() {
        return super.remove();
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        return super.addAll(c);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return super.contains(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized Iterator<E> iterator() {
        return super.iterator();
    }

    @Override
    public synchronized boolean remove(Object o) {
        return super.remove(o);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized Object[] toArray() {
        return super.toArray();
    }

    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return super.toArray(a);
    }

    @Override
    public synchronized boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }

    @Override
    public synchronized E element(int index) {
        return super.element(index);
    }

    @Override
    public synchronized void addListener(IoQueueListener<? super E> listener) {
        super.addListener(listener);
    }

    @Override
    public synchronized void removeListener(IoQueueListener<? super E> listener) {
        super.removeListener(listener);
    }
}