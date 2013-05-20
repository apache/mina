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
package org.apache.mina.session;

import static java.util.Collections.unmodifiableSet;
import static org.apache.mina.util.Assert.assertNotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link AttributeContainer} provides type-safe access to attribute values, using {@link AttributeKey}' s which as
 * reference-key to an attribute value. <br>
 * <br>
 * This class is Thread-Safe !
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
final class DefaultAttributeContainer implements AttributeContainer {
    /**
     * Contains all attributes
     * <ul>
     * <li>Key: the typesafe attribute key
     * <li>Value: the attribute value
     * </ul>
     */
    private final Map<AttributeKey<?>, Object> attributes = new ConcurrentHashMap<AttributeKey<?>, Object>();

    /**
     * Returns the value of the user-defined attribute for the given <code>key</code>.
     * 
     * @param key the attribute's key, must not be <code>null</code>
     * @return <tt>null</tt> if there is no attribute with the specified key
     * @exception IllegalArgumentException if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(AttributeKey<T> key) {
        assertNotNull(key, "key");

        T value = (T) attributes.get(key);

        return value;
    }

    /**
     * Returns the value of the user-defined attribute for the given <code>key</code>.
     * 
     * @param key the attribute's key, must not be <code>null</code>
     * @return <tt>null</tt> if there is no attribute with the specified key
     * @exception IllegalArgumentException if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(AttributeKey<T> key, T defaultValue) {
        assertNotNull(key, "key");

        T value = (T) attributes.get(key);

        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    /**
     * Sets a user-defined attribute. If the <code>value</code> is <code>null</code> the attribute will be removed from
     * this container.
     * 
     * @param key the attribute's key, must not be <code>null</code>
     * @param value the attribute's value, <code>null</code> to remove the attribute
     * @return The old attribute's value, <code>null</code> if there is no previous value
     * @exception IllegalArgumentException <ul>
     *            <li>if <code>key==null</code>
     *            <li>if <code>value</code> is not <code>null</code> and not an instance of type that is specified in by
     *            the given <code>key</code> (see {@link AttributeKey#getType()})
     * 
     *            </ul>
     * 
     * @see #getAttribute(AttributeKey)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T setAttribute(AttributeKey<? extends T> key, T value) {
        assertNotNull(key, "key");
        assertValueIsOfExpectedType(key, value);
        if (value == null) {
            return removeAttribute(key);
        }

        return (T) attributes.put(key, value);
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given <code>value</code> is not of the expected type and not
     * <code>null</code>.
     * 
     * @param <T>
     * @param key
     * @param value
     * @exception IllegalArgumentException if <code>value</code> is not an instance of {@link AttributeKey#getType()}
     */
    private static <T> void assertValueIsOfExpectedType(AttributeKey<? extends T> key, T value) {
        if (value == null) {
            return;
        }

        Class<? extends T> expectedValueType = key.getType();

        if (!expectedValueType.isInstance(value)) {
            throw new IllegalArgumentException("Invalid attribute value" + "\r\n  expected type: "
                    + expectedValueType.getName() + "\r\n  actual type  : " + value.getClass().getName()
                    + "\r\n  actual value : " + value);
        }
    }

    /**
     * Returns an unmodifiable {@link Set} of all Keys of this container. If this container contains no key's an empty
     * {@link Set} will be returned.
     * 
     * @return all Keys, never <code>null</code>
     * @see Collections#unmodifiableSet(Set)
     */
    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return unmodifiableSet(attributes.keySet());
    }

    /**
     * Removes the specified Attribute from this container. The old value will be returned, <code>null</code> will be
     * returned if there is no such attribute in this container.<br>
     * <br>
     * This method is equivalent to <code>setAttribute(key,null)</code>.
     * 
     * @param key of the attribute to be removed,must not be <code>null</code>
     * @return the removed value, <code>null</code> if this container doesn't contain the specified attribute
     * @exception IllegalArgumentException if <code>key==null</code>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(AttributeKey<T> key) {
        assertNotNull(key, "key");
        return (T) attributes.remove(key);
    }
}