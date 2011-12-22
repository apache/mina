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

import static org.apache.mina.util.Assert.assertNotNull;

import org.apache.mina.api.IoSession;

/**
 * Represents the Key for an attribute-value of an {@link IoSession}. A key
 * consists of the Type of the referenced attribute value and a name.<br>
 * <br>
 * Two {@link AttributeKey}'s are equal if the have the same attribute-type and
 * attribute-name.
 * 
 * @param <T> Type of the attribute-value this key is referring to
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class AttributeKey<T> {
    /** the {@link Class} of the referenced attribute-value */
    private final Class<T> attributeType;

    /** the name of this key */
    private final String attributeName;

    /** the cached hash code of this instance */
    private final int hashCode;

    /**
     * Creates a new {@link AttributeKey} with the given parameters. A
     * {@link IllegalArgumentException} will be thrown if any parameter is
     * <code>null</code>.
     * 
     * @param attributeType
     *            type of the referenced attribute-value, must not be
     *            <code>null</code>
     * @param attributeName
     *            name of this key, must not be <code>null</code>
     * @exception IllegalArgumentException
     *                if any parameter is <code>null</code>
     * @see #createKey(Class, String)
     */
    public AttributeKey(Class<T> attributeType, String attributeName) {
        this.attributeType = assertNotNull(attributeType, "attributeType");
        this.attributeName = assertNotNull(attributeName, "attributeName");

        this.hashCode = createHashCode();
    }

    /**
     * Creates a new {@link AttributeKey} with the given parameters. A
     * {@link IllegalArgumentException} will be thrown if any parameter is
     * <code>null</code>. <br>
     * This call is equal to {@link AttributeKey#AttributeKey(Class, String)}
     * 
     * @param attributeType
     *            type of the referenced attribute-value, must not be
     *            <code>null</code>
     * @param attributeName
     *            name of this key, must not be <code>null</code>
     * @exception IllegalArgumentException
     *                if any parameter is <code>null</code>
     * @see #AttributeKey(Class, String)
     */
    public static <T> AttributeKey<T> createKey(Class<T> attributeType, String attributeName) {
        return new AttributeKey<T>(attributeType, attributeName);
    }

    /**
     * Creates the hash code for this instance
     * 
     * @return
     */
    private int createHashCode() {
        final int prime = 31;
        int result = prime + attributeName.hashCode();
        result = prime * result + attributeType.hashCode();

        return result;
    }

    /**
     * Returns the name of this key.
     * 
     * @return name of this key, never <code>null</code>
     */
    public String getName() {
        return attributeName;
    }

    /**
     * Returns the type of this key.
     * 
     * @return type of this key, never <code>null</code>
     */
    public Class<T> getType() {
        return attributeType;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        AttributeKey<?> other = (AttributeKey<?>) obj;

        return hashCode == other.hashCode;
    }
}
