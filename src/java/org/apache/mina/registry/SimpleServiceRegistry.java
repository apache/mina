/*
 * @(#) $Id$
 */
package org.apache.mina.registry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.io.datagram.DatagramAcceptor;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.protocol.ProtocolAcceptor;
import org.apache.mina.protocol.ProtocolHandlerFilter;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.filter.ProtocolThreadPoolFilter;
import org.apache.mina.protocol.io.IoProtocolAcceptor;
import org.apache.mina.protocol.vmpipe.VmPipeAcceptor;
import org.apache.mina.protocol.vmpipe.VmPipeAddress;

/**
 * A simple implementation of {@link ServiceRegistry}.
 * 
 * This service registry supports socket, datagram, VM-pipe transport types,
 * and thread pools were added by default. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
 */
public class SimpleServiceRegistry implements ServiceRegistry
{
    protected final IoAcceptor socketIoAcceptor = new SocketAcceptor();

    protected final IoAcceptor datagramIoAcceptor = new DatagramAcceptor();

    protected final ProtocolAcceptor socketProtocolAcceptor = new IoProtocolAcceptor(
            socketIoAcceptor );

    protected final ProtocolAcceptor datagramProtocolAcceptor = new IoProtocolAcceptor(
            datagramIoAcceptor );

    protected final ProtocolAcceptor vmPipeAcceptor = new VmPipeAcceptor();

    protected final IoThreadPoolFilter ioThreadPoolFilter = new IoThreadPoolFilter();

    protected final ProtocolThreadPoolFilter protocolThreadPoolFilter = new ProtocolThreadPoolFilter();

    private final Set services = new HashSet();

    public SimpleServiceRegistry() throws IOException
    {
        socketIoAcceptor.addFilter( IoHandlerFilter.MAX_PRIORITY,
                ioThreadPoolFilter );
        datagramIoAcceptor.addFilter( IoHandlerFilter.MAX_PRIORITY,
                ioThreadPoolFilter );
        socketProtocolAcceptor.addFilter( ProtocolHandlerFilter.MAX_PRIORITY,
                protocolThreadPoolFilter );
        datagramProtocolAcceptor.addFilter(
                ProtocolHandlerFilter.MAX_PRIORITY, protocolThreadPoolFilter );
        vmPipeAcceptor.addFilter( ProtocolHandlerFilter.MAX_PRIORITY,
                protocolThreadPoolFilter );
    }

    public synchronized void bind( Service service, IoHandler ioHandler )
            throws IOException
    {
        IoAcceptor acceptor = findIoAcceptor( service.getTransportType() );
        acceptor.bind( new InetSocketAddress( service.getPort() ), ioHandler );
        startThreadPools();
        services.add( service );
    }

    public synchronized void bind( Service service,
                                  ProtocolProvider protocolProvider )
            throws IOException
    {
        ProtocolAcceptor acceptor = findProtocolAcceptor( service
                .getTransportType() );
        if( acceptor instanceof VmPipeAcceptor )
        {
            acceptor.bind( new VmPipeAddress( service.getPort() ),
                    protocolProvider );
        }
        else
        {
            acceptor.bind( new InetSocketAddress( service.getPort() ),
                    protocolProvider );
        }
        startThreadPools();
        services.add( service );
    }

    public synchronized void unbind( Service service )
    {
        ProtocolAcceptor acceptor = findProtocolAcceptor( service
                .getTransportType() );
        acceptor.unbind( new InetSocketAddress( service.getPort() ) );
        acceptor.unbind( new VmPipeAddress( service.getPort() ) );
        services.remove( service );
        stopThreadPools();
    }

    public synchronized void addFilter( int priority, IoHandlerFilter filter )
    {
        boolean s = false;
        boolean d = false;
        try
        {
            socketIoAcceptor.addFilter( priority, filter );
            s = true;
            datagramIoAcceptor.addFilter( priority, filter );
            d = true;
        }
        finally
        {
            if( !s || !d )
            {
                // rollback
                if( s )
                {
                    socketIoAcceptor.removeFilter( filter );
                }

                if( d )
                {
                    datagramIoAcceptor.removeFilter( filter );
                }
            }
        }
    }

