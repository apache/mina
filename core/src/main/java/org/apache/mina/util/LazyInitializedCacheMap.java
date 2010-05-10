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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * This map is specially useful when reads are much more frequent than writes and 
 * if the cost of instantiating the values is high like allocating an 
 * {@link IoBuffer} for example.
 *  
 * Based on the final implementation of Memoizer written by Brian Goetz and Tim
 * Peierls. This implementation will return an
 * {@link UnsupportedOperationException} on each method that is not intended to
 * be called by user code for performance reasons.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M2
 */
public class LazyInitializedCacheMap<K, V> implements Map<K, V> {
    private ConcurrentMap<K, LazyInitializer<V>> cache;

    /**
     * This class provides a noop {@link LazyInitializer} meaning it 
     * will return the same object it received when instantiated.
     */
    public class NoopInitializer extends LazyInitializer<V> {
        private V value;

        public NoopInitializer(V value) {
            this.value = value;
        }

        public V init() {
            return value;
        }
    }

    /**
     * Default constructor. Uses the default parameters to initialize its internal
     * {@link ConcurrentHashMap}.
     */
    public LazyInitializedCacheMap() {
        this.cache = new ConcurrentHashMap<K, LazyInitializer<V>>();
    }
    
    /**
     * This constructor allows to provide a fine tuned {@link ConcurrentHashMap}
     * to stick with each special case the user needs.
     */
    public LazyInitializedCacheMap(final ConcurrentHashMap<K, LazyInitializer<V>> map) {
        this.cache = map;
    }    

    /**
     * {@inheritDoc}
     */
    public V get(Object key) {
        LazyInitializer<V> c = cache.get(key);
        if (c != null) {
            return c.get();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public V remove(Object key) {
        LazyInitializer<V> c = cache.remove(key);
        if (c != null) {
            return c.get();
        }

        return null;
    }

    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * This is equivalent to
     * <pre>
     *   if (!map.containsKey(key))
     *       return map.put(key, value);
     *   else
     *       return map.get(key);</pre>
     * except that the action is performed atomically.
     *
     * @param key key with which the specified value is to be associated
     * @param value a lazy initialized value object.
     * 
     * @return the previous value associated with the specified key,
     *         or <tt>null</tt> if there was no mapping for the key
     */
    public V putIfAbsent(K key, LazyInitializer<V> value) {
        LazyInitializer<V> v = cache.get(key);
        if (v == null) {
            v = cache.putIfAbsent(key, value);
            if (v == null) {
                return value.get();
            }
        }

        return v.get();
    }

    /**
     * {@inheritDoc}
     */
    public V put(K key, V value) {
        LazyInitializer<V> c = cache.put(key, new NoopInitializer(value));
        if (c != null) {
            return c.get();
        }

        return null;
    }

    /**
     * @throws {@link UnsupportedOperationException} as this method would imply
     *         performance drops.
     */
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws {@link UnsupportedOperationException} as this method would imply
     *         performance drops.
     */
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws {@link UnsupportedOperationException} as this method would imply
     *         performance drops.
     */
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            cache.put(e.getKey(), new NoopInitializer(e.getValue()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<LazyInitializer<V>> getValues() {
        return cache.values();
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        cache.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        return cache.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return cache.size();
    }
}