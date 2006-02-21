/*
 *   @(#) $Id: IoFilterLifeCycleManager.java 350148 2005-12-01 04:13:18Z trustin $
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
package org.apache.mina.common.support;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterLifeCycleException;
import org.apache.mina.common.IoFilter.NextFilter;

/**
 * Manages the life cycle of {@link IoFilter}s globally.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 350148 $, $Date: 2005-12-01 13:13:18 +0900 $
 */
public class IoFilterLifeCycleManager
{
    private static final IoFilterLifeCycleManager INSTANCE = new IoFilterLifeCycleManager();
    
    public static IoFilterLifeCycleManager getInstance()
    {
        return INSTANCE;
    }
    
    private Map counts = new IdentityHashMap();
    
    private IoFilterLifeCycleManager()
    {
    }
    
    public void callInitIfNecessary( IoFilter filter )
    {
        boolean callInit = false;
        
        synchronized( this )
        {
            ReferenceCount count = ( ReferenceCount ) counts.get( filter );
            if( count == null )
            {
                count = new ReferenceCount();
                counts.put( filter, count );
                callInit = true;
            }
        }
        
        if( callInit )
        {
            try
            {
                filter.init();
            }
            catch( Throwable t )
            {
                throw new IoFilterLifeCycleException(
                        "init(): " + filter, t );
            }
        }
    }
    
    public void callOnPreAdd( IoFilterChain chain, String name, IoFilter filter, NextFilter nextFilter )
    {
        synchronized( this )
        {
            ReferenceCount count = ( ReferenceCount ) counts.get( filter );
            if( count == null )
            {
                throw new IllegalStateException();
            }
            
            count.increase();
        }
        
        try
        {
            filter.onPreAdd( chain, name, nextFilter );
        }
        catch( Throwable t )
        {
            throw new IoFilterLifeCycleException(
                    "onPreAdd(): " + name + ':' + filter + " in " +
                    chain.getSession(), t );
        }
    }

    public void callOnPreRemove( IoFilterChain chain, String name, IoFilter filter, NextFilter nextFilter )
    {
        synchronized( this )
        {
            ReferenceCount count = ( ReferenceCount ) counts.get( filter );
            if( count == null || count.get() == 0 )
            {
                return;
            }
        }

        try
        {
            filter.onPreRemove( chain, name, nextFilter );
        }
        catch( Throwable t )
        {
            throw new IoFilterLifeCycleException(
                    "onPreRemove(): " + name + ':' + filter + " in " +
                    chain.getSession(), t );
        }
    }
    
    public void callOnPostAdd( IoFilterChain chain, String name, IoFilter filter, NextFilter nextFilter )
    {
        synchronized( this )
        {
            ReferenceCount count = ( ReferenceCount ) counts.get( filter );
            if( count == null )
            {
                throw new IllegalStateException();
            }
        }
        
        try
        {
            filter.onPostAdd( chain, name, nextFilter );
        }
        catch( Throwable t )
        {
            // Revert back the reference count.
            decreaseCountSafely( filter );

            throw new IoFilterLifeCycleException(
                    "onPostAdd(): " + name + ':' + filter + " in " +
                    chain.getSession(), t );
        }
    }

    public void callOnPostRemove( IoFilterChain chain, String name, IoFilter filter, NextFilter nextFilter )
    {
        synchronized( this )
        {
            ReferenceCount count = ( ReferenceCount ) counts.get( filter );
            if( count == null || count.get() == 0 )
            {
                return;
            }
        }

        try
        {
            filter.onPostRemove( chain, name, nextFilter);
        }
        catch( Throwable t )
        {
            throw new IoFilterLifeCycleException(
                    "onPostRemove(): " + name + ':' + filter + " in " +
                    chain.getSession(), t );
        }
        finally
        {
            decreaseCountSafely( filter );
        }
    }

    private synchronized void decreaseCountSafely( IoFilter filter )
    {
        ReferenceCount count = ( ReferenceCount ) counts.get( filter );
        if( count == null )
        {
            throw new IllegalStateException();
        }
        
        count.decrease();
    }

    public synchronized void callDestroyIfNecessary( IoFilter filter )
    {
        boolean callDestroy = false;
        
        synchronized( this )
        {
            ReferenceCount count = ( ReferenceCount ) counts.get( filter );
            if( count == null )
            {
                return;
            }
            
            if( count.get() == 0 )
            {
                counts.remove( filter );
                callDestroy = true;
            }
        }

        if( callDestroy )
        {
            try
            {
                filter.destroy();
            }
            catch( Throwable t2 )
            {
                throw new IoFilterLifeCycleException( "Failed to destroy: " + filter, t2 );
            }
        }
    }
    
    /** Maintains the reference count of an {@link IoFilter}. */ 
    private static class ReferenceCount
    {
        private int count;
        
        public int get()
        {
            return count;
        }
        
        public int increase()
        {
            return count ++;
        }
        
        public int decrease()
        {
            return -- count;
        }
    }
}
