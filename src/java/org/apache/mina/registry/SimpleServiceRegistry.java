/*
 * @(#) $Id$
 */
package org.apache.mina.registry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.datagram.DatagramAcceptor;
import org.apache.mina.io.filter.IoThreadPoolFilter;
import org.apache.mina.io.socket.SocketAcceptor;
import org.apache.mina.protocol.ProtocolAcceptor;
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

    public SimpleServiceRegistry()
    {
        socketIoAcceptor.getFilterChain().addFirst( "threadPool", ioThreadPoolFilter );
        datagramIoAcceptor.getFilterChain().addFirst( "threadPool", ioThreadPoolFilter );
        socketProtocolAcceptor.getFilterChain().addFirst( "threadPool", protocolThreadPoolFilter );
        datagramProtocolAcceptor.getFilterChain().addFirst( "threadPool", protocolThreadPoolFilter );
        vmPipeAcceptor.getFilterChain().addFirst( "threadPool", protocolThreadPoolFilter );
    }

    public void bind( Service service, IoHandler ioHandler ) throws IOException
    {
        IoAcceptor acceptor = findIoAcceptor( service.getTransportType() );
        acceptor.bind( service.getAddress(), ioHandler );
        startThreadPools();
        services.add( service );
    }

    public synchronized void bind( Service service,
                                   ProtocolProvider protocolProvider ) throws IOException
    {
        ProtocolAcceptor acceptor = findProtocolAcceptor( service.getTransportType() );
        acceptor.bind( service.getAddress(), protocolProvider );
        startThreadPools();
        services.add( service );
    }

    public synchronized void unbind( Service service )
    {
        ProtocolAcceptor acceptor = findProtocolAcceptor( service
                .getTransportType() );
        try
        {
            acceptor.unbind( service.getAddress() );
        }
        catch( Exception e )
        {
            // ignore
        }
        
        try
        {
            acceptor.unbind( service.getAddress() );
        }
        catch( Exception e )
        {
            // ignore
        }

        services.remove( service );
        stopThreadPools();
    }
    
    public synchronized void unbindAll()
    {
        Iterator it = new HashSet( services ).iterator();
        while( it.hasNext() )
        {
            Service s = ( Service ) it.next();
            unbind( s );
        }
    }

    public IoAcceptor getIoAcceptor( TransportType transportType )
    {
        return findIoAcceptor( transportType );
    }

    public ProtocolAcceptor getProtocolAcceptor( TransportType transportType )
    {
        return findProtocolAcceptor( transportType );
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
            SocketAddress addr = s.getAddress();
            int servicePort;
            
            if( addr instanceof InetSocketAddress )
            {
                servicePort = ( ( InetSocketAddress ) addr ).getPort();
            }
            else if( addr instanceof VmPipeAddress )
            {
                servicePort = ( ( VmPipeAddress ) addr ).getPort();
            }
            else
            {
                servicePort = -1; // this cannot happen 
            }
            
            if( servicePort == port )
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
        // Start thread pools only when the first service is bound.
        // If any services are bound, it means that thread pools are started
        // already.
        if( !services.isEmpty() )
            return;

        ioThreadPoolFilter.start();
        protocolThreadPoolFilter.start();
    }

    private void stopThreadPools()
    {
        // Stop thread pools only when all services are unbound.
        if( !services.isEmpty() )
            return;

        ioThreadPoolFilter.stop();
        protocolThreadPoolFilter.stop();
    }
}
