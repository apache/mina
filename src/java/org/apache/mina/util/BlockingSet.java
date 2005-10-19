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

import java.util.HashSet;
import java.util.Iterator;

/**
 * A {@link HashSet} that can wait until it is not empty.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class BlockingSet extends HashSet
{
    private static final long serialVersionUID = 3258134669538309941L;

	private int waiters = 0;

    public synchronized boolean add( Object o )
    {
        boolean ret = super.add( o );
        if( ret && waiters > 0 )
            notify();
        return ret;
    }

    public Iterator iterator()
    {
        return super.iterator();
    }

    public synchronized boolean remove( Object o )
    {
        return super.remove( o );
    }

    public synchronized void waitForNewItem() throws InterruptedException
    {
        waiters++;
        try
        {
            while( isEmpty() )
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