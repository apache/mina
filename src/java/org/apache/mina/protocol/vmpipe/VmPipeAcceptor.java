/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.BaseSessionManager;
import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolFilterChain;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;

/**
 * Binds the specified {@link ProtocolProvider} to the specified
 * {@link VmPipeAddress}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAcceptor extends BaseSessionManager implements ProtocolAcceptor
{
    static final Map boundHandlers = new HashMap();

    private final VmPipeSessionManagerFilterChain filterChain =
        new VmPipeSessionManagerFilterChain( this );

    /**
     * Creates a new instance.
     */
    public VmPipeAcceptor()
    {
        filterChain.addFirst( "VMPipe", new VmPipeFilter() );
    }
    
    public void bind( SocketAddress address, ProtocolProvider protocolProvider ) throws IOException
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

            boundHandlers.put( address, 
                               new Entry( this,
                                          ( VmPipeAddress ) address,
                                          filterChain,
                                          protocolProvider.getHandler() ) );
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
    
    public ProtocolFilterChain getFilterChain()
    {
        return filterChain;
    }

    static class Entry
    {
        final VmPipeAcceptor acceptor;
        
        final VmPipeAddress address;

        final VmPipeSessionManagerFilterChain managerFilterChain;

        final ProtocolHandler handler;
        
        private Entry( VmPipeAcceptor acceptor,
                       VmPipeAddress address,
                       VmPipeSessionManagerFilterChain managerFilterChain,
                       ProtocolHandler handler )
        {
            this.acceptor = acceptor;
            this.address = address;
            this.managerFilterChain = managerFilterChain;
            this.handler = handler;
        }
    }

    public ProtocolSession newSession( SocketAddress remoteAddress, SocketAddress localAddress )
    {
        throw new UnsupportedOperationException();
    }
}
