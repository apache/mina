/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe.support;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.util.Queue;

/**
 * A {@link IoSession} for in-VM transport (VM_PIPE).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeSessionImpl extends BaseIoSession
{
    private static final IoSessionConfig CONFIG = new IoSessionConfig() {};
    
    private final IoService manager;
    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private final IoHandler handler;
    private final VmPipeFilterChain filterChain;
    private final Set managedSessions;
    final VmPipeSessionImpl remoteSession;
    final Object lock;
    final Queue pendingDataQueue;

    /**
     * Constructor for client-side session.
     */
    public VmPipeSessionImpl( IoService manager, Object lock, SocketAddress localAddress,
                   IoHandler handler, IoFilterChainBuilder filterChainBuilder,
                   VmPipe remoteEntry ) throws IOException
    {
        this.manager = manager;
        this.lock = lock;
        this.localAddress = localAddress;
        this.remoteAddress = remoteEntry.getAddress();
        this.handler = handler;
        this.filterChain = new VmPipeFilterChain( this );
        this.pendingDataQueue = new Queue();

        this.managedSessions = remoteEntry.getManagedClientSessions();
        
        remoteSession = new VmPipeSessionImpl( manager, this, remoteEntry );
        
        // initialize remote session
        try
        {
            remoteEntry.getConfig().getFilterChainBuilder().buildFilterChain( remoteSession.getFilterChain() );
            ( ( VmPipeFilterChain ) remoteSession.getFilterChain() ).sessionCreated( remoteSession );
        }
        catch( Throwable t )
        {
            ExceptionMonitor.getInstance().exceptionCaught( t );
            IOException e = new IOException( "Failed to initialize remote session." );
            e.initCause( t );
            throw e;
        }
        
        // initialize client session
        try
        {
            filterChainBuilder.buildFilterChain( filterChain );
            handler.sessionCreated( this );
        }
        catch( Throwable t )
        {
            throw ( IOException ) new IOException( "Failed to create a session." ).initCause( t );
        }

        VmPipeIdleStatusChecker.getInstance().addSession( remoteSession );
        VmPipeIdleStatusChecker.getInstance().addSession( this );
        
        remoteSession.managedSessions.add( remoteSession );
        this.managedSessions.add( this );
        
        ( ( VmPipeFilterChain ) remoteSession.getFilterChain() ).sessionOpened( remoteSession );
        filterChain.sessionOpened( this );
    }

    /**
     * Constructor for server-side session.
     */
    private VmPipeSessionImpl( IoService manager, VmPipeSessionImpl remoteSession, VmPipe entry )
    {
        this.manager = manager;
        this.lock = remoteSession.lock;
        this.localAddress = remoteSession.remoteAddress;
        this.remoteAddress = remoteSession.localAddress;
        this.handler = entry.getHandler();
        this.filterChain = new VmPipeFilterChain( this );
        this.remoteSession = remoteSession;
        this.pendingDataQueue = new Queue();
        this.managedSessions = entry.getManagedServerSessions();
    }
    
    Set getManagedSessions()
    {
        return managedSessions;
    }

    public IoService getService()
    {
        return manager;
    }
    
    public IoSessionConfig getConfig()
    {
        return CONFIG;
    }

    public IoFilterChain getFilterChain()
    {
        return filterChain;
    }

    public IoHandler getHandler()
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
    
    protected void close0( CloseFuture closeFuture )
    {
        filterChain.filterClose( this, closeFuture );
    }
    
    protected void write0( WriteRequest writeRequest )
    {
        this.filterChain.filterWrite( this, writeRequest );
    }

    public int getScheduledWriteRequests()
    {
        return 0;
    }

    public TransportType getTransportType()
    {
        return TransportType.VM_PIPE;
    }

    public SocketAddress getRemoteAddress()
    {
        return remoteAddress;
    }

    public SocketAddress getLocalAddress()
    {
        return localAddress;
    }

    protected void updateTrafficMask()
    {
        if( getTrafficMask().isReadable() || getTrafficMask().isWritable())
        {
            Object[] data = null;
            synchronized( pendingDataQueue )
            {
                data = pendingDataQueue.toArray();
                pendingDataQueue.clear();
            }
            
            for( int i = 0; i < data.length; i++ )
            {
                if( data[ i ] instanceof WriteRequest )
                {
                    WriteRequest wr = ( WriteRequest ) data[ i ];
                    filterChain.doWrite( this, wr );
                }
                else
                {
                    filterChain.messageReceived( this, data[ i ] );
                }
            }
        }
    }
}
