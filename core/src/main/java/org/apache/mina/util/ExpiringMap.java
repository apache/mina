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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A map with expiration.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * TODO Change time unit to 'seconds'.
 */
public class ExpiringMap implements Map
{
    public static final long DEFAULT_EXPIRATION_TIME = 5000;

    public static final long DEFAULT_EXPIRER_DELAY = 5000;

    private static volatile int expirerCount = 1;

    private long expirationTimeMillis;

    private long expirerDelay;

    private HashMap delegate;

    private HashMap expirationInfos;

    private List expirationListeners;

    private Expirer expirer;

    public ExpiringMap()
    {
        this( new HashMap(), new HashMap(), new LinkedList(), DEFAULT_EXPIRATION_TIME, DEFAULT_EXPIRER_DELAY );
    }

    public ExpiringMap( long expirationTimeMillis )
    {
        this( new HashMap(), new HashMap(), new LinkedList(), expirationTimeMillis, DEFAULT_EXPIRER_DELAY );
    }

    public ExpiringMap( long expirationTimeMillis, long expirerDelay )
    {
        this( new HashMap(), new HashMap(), new LinkedList(), expirationTimeMillis, expirerDelay );
    }

    private ExpiringMap( HashMap delegate, HashMap accessTimes, List expirationListeners, long expirationTimeMillis,
            long expirerDelay )
    {
        this.delegate = delegate;
        this.expirationInfos = accessTimes;
        this.expirationTimeMillis = expirationTimeMillis;
        this.expirationListeners = expirationListeners;
        this.expirerDelay = expirerDelay;

        this.expirer = new Expirer();
        this.expirer.start();
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear()
    {
        expirationInfos.clear();
        delegate.clear();
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey( Object key )
    {
        return delegate.containsKey( key );
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue( Object value )
    {
        return delegate.containsValue( value );
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set entrySet()
    {
        return delegate.entrySet();
    }

    /**
     * @see java.util.Map#equals(java.util.Map)
     */
    public boolean equals( Object o )
    {
        return delegate.equals( o );
    }

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get( Object key )
    {
        Object object = delegate.get( key );

        if ( object != null )
        {
            updateAccessTime( object );
        }

        return object;
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public Set keySet()
    {
        return delegate.keySet();
    }

    public Object put( Object key, Object value )
    {
        if ( value != null )
        {
            addAccessTime( key, value );
        }

        return delegate.put( key, value );
    }

    public void putAll( Map map )
    {
        Iterator mapKeyIterator = map.keySet().iterator();

        while ( mapKeyIterator.hasNext() )
        {
            Object key = mapKeyIterator.next();
            Object value = map.get( key );

            if ( value != null )
            {
                addAccessTime( key, value );
            }
        }

        delegate.putAll( map );
    }

    public Object remove( Object key )
    {
        Object object = delegate.remove( key );

        if ( object != null )
        {
            expirationInfos.remove( object );
        }

        return object;
    }

    public int size()
    {
        return delegate.size();
    }

    public Collection values()
    {
        return delegate.values();
    }

    private void addAccessTime( Object key, Object value )
    {
        ExpirationInfo info = new ExpirationInfo();
        info.key = key;
        info.accesstime = System.currentTimeMillis();

        expirationInfos.put( value, info );
    }

    public void updateAccessTime( Object object )
    {
        Object infoObject = expirationInfos.get( object );

        if ( infoObject != null )
        {
            ExpirationInfo info = ( ExpirationInfo ) infoObject;
            info.accesstime = System.currentTimeMillis();
        }
    }

    private ExpirationInfo getExpirationInfo( Object object )
    {
        Object infoObject = expirationInfos.get( object );

        if ( infoObject != null )
        {
            ExpirationInfo info = ( ExpirationInfo ) infoObject;

            return info;
        }

        return null;
    }

    public void addExpirationListener( ExpirationListener listener )
    {
        synchronized ( expirationListeners )
        {
            expirationListeners.add( listener );
        }
    }

    public void removeExpirationListener( ExpirationListener listener )
    {
        synchronized ( expirationListeners )
        {
            expirationListeners.remove( listener );
        }
    }

    public Object[] findMappedObjects()
    {
        Object results[] = null;
        synchronized ( delegate )
        {
            results = new Object[delegate.size()];
            results = delegate.values().toArray( results );
        }
        return ( results );
    }

    public void startExpirer()
    {
        synchronized ( expirer )
        {
            if ( !expirer.isRunning() )
            {
                expirer.setRunning( true );
                expirer.interrupt();
            }
        }
    }

    public void stopExpirer()
    {
        synchronized ( expirer )
        {
            if ( expirer.isRunning() )
            {
                expirer.setRunning( false );
                expirer.interrupt();
            }
        }
    }

    private class ExpirationInfo
    {
        public Object key;

        public long accesstime;
    }

    private class Expirer extends Thread
    {
        private boolean running = true;

        public Expirer()
        {
            super( "MapExpirer-" + expirerCount++ );
        }

        public void run()
        {
            while ( running )
            {
                processExpires();

                try
                {
                    Thread.sleep( expirerDelay );
                }
                catch ( InterruptedException e )
                {
                }
            }
        }

        private void processExpires()
        {
            long timeNow = System.currentTimeMillis();
            Object mappedObjects[] = findMappedObjects();

            for ( int i = 0; i < mappedObjects.length; i++ )
            {
                Object mappedObject = mappedObjects[i];

                ExpirationInfo info = getExpirationInfo( mappedObject );

                if ( info != null )
                {
                    if ( expirationTimeMillis < 0 )
                        continue;
                    long timeIdle = timeNow - info.accesstime;

                    if ( timeIdle >= expirationTimeMillis )
                    {
                        delegate.remove( info.key );
                        expirationInfos.remove( info.key );

                        synchronized ( expirationListeners )
                        {
                            Iterator listenerIterator = expirationListeners.iterator();

                            while ( listenerIterator.hasNext() )
                            {
                                ExpirationListener listener = ( ExpirationListener ) listenerIterator.next();

                                listener.expired( mappedObject );
                            }
                        }
                    }
                }
            }
        }

        public void setRunning( boolean running )
        {
            this.running = running;
        }

        public boolean isRunning()
        {
            return running;
        }
    }
}
