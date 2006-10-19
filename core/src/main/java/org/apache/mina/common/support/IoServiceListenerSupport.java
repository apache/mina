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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.IdentityHashSet;

/**
 * A helper which provides addition and removal of {@link IoServiceListener}s and firing
 * events.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceListenerSupport
{
    /**
     * The {@link IoService} that this instance manages.
     */
    private final IoService service;
    
    /**
     * A list of {@link IoServiceListener}s.
     */
    private final List listeners = new ArrayList();

    /**
     * Tracks managed sesssions.
     */
    private final Set managedSessions = new IdentityHashSet();
    
    private boolean activated;
    
    /**
     * Creates a new instance.
     */
    public IoServiceListenerSupport( IoService service )
    {
        if( service == null )
        {
            throw new NullPointerException( "service" );
        }
        this.service = service;
    }
    
    /**
     * Adds a new listener.
     */
    public void add( IoServiceListener listener )
    {
        synchronized( listeners )
        {
            listeners.add( listener );
        }
    }

    /**
     * Removes an existing listener.
     */
    public void remove( IoServiceListener listener )
    {
        synchronized( listeners )
        {
            listeners.remove( listener );
        }
    }
    
    public Set getManagedSessions()
    {
        synchronized( managedSessions )
        {
            return new IdentityHashSet( managedSessions );
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceActivated(IoService)}
     * for all registered listeners.
     */
    public void fireServiceActivated()
    {
        synchronized( listeners )
        {
            if( activated )
            {
                return;
            }
            
            try
            {
                for( Iterator i = listeners.iterator(); i.hasNext(); )
                {
                    ( ( IoServiceListener ) i.next() ).serviceActivated( service );
                }
            }
            finally
            {
                activated = true;
            }
        }
    }
    
    /**
     * Calls {@link IoServiceListener#serviceDeactivated(IoService)}
     * for all registered listeners.
     */
    public void fireServiceDeactivated()
    {
        boolean disconnect = false;
        try
        {
            synchronized( listeners )
            {
                if( !activated )
                {
                    return;
                }
                
                disconnect = true;

                try
                {
                    for( Iterator i = listeners.iterator(); i.hasNext(); )
                    {
                        ( ( IoServiceListener ) i.next() ).serviceDeactivated( service );
                    }
                }
                finally
                {
                    activated = false;
                }
            }
        }
        finally
        {
            if( disconnect )
            {
                disconnectSessions();
            }
        }
    }
    
    
    /**
     * Calls {@link IoServiceListener#sessionCreated(IoSession)} for all registered listeners.
     */
    public void fireSessionCreated( IoSession session )
    {
        boolean firstSession = false;
        synchronized( managedSessions )
        {
            firstSession = managedSessions.isEmpty();

            // If already registered, ignore.
            if ( !managedSessions.add( session ) )
            {
                return;
            }
        }
        
        // If the first connector session, fire a virtual service activation event.
        if( session.getService() instanceof IoConnector && firstSession )
        {
            fireServiceActivated();
        }

        // Fire session events.
        session.getFilterChain().fireSessionCreated( session );
        session.getFilterChain().fireSessionOpened( session);
        
        // Fire listener events.
        synchronized( listeners )
        {
            for( Iterator i = listeners.iterator(); i.hasNext(); )
            {
                ( ( IoServiceListener ) i.next() ).sessionCreated( session );
            }
        }
    }
    
    /**
     * Calls {@link IoServiceListener#sessionDestroyed(IoSession)} for all registered listeners.
     */
    public void fireSessionDestroyed( IoSession session )
    {
        boolean lastSession = false;
        synchronized( managedSessions )
        {
            // Try to remove the remaining empty seession set after removal.
            if( !managedSessions.remove( session ) )
            {
                return;
            }
            
            lastSession = managedSessions.isEmpty();
        }
        
        // Fire session events.
        session.getFilterChain().fireSessionClosed( session );
        
        // Fire listener events.
        try
        {
            synchronized( listeners )
            {
                for( Iterator i = listeners.iterator(); i.hasNext(); )
                {
                    ( ( IoServiceListener ) i.next() ).sessionDestroyed( session );
                }
            }
        }
        finally
        {
            // Fire a virtual service deactivation event for the last session of the connector.
            if( session.getService() instanceof IoConnector && lastSession )
            {
                fireServiceDeactivated();
            }
        }
    }

    private void disconnectSessions()
    {
        if( !( service instanceof IoAcceptor ) )
        {
            return;
        }

        if( !( ( IoAcceptor ) service ).isDisconnectOnUnbind() )
        {
            return;
        }

        final Object lock = new Object();
        Set sessionsCopy;
        synchronized( managedSessions )
        {
            sessionsCopy = new IdentityHashSet( managedSessions );
        }
        
        for( Iterator i = sessionsCopy.iterator(); i.hasNext(); )
        {
            ( ( IoSession ) i.next() ).close().addListener( new IoFutureListener()
            {
                public void operationComplete( IoFuture future )
                {
                    synchronized( lock )
                    {
                        lock.notifyAll();
                    }
                }
            } );
        }

        try
        {
            synchronized( lock )
            {
                while( !managedSessions.isEmpty() )
                {
                    lock.wait( 500 );
                }
            }
        }
        catch( InterruptedException ie )
        {
            // Ignored
        }
    }
}
