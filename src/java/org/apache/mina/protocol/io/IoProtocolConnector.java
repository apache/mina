/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.io;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.io.IoConnector;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolConnector;
import org.apache.mina.protocol.ProtocolFilterChain;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;

/**
 * A {@link ProtocolConnector} which wraps {@link IoConnector} to provide
 * low-level I/O.
 * <p>
 * Please note that the user-defined attributes of {@link ProtocolSession}
 * and its wrapping {@link IoSession} are shared.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoProtocolConnector implements ProtocolConnector
{
    private final IoConnector connector;

    private final IoAdapter adapter = new IoAdapter( new IoProtocolSessionManagerFilterChain( this ) );

    /**
     * Creates a new instance with the specified {@link IoConnector}.
     */
    public IoProtocolConnector( IoConnector connector )
    {
        if( connector == null )
            throw new NullPointerException( "connector" );
        this.connector = connector;
    }

    /**
     * Returns the underlying {@link IoConnector} instance this acceptor is
     * wrapping.
     */
    public IoConnector getIoConnector()
    {
        return connector;
    }

    public ProtocolSession connect( SocketAddress address,
                                    ProtocolProvider provider ) throws IOException
    {
        IoSession session = connector.connect(
                address, adapter.adapt( provider ) );
        return adapter.toProtocolSession( session );
    }

    public ProtocolSession connect( SocketAddress address, SocketAddress localAddress,
                                    ProtocolProvider provider ) throws IOException
    {
        IoSession session = connector.connect(
                address, localAddress, adapter.adapt( provider ) );
        return adapter.toProtocolSession( session );
    }

    public ProtocolSession connect( SocketAddress address, int timeout,
                                    ProtocolProvider provider ) throws IOException
    {
        IoSession session = connector.connect(
                address, timeout, adapter.adapt( provider ) );
        return adapter.toProtocolSession( session );
    }

    public ProtocolSession connect( SocketAddress address, SocketAddress localAddress,
                                    int timeout, ProtocolProvider provider ) throws IOException
    {
        IoSession session = connector.connect(
                address, localAddress, timeout, adapter.adapt( provider ) );
        return adapter.toProtocolSession( session );
    }

    public ProtocolFilterChain getFilterChain()
    {
        return adapter.getFilterChain();
    }

    public ExceptionMonitor getExceptionMonitor()
    {
        return connector.getExceptionMonitor();
    }

    public void setExceptionMonitor( ExceptionMonitor monitor )
    {
        connector.setExceptionMonitor( monitor );
    }
}