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

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.TransportType;
import org.apache.mina.filter.ThreadPoolFilter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;

/**
 * A simple implementation of {@link ServiceRegistry}.
 * 
 * This service registry supports socket, datagram, VM-pipe transport types,
 * and thread pools were added by default. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$, 
 */
public class SimpleServiceRegistry implements ServiceRegistry
{
    protected final IoAcceptor socketIoAcceptor = new SocketAcceptor();
    protected final IoAcceptor datagramIoAcceptor = new DatagramAcceptor();
    protected final IoAcceptor vmPipeAcceptor = new VmPipeAcceptor();
    protected final ThreadPoolFilter threadPoolFilter = new ThreadPoolFilter();
    private final Set services = new HashSet();

    public SimpleServiceRegistry()
    {
    }

    public void bind( Service service, IoHandler handler ) throws IOException
    {
        bind( service, handler, null );
    }
    
    public void bind( Service service, IoHandler handler, IoFilterChainBuilder filterChainBuilder ) throws IOException
    {
        IoAcceptor acceptor = findAcceptor( service.getTransportType() );
        if( filterChainBuilder == null )
        {
            filterChainBuilder = IoFilterChainBuilder.NOOP;
        }
        acceptor.bind(
                service.getAddress(), handler,
                new IoFilterChainBuilderWrapper( service, filterChainBuilder ) );
        services.add( service );
    }

    public synchronized void unbind( Service service )
    {
        IoAcceptor acceptor = findAcceptor( service
                .getTransportType() );
        try
        {
            acceptor.unbind( service.getAddress() );
        }
        catch( Exception e )
        {
            // ignore
        }
        
        services.remove( service );
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

    public IoAcceptor getAcceptor( TransportType transportType )
    {
        return findAcceptor( transportType );
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

    protected IoAcceptor findAcceptor( TransportType transportType )
    {
        if( transportType == TransportType.SOCKET )
            return socketIoAcceptor;
        else if( transportType == TransportType.DATAGRAM )
            return datagramIoAcceptor;
        else
            return vmPipeAcceptor;

    }
    
    private class IoFilterChainBuilderWrapper implements IoFilterChainBuilder
    {
        private final Service service;
        private final IoFilterChainBuilder originalBuilder;
        
        private IoFilterChainBuilderWrapper( Service service, IoFilterChainBuilder originalBuilder )
        {
            this.service = service;
            this.originalBuilder = originalBuilder;
        }
        
        public void buildFilterChain( IoFilterChain chain ) throws Exception
        {
            chain.getSession().setAttribute( SERVICE, service );

            try
            {
                originalBuilder.buildFilterChain( chain );
            }
            finally
            {
                chain.addFirst( "threadPool", threadPoolFilter );
            }
        }
    }
}
