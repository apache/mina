/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.transport.vmpipe.support.VmPipe;

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

    public void bind( SocketAddress address, IoHandler handler, IoFilterChainBuilder filterChainBuilder ) throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );
        if( !( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                    "address must be VmPipeAddress." );

        if( filterChainBuilder == null )
        {
            filterChainBuilder = IoFilterChainBuilder.NOOP;
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
                                          handler, filterChainBuilder ) );
        }
    }

    public Collection getManagedSessions( SocketAddress address )
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
        return Collections.unmodifiableCollection( Arrays.asList( managedSessions.toArray() ) );
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
        
        if( isDisconnectClientsOnUnbind() && managedSessions != null )
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
}
