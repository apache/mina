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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A map with expiration.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 */
public class ExpiringMap<K, V> implements Map<K, V> {
    public static final int DEFAULT_TIME_TO_LIVE = 60;

    public static final int DEFAULT_EXPIRATION_INTERVAL = 1;

    private static volatile int expirerCount = 1;

    private final ConcurrentHashMap<K, ExpiringObject> delegate;

    private final CopyOnWriteArrayList<ExpirationListener> expirationListeners;

    private final Expirer expirer;

    public ExpiringMap() {
        this(DEFAULT_TIME_TO_LIVE, DEFAULT_EXPIRATION_INTERVAL);
    }

    public ExpiringMap(int timeToLive) {
        this(timeToLive, DEFAULT_EXPIRATION_INTERVAL);
    }

    public ExpiringMap(int timeToLive, int expirationInterval) {
        this(new ConcurrentHashMap<K, ExpiringObject>(),
                new CopyOnWriteArrayList<ExpirationListener>(), timeToLive,
                expirationInterval);
    }

    private ExpiringMap(ConcurrentHashMap<K, ExpiringObject> delegate,
            CopyOnWriteArrayList<ExpirationListener> expirationListeners,
            int timeToLive, int expirationInterval) {
        this.delegate = delegate;
        this.expirationListeners = expirationListeners;

        this.expirer = new Expirer();
        expirer.setTimeToLive(timeToLive);
        expirer.setExpirationInterval(expirationInterval);
    }

    public V put(K key, V value) {
        ExpiringObject answer = delegate.put(key, new ExpiringObject(key,
                value, System.currentTimeMillis()));
        if (answer == null) {
            return null;
        }

        return answer.getValue();
    }

    public V get(Object key) {
        ExpiringObject object = delegate.get(key);

        if (object != null) {
            object.setLastAccessTime(System.currentTimeMillis());

            return object.getValue();
        }

        return null;
    }

    public V remove(Object key) {
        ExpiringObject answer = delegate.remove(key);
        if (answer == null) {
            return null;
        }

        return answer.getValue();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public void clear() {
        delegate.clear();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public void putAll(Map<? extends K, ? extends V> inMap) {
        for (Entry<? extends K, ? extends V> e : inMap.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public void addExpirationListener(ExpirationListener<? extends V> listener) {
        expirationListeners.add(listener);
    }

    public void removeExpirationListener(
            ExpirationListener<? extends V> listener) {
        expirationListeners.remove(listener);
    }

    public Expirer getExpirer() {
        return expirer;
    }

    public int getExpirationInterval() {
        return expirer.getExpirationInterval();
    }

    public int getTimeToLive() {
        return expirer.getTimeToLive();
    }

    public void setExpirationInterval(int expirationInterval) {
        expirer.setExpirationInterval(expirationInterval);
    }

    public void setTimeToLive(int timeToLive) {
        expirer.setTimeToLive(timeToLive);
    }

    private class ExpiringObject {
        private K key;

        private V value;

        private long lastAccessTime;

        private ReadWriteLock lastAccessTimeLock = new ReentrantReadWriteLock();

        ExpiringObject(K key, V value, long lastAccessTime) {
            if (value == null) {
                throw new IllegalArgumentException(
                        "An expiring object cannot be null.");
            }

            this.key = key;
            this.value = value;
            this.lastAccessTime = lastAccessTime;
        }

        public long getLastAccessTime() {
            lastAccessTimeLock.readLock().lock();

            try {
                return lastAccessTime;
            } finally {
                lastAccessTimeLock.readLock().unlock();
            }
        }

        public void setLastAccessTime(long lastAccessTime) {
            lastAccessTimeLock.writeLock().lock();

            try {
                this.lastAccessTime = lastAccessTime;
            } finally {
                lastAccessTimeLock.writeLock().unlock();
            }
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return value.equals(obj);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    public class Expirer implements Runnable {
        private ReadWriteLock stateLock = new ReentrantReadWriteLock();

        private long timeToLiveMillis;

        private long expirationIntervalMillis;

        private boolean running = false;

        private final Thread expirerThread;

        public Expirer() {
            expirerThread = new Thread(this, "ExpiringMapExpirer-"
                    + (expirerCount++));
            expirerThread.setDaemon(true);
        }

        public void run() {
            while (running) {
                processExpires();

                try {
                    Thread.sleep(expirationIntervalMillis);
                } catch (InterruptedException e) {
                }
            }
        }

        private void processExpires() {
            long timeNow = System.currentTimeMillis();

            for (ExpiringObject o : delegate.values()) {

                if (timeToLiveMillis <= 0)
                    continue;

                long timeIdle = timeNow - o.getLastAccessTime();

                if (timeIdle >= timeToLiveMillis) {
                    delegate.remove(o.getKey());

                    for (ExpirationListener<V> listener : expirationListeners) {
                        listener.expired(o.getValue());
                    }
                }
            }
        }

        public void startExpiring() {
            stateLock.writeLock().lock();

            try {
                if (!running) {
                    running = true;
                    expirerThread.start();
                }
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public void startExpiringIfNotStarted() {
            stateLock.readLock().lock();
            try {
                if (running) {
                    return;
                }
            } finally {
                stateLock.readLock().unlock();
            }

            stateLock.writeLock().lock();
            try {
                if (!running) {
                    running = true;
                    expirerThread.start();
                }
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public void stopExpiring() {
            stateLock.writeLock().lock();

            try {
                if (running) {
                    running = false;
                    expirerThread.interrupt();
                }
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public boolean isRunning() {
            stateLock.readLock().lock();

            try {
                return running;
            } finally {
                stateLock.readLock().unlock();
            }
        }

        public int getTimeToLive() {
            stateLock.readLock().lock();

            try {
                return (int) timeToLiveMillis / 1000;
            } finally {
                stateLock.readLock().unlock();
            }
        }

        public void setTimeToLive(long timeToLive) {
            stateLock.writeLock().lock();

            try {
                this.timeToLiveMillis = timeToLive * 1000;
            } finally {
                stateLock.writeLock().unlock();
            }
        }

        public int getExpirationInterval() {
            stateLock.readLock().lock();

            try {
                return (int) expirationIntervalMillis / 1000;
            } finally {
                stateLock.readLock().unlock();
            }
        }

        public void setExpirationInterval(long expirationInterval) {
            stateLock.writeLock().lock();

            try {
                this.expirationIntervalMillis = expirationInterval * 1000;
            } finally {
                stateLock.writeLock().unlock();
            }
        }
    }
}
