/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.mina.http.util;

import java.io.Serializable;


public class NameValuePair  implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     *
     */
    public NameValuePair() {
        this (null, null);
    }

    /**
     * Constructor.
     * @param name The name.
     * @param value The value.
     */
    public NameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * Name.
     */
    private String name = null;

    /**
     * Value.
     */
    private String value = null;

    // ------------------------------------------------------------- Properties

    /**
     * Set the name.
     *
     * @param name The new name
     * @see #getName()
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Return the name.
     *
     * @return String name The name
     * @see #setName(String)
     */
    public String getName() {
        return name;
    }


    /**
     * Set the value.
     *
     * @param value The new value.
     */
    public void setValue(String value) {
        this.value = value;
    }


    /**
     * Return the current value.
     *
     * @return String value The current value.
     */
    public String getValue() {
        return value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Get a String representation of this pair.
     * @return A string representation.
     */
    public String toString() {
        return ("name=" + name + ", " + "value=" + value);
    }

    public boolean equals(final Object object) {
        if (object == null) return false;
        if (this == object) return true;
        if (object instanceof NameValuePair) {
            NameValuePair that = (NameValuePair) object;
            return LangUtils.equals(this.name, that.name)
                  && LangUtils.equals(this.value, that.value);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.name);
        hash = LangUtils.hashCode(hash, this.value);
        return hash;
    }
}

