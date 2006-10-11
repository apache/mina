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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReadWriteLock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A map with expiration.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 */
public class ExpiringMap implements Map
{
    public static final int DEFAULT_TIME_TO_LIVE = 60;

    public static final int DEFAULT_EXPIRATION_INTERVAL = 1;

    private static volatile int expirerCount = 1;

    private final ConcurrentHashMap delegate;

    private final CopyOnWriteArrayList expirationListeners;

    private final Expirer expirer;

    public ExpiringMap()
    {
        this( DEFAULT_TIME_TO_LIVE, DEFAULT_EXPIRATION_INTERVAL );
    }

    public ExpiringMap( int timeToLive )
    {
        this( timeToLive, DEFAULT_EXPIRATION_INTERVAL );
    }

    public ExpiringMap( int timeToLive, int expirationInterval )
    {
        this( new ConcurrentHashMap(), new CopyOnWriteArrayList(), timeToLive, expirationInterval );
    }

    private ExpiringMap(
            ConcurrentHashMap delegate, CopyOnWriteArrayList expirationListeners,
            int timeToLive, int expirationInterval )
    {
        this.delegate = delegate;
        this.expirationListeners = expirationListeners;

        this.expirer = new Expirer();
        expirer.setTimeToLive( timeToLive );
        expirer.setExpirationInterval( expirationInterval );
    }

    public Object put( Object key, Object value )
    {
        return delegate.put( key, new ExpiringObject( key, value, System.currentTimeMillis() ) );
    }

    public Object get( Object key )
    {
        Object object = delegate.get( key );

        if( object != null )
        {
            ExpiringObject expObject = ( ExpiringObject ) object;
            expObject.setLastAccessTime( System.currentTimeMillis() );

            return expObject.getValue();
        }

        return object;
    }

    public Object remove( Object key )
    {
        return delegate.remove( key );
    }

    public boolean containsKey( Object key )
    {
        return delegate.containsKey( key );
    }

    public boolean containsValue( Object value )
    {
        return delegate.containsValue( value );
    }

