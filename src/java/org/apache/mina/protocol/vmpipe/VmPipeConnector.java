/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.protocol.ProtocolConnector;
import org.apache.mina.protocol.ProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.vmpipe.VmPipeAcceptor.Entry;

/**
 * Connects to {@link ProtocolProvider}s which is bound on the specified
 * {@link VmPipeAddress}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeConnector implements ProtocolConnector
{
    private final VmPipeFilterChain filters = new VmPipeFilterChain();

    /**
     * Creates a new instance.
     */
    public VmPipeConnector()
    {
        filters.addLast( "VMPipe", new VmPipeFilter() );
    }
    
    public ProtocolHandlerFilterChain newFilterChain()
    {
        return new VmPipeFilterChain();
    }
    
    public ProtocolHandlerFilterChain getFilterChain()
    {
        return filters;
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
                                                   filters,
                                                   protocolProvider
                                                           .getHandler(),
                                                   entry.filters,
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