/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.vmpipe;

import java.net.SocketAddress;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.util.ProtocolHandlerFilterManager;
import org.apache.mina.util.ProtocolHandlerFilterManager.WriteCommand;

/**
 * TODO Document me.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
class VmPipeSession implements ProtocolSession
{
    private final Object lock;

    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    private final ProtocolHandler localHandler;

    private final VmPipeSessionConfig config = new VmPipeSessionConfig();

    private final WriteCommand writeCommand = new WriteCommandImpl();

    final ProtocolHandlerFilterManager localFilterManager;

    final ProtocolHandlerFilterManager remoteFilterManager;

    final VmPipeSession remoteSession;

    private Object attachment;

    boolean closed;

    long lastReadTime;

    long lastWriteTime;

    boolean bothIdle;

    boolean readerIdle;

    boolean writerIdle;

    /**
     * Constructor for client-side session.
     */
    VmPipeSession( Object lock, SocketAddress localAddress,
                  SocketAddress remoteAddress,
                  ProtocolHandlerFilterManager localFilterManager,
                  ProtocolHandler localHandler,
                  ProtocolHandlerFilterManager remoteFilterManager,
                  ProtocolHandler remoteHandler )
    {
        this.lock = lock;
        this.localAddress = localAddress;
        this.localHandler = localHandler;
        this.localFilterManager = localFilterManager;
        this.remoteAddress = remoteAddress;
        this.remoteFilterManager = remoteFilterManager;

        remoteSession = new VmPipeSession( this, remoteHandler );

        remoteFilterManager.fireSessionOpened( remoteSession );
        localFilterManager.fireSessionOpened( this );
    }

    /**
     * Constructor for server-side session.
     */
    VmPipeSession( VmPipeSession remoteSession, ProtocolHandler localHandler )
    {
        this.lock = remoteSession.lock;
        this.localAddress = remoteSession.remoteAddress;
        this.localHandler = localHandler;
        this.localFilterManager = remoteSession.remoteFilterManager;
        this.remoteAddress = remoteSession.localAddress;
        this.remoteFilterManager = remoteSession.localFilterManager;

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

    public void close()
    {
        synchronized( lock )
        {
            if( closed )
                return;

            closed = remoteSession.closed = true;
            localFilterManager.fireSessionClosed( this );
            remoteFilterManager.fireSessionClosed( remoteSession );
        }
    }

    public Object getAttachment()
    {
        return attachment;
    }

    public void setAttachment( Object attachment )
    {
        this.attachment = attachment;
    }

    public void write( Object message )
    {
        localFilterManager.write( this, writeCommand, message );
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

    public long getReadBytes()
    {
        return 0;
    }

    public long getWrittenBytes()
    {
        return 0;
    }

    public long getLastIoTime()
    {
        return Math.max( lastReadTime, lastWriteTime );
    }

    public long getLastReadTime()
    {
        return lastReadTime;
    }

    public long getLastWriteTime()
    {
        return lastWriteTime;
    }

    public boolean isIdle( IdleStatus status )
    {
        if( status == null )
            throw new NullPointerException( "status" );

        if( status == IdleStatus.BOTH_IDLE )
            return bothIdle;
        if( status == IdleStatus.READER_IDLE )
            return readerIdle;
        if( status == IdleStatus.WRITER_IDLE )
            return writerIdle;

        throw new IllegalArgumentException( "Illegal statue: " + status );
    }

    private class WriteCommandImpl implements WriteCommand
    {
        public void execute( Object message )
        {
            synchronized( lock )
            {
                if( closed )
                    throw new IllegalStateException( "Session is closed." );
                remoteFilterManager.fireMessageReceived( remoteSession,
                                                         message );
            }
        }
    }

}