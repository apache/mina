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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.SessionInitializer;
import org.apache.mina.io.IoConnector;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilterChain;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.BaseSessionManager;
import org.apache.mina.util.Queue;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class SocketConnector extends BaseSessionManager implements IoConnector
{
    private static volatile int nextId = 0;

    private final int id = nextId++;

    private final SocketFilterChain filters = new SocketFilterChain();

    private final Selector selector;

    private final Queue connectQueue = new Queue();

    private Worker worker;

    /**
     * Creates a new instance.
     * 
     * @throws IOException
     */
    public SocketConnector() throws IOException
    {
        selector = Selector.open();
    }

    public IoSession connect( SocketAddress address, IoHandler handler ) throws IOException
    {
        return connect( address, null, Integer.MAX_VALUE, handler, null);
    }

    public IoSession connect( SocketAddress address, SocketAddress localAddress, IoHandler handler ) throws IOException
    {
        return connect( address, localAddress, Integer.MAX_VALUE, handler, null);
    }

    public IoSession connect( SocketAddress address, int timeout, IoHandler handler ) throws IOException
    {
        return connect( address, null, timeout, handler, null);
    }

    public IoSession connect( SocketAddress address, SocketAddress localAddress, int timeout, IoHandler handler ) throws IOException
    {
        return connect( address, localAddress, timeout, handler, null);
    }

    public IoSession connect( SocketAddress address, IoHandler handler, SessionInitializer initializer ) throws IOException
    {
        return connect( address, null, Integer.MAX_VALUE, handler, initializer );
    }

    public IoSession connect( SocketAddress address, SocketAddress localAddress, IoHandler handler, SessionInitializer initializer ) throws IOException
    {
        return connect( address, localAddress, Integer.MAX_VALUE, handler, initializer );
    }

    public IoSession connect( SocketAddress address, int timeout, IoHandler handler, SessionInitializer initializer ) throws IOException
    {
        return connect( address, null, timeout, handler, initializer );
    }

    public IoSession connect( SocketAddress address, SocketAddress localAddress,
                              int timeout, IoHandler handler,
                              SessionInitializer initializer ) throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );

        if( timeout <= 0 )
            throw new IllegalArgumentException( "Illegal timeout: " + timeout );

        if( ! ( address instanceof InetSocketAddress ) )
            throw new IllegalArgumentException( "Unexpected address type: "
                                                + address.getClass() );

        if( localAddress != null && !( localAddress instanceof InetSocketAddress ) )
            throw new IllegalArgumentException( "Unexpected local address type: "
                                                + localAddress.getClass() );

        if( initializer == null )
        {
            initializer = defaultInitializer;
        }

        SocketChannel ch = SocketChannel.open();
        boolean success = false;
        try
        {
            if( localAddress != null )
            {
                ch.socket().bind( localAddress );
            }
    
            ch.configureBlocking( false );

            if( ch.connect( address ) )
            {
                SocketSession session = newSession( ch, handler, initializer );
                success = true;
                return session;
            }
            
            success = true;
        }
        finally
        {
            if( !success )
            {
                ch.close();
            }
        }
        
        ConnectionRequest request = new ConnectionRequest( ch, timeout, handler, initializer );
        synchronized( this )
        {
            synchronized( connectQueue )
            {
                connectQueue.push( request );
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

        return request.session;
    }
    
    private synchronized void startupWorker()
    {
        if( worker == null )
        {
            worker = new Worker();
            worker.start();
        }
    }

    private void registerNew()
    {
        if( connectQueue.isEmpty() )
            return;

        for( ;; )
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
                req.exception = e;
                synchronized( req )
                {
                    req.done = true;
                    req.notify();
                }
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

            try
            {
                ch.finishConnect();
                SocketSession session = newSession( ch, entry.handler, entry.initializer );
                entry.session = session;
            }
            catch( IOException e )
            {
                entry.exception = e;
            }
            finally
            {
                key.cancel();
                if( entry.session == null )
                {
                    try
                    {
                        ch.close();
                    }
                    catch( IOException e )
                    {
                        exceptionMonitor.exceptionCaught( this, e );
                    }
                }

                synchronized( entry )
                {
                    entry.done = true;
                    entry.notify();
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
                entry.exception = new ConnectException();
                entry.done = true;

                synchronized( entry )
                {
                    entry.notify();
                }

                key.cancel();
            }
        }
    }

    private SocketSession newSession( SocketChannel ch, IoHandler handler, SessionInitializer initializer ) throws IOException
    {
        SocketSession session = new SocketSession( filters, ch, handler );
        initializer.initializeSession( session );
        SocketIoProcessor.getInstance().addSession( session );
        return session;
    }

    private class Worker extends Thread
    {
        public Worker()
        {
            super( "SocketConnector-" + id );
        }

        public void run()
        {
            for( ;; )
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
                        synchronized( SocketConnector.this )
                        {
                            if( selector.keys().isEmpty() &&
                                connectQueue.isEmpty() )
                            {
                                worker = null;
                                break;
                            }
                        }
                    }
                }
                catch( IOException e )
                {
                    exceptionMonitor.exceptionCaught( SocketConnector.this, e );

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

    private static class ConnectionRequest
    {
        private final SocketChannel channel;
        
        private final long deadline;

        private final IoHandler handler;
        
        private final SessionInitializer initializer;

        private SocketSession session;

        private boolean done;

        private IOException exception;

        private ConnectionRequest( SocketChannel channel, int timeout, IoHandler handler, SessionInitializer initializer )
        {
            this.channel = channel;
            this.deadline = System.currentTimeMillis() + timeout * 1000L;
            this.handler = handler;
            this.initializer = initializer;
        }
    }

    public IoHandlerFilterChain getFilterChain()
    {
        return filters;
    }
}