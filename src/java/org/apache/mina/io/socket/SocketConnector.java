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
import java.util.List;
import java.util.Set;

import org.apache.mina.io.DefaultExceptionMonitor;
import org.apache.mina.io.ExceptionMonitor;
import org.apache.mina.io.IoConnector;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.io.IoSession;
import org.apache.mina.util.IoHandlerFilterManager;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class SocketConnector implements IoConnector
{
    private static volatile int nextId = 0;

    private final int id = nextId++;

    private final IoHandlerFilterManager filterManager = new IoHandlerFilterManager();

    private final Selector selector;

    private ExceptionMonitor exceptionMonitor = new DefaultExceptionMonitor();

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

    public IoSession connect( SocketAddress address, IoHandler handler )
            throws IOException
    {
        return connect( address, Integer.MAX_VALUE, handler );
    }

    public IoSession connect( SocketAddress address, int timeout,
                             IoHandler handler ) throws IOException
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

        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking( false );

        IoSession session;
        if( ch.connect( address ) )
        {
            session = newSession( ch, handler );
        }
        else
        {
            ConnectEntry entry = new ConnectEntry( timeout, handler );

            synchronized( this )
            {
                ch.register( selector, SelectionKey.OP_CONNECT, entry );

                if( worker == null )
                {
                    worker = new Worker();
                    worker.start();
                }
            }

            synchronized( entry )
            {
                while( !entry.done )
                {
                    try
                    {
                        entry.wait();
                    }
                    catch( InterruptedException e )
                    {
                    }
                }
            }

            if( entry.exception != null )
                throw entry.exception;

            session = entry.session;
        }

        return session;
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
            ConnectEntry entry = ( ConnectEntry ) key.attachment();

            try
            {
                ch.finishConnect();
                SocketSession session = newSession( ch, entry.handler );
                entry.session = session;
                entry.done = true;

                synchronized( entry )
                {
                    entry.notify();
                }
            }
            catch( IOException e )
            {
                entry.exception = e;
                entry.done = true;

                synchronized( entry )
                {
                    entry.notify();
                }
            }
            finally
            {
                key.cancel();
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

            ConnectEntry entry = ( ConnectEntry ) key.attachment();

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

    private SocketSession newSession( SocketChannel ch, IoHandler handler )
    {
        SocketSession session = new SocketSession( filterManager, ch, handler );
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

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( SocketConnector.this )
                        {
                            if( selector.keys().isEmpty() )
                            {
                                worker = null;
                                break;
                            }
                        }
                    }

                    if( nKeys > 0 )
                        processSessions( selector.selectedKeys() );

                    processTimedOutSessions( selector.keys() );
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

    private static class ConnectEntry
    {
        private final long deadline;

        private final IoHandler handler;

        private SocketSession session;

        private boolean done;

        private IOException exception;

        private ConnectEntry( int timeout, IoHandler handler )
        {
            this.deadline = System.currentTimeMillis() + timeout * 1000L;
            this.handler = handler;
        }
    }

    public void addFilter( int priority, IoHandlerFilter filter )
    {
        filterManager.addFilter( priority, false, filter );

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