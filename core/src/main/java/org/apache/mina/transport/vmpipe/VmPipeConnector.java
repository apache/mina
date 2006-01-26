/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.BaseIoConnectorConfig;
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
    private static final IoSessionConfig CONFIG = new IoSessionConfig() {};
    private final IoServiceConfig defaultConfig = new BaseIoConnectorConfig()
    {
        public IoSessionConfig getSessionConfig()
        {
            return CONFIG;
        }
    };

    public ConnectFuture connect( SocketAddress address, IoHandler handler, IoServiceConfig config ) 
    {
        return connect( address, null, handler, config );
    }

    public ConnectFuture connect( SocketAddress address, SocketAddress localAddress, IoHandler handler, IoServiceConfig config )
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );
        if( ! ( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                                                "address must be VmPipeAddress." );

        if( config == null )
        {
            config = getDefaultConfig();
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
                        config.getFilterChainBuilder(),
                        entry );
            future.setSession( session );
        }
        catch( IOException e )
        {
            future.setException( e );
        }
        return future;
    }
    
    public IoServiceConfig getDefaultConfig()
    {
        return defaultConfig;
    }
}