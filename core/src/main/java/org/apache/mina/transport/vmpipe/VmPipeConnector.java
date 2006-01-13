/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.transport.vmpipe.support.VmPipe;
import org.apache.mina.transport.vmpipe.support.VmPipeSessionImpl;
import org.apache.mina.util.AnonymousSocketAddress;

/**
 * Connects to {@link IoHandler}s which is bound on the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeConnector extends BaseIoConnector
{
    public ConnectFuture connect( SocketAddress address, IoHandler handler, IoFilterChainBuilder filterChainBuilder ) 
    {
        return connect( address, null, handler, filterChainBuilder );
    }

    public ConnectFuture connect( SocketAddress address, SocketAddress localAddress, IoHandler handler, IoFilterChainBuilder filterChainBuilder )
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );
        if( ! ( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                                                "address must be VmPipeAddress." );

        if( filterChainBuilder == null )
        {
            filterChainBuilder = IoFilterChainBuilder.NOOP;
        }

        VmPipe entry = ( VmPipe ) VmPipeAcceptor.boundHandlers.get( address );
        if( entry == null )
        {
            return ConnectFuture.newFailedFuture(
                    new IOException( "Endpoint unavailable: " + address ) );
        }

        ConnectFuture future = new ConnectFuture();
        try
        {
            VmPipeSessionImpl session =
                new VmPipeSessionImpl(
                        this,
                        new Object(), // lock
                        AnonymousSocketAddress.INSTANCE,
                        handler,
                        filterChainBuilder,
                        entry );
            future.setSession( session );
        }
        catch( IOException e )
        {
            future.setException( e );
        }
        return future;
    }
}