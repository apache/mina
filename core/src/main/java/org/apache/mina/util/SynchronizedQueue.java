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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SynchronizedQueue<E> implements Queue<E>, Serializable {
    
    private static final long serialVersionUID = -1439242290701194806L;
    
    private final Queue<E> q;

    public SynchronizedQueue(Queue<E> q) {
        this.q = q;
    }
    
    public synchronized boolean add(E e) {
        return q.add(e);
    }

    public synchronized E element() {
        return q.element();
    }

    public synchronized boolean offer(E e) {
        return q.offer(e);
    }

    public synchronized E peek() {
        return q.peek();
    }

    public synchronized E poll() {
        return q.poll();
    }

    public synchronized E remove() {
        return q.remove();
    }

    public synchronized boolean addAll(Collection<? extends E> c) {
        return q.addAll(c);
    }

    public synchronized void clear() {
        q.clear();
    }

    public synchronized boolean contains(Object o) {
        return q.contains(o);
    }

    public synchronized boolean containsAll(Collection<?> c) {
        return q.containsAll(c);
    }

    public synchronized boolean isEmpty() {
        return q.isEmpty();
    }

    public synchronized Iterator<E> iterator() {
        return q.iterator();
    }

    public synchronized boolean remove(Object o) {
        return q.remove(o);
    }

    public synchronized boolean removeAll(Collection<?> c) {
        return q.removeAll(c);
    }

    public synchronized boolean retainAll(Collection<?> c) {
        return q.retainAll(c);
    }

    public synchronized int size() {
        return q.size();
    }

    public synchronized Object[] toArray() {
        return q.toArray();
    }

    public synchronized <T> T[] toArray(T[] a) {
        return q.toArray(a);
    }

    @Override
    public synchronized boolean equals(Object obj) {
        return q.equals(obj);
    }

    @Override
    public synchronized int hashCode() {
        return q.hashCode();
    }

    @Override
    public synchronized String toString() {
        return q.toString();
    }
}