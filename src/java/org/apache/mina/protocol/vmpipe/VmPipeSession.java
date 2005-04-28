/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.BaseSession;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.SessionInitializer;
import org.apache.mina.common.TransportType;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.vmpipe.VmPipeAcceptor.Entry;

/**
 * A {@link ProtocolSession} for in-VM transport (VM_PIPE).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeSession extends BaseSession implements ProtocolSession
{
    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    private final ProtocolHandler localHandler;
    
    private final VmPipeSessionConfig config = new VmPipeSessionConfig();

    final VmPipeFilterChain localFilters;

    final VmPipeFilterChain remoteFilters;

    final VmPipeSession remoteSession;

    final Object lock;

    boolean closed;

    /**
     * Constructor for client-side session.
     */
    VmPipeSession( Object lock, SocketAddress localAddress,
                   VmPipeFilterChain localFilters,
                   ProtocolHandler localHandler,
                   SessionInitializer initializer,
                   Entry remoteEntry ) throws IOException
    {
        this.lock = lock;
        this.localAddress = localAddress;
        this.localHandler = localHandler;
        this.localFilters = localFilters;
        this.remoteAddress = remoteEntry.address;
        this.remoteFilters = remoteEntry.filters;

        remoteSession = new VmPipeSession( this, remoteEntry.handler );
        
        // initialize remote session
        try
        {
            remoteEntry.initializer.initializeSession( remoteSession );
        }
        catch( Throwable t )
        {
            remoteEntry.acceptor.getExceptionMonitor().exceptionCaught( remoteEntry.acceptor, t );
            IOException e = new IOException( "Failed to initialize remote session." );
            e.initCause( t );
            throw e;
        }
        
        // initialize client session
        initializer.initializeSession( this );

        remoteEntry.filters.sessionOpened( remoteSession );
        localFilters.sessionOpened( this );
    }

    /**
     * Constructor for server-side session.
     */
    VmPipeSession( VmPipeSession remoteSession, ProtocolHandler localHandler )
    {
        this.lock = remoteSession.lock;
        this.localAddress = remoteSession.remoteAddress;
        this.localHandler = localHandler;
        this.localFilters = remoteSession.remoteFilters;
        this.remoteAddress = remoteSession.localAddress;
        this.remoteFilters = remoteSession.localFilters;

        this.remoteSession = remoteSession;
    }

    public ProtocolHandler getHandler()
    {
        return localHandler;
    }

    public ProtocolEncoder getEncoder()
    {
        return null;
    }

    public ProtocolDecoder getDecoder()
    {
        return null;
    }

    public void close( boolean wait )
    {
        synchronized( lock )
        {
            if( closed )
                return;

            closed = remoteSession.closed = true;
            localFilters.sessionClosed( this );
            remoteFilters.sessionClosed( remoteSession );
        }
    }

    public void write( Object message )
    {
        localFilters.filterWrite( this, message );
    }

    public TransportType getTransportType()
    {
        return TransportType.VM_PIPE;
    }

    public boolean isConnected()
    {
        return !closed;
    }

    public SessionConfig getConfig()
    {
        return config;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }
}