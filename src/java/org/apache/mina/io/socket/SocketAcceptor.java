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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.mina.common.FilterChainType;
import org.apache.mina.io.DefaultExceptionMonitor;
import org.apache.mina.io.ExceptionMonitor;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilterChain;
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

    private final SocketFilterChain filters = new SocketFilterChain( FilterChainType.PREPROCESS );

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

        RegistrationRequest request = new RegistrationRequest( address, backlog, handler );
        synchronized( registerQueue )
        {
            registerQueue.push( request );
        }

        synchronized( this )
        {
            if( worker == null )
            {
                worker = new Worker();
                worker.start();
            }
        }

        selector.wakeup();
        
        synchronized( request )
        {
            while( !request.done )
            {
                try
                {
                    request.wait();
                }
                catch( InterruptedException e )
                {
                }
            }
        }
        
        if( request.exception != null )
        {
            request.exception.fillInStackTrace();
            throw request.exception;
        }
    }

    public void unbind( SocketAddress address )
    {
        if( address == null )
            throw new NullPointerException( "address" );
        
        CancellationRequest request = new CancellationRequest( address );
        synchronized( cancelQueue )
        {
            cancelQueue.push( request );
        }

        selector.wakeup();

        synchronized( request )
        {
            while( !request.done )
            {
                try
                {
                    request.wait();
                }
                catch( InterruptedException e )
                {
                }
            }
        }
        
        if( request.exception != null )
        {
            request.exception.fillInStackTrace();
            throw request.exception;
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
                                filters, ch, ( IoHandler ) key
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

    private void registerNew()
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

            ServerSocketChannel ssc = null;
            try
            {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking( false );
                ssc.socket().bind( req.address, req.backlog );
                ssc.register( selector, SelectionKey.OP_ACCEPT,
                              req.handler );
                channels.put( req.address, ssc );
            }
            catch( IOException e )
            {
                req.exception = e;
            }
            finally
            {
                synchronized( req )
                {
                    req.done = true;
                    req.notify();
                }

                if( ssc != null && req.exception != null )
                {
                    try
                    {
                        ssc.close();
                    }
                    catch( IOException e )
                    {
                        exceptionMonitor.exceptionCaught( this, e );
                    }
                }
            }
        }
    }

    private void cancelKeys()
    {
        if( cancelQueue.isEmpty() )
            return;

        for( ;; )
        {
            CancellationRequest request;
            synchronized( cancelQueue )
            {
                request = ( CancellationRequest ) cancelQueue.pop();
            }
            
            if( request == null )
            {
                break;
            }

            ServerSocketChannel ssc = ( ServerSocketChannel ) channels.get( request.address );
            if( ssc == null )
            {
                synchronized( request )
                {
                    request.done = true;
                    request.exception = new IllegalArgumentException(
                            "Address not bound: " + request.address );
                }
                continue;
            }
            
            SelectionKey key = ssc.keyFor( selector );
            key.cancel();
            selector.wakeup(); // wake up again to trigger thread death
            
            // close the channel
            try
            {
                ssc.close();
            }
            catch( IOException e )
            {
                exceptionMonitor.exceptionCaught( this, e );
            }
            finally
            {
                synchronized( request )
                {
                    request.done = true;
                    request.notify();
                }
            }
        }
    }
    
    public IoHandlerFilterChain newFilterChain( FilterChainType type )
    {
        return new SocketFilterChain( type );
    }
    
    public IoHandlerFilterChain getFilterChain()
    {
        return filters;
    }
    
    private static class RegistrationRequest
    {
        private final SocketAddress address;
        
        private final int backlog;

        private final IoHandler handler;
        
        private IOException exception; 
        
        private boolean done;
        
        private RegistrationRequest( SocketAddress address, int backlog,
                                     IoHandler handler )
        {
            this.address = address;
            this.backlog = backlog;
            this.handler = handler;
        }
    }

    private static class CancellationRequest
    {
        private final SocketAddress address;
        private boolean done;
        private RuntimeException exception;
        
        private CancellationRequest( SocketAddress address )
        {
            this.address = address;
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
