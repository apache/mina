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
package org.apache.mina.filter.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoFilter} that sets initial attributes when a new
 * {@link IoSession} is created.  By default, the attribute map is empty when
 * an {@link IoSession} is newly created.  Inserting this filter will make
 * the pre-configured attributes available after this filter executes the
 * <tt>sessionCreated</tt> event.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class SessionAttributeInitializingFilter extends IoFilterAdapter {
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    /**
     * Creates a new instance with no default attributes.  You can set
     * the additional attributes by calling methods such as
     * {@link #setAttribute(String, Object)} and {@link #setAttributes(Map)}.
     */
    public SessionAttributeInitializingFilter() {
        // Do nothing
    }

    /**
     * Creates a new instance with the specified default attributes.  You can
     * set the additional attributes by calling methods such as
     * {@link #setAttribute(String, Object)} and {@link #setAttributes(Map)}.
     */
    public SessionAttributeInitializingFilter(
            Map<String, ? extends Object> attributes) {
        setAttributes(attributes);
    }

    /**
     * Returns the value of user-defined attribute.
     *
     * @param key the key of the attribute
     * @return <tt>null</tt> if there is no attribute with the specified key
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Sets a user-defined attribute.
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    public Object setAttribute(String key, Object value) {
        if (value == null) {
            return removeAttribute(key);
        }

        return attributes.put(key, value);
    }

    /**
     * Sets a user defined attribute without a value.  This is useful when
     * you just want to put a 'mark' attribute.  Its value is set to
     * {@link Boolean#TRUE}.
     *
     * @param key the key of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    public Object setAttribute(String key) {
        return attributes.put(key, Boolean.TRUE);
    }

    /**
     * Removes a user-defined attribute with the specified key.
     *
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    /**
     * Returns <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     */
    boolean containsAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Returns the set of keys of all user-defined attributes.
     */
    public Set<String> getAttributeKeys() {
        return attributes.keySet();
    }

    /**
     * Sets the attribute map.  The specified attributes are copied into the
     * underlying map, so modifying the specified attributes parameter after
     * the call won't change the internal state.
     */
    public void setAttributes(Map<String, ? extends Object> attributes) {
        if (attributes == null) {
            attributes = new ConcurrentHashMap<String, Object>();
        }

        this.attributes.clear();
        this.attributes.putAll(attributes);
    }

    /**
     * Puts all pre-configured attributes into the actual session attribute
     * map and forward the event to the next filter.
     */
    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            session.setAttribute(e.getKey(), e.getValue());
        }

        nextFilter.sessionCreated(session);
    }
}
