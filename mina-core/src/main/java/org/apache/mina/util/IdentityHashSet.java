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
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * An {@link IdentityHashMap}-backed {@link Set}.
 * 
 * @param <E> The element type
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IdentityHashSet<E> extends MapBackedSet<E> {
    private static final long serialVersionUID = 6948202189467167147L;

    /**
     * Creates a new IdentityHashSet instance
     */
    public IdentityHashSet() {
        super(new IdentityHashMap<E, Boolean>());
    }

    /**
     * Creates a new IdentityHashSet instance
     * 
     * @param expectedMaxSize The maximum size for the map
     */
    public IdentityHashSet(int expectedMaxSize) {
        super(new IdentityHashMap<E, Boolean>(expectedMaxSize));
    }

    /**
     * Creates a new IdentityHashSet instance
     * 
     * @param c The elements to put in the map
     */
    public IdentityHashSet(Collection<E> c) {
        super(new IdentityHashMap<E, Boolean>(), c);
    }
}
