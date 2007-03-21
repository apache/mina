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
package org.apache.mina.common.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;

/**
 * A default implementation of {@link IoFuture}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFuture implements IoFuture
{
    private final IoSession session;
    private final CountDownLatch completionLatch = new CountDownLatch( 1 );
    private final List<IoFutureListener> listeners = new CopyOnWriteArrayList<IoFutureListener>();
    private final AtomicBoolean ready = new AtomicBoolean( false );

    private volatile Object result;

    /**
     * Creates a new instance.
     *
     * @param session an {@link IoSession} which is associated with this future
     */
    public DefaultIoFuture( IoSession session )
    {
        this.session = session;
    }

    public IoSession getSession()
    {
        return session;
    }

    public void join()
    {
        for( ;; )
        {
            try
            {
                completionLatch.await();
            }
            catch( InterruptedException e )
            {
            }
        }
    }

    public boolean join( long timeoutInMillis )
    {
        long startTime = ( timeoutInMillis <= 0 ) ? 0 : System
                .currentTimeMillis();
        long waitTime = timeoutInMillis;
        
        for( ;; )
        {
            boolean ready = false;
            try
            {
                ready = completionLatch.await( waitTime, TimeUnit.MILLISECONDS );
            }
            catch( InterruptedException e )
            {
            }

            if( ready )
                return true;
            else
            {
                waitTime = timeoutInMillis - ( System.currentTimeMillis() - startTime );
                if( waitTime <= 0 )
                {
                    return ready;
                }
            }
        }
    }

    public boolean isReady()
    {
        return ready.get();
    }

    /**
     * Sets the result of the asynchronous operation, and mark it as finished.
     */
    protected void setValue( Object newValue )
    {
        if( ready.compareAndSet( false, true ) )
        {
            result = newValue;
            completionLatch.countDown();
            notifyListeners();
        }
    }

    /**
     * Returns the result of the asynchronous operation.
     */
    protected Object getValue()
    {
        return result;
    }

    public void addListener( IoFutureListener listener )
    {
        if( listener == null )
        {
            throw new NullPointerException( "listener" );
        }

        listeners.add( listener );

        if( ready.get() )
        {
            listener.operationComplete( this );
        }
    }

    public void removeListener( IoFutureListener listener )
    {
        if( listener == null )
        {
            throw new NullPointerException( "listener" );
        }

        listeners.remove( listener );
    }

    private void notifyListeners()
    {
        for( IoFutureListener listener : listeners )
        {
            listener.operationComplete( this );
        }
    }
}
