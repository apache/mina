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
package org.apache.mina.coap.retry;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Map} implementation backed with a {@link ConcurrentHashMap} providing entry expiration facilities.
 * 
 * <p>
 * A worker thread is started to check periodically if expired entries should be removed from the underlying map.
 * </p>
 * 
 * @see ConcurrentHashMap
 * @param <K> type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ExpiringMap<K, V> implements Map<K, V> {

    private final Map<K, ExpiringValue<V>> map = new ConcurrentHashMap<>();

    /** The default time to live for an entry : 30 seconds */
    private static final int EXPIRATION_PERIOD_IN_SEC = 30;

    /** The default period between two expiration checks : 10 seconds */
    private static final int CHECKER_PERIOD_IN_SEC = 10;

    private final int expirationPeriod;
    private final int checkerPeriod;

    /** For running expiration tasks */
    private ScheduledExecutorService executor;

    /**
     * A new expiring map
     * 
     * @param expirationPeriod the expiration period for an entry
     * @param checkerPeriod the period between two checks of expired elements
     * @param executor scheduled executor to be used for scheduling expiration tasks
     */
    public ExpiringMap(int expirationPeriod, int checkerPeriod, ScheduledExecutorService executor) {
        this.expirationPeriod = expirationPeriod;
        this.checkerPeriod = checkerPeriod;
        this.executor = executor;
        executor.scheduleAtFixedRate(new ExpirationTask(), checkerPeriod, checkerPeriod, TimeUnit.SECONDS);
    }

    /**
     * A map with an expiration period of 30 seconds. The worker in charge of expiring the map entries will run every 10
     * seconds.
     * @param executor scheduled executor to be used for scheduling expiration tasks
     */
    public ExpiringMap(ScheduledExecutorService executor) {
        this(EXPIRATION_PERIOD_IN_SEC, CHECKER_PERIOD_IN_SEC, executor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(Object key) {
        ExpiringValue<V> expValue = map.get(key);
        if (expValue != null) {
            return expValue.value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(K key, V value) {
        ExpiringValue<V> expValue = map.put(key, new ExpiringValue<V>(value));
        if (expValue != null) {
            return expValue.value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(Object key) {
        ExpiringValue<V> expValue = map.remove(key);
        if (expValue != null) {
            return expValue.value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            map.put(e.getKey(), new ExpiringValue<V>(e.getValue()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove all expired entries.
     * 
     * @param date all entries with an expiration date after this date are removed.
     */
    private void expire(long date) {
        for (Entry<K, ExpiringValue<V>> e : map.entrySet()) {
            if (e.getValue().expiringDate < date) {
                map.remove(e.getKey());
            }
        }
    }

    /**
     * Task in chage of expriring values, to be scheduled.
     */
    private class ExpirationTask implements Runnable {

        @Override
        public void run() {
            expire(System.currentTimeMillis());
        }
    }

    /**
     * An entry value with an expiration date.
     * 
     * @param <T> the type of the value
     */
    class ExpiringValue<T> {

        private T value;
        private long expiringDate;

        public ExpiringValue(T value) {
            this.value = value;

            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, expirationPeriod);
            expiringDate = c.getTime().getTime();
        }

    }

}
