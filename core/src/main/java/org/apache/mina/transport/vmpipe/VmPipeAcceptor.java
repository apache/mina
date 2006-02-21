/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.BaseIoAcceptorConfig;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.transport.vmpipe.support.VmPipe;
import org.apache.mina.util.IdentityHashSet;

/**
 * Binds the specified {@link IoHandler} to the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAcceptor extends BaseIoAcceptor
{
    static final Map boundHandlers = new HashMap();
    
    private static final IoSessionConfig CONFIG = new BaseIoSessionConfig() {};
    private final IoServiceConfig defaultConfig = new BaseIoAcceptorConfig()
    {
        public IoSessionConfig getSessionConfig()
        {
            return CONFIG;
        }
    };

    public void bind( SocketAddress address, IoHandler handler, IoServiceConfig config ) throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );
        if( !( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                    "address must be VmPipeAddress." );

        if( config == null )
        {
            config = getDefaultConfig();
        }

        synchronized( boundHandlers )
        {
            if( boundHandlers.containsKey( address ) )
            {
                throw new IOException( "Address already bound: " + address );
            }

            boundHandlers.put( address, 
                               new VmPipe( this,
                                          ( VmPipeAddress ) address,
                                          handler, config ) );
        }
    }
    
    public Set getManagedSessions( SocketAddress address )
    {
        if( address == null )
            throw new NullPointerException( "address" );
        
        VmPipe pipe = null;
        synchronized( boundHandlers )
        {
            pipe = ( VmPipe ) boundHandlers.get( address );
            if( pipe == null )
            {
                throw new IllegalArgumentException( "Address not bound: " + address );
            }
        }
        
        Set managedSessions = pipe.getManagedServerSessions();
        return Collections.unmodifiableSet(
                new IdentityHashSet( Arrays.asList( managedSessions.toArray() ) ) );
    }

    public void unbind( SocketAddress address )
    {
        if( address == null )
            throw new NullPointerException( "address" );

        VmPipe pipe = null;
        synchronized( boundHandlers )
        {
            if( !boundHandlers.containsKey( address ) )
            {
                throw new IllegalArgumentException( "Address not bound: " + address );
            }
            
            pipe = ( VmPipe ) boundHandlers.remove( address );
        }
        
        Set managedSessions = pipe.getManagedServerSessions();
        
        IoServiceConfig cfg = pipe.getConfig();
        boolean disconnectOnUnbind;
        if( cfg instanceof IoAcceptorConfig )
        {
            disconnectOnUnbind = ( ( IoAcceptorConfig ) cfg ).isDisconnectOnUnbind();
        }
        else
        {
            disconnectOnUnbind = ( ( IoAcceptorConfig ) getDefaultConfig() ).isDisconnectOnUnbind();
        }
        if( disconnectOnUnbind && managedSessions != null )
        {
            IoSession[] tempSessions = ( IoSession[] ) 
                                  managedSessions.toArray( new IoSession[ 0 ] );
            
            final Object lock = new Object();
            
            for( int i = 0; i < tempSessions.length; i++ )
            {
                if( !managedSessions.contains( tempSessions[ i ] ) )
                {
                    // The session has already been closed and have been 
                    // removed from managedSessions by the VmPipeFilterChain.
                    continue;
                }
                tempSessions[ i ].close().setCallback( new IoFuture.Callback()
                {
                    public void operationComplete( IoFuture future )
                    {
                        synchronized( lock )
                        {
                            lock.notify();
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
                        lock.wait( 1000 );
                    }
                }
            }
            catch( InterruptedException ie )
            {
                // Ignored
            }
        }                
    }
    
    public void unbindAll()
    {
        synchronized( boundHandlers )
        {
            List addresses = new ArrayList( boundHandlers.keySet() );
            for( Iterator i = addresses.iterator(); i.hasNext(); )
            {
                unbind( ( SocketAddress ) i.next() );
            }
        }
    }
    
    public boolean isBound( SocketAddress address )
    {
        synchronized( boundHandlers )
        {
            return boundHandlers.containsKey( address );
        }
    }
    
    public IoServiceConfig getDefaultConfig()
    {
        return defaultConfig;
    }
}
