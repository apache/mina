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

/**
 * A synchronized version of {@link Queue}.
 *
 * @author Trustin Lee
 * @version $Rev$, $Date$
 */
public class BlockingQueue extends Queue
{
    private static final long serialVersionUID = 5516588196355725567L;

    private int waiters = 0;

    public BlockingQueue()
    {
    }

    public synchronized int capacity()
    {
        return super.capacity();
    }

    public synchronized void clear()
    {
        super.clear();
    }

    public synchronized Object first()
    {
        return super.first();
    }

    public synchronized Object get( int idx )
    {
        return super.get( idx );
    }

    public synchronized boolean isEmpty()
    {
        return super.isEmpty();
    }

    public synchronized Object last()
    {
        return super.last();
    }

    public synchronized Object pop()
    {
        return super.pop();
    }

    public synchronized void push( Object obj )
    {
        super.push( obj );
        if( waiters > 0 )
            notify();
    }

    public synchronized int size()
    {
        return super.size();
    }

    public synchronized String toString()
    {
        return super.toString();
    }

    /**
     * Waits until any elements are in this queue.
     * 
     * @throws InterruptedException if the current thread is interrupted
     */
    public synchronized void waitForNewItem() throws InterruptedException
    {
        waiters++;
        try
        {
            while( super.isEmpty() )
            {
                wait();
            }
        }
        finally
        {
            waiters--;
        }
    }
}
