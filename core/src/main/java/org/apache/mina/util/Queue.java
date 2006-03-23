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
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.mina.common.ByteBuffer;

/**
 * A unbounded circular queue.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Queue extends AbstractList implements List, Serializable
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
     * Returns the capacity of this queue.
     */
    public int capacity()
    {
        return items.length;
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
        decreaseSize();

        return ret;
    }

    /**
     * Enqueue into this queue.
     */
    public void push( Object item )
    {
        if( item == null )
        {
            throw new NullPointerException( "item" );
        }
        ensureCapacity();
        items[ last ] = item;
        increaseSize();
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

    /**
     * Returns the last element of the queue.
     * 
     * @return <code>null</code>, if the queue is empty, or the element is
     *         really <code>null</code>.
     */
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
        checkIndex(idx);
        return items[ getRealIndex(idx) ];
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
    
    /**
     * Returns the sum of the '<tt>remaining</tt>' of all {@link ByteBuffer}s
     * in this queue.
     * 
     * @throws ClassCastException if an element is not a {@link ByteBuffer}
     */
    public int byteSize()
    {
        if( isEmpty() )
        {
            return 0;
        }
        
        int byteSize = 0;
        
        if( first < last )
        {
            for( int i = first; i < last; i++ )
            {
                byteSize += ( ( ByteBuffer ) items[ i ] ).remaining();
            }
        }
        else
        {
            for( int i = first; i < items.length; i++ )
            {
                byteSize += ( ( ByteBuffer ) items[ i ] ).remaining();
            }
            for( int i = last - 1; i >= 0; i-- )
            {
                byteSize += ( ( ByteBuffer ) items[ i ] ).remaining();
            }
        }
        
        return byteSize;
    }
    
    public String toString()
    {
        return "first=" + first + ", last=" + last + ", size=" + size + ", mask = " + mask;
    }

    private void checkIndex( int idx )
    {
        if( idx < 0 || idx >= size )
        {
            throw new IndexOutOfBoundsException( String.valueOf( idx ) );
        }
    }

    private int getRealIndex( int idx )
    {
        return ( first + idx ) & mask;
    }

    private void increaseSize()
    {
        last = ( last + 1 ) & mask;
        size++;
    }

    private void decreaseSize() {
        first = ( first + 1 ) & mask;
        size--;
    }

    private void ensureCapacity()
    {
        if( size < items.length )
        {
            return;
        }
        
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

    //////////////////////////////////////////
    // java.util.List compatibility methods //
    //////////////////////////////////////////

    public boolean add( Object o )
    {
        push( o );
        return true;
    }

    public Object set(int idx, Object o) {
        checkIndex(idx);
        
        int realIdx = getRealIndex(idx);
        Object old = items[ realIdx ];
        items[ realIdx ] = o;
        return old;
    }

    public void add( int idx, Object o )
    {
        if( idx == size )
        {
            push( o );
            return;
        }
        
        checkIndex( idx );
        ensureCapacity();
        
        int realIdx = getRealIndex( idx );
        
        // Make a room for a new element.
        if( first < last )
        {
            System.arraycopy( items, realIdx, items, realIdx + 1, last - realIdx );
        }
        else
        {
            if( realIdx >= first )
            {
                System.arraycopy( items, 0, items, 1, last );
                items[ 0 ] = items[ items.length - 1 ];
                System.arraycopy( items, realIdx, items, realIdx + 1, items.length - realIdx - 1 );
            }
            else
            {
                System.arraycopy( items, realIdx, items, realIdx + 1, last - realIdx );
            }
        }
        
        items[ realIdx ] = o;
        increaseSize();
    }

    public Object remove( int idx )
    {
        if( idx == 0 )
        {
            return pop();
        }
        
        checkIndex( idx );
        
        int realIdx = getRealIndex( idx );
        Object removed = items[ realIdx ];
        
        // Remove a room for the removed element.
        if( first < last )
        {
            System.arraycopy( items, first, items, first + 1, realIdx - first );
        }
        else
        {
            if( realIdx >= first )
            {
                System.arraycopy( items, first, items, first + 1, realIdx - first );
            }
            else
            {
                System.arraycopy( items, 0, items, 1, realIdx );
                items[ 0 ] = items[ items.length - 1 ];
                System.arraycopy( items, first, items, first + 1, items.length - first - 1 );
            }
        }
        
        items[ first ] = null;
        decreaseSize();

        return removed;
    }

    ///////////////////////////////////////////
    // java.util.Queue compatibility methods //
    ///////////////////////////////////////////

    public boolean offer( Object o )
    {
        push( o );
        return true;
    }

    public Object poll()
    {
        return pop();
    }

    public Object remove()
    {
        if( size == 0 )
        {
            throw new NoSuchElementException();
        }
        return pop();
    }

    public Object peek()
    {
        return first();
    }

    public Object element()
    {
        if( size == 0 )
        {
            throw new NoSuchElementException();
        }
        return first();
    }
}