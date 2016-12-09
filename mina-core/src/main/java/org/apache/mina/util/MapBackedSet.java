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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A {@link Map}-backed {@link Set}.
 * 
 * @param <E> The element stored in the set
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MapBackedSet<E> extends AbstractSet<E> implements Serializable {

    private static final long serialVersionUID = -8347878570391674042L;

    protected final Map<E, Boolean> map;

    /**
     * Creates a new MapBackedSet instance
     * 
     * @param map The map that we want to back
     */
    public MapBackedSet(Map<E, Boolean> map) {
        this.map = map;
    }

    /**
     * Creates a new MapBackedSet instance
     * 
     * @param map The map that we want to back
     * @param c The elements we want to add in the map
     */
    public MapBackedSet(Map<E, Boolean> map, Collection<E> c) {
        this.map = map;
        addAll(c);
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
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(E o) {
        return map.put(o, Boolean.TRUE) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        map.clear();
    }
}
