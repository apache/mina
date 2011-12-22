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

import java.util.Collections;
import java.util.Set;

/**
 * An interface exposing the getters and setters on Attributes
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
interface AttributeContainer {
    /**
     * Returns the value of the user-defined attribute for the given
     * <code>key</code>.
     * 
     * @param key
     *            the attribute's key, must not be <code>null</code>
     * @return <tt>null</tt> if there is no attribute with the specified key
     * @exception IllegalArgumentException
     *                if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    <T> T getAttribute(AttributeKey<T> key);

    /**
     * Returns the value of the user-defined attribute for the given
     * <code>key</code>.
     * 
     * @param key
     *            the attribute's key, must not be <code>null</code>
     * @return <tt>null</tt> if there is no attribute with the specified key
     * @exception IllegalArgumentException
     *                if <code>key==null</code>
     * @see #setAttribute(AttributeKey, Object)
     */
    <T> T getAttribute(AttributeKey<T> key, T value);

    /**
     * Removes the specified Attribute from this container. The old value will
     * be returned, <code>null</code> will be returned if there is no such
     * attribute in this container.<br>
     * <br>
     * This method is equivalent to <code>setAttribute(key,null)</code>.
     * 
     * @param key
     *            of the attribute to be removed,must not be <code>null</code>
     * @return the removed value, <code>null</code> if this container doesn't
     *         contain the specified attribute
     * @exception IllegalArgumentException
     *                if <code>key==null</code>
     */
    <T> T removeAttribute(AttributeKey<T> key);

    /**
     * Sets a user-defined attribute. If the <code>value</code> is
     * <code>null</code> the attribute will be removed from this container.
     * 
     * @param key
     *            the attribute's key, must not be <code>null</code>
     * @param value
     *            the attribute's value, <code>null</code> to remove the
     *            attribute
     * @return The old attribute's value. <code>null</code> if there is no
     *         previous value or if the value is <code>null</code>
     * @exception IllegalArgumentException
     *                if {@code value!=null} and not an instance of type that is
     *                specified in the key (see {@link AttributeKey#getType()})
     * 
     * @see #getAttribute(AttributeKey)
     */
    <T> T setAttribute(AttributeKey<? extends T> key, T value);

    /**
     * Returns an unmodifiable {@link Set} including all Keys of this container. If
     * this container contains no key's an empty {@link Set} will be returned.
     * 
     * @return all Keys, never <code>null</code>
     * @see Collections#unmodifiableSet(Set)
     */
    Set<AttributeKey<?>> getAttributeKeys();
}