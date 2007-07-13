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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An {@link IdentityHashMap}-backed {@link Set}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IdentityHashSet extends AbstractSet {
    private final Map delegate = new IdentityHashMap();

    public IdentityHashSet() {
    }

    public IdentityHashSet(Collection c) {
        addAll(c);
    }

    public int size() {
        return delegate.size();
    }

    public boolean contains(Object o) {
        return delegate.containsKey(o);
    }

    public Iterator iterator() {
        return delegate.keySet().iterator();
    }

    public boolean add(Object arg0) {
        return delegate.put(arg0, Boolean.TRUE) == null;
    }

    public boolean remove(Object o) {
        return delegate.remove(o) != null;
    }

    public void clear() {
        delegate.clear();
    }
}
