/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.io;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.SessionInitializer;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoSession;
import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolHandlerFilterChain;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.ProtocolSession;

/**
 * A {@link ProtocolAcceptor} which wraps {@link IoAcceptor} to provide
 * low-level I/O.
 * <p>
 * Please note that the user-defined attributes of {@link ProtocolSession}
 * and its wrapping {@link IoSession} are shared.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoProtocolAcceptor implements ProtocolAcceptor
{
    private final IoAcceptor acceptor;

    private final IoAdapter adapter = new IoAdapter( new IoProtocolSessionManagerFilterChain( this ) );

    /**
     * Creates a new instance with the specified {@link IoAcceptor}.
     */
    public IoProtocolAcceptor( IoAcceptor acceptor )
    {
        if( acceptor == null )
            throw new NullPointerException( "acceptor" );

        this.acceptor = acceptor;
    }

    /**
     * Returns the underlying {@link IoAcceptor} instance this acceptor is
     * wrapping.
     */
    public IoAcceptor getIoAcceptor()
    {
        return acceptor;
    }

    public void bind( SocketAddress address, ProtocolProvider provider )
            throws IOException
    {
        acceptor.bind( address, adapter.adapt( provider ) );
    }
    
    public void bind( SocketAddress address, ProtocolProvider provider,
                      SessionInitializer initializer ) throws IOException
    {
        acceptor.bind( address, adapter.adapt( provider ), initializer );
    }

    public void unbind( SocketAddress address )
    {
        acceptor.unbind( address );
    }
    
    public ProtocolHandlerFilterChain getFilterChain()
    {
        return adapter.getFilterChain();
    }
    
    public SessionInitializer getDefaultSessionInitializer()
    {
        return acceptor.getDefaultSessionInitializer();
    }
    
    public void setDefaultSessionInitializer( SessionInitializer defaultInitializer )
    {
        acceptor.setDefaultSessionInitializer( defaultInitializer );
    }

    public ExceptionMonitor getExceptionMonitor()
    {
        return acceptor.getExceptionMonitor();
    }

    public void setExceptionMonitor( ExceptionMonitor monitor )
    {
        acceptor.setExceptionMonitor( monitor );
    }
}