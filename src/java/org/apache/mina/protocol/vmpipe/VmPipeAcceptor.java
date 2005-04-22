/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.SessionInitializer;
import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.util.BaseSessionManager;

/**
 * Binds the specified {@link ProtocolProvider} to the specified
 * {@link VmPipeAddress}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAcceptor extends BaseSessionManager implements ProtocolAcceptor
{
    static final Map boundHandlers = new HashMap();

    private final VmPipeFilterChain filters = new VmPipeFilterChain();

    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor()
    {
        filters.addLast( "VMPipe", new VmPipeFilter() );
    }
    
    public void bind( SocketAddress address, ProtocolProvider protocolProvider ) throws IOException
    {
        bind( address, protocolProvider, null );
    }

    public void bind( SocketAddress address, ProtocolProvider protocolProvider,
                      SessionInitializer initializer ) throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( protocolProvider == null )
            throw new NullPointerException( "protocolProvider" );
        if( !( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                    "address must be VmPipeAddress." );
        if( initializer == null )
        {
            initializer = defaultInitializer;
        }

        synchronized( boundHandlers )
        {
            if( boundHandlers.containsKey( address ) )
            {
                throw new IOException( "Address already bound: " + address );
            }

            boundHandlers.put( address, 
                               new Entry( this,
                                          ( VmPipeAddress ) address,
                                          filters,
                                          protocolProvider.getHandler(),
                                          initializer ) );
        }
    }

    public void unbind( SocketAddress address )
    {
        if( address == null )
            throw new NullPointerException( "address" );

        synchronized( boundHandlers )
        {
            boundHandlers.remove( address );
        }
    }
    
    public ProtocolHandlerFilterChain getFilterChain()
    {
        return filters;
    }

    static class Entry
    {
        final VmPipeAcceptor acceptor;
        
        final VmPipeAddress address;

        final VmPipeFilterChain filters;

        final ProtocolHandler handler;
        
        final SessionInitializer initializer;

        private Entry( VmPipeAcceptor acceptor,
                       VmPipeAddress address,
                       VmPipeFilterChain filters,
                       ProtocolHandler handler,
                       SessionInitializer initializer )
        {
            this.acceptor = acceptor;
            this.address = address;
            this.filters = filters;
            this.handler = handler;
            this.initializer = initializer;
        }
    }
}
