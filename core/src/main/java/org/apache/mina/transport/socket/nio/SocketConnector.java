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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.NewThreadExecutor;
import org.apache.mina.util.Queue;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 389042 $, $Date: 2006-03-27 07:49:41Z $
 */
public class SocketConnector extends BaseIoConnector
{
    /**
     * @noinspection StaticNonFinalField
     */
    private static volatile int nextId = 0;

    private IoSessionConfig sessionConfig = new SocketSessionConfigImpl();

    private final Object lock = new Object();
    private final int id = nextId++;
    private final String threadName = "SocketConnector-" + id;
    private final Queue connectQueue = new Queue();
    private final SocketIoProcessor[] ioProcessors;
    private final int processorCount;
    private final Executor executor;

    /**
     * @noinspection FieldAccessedSynchronizedAndUnsynchronized
     */
    private Selector selector;
    private Worker worker;
    private int processorDistributor = 0;
    private int workerTimeout = 60;  // 1 min.

    /**
     * Create a connector with a single processing thread using a NewThreadExecutor 
     */
    public SocketConnector()
    {
        this( 1, new NewThreadExecutor() );
    }

    /**
     * Create a connector with the desired number of processing threads
     *
     * @param processorCount Number of processing threads
     * @param executor Executor to use for launching threads
     */
    public SocketConnector( int processorCount, Executor executor )
    {
        if( processorCount < 1 )
        {
            throw new IllegalArgumentException( "Must have at least one processor" );
        }

        this.executor = executor;
        this.processorCount = processorCount;
        ioProcessors = new SocketIoProcessor[processorCount];

        for( int i = 0; i < processorCount; i++ )
        {
            ioProcessors[i] = new SocketIoProcessor( "SocketConnectorIoProcessor-" + id + "." + i, executor );
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
     * How many seconds to keep the connection thread alive between connection requests
     *
     * @return Number of seconds to keep connection thread alive
     */
    public int getWorkerTimeout()
    {
        return workerTimeout;
    }

    /**
     * Set how many seconds the connection worker thread should remain alive once idle before terminating itself.
     *
     * @param workerTimeout Number of seconds to keep thread alive. Must be >=0
     */
    public void setWorkerTimeout( int workerTimeout )
    {
        if( workerTimeout < 0 )
        {
            throw new IllegalArgumentException( "Must be >= 0" );
        }
        this.workerTimeout = workerTimeout;
    }

    protected ConnectFuture doConnect()
    {
        SocketChannel ch = null;
        boolean success = false;
        try
        {
            ch = SocketChannel.open();
            ch.socket().setReuseAddress( true );
            if( getLocalAddress() != null )
            {
                ch.socket().bind( getLocalAddress() );
            }

            ch.configureBlocking( false );

            if( ch.connect( getRemoteAddress() ) )
            {
                DefaultConnectFuture future = new DefaultConnectFuture();
                newSession( ch, future );
                success = true;
                return future;
            }

            success = true;
        }
        catch( IOException e )
        {
            return DefaultConnectFuture.newFailedFuture( e );
        }
        finally
        {
            if( !success && ch != null )
            {
                try
                {
                    ch.close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
            }
        }

        ConnectionRequest request = new ConnectionRequest( ch );
        synchronized( lock )
        {
            try
            {
                startupWorker();
            }
            catch( IOException e )
            {
                try
                {
                    ch.close();
                }
                catch( IOException e2 )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e2 );
                }

                return DefaultConnectFuture.newFailedFuture( e );
            }
        }

        synchronized( connectQueue )
        {
            connectQueue.push( request );
        }
        selector.wakeup();

        return request;
    }
    
    private synchronized void startupWorker() throws IOException
    {
        if( worker == null )
        {
            selector = Selector.open();
            worker = new Worker();
            executor.execute( new NamePreservingRunnable( worker ) );
        }
    }

    private void registerNew()
    {
        if( connectQueue.isEmpty() )
            return;

        for( ; ; )
        {
            ConnectionRequest req;
            synchronized( connectQueue )
            {
                req = ( ConnectionRequest ) connectQueue.pop();
            }

            if( req == null )
                break;

            SocketChannel ch = req.channel;
            try
            {
                ch.register( selector, SelectionKey.OP_CONNECT, req );
            }
            catch( IOException e )
            {
                req.setException( e );
            }
        }
    }

    private void processSessions( Set keys )
    {
        Iterator it = keys.iterator();

        while( it.hasNext() )
        {
            SelectionKey key = ( SelectionKey ) it.next();

            if( !key.isConnectable() )
                continue;

            SocketChannel ch = ( SocketChannel ) key.channel();
            ConnectionRequest entry = ( ConnectionRequest ) key.attachment();

            boolean success = false;
            try
            {
                ch.finishConnect();
                newSession( ch, entry );
                success = true;
            }
            catch( Throwable e )
            {
                entry.setException( e );
            }
            finally
            {
                key.cancel();
                if( !success )
                {
                    try
                    {
                        ch.close();
                    }
                    catch( IOException e )
                    {
                        ExceptionMonitor.getInstance().exceptionCaught( e );
                    }
                }
            }
        }

        keys.clear();
    }

    private void processTimedOutSessions( Set keys )
    {
        long currentTime = System.currentTimeMillis();
        Iterator it = keys.iterator();

        while( it.hasNext() )
        {
            SelectionKey key = ( SelectionKey ) it.next();

            if( !key.isValid() )
                continue;

            ConnectionRequest entry = ( ConnectionRequest ) key.attachment();

            if( currentTime >= entry.deadline )
            {
                entry.setException( new ConnectException() );
                try
                {
                    key.channel().close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
                finally
                {
                    key.cancel();
                }
            }
        }
    }

    private void newSession( SocketChannel ch, ConnectFuture connectFuture )
        throws IOException
    {
        SocketSessionImpl session =
            new SocketSessionImpl( this, getListeners(), nextProcessor(), ch );

        try
        {
            getFilterChainBuilder().buildFilterChain( session.getFilterChain() );
            getThreadModel().buildFilterChain( session.getFilterChain() );
        }
        catch( Throwable e )
        {
            throw ( IOException ) new IOException( "Failed to create a session." ).initCause( e );
        }
        
        // Set the ConnectFuture of the specified session, which will be
        // removed and notified by AbstractIoFilterChain eventually.
        session.setAttribute( AbstractIoFilterChain.CONNECT_FUTURE, connectFuture );

        // Forward the remaining process to the SocketIoProcessor.
        session.getIoProcessor().addNew( session );
    }

    private SocketIoProcessor nextProcessor()
    {
        return ioProcessors[processorDistributor++ % processorCount];
    }

    private class Worker implements Runnable
    {
        private long lastActive = System.currentTimeMillis();

        public void run()
        {
            Thread.currentThread().setName( SocketConnector.this.threadName );

            for( ; ; )
            {
                try
                {
                    int nKeys = selector.select( 1000 );

                    registerNew();

                    if( nKeys > 0 )
                    {
                        processSessions( selector.selectedKeys() );
                    }

                    processTimedOutSessions( selector.keys() );

                    if( selector.keys().isEmpty() )
                    {
                        if( System.currentTimeMillis() - lastActive > workerTimeout * 1000L )
                        {
                            synchronized( lock )
                            {
                                if( selector.keys().isEmpty() &&
                                    connectQueue.isEmpty() )
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
                    else
                    {
                        lastActive = System.currentTimeMillis();
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
    }

    private class ConnectionRequest extends DefaultConnectFuture
    {
        private final SocketChannel channel;
        private final long deadline;

        private ConnectionRequest( SocketChannel channel )
        {
            this.channel = channel;
            this.deadline = System.currentTimeMillis() + getConnectTimeoutMillis();
        }
    }
}