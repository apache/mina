/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.FilterChainType;
import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolProvider;

/**
 * Binds the specified {@link ProtocolProvider} to the specified
 * {@link VmPipeAddress}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAcceptor implements ProtocolAcceptor
{
    static final Map boundHandlers = new HashMap();

    private final VmPipeFilterChain filters = new VmPipeFilterChain( FilterChainType.PREPROCESS );

    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor()
    {
        filters.addLast( "VMPipe", new VmPipeFilter() );
    }

    public void bind( SocketAddress address, ProtocolProvider protocolProvider )
            throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( protocolProvider == null )
            throw new NullPointerException( "protocolProvider" );
        if( !( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                    "address must be VmPipeAddress." );

        synchronized( boundHandlers )
        {
            if( boundHandlers.containsKey( address ) )
            {
                throw new IOException( "Address already bound: " + address );
            }

            boundHandlers.put( address, new Entry( ( VmPipeAddress ) address,
                    filters, protocolProvider.getHandler() ) );
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
    
    public ProtocolHandlerFilterChain newFilterChain( FilterChainType type )
    {
        return new VmPipeFilterChain( type );
    }
    
    public ProtocolHandlerFilterChain getFilterChain()
    {
        return filters;
    }

    static class Entry
    {
        final VmPipeAddress address;

        final ProtocolHandlerFilterChain filters;

        final ProtocolHandler handler;

        private Entry( VmPipeAddress address,
                      ProtocolHandlerFilterChain filters,
                      ProtocolHandler handler )
        {
            this.address = address;
            this.filters = filters;
            this.handler = handler;
        }
    }
}
