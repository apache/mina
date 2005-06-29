/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.BaseSession;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolFilterChain;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolSessionFilterChain;
import org.apache.mina.protocol.vmpipe.VmPipeAcceptor.Entry;
import org.apache.mina.util.ExceptionUtil;

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

    private final ProtocolHandler handler;
    
    private final VmPipeSessionConfig config = new VmPipeSessionConfig();

    private final ProtocolSessionFilterChain filterChain;
    
    private final VmPipeSessionManagerFilterChain managerFilterChain;

    final VmPipeSession remoteSession;

    final Object lock;

    boolean closed;

    /**
     * Constructor for client-side session.
     */
    VmPipeSession( Object lock, SocketAddress localAddress,
                   VmPipeSessionManagerFilterChain managerFilterChain,
                   ProtocolHandler handler,
                   Entry remoteEntry ) throws IOException
    {
        this.lock = lock;
        this.localAddress = localAddress;
        this.remoteAddress = remoteEntry.address;
        this.handler = handler;
        this.filterChain = new ProtocolSessionFilterChain( managerFilterChain );
        this.managerFilterChain = managerFilterChain;

        remoteSession = new VmPipeSession( this, remoteEntry );
        
        // initialize remote session
        try
        {
            remoteEntry.handler.sessionCreated( remoteSession );
        }
        catch( Throwable t )
        {
            remoteEntry.acceptor.getExceptionMonitor().exceptionCaught( remoteEntry.acceptor, t );
            IOException e = new IOException( "Failed to initialize remote session." );
            e.initCause( t );
            throw e;
        }
        
        // initialize client session
        try
        {
            handler.sessionCreated( this );
        }
        catch( Throwable t )
        {
            ExceptionUtil.throwException( t );
        }

        remoteEntry.managerFilterChain.sessionOpened( remoteSession );
        managerFilterChain.sessionOpened( this );
    }

    /**
     * Constructor for server-side session.
     */
    VmPipeSession( VmPipeSession remoteSession, Entry entry )
    {
        this.lock = remoteSession.lock;
        this.localAddress = remoteSession.remoteAddress;
        this.remoteAddress = remoteSession.localAddress;
        this.handler = entry.handler;
        this.managerFilterChain = entry.managerFilterChain;
        this.filterChain = new ProtocolSessionFilterChain( entry.managerFilterChain );
        this.remoteSession = remoteSession;
    }
    
    VmPipeSessionManagerFilterChain getManagerFilterChain()
    {
        return managerFilterChain;
    }
    
    public ProtocolFilterChain getFilterChain()
    {
        return filterChain;
    }

    public ProtocolHandler getHandler()
    {
        return handler;
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
            managerFilterChain.sessionClosed( this );
            remoteSession.getManagerFilterChain().sessionClosed( remoteSession );
        }
    }

    public void write( Object message )
    {
        this.filterChain.filterWrite( this, message );
    }

    public int getScheduledWriteRequests()
    {
        return 0;
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