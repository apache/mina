/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import org.apache.mina.protocol.ProtocolConnector;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.vmpipe.VmPipeAcceptor.Entry;
import org.apache.mina.util.ProtocolHandlerFilterManager;

/**
 * Connects to {@link ProtocolProvider}s which is bound on the specified
 * {@link VmPipeAddress}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeConnector implements ProtocolConnector
{
    private final ProtocolHandlerFilterManager filterManager = new ProtocolHandlerFilterManager();

    /**
     * Creates a new instance.
     */
    public VmPipeConnector()
    {
        filterManager.addFilter( Integer.MIN_VALUE - 1, new VmPipeFilter() );
    }

    public void addFilter( int priority, ProtocolHandlerFilter filter )
    {
        filterManager.addFilter( priority, filter );
    }

    public void removeFilter( ProtocolHandlerFilter filter )
    {
        filterManager.removeFilter( filter );
    }

    public void removeAllFilters()
    {
    	filterManager.removeAllFilters();
    }

    public List getAllFilters()
    {
    	return filterManager.getAllFilters();
    }

    public ProtocolSession connect( SocketAddress address,
                                   ProtocolProvider protocolProvider )
            throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( protocolProvider == null )
            throw new NullPointerException( "protocolProvider" );
        if( ! ( address instanceof VmPipeAddress ) )
            throw new IllegalArgumentException(
                                                "address must be VmPipeAddress." );

        Entry entry = ( Entry ) VmPipeAcceptor.boundHandlers.get( address );
        if( entry == null )
            throw new IOException( "Endpoint unavailable: " + address );

        VmPipeSession session = new VmPipeSession(
                                                   new Object(), // lock
                                                   AnonymousVmPipeAddress.INSTANCE,
                                                   entry.address,
                                                   filterManager,
                                                   protocolProvider
                                                           .getHandler(),
                                                   entry.filterManager,
                                                   entry.handler );
        VmPipeIdleStatusChecker.INSTANCE.addSession( session );
        return session;
    }

    public ProtocolSession connect( SocketAddress address, int timeout,
                                   ProtocolProvider protocolProvider )
            throws IOException
    {
        return connect( address, protocolProvider );
    }
}