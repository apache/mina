/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.util.ProtocolHandlerFilterManager;

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

    private final ProtocolHandlerFilterManager filterManager = new ProtocolHandlerFilterManager(
            ProtocolHandlerFilter.MIN_PRIORITY - 1,
            ProtocolHandlerFilter.MAX_PRIORITY );

    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor()
    {
        filterManager.addFilter( -1, new VmPipeFilter() );
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
                    filterManager, protocolProvider.getHandler() ) );
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

    public void addFilter( int priority, ProtocolHandlerFilter filter )
    {
        filterManager.addFilter( priority, filter );
    }

    public void removeFilter( ProtocolHandlerFilter filter )
    {
        filterManager.removeFilter( filter );
    }

    public List getAllFilters()
    {
        return filterManager.getAllFilters();
    }

    public void removeAllFilters()
    {
        filterManager.removeAllFilters();
    }

    static class Entry
    {
        final VmPipeAddress address;

        final ProtocolHandlerFilterManager filterManager;

        final ProtocolHandler handler;

        private Entry( VmPipeAddress address,
                      ProtocolHandlerFilterManager filterManager,
                      ProtocolHandler handler )
        {
            this.address = address;
            this.filterManager = filterManager;
            this.handler = handler;
        }
    }
}
