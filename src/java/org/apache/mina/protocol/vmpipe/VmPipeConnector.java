/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.BaseSessionManager;
import org.apache.mina.protocol.ProtocolConnector;
import org.apache.mina.protocol.ProtocolFilterChain;
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
public class VmPipeConnector extends BaseSessionManager implements ProtocolConnector
{
    private final VmPipeSessionManagerFilterChain filterChain =
        new VmPipeSessionManagerFilterChain( this );

    /**
     * Creates a new instance.
     */
    public VmPipeConnector()
    {
        filterChain.addFirst( "VMPipe", new VmPipeFilter() );
    }
    
    public ProtocolFilterChain getFilterChain()
    {
        return filterChain;
    }

    public ProtocolSession connect( SocketAddress address, ProtocolProvider protocolProvider ) throws IOException 
    {
        return connect( address, null, Integer.MAX_VALUE, protocolProvider );
    }

    public ProtocolSession connect( SocketAddress address, SocketAddress localAddress, ProtocolProvider protocolProvider ) throws IOException
    {
        return connect( address, localAddress, Integer.MAX_VALUE, protocolProvider );
    }

    public ProtocolSession connect( SocketAddress address, int timeout, ProtocolProvider protocolProvider ) throws IOException
    {
        return connect( address, null, timeout, protocolProvider );
    }

    public ProtocolSession connect( SocketAddress address, SocketAddress localAddress, int timeout, ProtocolProvider protocolProvider ) throws IOException
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

        VmPipeSession session = new VmPipeSession( new Object(), // lock
                                                   AnonymousVmPipeAddress.INSTANCE,
                                                   filterChain,
                                                   protocolProvider.getHandler(),
                                                   entry );

        VmPipeIdleStatusChecker.INSTANCE.addSession( session );
        return session;
    }
}