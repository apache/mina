/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.io.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.DefaultExceptionMonitor;
import org.apache.mina.io.ExceptionMonitor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.util.IoHandlerFilterManager;
import org.apache.mina.util.Queue;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class SocketAcceptor implements IoAcceptor
{
    private static volatile int nextId = 0;

    private final IoHandlerFilterManager filterManager = new IoHandlerFilterManager();

    private final int id = nextId ++ ;

    private final Selector selector;

    private final Map channels = new HashMap();

    private final Queue registerQueue = new Queue();

    private final Queue cancelQueue = new Queue();

    private ExceptionMonitor exceptionMonitor = new DefaultExceptionMonitor();

    private Worker worker;

    /**
     * Creates a new instance.
     * 
     * @throws IOException
     */
    public SocketAcceptor() throws IOException
    {
        selector = Selector.open();
    }

    public void bind( SocketAddress address, IoHandler handler )
            throws IOException
    {
        this.bind( address, 50, handler );
    }

    /**
     * Binds to the specified <code>address</code> and handles incoming
     * connections with the specified <code>handler</code>.
     *
     * @param backlog the listen backlog length 
     * @throws IOException if failed to bind
     */
    public void bind( SocketAddress address, int backlog, IoHandler handler )
            throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );

        if( !( address instanceof InetSocketAddress ) )
            throw new IllegalArgumentException( "Unexpected address type: "
                                                + address.getClass() );
        if( ( ( InetSocketAddress ) address ).getPort() == 0 )
            throw new IllegalArgumentException( "Unsupported port number: 0" );

        ServerSocketChannel ssc = ServerSocketChannel.open();
        boolean bound = false;
        try
        {
            ssc.configureBlocking( false );
            ssc.socket().bind( address, backlog );
            bound = true;
        }
        finally
        {
            if( !bound )
            {
                ssc.close();
            }
        }

        synchronized( this )
        {
            synchronized( registerQueue )
            {
                registerQueue.push( new RegistrationRequest( ssc, handler ) );
            }
            channels.put( address, ssc );

            if( worker == null )
            {
                worker = new Worker();
                worker.start();
            }
        }

        selector.wakeup();
    }

    public void unbind( SocketAddress address )
    {
        if( address == null )
            throw new NullPointerException( "address" );

        ServerSocketChannel ssc;

        synchronized( this )
        {
            ssc = ( ServerSocketChannel ) channels.get( address );

            if( ssc == null )
                return;

            SelectionKey key = ssc.keyFor( selector );
            channels.remove( address );
            synchronized( cancelQueue )
            {
                cancelQueue.push( key );
            }
        }

        selector.wakeup();

        try
        {
            ssc.close();
        }
        catch( IOException e )
        {
            exceptionMonitor.exceptionCaught( this, e );
        }
    }

    private class Worker extends Thread
    {
        public Worker()
        {
            super( "SocketAcceptor-" + id );
        }

        public void run()
        {
            for( ;; )
            {
                try
                {
                    int nKeys = selector.select();

                    registerNew();

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( SocketAcceptor.this )
                        {
                            if( selector.keys().isEmpty() )
                            {
                                worker = null;
                                break;
                            }
                        }
                    }

                    cancelKeys();

                    if( nKeys == 0 )
                        continue;

                    Iterator it = selector.selectedKeys().iterator();

                    while( it.hasNext() )
                    {
                        SelectionKey key = ( SelectionKey ) it.next();
                        it.remove();

                        if( !key.isAcceptable() )
                            continue;

                        ServerSocketChannel ssc = ( ServerSocketChannel ) key
                                .channel();
                        SocketChannel ch = ssc.accept();

                        if( ch == null )
                            continue;

                        SocketSession session = new SocketSession(
                                filterManager, ch, ( IoHandler ) key
                                        .attachment() );
                        SocketIoProcessor.getInstance().addSession( session );
                    }
                }
                catch( IOException e )
                {
                    exceptionMonitor.exceptionCaught( SocketAcceptor.this, e );

                    try
                    {
                        Thread.sleep( 1000 );
                    }
                    catch( InterruptedException e1 )
                    {
                    }
                }
            }
        }
    }

    private void registerNew() throws ClosedChannelException
    {
        if( registerQueue.isEmpty() )
            return;

        for( ;; )
        {
            RegistrationRequest req;
            synchronized( registerQueue )
            {
                req = ( RegistrationRequest ) registerQueue.pop();
            }

            if( req == null )
                break;

            req.channel.register( selector, SelectionKey.OP_ACCEPT,
                    req.handler );
        }
    }

    private void cancelKeys()
    {
        if( cancelQueue.isEmpty() )
            return;

        for( ;; )
        {
            SelectionKey key;
            synchronized( cancelQueue )
            {
                key = ( SelectionKey ) cancelQueue.pop();
            }

            if( key == null )
                break;
            else
            {
                key.cancel();
                selector.wakeup(); // wake up again to trigger thread death
            }
        }
    }

    public void addFilter( int priority, IoHandlerFilter filter )
    {
        filterManager.addFilter( priority, filter );
    }

    public void removeFilter( IoHandlerFilter filter )
    {
        filterManager.removeFilter( filter );
    }

    public void removeAllFilters()
    {
        filterManager.removeAllFilters();
    }

    public List getAllFilters()
    {
        return filterManager.getAllFilters();
    }

    private static class RegistrationRequest
    {
        private final ServerSocketChannel channel;

        private final IoHandler handler;

        private RegistrationRequest( ServerSocketChannel channel,
                                    IoHandler handler )
        {
            this.channel = channel;
            this.handler = handler;
        }
    }

    public ExceptionMonitor getExceptionMonitor()
    {
        return exceptionMonitor;
    }

    public void setExceptionMonitor( ExceptionMonitor monitor )
    {
        if( monitor == null )
        {
            monitor = new DefaultExceptionMonitor();
        }

        this.exceptionMonitor = monitor;
    }
}
