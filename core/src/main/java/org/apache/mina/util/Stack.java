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
import java.util.Arrays;

/**
 * A unbounded stack.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Stack implements Serializable {
    private static final long serialVersionUID = 3546919169401434168L;

    private static final int DEFAULT_CAPACITY = 4;

    private Object[] items;

    private int size = 0;

    /**
     * Construct a new, empty stack.
     */
    public Stack() {
        items = new Object[DEFAULT_CAPACITY];
    }

    /**
     * Clears this stack.
     */
    public void clear() {
        Arrays.fill(items, null);
        size = 0;
    }

    /**
     * Pops from this stack.
     * 
     * @return <code>null</code>, if this stack is empty or the element is
     *         really <code>null</code>.
     */
    public Object pop() {
        if (size == 0) {
            return null;
        }

        int pos = size - 1;
        Object ret = items[pos];
        items[pos] = null;
        size--;

        return ret;
    }

    /**
     * Push into this stack.
     */
    public void push(Object obj) {
        if (size == items.length) {
            // expand queue
            final int oldLen = items.length;
            Object[] tmp = new Object[oldLen * 2];
            System.arraycopy(items, 0, tmp, 0, size);
            items = tmp;
        }

        items[size] = obj;
        size++;
    }

    public void remove(Object o) {
        for (int i = size - 1; i >= 0; i--) {
            if (items[i] == o) {
                System.arraycopy(items, i + 1, items, i, size - i - 1);
                items[size - 1] = null;
                size--;
                break;
            }
        }
    }

    /**
     * Returns the first element of the stack.
     * 
     * @return <code>null</code>, if the stack is empty, or the element is
     *         really <code>null</code>.
     */
    public Object first() {
        if (size == 0) {
            return null;
        }

        return items[size - 1];
    }

    public Object last() {
        if (size == 0) {
            return null;
        }

        return items[0];
    }

    /**
     * Returns <code>true</code> if the stack is empty.
     */
    public boolean isEmpty() {
        return (size == 0);
    }

    /**
     * Returns the number of elements in the stack.
     */
    public int size() {
        return size;
    }
}