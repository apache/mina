/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A unbounded circular queue.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class Queue implements Serializable
{
    private static final long serialVersionUID = 3835151744526464313L;

	private static final int DEFAULT_CAPACITY = 4;

    private static final int DEFAULT_MASK = DEFAULT_CAPACITY - 1;

    private Object[] items;

    private int mask;

    private int first = 0;

    private int last = 0;

    private int size = 0;

    /**
     * Construct a new, empty queue.
     */
    public Queue()
    {
        items = new Object[ DEFAULT_CAPACITY ];
        mask = DEFAULT_MASK;
    }

    /**
     * Clears this queue.
     */
    public void clear()
    {
        Arrays.fill( items, null );
        first = 0;
        last = 0;
        size = 0;
    }

    /**
     * Dequeues from this queue.
     * 
     * @return <code>null</code>, if this queue is empty or the element is
     *         really <code>null</code>.
     */
    public Object pop()
    {
        if( size == 0 )
        {
            return null;
        }

        Object ret = items[ first ];
        items[ first ] = null;
        first = ( first + 1 ) & mask;

        size--;

        return ret;
    }

    /**
     * Enqueue into this queue.
     */
    public void push( Object obj )
    {
        if( size == items.length )
        {
            // expand queue
            final int oldLen = items.length;
            Object[] tmp = new Object[ oldLen * 2 ];

            if( first < last )
            {
                System.arraycopy( items, first, tmp, 0, last - first );
            }
            else
            {
                System.arraycopy( items, first, tmp, 0, oldLen - first );
                System.arraycopy( items, 0, tmp, oldLen - first, last );
            }

            first = 0;
            last = oldLen;
            items = tmp;
            mask = tmp.length - 1;
        }

        items[ last ] = obj;
        last = ( last + 1 ) & mask;
        size++;
    }

    /**
     * Returns the first element of the queue.
     * 
     * @return <code>null</code>, if the queue is empty, or the element is
     *         really <code>null</code>.
     */
    public Object first()
    {
        if( size == 0 )
        {
            return null;
        }

        return items[ first ];
    }

    public Object last()
    {
        if( size == 0 )
        {
            return null;
        }

        return items[ ( last + items.length - 1 ) & mask ];
    }
    
    public Object get( int idx )
    {
        return items[ ( first + idx ) & mask ];
    }

    /**
     * Returns <code>true</code> if the queue is empty.
     */
    public boolean isEmpty()
    {
        return ( size == 0 );
    }

    /**
     * Returns the number of elements in the queue.
     */
    public int size()
    {
        return size;
    }
}