    public synchronized void addFilter( int priority,
                                       ProtocolHandlerFilter filter )
    {
        boolean s = false;
        boolean d = false;
        boolean v = false;
        try
        {
            socketProtocolAcceptor.addFilter( priority, filter );
            s = true;
            datagramProtocolAcceptor.addFilter( priority, filter );
            d = true;
            vmPipeAcceptor.addFilter( priority, filter );
            v = true;
        }
        finally
        {
            if( !s || !d || !v )
            {
                // rollback
                if( s )
                {
                    socketProtocolAcceptor.removeFilter( filter );
                }

                if( d )
                {
                    datagramProtocolAcceptor.removeFilter( filter );
                }

                if( v )
                {
                    vmPipeAcceptor.removeFilter( filter );
                }
            }
        }
    }

    public synchronized void addFilter( TransportType transportType,
                                       int priority, IoHandlerFilter filter )
    {
        IoAcceptor acceptor = findIoAcceptor( transportType );
        acceptor.addFilter( priority, filter );
    }

    public synchronized void addFilter( TransportType transportType,
                                       int priority,
                                       ProtocolHandlerFilter filter )
    {
        ProtocolAcceptor acceptor = findProtocolAcceptor( transportType );
        acceptor.addFilter( priority, filter );
    }

    public synchronized void removeFilter( IoHandlerFilter filter )
    {
        socketIoAcceptor.removeFilter( filter );
        datagramIoAcceptor.removeFilter( filter );
    }

    public synchronized void removeFilter( ProtocolHandlerFilter filter )
    {
        socketProtocolAcceptor.removeFilter( filter );
        datagramProtocolAcceptor.removeFilter( filter );
        vmPipeAcceptor.removeFilter( filter );
    }

    public synchronized void removeFilter( TransportType transportType,
                                          IoHandlerFilter filter )
    {
        IoAcceptor acceptor = findIoAcceptor( transportType );
        acceptor.removeFilter( filter );
    }

    public synchronized void removeFilter( TransportType transportType,
                                          ProtocolHandlerFilter filter )
    {
        ProtocolAcceptor acceptor = findProtocolAcceptor( transportType );
        acceptor.removeFilter( filter );
    }

    public synchronized Set getAllServices()
    {
        return new HashSet( services );
    }

    public synchronized Set getServices( String name )
    {
        Set result = new HashSet();
        Iterator it = services.iterator();
        while( it.hasNext() )
        {
            Service s = ( Service ) it.next();
            if( name.equals( s.getName() ) )
            {
                result.add( s );
            }
        }
        return result;
    }

    public Set getServices( TransportType transportType )
    {
        Set result = new HashSet();
        Iterator it = services.iterator();
        while( it.hasNext() )
        {
            Service s = ( Service ) it.next();
            if( s.getTransportType() == transportType )
            {
                result.add( s );
            }
        }
        return result;
    }

    public Set getServices( int port )
    {
        Set result = new HashSet();
        Iterator it = services.iterator();
        while( it.hasNext() )
        {
            Service s = ( Service ) it.next();
            if( s.getPort() == port )
            {
                result.add( s );
            }
        }
        return result;
    }

    protected IoAcceptor findIoAcceptor( TransportType transportType )
    {
        if( transportType == TransportType.SOCKET )
            return socketIoAcceptor;
        else if( transportType == TransportType.DATAGRAM )
            return datagramIoAcceptor;
        else
            throw new IllegalArgumentException(
                    "Unsupported transport type: " + transportType );

    }

    protected ProtocolAcceptor findProtocolAcceptor(
                                                    TransportType transportType )
    {
        if( transportType == TransportType.SOCKET )
            return socketProtocolAcceptor;
        else if( transportType == TransportType.DATAGRAM )
            return datagramProtocolAcceptor;
        else if( transportType == TransportType.VM_PIPE )
            return vmPipeAcceptor;
        else
            throw new IllegalArgumentException(
                    "Unsupported transport type: " + transportType );
    }

    private void startThreadPools()
    {
        if( !services.isEmpty() )
            return;

        ioThreadPoolFilter.start();
        protocolThreadPoolFilter.start();
    }

    private void stopThreadPools()
    {
        if( !services.isEmpty() )
            return;

        ioThreadPoolFilter.stop();
        protocolThreadPoolFilter.stop();
    }
}
