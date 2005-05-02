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
package org.apache.mina.io.datagram;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoConnector;
import org.apache.mina.io.IoFilterChain;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.IoSessionManagerFilterChain;
import org.apache.mina.util.ExceptionUtil;
import org.apache.mina.util.Queue;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramConnector extends DatagramSessionManager implements IoConnector
{
    private static volatile int nextId = 0;

    private final IoSessionManagerFilterChain filters =
        new DatagramSessionManagerFilterChain( this );

    private final int id = nextId ++ ;

    private final Selector selector;

    private final Queue registerQueue = new Queue();

    private final Queue cancelQueue = new Queue();

    private final Queue flushingSessions = new Queue();

    private Worker worker;

    /**
     * Creates a new instance.
     * 
     * @throws IOException if failed to open a selector
     */
    public DatagramConnector() throws IOException
    {
        selector = Selector.open();
    }

    public IoSession connect( SocketAddress address, IoHandler handler ) throws IOException
    {
        return connect( address, null, handler);
    }

    public IoSession connect( SocketAddress address, int timeout, IoHandler handler ) throws IOException
    {
        return connect( address, null, handler );
    }

    public IoSession connect( SocketAddress address, SocketAddress localAddress, int timeout, IoHandler handler ) throws IOException
    {
        return connect( address, localAddress, handler );
    }

    public IoSession connect( SocketAddress address, SocketAddress localAddress,
                              IoHandler handler ) throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );

        if( !( address instanceof InetSocketAddress ) )
            throw new IllegalArgumentException( "Unexpected address type: "
                                                + address.getClass() );
        
        if( localAddress != null && !( localAddress instanceof InetSocketAddress ) )
        {
            throw new IllegalArgumentException( "Unexpected local address type: "
                                                + localAddress.getClass() );
        }
        
        DatagramChannel ch = DatagramChannel.open();
        boolean initialized = false;
        try
        {
            ch.socket().setReuseAddress( true );
            if( localAddress != null )
            {
                ch.socket().bind( localAddress );
            }
            ch.connect( address );
            ch.configureBlocking( false );
            initialized = true;
        }
        finally
        {
            if( !initialized )
            {
                ch.close();
            }
        }

        RegistrationRequest request = new RegistrationRequest( ch, handler );
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
            ExceptionUtil.throwException( request.exception );
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

    void closeSession( DatagramSession session )
    {
        synchronized( this )
        {
            SelectionKey key = session.getSelectionKey();
            synchronized( cancelQueue )
            {
                cancelQueue.push( key );
            }
            startupWorker();
        }

        selector.wakeup();
    }

    void flushSession( DatagramSession session )
    {
        scheduleFlush( session );
        selector.wakeup();
    }

    private void scheduleFlush( DatagramSession session )
    {
        synchronized( flushingSessions )
        {
            flushingSessions.push( session );
        }
    }

    private class Worker extends Thread
    {
        public Worker()
        {
            super( "DatagramAcceptor-" + id );
        }

        public void run()
        {
            for( ;; )
            {
                try
                {
                    int nKeys = selector.select();

                    registerNew();

                    if( nKeys > 0 )
                    {
                        processReadySessions( selector.selectedKeys() );
                    }

                    flushSessions();
                    cancelKeys();

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( DatagramConnector.this )
                        {
                            if( selector.keys().isEmpty() &&
                                registerQueue.isEmpty() &&
                                cancelQueue.isEmpty() )
                            {
                                worker = null;
                                break;
                            }
                        }
                    }
                }
                catch( IOException e )
                {
                    exceptionMonitor.exceptionCaught( DatagramConnector.this,
                            e );

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

    private void processReadySessions( Set keys )
    {
        Iterator it = keys.iterator();
        while( it.hasNext() )
        {
            SelectionKey key = ( SelectionKey ) it.next();
            it.remove();

            DatagramSession session = ( DatagramSession ) key.attachment();

            if( key.isReadable() )
            {
                readSession( session );
            }

            if( key.isWritable() )
            {
                scheduleFlush( session );
            }
        }
    }

    private void readSession( DatagramSession session )
    {

        ByteBuffer readBuf = ByteBuffer.allocate( 2048 );
        try
        {
            int readBytes = session.getChannel().read( readBuf.buf() );
            if( readBytes > 0 )
            {
                readBuf.flip();
                ByteBuffer newBuf = ByteBuffer.allocate( readBuf.limit() );
                newBuf.put( readBuf );
                newBuf.flip();

                session.increaseReadBytes( readBytes );
                filters.dataRead( session, newBuf );
            }
        }
        catch( IOException e )
        {
            filters.exceptionCaught( session, e );
        }
        finally
        {
            readBuf.release();
        }
    }

    private void flushSessions()
    {
        if( flushingSessions.size() == 0 )
            return;

        for( ;; )
        {
            DatagramSession session;

            synchronized( flushingSessions )
            {
                session = ( DatagramSession ) flushingSessions.pop();
            }

            if( session == null )
                break;

            try
            {
                flush( session );
            }
            catch( IOException e )
            {
                session.getManagerFilterChain().exceptionCaught( session, e );
            }
        }
    }

    private void flush( DatagramSession session ) throws IOException
    {
        DatagramChannel ch = session.getChannel();

        Queue writeBufferQueue = session.getWriteBufferQueue();
        Queue writeMarkerQueue = session.getWriteMarkerQueue();

        ByteBuffer buf;
        Object marker;
        for( ;; )
        {
            synchronized( writeBufferQueue )
            {
                buf = ( ByteBuffer ) writeBufferQueue.first();
                marker = writeMarkerQueue.first();
            }

            if( buf == null )
                break;

            if( buf.remaining() == 0 )
            {
                // pop and fire event
                synchronized( writeBufferQueue )
                {
                    writeBufferQueue.pop();
                    writeMarkerQueue.pop();
                }

                try
                {
                    buf.release();
                }
                catch( IllegalStateException e )
                {
                    session.getManagerFilterChain().exceptionCaught( session, e );
                }

                session.getManagerFilterChain().dataWritten( session, marker );
                continue;
            }

            int writtenBytes = ch.write( buf.buf() );

            SelectionKey key = session.getSelectionKey();
            if( writtenBytes == 0 )
            {
                // Kernel buffer is full
                key.interestOps( key.interestOps() | SelectionKey.OP_WRITE );
            }
            else if( writtenBytes > 0 )
            {
                key.interestOps( key.interestOps()
                                 & ( ~SelectionKey.OP_WRITE ) );

                // pop and fire event
                synchronized( writeBufferQueue )
                {
                    writeBufferQueue.pop();
                    writeMarkerQueue.pop();
                }

                session.increaseWrittenBytes( writtenBytes );
                session.getManagerFilterChain().dataWritten( session, marker );
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

            DatagramSession session = new DatagramSession(
                    filters, req.channel, req.handler );

            try
            {
                req.handler.sessionCreated( session );

                SelectionKey key = req.channel.register( selector,
                        SelectionKey.OP_READ, session );
    
                session.setSelectionKey( key );
            }
            catch( Throwable t )
            {
                req.exception = t;
            }
            finally 
            {
                synchronized( req )
                {
                    req.done = true;
                    req.session = session;
                    req.notify();
                }
                
                if( req.exception != null )
                {
                    try
                    {
                        req.channel.close();
                    }
                    catch (IOException e)
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
            SelectionKey key;
            synchronized( cancelQueue )
            {
                key = ( SelectionKey ) cancelQueue.pop();
            }

            if( key == null )
                break;
            else
            {
                DatagramChannel ch = ( DatagramChannel ) key.channel();
                try
                {
                    ch.close();
                }
                catch( IOException e )
                {
                    exceptionMonitor.exceptionCaught( this, e );
                }
                key.cancel();
                selector.wakeup(); // wake up again to trigger thread death
            }
        }
    }

    public IoFilterChain getFilterChain()
    {
        return filters;
    }

    private static class RegistrationRequest
    {
        private final DatagramChannel channel;

        private final IoHandler handler;
        
        private boolean done;
        
        private DatagramSession session;
        
        private Throwable exception;

        private RegistrationRequest( DatagramChannel channel,
                                     IoHandler handler )
        {
            this.channel = channel;
            this.handler = handler;
        }
    }
}
