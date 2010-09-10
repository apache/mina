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

/**
 * An abstract class which provides semantics for an object which will be only
 * fully initialized when requested to. It allows to avoid loosing time when 
 * early initializing unnecessary objects.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M2
 */
public abstract class LazyInitializer<V> {

    /**
     * The value that results on the {@link #init()} call. If null,
     * it means that the value has not been initialized yet.
     */
    private V value;

    /**
     * Initializes the value.
     *
     * @return the initialized value
     */
    public abstract V init();

    /**
     * Returns the value resulting from the initialization.
     * @return the initialized value
     */
    public V get() {
        if (value == null) {
            value = init();
        }

        return value;
    }
}