    public int size()
    {
        return delegate.size();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public void clear()
    {
        delegate.clear();
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public Set keySet()
    {
        return delegate.keySet();
    }

    public boolean equals( Object obj )
    {
        return delegate.equals( obj );
    }

    public void putAll( Map inMap )
    {
        synchronized( inMap )
        {
            Iterator inMapKeysIt = inMap.keySet().iterator();

            while( inMapKeysIt.hasNext() )
            {
                Object key = inMapKeysIt.next();
                Object value = inMap.get( key );

                if( value instanceof ExpiringObject )
                {
                    delegate.put( key, value );
                }
            }
        }
    }

    public Collection values()
    {
        return delegate.values();
    }

    public Set entrySet()
    {
        return delegate.entrySet();
    }

    public void addExpirationListener( ExpirationListener listener )
    {
        expirationListeners.add( listener );
    }

    public void removeExpirationListener( ExpirationListener listener )
    {
        expirationListeners.remove( listener );
    }
    
    public Expirer getExpirer()
    {
        return expirer;
    }

    public int getExpirationInterval()
    {
        return expirer.getExpirationInterval();
    }

    public int getTimeToLive()
    {
        return expirer.getTimeToLive();
    }

    public void setExpirationInterval( int expirationInterval )
    {
        expirer.setExpirationInterval( expirationInterval );
    }

    public void setTimeToLive( int timeToLive )
    {
        expirer.setTimeToLive( timeToLive );
    }

    private class ExpiringObject
    {
        private Object key;

        private Object value;

        private long lastAccessTime;

        private ReadWriteLock lastAccessTimeLock = new ReentrantReadWriteLock();

        public ExpiringObject( Object key, Object value, long lastAccessTime )
        {
            if( value == null )
            {
                throw new IllegalArgumentException( "An expiring object cannot be null." );
            }

            this.key = key;
            this.value = value;
            this.lastAccessTime = lastAccessTime;
        }

        public long getLastAccessTime()
        {
            lastAccessTimeLock.readLock().lock();

            try
            {
                return lastAccessTime;
            }
            finally
            {
                lastAccessTimeLock.readLock().unlock();
            }
        }

        public void setLastAccessTime( long lastAccessTime )
        {
            lastAccessTimeLock.writeLock().lock();

            try
            {
                this.lastAccessTime = lastAccessTime;
            }
            finally
            {
                lastAccessTimeLock.writeLock().unlock();
            }
        }

        public Object getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public boolean equals( Object obj )
        {
            return value.equals( obj );
        }

        public int hashCode()
        {
            return value.hashCode();
        }
    }

    public class Expirer implements Runnable
    {
        private ReadWriteLock stateLock = new ReentrantReadWriteLock();

        private long timeToLiveMillis;

        private long expirationIntervalMillis;

        private boolean running = false;

        private final Thread expirerThread;

        public Expirer()
        {
            expirerThread = new Thread( this, "ExpiringMapExpirer-" + ( expirerCount++ ) );
            expirerThread.setDaemon( true );
        }

        public void run()
        {
            while( running )
            {
                processExpires();

                try
                {
                    Thread.sleep( expirationIntervalMillis );
                }
                catch( InterruptedException e )
                {
                }
            }
        }

        private void processExpires()
        {
            long timeNow = System.currentTimeMillis();

            Iterator expiringObjectsIterator = delegate.values().iterator();

            while( expiringObjectsIterator.hasNext() )
            {
                ExpiringObject expObject = ( ExpiringObject ) expiringObjectsIterator.next();

                if( timeToLiveMillis <= 0 )
                    continue;

                long timeIdle = timeNow - expObject.getLastAccessTime();

                if( timeIdle >= timeToLiveMillis )
                {
                    delegate.remove( expObject.getKey() );

                    Iterator listenerIterator = expirationListeners.iterator();

                    while( listenerIterator.hasNext() )
                    {
                        ExpirationListener listener = ( ExpirationListener ) listenerIterator.next();

                        listener.expired( expObject.getValue() );
                    }
                }
            }
        }

        public void startExpiring()
        {
            stateLock.writeLock().lock();

            try
            {
                if( !running )
                {
                    running = true;
                    expirerThread.start();
                }
            }
            finally
            {
                stateLock.writeLock().unlock();
            }
        }

        public void startExpiringIfNotStarted()
        {
            stateLock.readLock().lock();
            try
            {
                if( running )
                {
                    return;
                }
            }
            finally
            {
                stateLock.readLock().unlock();
            }

            stateLock.writeLock().lock();
            try
            {
                if( !running )
                {
                    running = true;
                    expirerThread.start();
                }
            }
            finally
            {
                stateLock.writeLock().unlock();
            }
        }

        public void stopExpiring()
        {
            stateLock.writeLock().lock();

            try
            {
                if( running )
                {
                    running = false;
                    expirerThread.interrupt();
                }
            }
            finally
            {
                stateLock.writeLock().unlock();
            }
        }

        public boolean isRunning()
        {
            stateLock.readLock().lock();

            try
            {
                return running;
            }
            finally
            {
                stateLock.readLock().unlock();
            }
        }

        public int getTimeToLive()
        {
            stateLock.readLock().lock();

            try
            {
                return ( int ) timeToLiveMillis / 1000;
            }
            finally
            {
                stateLock.readLock().unlock();
            }
        }

        public void setTimeToLive( long timeToLive )
        {
            stateLock.writeLock().lock();

            try
            {
                this.timeToLiveMillis = timeToLive * 1000;
            }
            finally
            {
                stateLock.writeLock().unlock();
            }
        }

        public int getExpirationInterval()
        {
            stateLock.readLock().lock();

            try
            {
                return ( int ) expirationIntervalMillis / 1000;
            }
            finally
            {
                stateLock.readLock().unlock();
            }
        }

        public void setExpirationInterval( long expirationInterval )
        {
            stateLock.writeLock().lock();

            try
            {
                this.expirationIntervalMillis = expirationInterval * 1000;
            }
            finally
            {
                stateLock.writeLock().unlock();
            }
        }
    }
}
