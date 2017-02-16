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
package org.apache.mina.core.session;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

/**
 * The default {@link IoSessionDataStructureFactory} implementation
 * that creates a new {@link HashMap}-based {@link IoSessionAttributeMap}
 * instance and a new synchronized {@link ConcurrentLinkedQueue} instance per
 * {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultIoSessionDataStructureFactory implements IoSessionDataStructureFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public IoSessionAttributeMap getAttributeMap(IoSession session) throws Exception {
        return new DefaultIoSessionAttributeMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteRequestQueue getWriteRequestQueue(IoSession session) throws Exception {
        return new DefaultWriteRequestQueue();
    }

    private static class DefaultIoSessionAttributeMap implements IoSessionAttributeMap {
        private final ConcurrentHashMap<Object, Object> attributes = new ConcurrentHashMap<>(4);

        /**
         * Default constructor
         */
        public DefaultIoSessionAttributeMap() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getAttribute(IoSession session, Object key, Object defaultValue) {
            if (key == null) {
                throw new IllegalArgumentException("key");
            }

            if (defaultValue == null) {
                return attributes.get(key);
            }

            Object object = attributes.putIfAbsent(key, defaultValue);

            if (object == null) {
                return defaultValue;
            } else {
                return object;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object setAttribute(IoSession session, Object key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("key");
            }

            if (value == null) {
                return attributes.remove(key);
            }

            return attributes.put(key, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object setAttributeIfAbsent(IoSession session, Object key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("key");
            }

            if (value == null) {
                return null;
            }

            return attributes.putIfAbsent(key, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object removeAttribute(IoSession session, Object key) {
            if (key == null) {
                throw new IllegalArgumentException("key");
            }

            return attributes.remove(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean removeAttribute(IoSession session, Object key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("key");
            }

            if (value == null) {
                return false;
            }

            try {
                return attributes.remove(key, value);
            } catch (NullPointerException e) {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean replaceAttribute(IoSession session, Object key, Object oldValue, Object newValue) {
            try {
                return attributes.replace(key, oldValue, newValue);
            } catch (NullPointerException e) {
            }

            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean containsAttribute(IoSession session, Object key) {
            return attributes.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Object> getAttributeKeys(IoSession session) {
            synchronized (attributes) {
                return new HashSet<>(attributes.keySet());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose(IoSession session) throws Exception {
            // Do nothing
        }
    }

    private static class DefaultWriteRequestQueue implements WriteRequestQueue {
        /** A queue to store incoming write requests */
        private final Queue<WriteRequest> q = new ConcurrentLinkedQueue<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose(IoSession session) {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear(IoSession session) {
            q.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty(IoSession session) {
            return q.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void offer(IoSession session, WriteRequest writeRequest) {
            q.offer(writeRequest);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WriteRequest poll(IoSession session) {
            WriteRequest answer = q.poll();

            if (answer == AbstractIoSession.CLOSE_REQUEST) {
                session.closeNow();
                dispose(session);
                answer = null;
            }

            return answer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return q.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return q.size();
        }
    }
}
