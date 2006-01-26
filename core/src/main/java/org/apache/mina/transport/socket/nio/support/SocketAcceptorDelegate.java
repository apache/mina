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
package org.apache.mina.transport.socket.nio.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.apache.mina.util.IdentityHashSet;
import org.apache.mina.util.Queue;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketAcceptorDelegate extends BaseIoAcceptor
{
    private static volatile int nextId = 0;

    private final IoAcceptor wrapper;
    private final int id = nextId ++ ;
    private final String threadName = "SocketAcceptor-" + id;
    private final IoServiceConfig defaultConfig = new SocketAcceptorConfig();
    private Selector selector;
    private final Map channels = new HashMap();
    private final Hashtable sessions = new Hashtable();

    private final Queue registerQueue = new Queue();
    private final Queue cancelQueue = new Queue();
    
    private Worker worker;

    /**
     * Creates a new instance.
     */
    public SocketAcceptorDelegate( IoAcceptor wrapper )
    {
        this.wrapper = wrapper;
    }

    /**
     * Binds to the specified <code>address</code> and handles incoming
     * connections with the specified <code>handler</code>.  Backlog value
     * is configured to the value of <code>backlog</code> property.
     *
     * @throws IOException if failed to bind
     */
    public void bind( SocketAddress address, IoHandler handler, IoServiceConfig config ) throws IOException
    {
        if( address == null )
        {
            throw new NullPointerException( "address" );
        }

        if( handler == null )
        {
            throw new NullPointerException( "handler" );
        }

        if( !( address instanceof InetSocketAddress ) )
        {
            throw new IllegalArgumentException( "Unexpected address type: " + address.getClass() );
        }

        if( ( ( InetSocketAddress ) address ).getPort() == 0 )
        {
            throw new IllegalArgumentException( "Unsupported port number: 0" );
        }
        
        if( config == null )
        {
            config = getDefaultConfig();
        }
        
        RegistrationRequest request = new RegistrationRequest( address, handler, config );

        synchronized( this )
        {
            synchronized( registerQueue )
            {
                registerQueue.push( request );
            }
            startupWorker();
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
            throw request.exception;
        }
    }


    private synchronized void startupWorker() throws IOException
    {
        if( worker == null )
        {
            selector = Selector.open();
            worker = new Worker();

            worker.start();
        }
    }

    public Set getManagedSessions( SocketAddress address )
    {
        if( address == null )
        {
            throw new NullPointerException( "address" );
        }
        
        Set managedSessions = ( Set ) sessions.get( address );
        
        if( managedSessions == null )
        {
            throw new IllegalArgumentException( "Address not bound: " + address );
        }
        
        return Collections.unmodifiableSet(
                new IdentityHashSet( Arrays.asList( managedSessions.toArray() ) ) );
    }
    
    public void unbind( SocketAddress address )
    {
        if( address == null )
        {
            throw new NullPointerException( "address" );
        }

        final Set managedSessions = ( Set ) sessions.get( address );
        CancellationRequest request = new CancellationRequest( address );
        synchronized( this )
        {
            try
            {
                startupWorker();
            }
            catch( IOException e )
            {
                // IOException is thrown only when Worker thread is not
                // running and failed to open a selector.  We simply throw
                // IllegalArgumentException here because we can simply
                // conclude that nothing is bound to the selector.
                throw new IllegalArgumentException( "Address not bound: " + address );
            }

            synchronized( cancelQueue )
            {
                cancelQueue.push( request );
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
        
        
        // Disconnect all clients
        IoServiceConfig cfg = request.registrationRequest.config;
        boolean disconnectOnUnbind;
        if( cfg instanceof IoAcceptorConfig )
        {
            disconnectOnUnbind = ( ( IoAcceptorConfig ) cfg ).isDisconnectOnUnbind();
        }
        else
        {
            disconnectOnUnbind = ( ( IoAcceptorConfig ) getDefaultConfig() ).isDisconnectOnUnbind();
        }

        if( disconnectOnUnbind && managedSessions != null )
        {
            IoSession[] tempSessions = ( IoSession[] ) 
                                  managedSessions.toArray( new IoSession[ 0 ] );
            
            final Object lock = new Object();
            
            for( int i = 0; i < tempSessions.length; i++ )
            {
                if( !managedSessions.contains( tempSessions[ i ] ) )
                {
                    // The session has already been closed and have been 
                    // removed from managedSessions by the SocketIoProcessor.
                    continue;
                }
                tempSessions[ i ].close().setCallback( new IoFuture.Callback()
                {
                    public void operationComplete( IoFuture future )
                    {
                        synchronized( lock )
                        {
                            lock.notify();
                        }
                    }
                } );
            }

            try
            {
                synchronized( lock )
                {
                    while( !managedSessions.isEmpty() )
                    {
                        lock.wait( 1000 );
                    }
                }
            }
            catch( InterruptedException ie )
            {
                // Ignored
            }
            
        }        
    }
    
    private class Worker extends Thread
    {
        public Worker()
        {
            super( SocketAcceptorDelegate.this.threadName );
        }

        public void run()
        {
            for( ;; )
            {
                try
                {
                    int nKeys = selector.select();

                    registerNew();
                    cancelKeys();

                    if( nKeys > 0 )
                    {
                        processSessions( selector.selectedKeys() );
                    }

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( SocketAcceptorDelegate.this )
                        {
                            if( selector.keys().isEmpty() &&
                                registerQueue.isEmpty() &&
                                cancelQueue.isEmpty() )
                            {
                                worker = null;
                                try
                                {
                                    selector.close();
                                }
                                catch( IOException e )
                                {
                                    ExceptionMonitor.getInstance().exceptionCaught( e );
                                }
                                finally
                                {
                                    selector = null;
                                }
                                break;
                            }
                        }
                    }
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );

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

        private void processSessions( Set keys ) throws IOException
        {
            Iterator it = keys.iterator();
            while( it.hasNext() )
            {
                SelectionKey key = ( SelectionKey ) it.next();
   
                it.remove();
   
                if( !key.isAcceptable() )
                {
                    continue;
                }
   
                ServerSocketChannel ssc = ( ServerSocketChannel ) key.channel();
   
                SocketChannel ch = ssc.accept();
   
                if( ch == null )
                {
                    continue;
                }
   
                boolean success = false;
                SocketSessionImpl session = null;
                try
                {
                    RegistrationRequest req = ( RegistrationRequest ) key.attachment();
                    session = new SocketSessionImpl(
                            SocketAcceptorDelegate.this.wrapper,
                            ( Set ) sessions.get( req.address ),
                            ( SocketSessionConfig ) req.config.getSessionConfig(),
                            ch, req.handler );
                    req.config.getFilterChainBuilder().buildFilterChain( session.getFilterChain() );
                    ( ( SocketFilterChain ) session.getFilterChain() ).sessionCreated( session );
                    session.getManagedSessions().add( session );
                    session.getIoProcessor().addNew( session );
                    success = true;
                }
                catch( Throwable t )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( t );
                }
                finally
                {
                    if( !success )
                    {
                        if( session != null )
                        {
                            session.getManagedSessions().remove( session );
                        }
                        ch.close();
                    }
                }
            }
        }
    }

    public IoServiceConfig getDefaultConfig()
    {
        return defaultConfig;
    }

    private void registerNew()
    {
        if( registerQueue.isEmpty() )
        {
            return;
        }

        for( ;; )
        {
            RegistrationRequest req;

            synchronized( registerQueue )
            {
                req = ( RegistrationRequest ) registerQueue.pop();
            }

            if( req == null )
            {
                break;
            }

            ServerSocketChannel ssc = null;

            try
            {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking( false );
                
                // Configure the server socket,
                SocketAcceptorConfig cfg;
                if( req.config instanceof SocketAcceptorConfig )
                {
                    cfg = ( SocketAcceptorConfig ) req.config;
                }
                else
                {
                    cfg = ( SocketAcceptorConfig ) getDefaultConfig();
                }
                
                ssc.socket().setReuseAddress( cfg.isReuseAddress() );
                ssc.socket().setReceiveBufferSize(
                        ( ( SocketSessionConfig ) cfg.getSessionConfig() ).getReceiveBufferSize() );
                
                // and bind.
                ssc.socket().bind( req.address, cfg.getBacklog() );
                ssc.register( selector, SelectionKey.OP_ACCEPT, req );

                channels.put( req.address, ssc );
                sessions.put( req.address, Collections.synchronizedSet( new HashSet() ) );
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
                        ExceptionMonitor.getInstance().exceptionCaught( e );
                    }
                }
            }
        }
    }


    private void cancelKeys()
    {
        if( cancelQueue.isEmpty() )
        {
            return;
        }

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

            sessions.remove( request.address );
            ServerSocketChannel ssc = ( ServerSocketChannel ) channels.remove( request.address );
            
            // close the channel
            try
            {
                if( ssc == null )
                {
                    request.exception = new IllegalArgumentException( "Address not bound: " + request.address );
                }
                else
                {
                    SelectionKey key = ssc.keyFor( selector );
                    request.registrationRequest = ( RegistrationRequest ) key.attachment();
                    key.cancel();

                    selector.wakeup(); // wake up again to trigger thread death

                    ssc.close();
                }
            }
            catch( IOException e )
            {
                ExceptionMonitor.getInstance().exceptionCaught( e );
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

    private static class RegistrationRequest
    {
        private final SocketAddress address;
        private final IoHandler handler;
        private final IoServiceConfig config;
        private IOException exception;
        private boolean done;
        
        private RegistrationRequest( SocketAddress address, IoHandler handler, IoServiceConfig config )
        {
            this.address = address;
            this.handler = handler;
            this.config = config;
        }
    }


    private static class CancellationRequest
    {
        private final SocketAddress address;
        private boolean done;
        private RegistrationRequest registrationRequest;
        private RuntimeException exception;
        
        private CancellationRequest( SocketAddress address )
        {
            this.address = address;
        }
    }
}
