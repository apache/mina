/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.IoServiceListenerSupport;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;
import org.apache.mina.util.Queue;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class SocketAcceptor extends BaseIoAcceptor
{
    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private IoSessionConfig sessionConfig = new SocketSessionConfigImpl();
    private int backlog = 50;
    private boolean reuseAddress;

    private final Executor executor;
    private final Object lock = new Object();
    private final int id = nextId ++;
    private final String threadName = "SocketAcceptor-" + id;

    private ServerSocketChannel serverSocketChannel;
    private final Queue registerQueue = new Queue();
    private final Queue cancelQueue = new Queue();

    private final SocketIoProcessor[] ioProcessors;
    private final int processorCount;

    /**
     * @noinspection FieldAccessedSynchronizedAndUnsynchronized
     */
    private Selector selector;
    private Worker worker;
    private int processorDistributor = 0;

    /**
     * Create an acceptor with a single processing thread using a NewThreadExecutor
     */
    public SocketAcceptor()
    {
        this( 1, new NewThreadExecutor() );
    }

    /**
     * Create an acceptor with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor Executor to use for launching threads
     */
    public SocketAcceptor( int processorCount, Executor executor )
    {
        if( processorCount < 1 )
        {
            throw new IllegalArgumentException( "Must have at least one processor" );
        }
        
        // Get the default configuration
        ServerSocket s = null;
        try
        {
            s = new ServerSocket();
            reuseAddress = s.getReuseAddress();
        }
        catch( IOException e )
        {
            throw new RuntimeIOException( "Failed to get the default configuration.", e );
        }
        finally
        {
            if( s != null )
            {
                try
                {
                    s.close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
            }
        }
        
        // Set other properties and initialize
        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new SocketIoProcessor[processorCount];

        for( int i = 0; i < processorCount; i++ )
        {
            ioProcessors[i] = new SocketIoProcessor( "SocketAcceptorIoProcessor-" + id + "." + i, executor );
        }
    }

    protected Class getAddressType()
    {
        return InetSocketAddress.class;
    }

    public IoSessionConfig getSessionConfig()
    {
        return sessionConfig;
    }

    /**
     * @see ServerSocket#getReuseAddress()
     */
    public boolean isReuseAddress()
    {
        return reuseAddress;
    }

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress( boolean reuseAddress )
    {
        synchronized( bindLock )
        {
            if( isBound() )
            {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound." );
            }
    
            this.reuseAddress = reuseAddress;
        }
    }

    public int getBacklog()
    {
        return backlog;
    }

    public void setBacklog( int backlog )
    {
        synchronized( bindLock )
        {
            if( isBound() )
            {
                throw new IllegalStateException(
                        "backlog can't be set while the acceptor is bound." );
            }
    
            this.backlog = backlog;
        }
    }
    
    protected void doBind() throws IOException
    {
        RegistrationRequest request = new RegistrationRequest();

        synchronized( registerQueue )
        {
            registerQueue.push( request );
        }

        startupWorker();

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
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
            }
        }

        if( request.exception != null )
        {
            // TODO better exception handling.
            if( request.exception instanceof RuntimeException )
            {
                throw ( RuntimeException ) request.exception;
            }
            else if( request.exception instanceof IOException )
            {
                throw ( IOException ) request.exception;
            }
            else
            {
                throw new RuntimeIOException( request.exception );
            }
        }
        else
        {
            // Update the local address.
            // setLocalAddress() shouldn't be called from the worker thread
            // because of deadlock.
            setLocalAddress(serverSocketChannel.socket().getLocalSocketAddress());
        }
    }


    private synchronized void startupWorker() throws IOException
    {
        synchronized( lock )
        {
            if( worker == null )
            {
                selector = Selector.open();
                worker = new Worker();

                executor.execute( new NamePreservingRunnable( worker ) );
            }
        }
    }

    protected void doUnbind()
    {
        CancellationRequest request = new CancellationRequest();

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
            throw new IllegalArgumentException( "Address not bound: " + getLocalAddress() );
        }

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
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
            }
        }

        if( request.exception != null )
        {
            request.exception.fillInStackTrace();

            throw request.exception;
        }
    }

    private class Worker implements Runnable
    {
        public void run()
        {
            Thread.currentThread().setName(SocketAcceptor.this.threadName );

            for( ; ; )
            {
                try
                {
                    int nKeys = selector.select();

                    registerNew();

                    if( nKeys > 0 )
                    {
                        processSessions( selector.selectedKeys() );
                    }

                    cancelKeys();

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( lock )
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
                        ExceptionMonitor.getInstance().exceptionCaught( e1 );
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
                try
                {
                    SocketSessionImpl session = new SocketSessionImpl(
                            SocketAcceptor.this, nextProcessor(), ch );
                    getFilterChainBuilder().buildFilterChain( session.getFilterChain() );
                    getThreadModel().buildFilterChain( session.getFilterChain() );
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
                        ch.close();
                    }
                }
            }
        }
    }

    private SocketIoProcessor nextProcessor()
    {
        return ioProcessors[processorDistributor++ % processorCount];
    }

    private void registerNew()
    {
        if( registerQueue.isEmpty() )
        {
            return;
        }

        for( ; ; )
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
                ssc.socket().setReuseAddress( isReuseAddress() );
                ssc.socket().setReceiveBufferSize(
                    ( ( SocketSessionConfig ) getSessionConfig() ).getReceiveBufferSize() );

                // and bind.
                ssc.socket().bind( getLocalAddress(), getBacklog() );
                ssc.register( selector, SelectionKey.OP_ACCEPT, req );
                
                serverSocketChannel = ssc;
                
                // and notify.
                getListeners().fireServiceActivated();
            }
            catch( Throwable e )  // TODO better exception handling.
            {
                req.exception = e;
            }
            finally
            {
                synchronized( req )
                {
                    req.done = true;

                    req.notifyAll();
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

        for( ; ; )
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

            // close the channel
            try
            {
                SelectionKey key = serverSocketChannel.keyFor( selector );
                key.cancel();

                selector.wakeup(); // wake up again to trigger thread death

                serverSocketChannel.close();
                serverSocketChannel = null;
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
                    request.notifyAll();
                }

                if( request.exception == null )
                {
                    getListeners().fireServiceDeactivated();
                }
            }
        }
    }

    private static class RegistrationRequest
    {
        private Throwable exception;
        private boolean done;
    }


    private static class CancellationRequest
    {
        private boolean done;
        private RuntimeException exception;
    }

    public IoSession newSession( SocketAddress remoteAddress )
    {
        throw new UnsupportedOperationException();
    }
    
    protected IoServiceListenerSupport getListeners()
    {
        return super.getListeners();
    }
}
