/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.io;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.FilterChainType;
import org.apache.mina.io.IoConnector;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolConnector;
import org.apache.mina.protocol.ProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;

/**
 * A {@link ProtocolConnector} which wraps {@link IoConnector} to provide
 * low-level I/O.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoProtocolConnector implements ProtocolConnector
{
    private final IoConnector connector;

    private final IoAdapter adapter = new IoAdapter();

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
                                   ProtocolProvider provider )
            throws IOException
    {
        IoSession session = connector.connect( address, adapter
                .adapt( provider ) );
        return adapter.toProtocolSession( session );
    }

    public ProtocolSession connect( SocketAddress address, int timeout,
                                   ProtocolProvider provider )
            throws IOException
    {
        IoSession session = connector.connect( address, timeout, adapter
                .adapt( provider ) );
        return adapter.toProtocolSession( session );
    }

    public ProtocolHandlerFilterChain newFilterChain( FilterChainType type )
    {
        return adapter.newFilterChain( type );
    }

    public ProtocolHandlerFilterChain getFilterChain()
    {
        return adapter.getFilterChain();
    }
}