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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.DefaultExceptionMonitor;
import org.apache.mina.io.ExceptionMonitor;
import org.apache.mina.io.IoAcceptor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilterChain;
import org.apache.mina.util.Queue;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptor extends DatagramProcessor implements IoAcceptor
{
    private static volatile int nextId = 0;

    private final DatagramFilterChain filters = new DatagramFilterChain( this );

    private final int id = nextId ++ ;

    private final Selector selector;

    private final Map channels = new HashMap();

    private final Queue registerQueue = new Queue();

    private final Queue cancelQueue = new Queue();

    private final Queue flushingSessions = new Queue();

    private ExceptionMonitor exceptionMonitor = new DefaultExceptionMonitor();

    private Worker worker;

    /**
     * Creates a new instance.
     * 
     * @throws IOException
     */
    public DatagramAcceptor() throws IOException
    {
        selector = Selector.open();
    }

    public void bind( SocketAddress address, IoHandler handler )
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

        RegistrationRequest request = new RegistrationRequest( address, handler );
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
            request.exception.fillInStackTrace();
            if( request.exception instanceof IOException )
            {
                throw ( IOException ) request.exception;
            }
            else if( request.exception instanceof RuntimeException )
            {
                throw ( RuntimeException ) request.exception;
            }
            else if( request.exception instanceof Error )
            {
                throw ( Error ) request.exception;
            }
            else
            {
                throw new IllegalStateException();
            }
        }
    }

    public void unbind( SocketAddress address )
    {
        if( address == null )
            throw new NullPointerException( "address" );

        CancellationRequest request = new CancellationRequest( address );
        synchronized( this )
        {
            synchronized( cancelQueue )
            {
                cancelQueue.push( request );
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
            request.exception.fillInStackTrace();
            throw request.exception;
        }
    }
    
    private synchronized void startupWorker()
    {
        if( worker == null )
        {
            worker = new Worker();
            worker.start();
        }
    }

    public void flushSession( DatagramSession session )
    {
        scheduleFlush( session );
        selector.wakeup();
    }

    public void closeSession( DatagramSession session )
    {
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
                        synchronized( DatagramAcceptor.this )
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
                    exceptionMonitor.exceptionCaught( DatagramAcceptor.this,
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

            DatagramChannel ch = ( DatagramChannel ) key.channel();

            DatagramSession session = new DatagramSession(
                    filters, ch, ( IoHandler ) key.attachment() );
            session.setSelectionKey( key );

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
            SocketAddress remoteAddress = session.getChannel().receive(
                    readBuf.buf() );
            if( remoteAddress != null )
            {
                readBuf.flip();
                session.setRemoteAddress( remoteAddress );

                ByteBuffer newBuf = ByteBuffer.allocate( readBuf.limit() );
                newBuf.put( readBuf );
                newBuf.flip();

                session.increaseReadBytes( newBuf.remaining() );
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
                session.getFilters().exceptionCaught( session, e );
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
                    session.getFilters().exceptionCaught( session, e );
                }

                session.getFilters().dataWritten( session, marker );
                continue;
            }

            int writtenBytes = ch
                    .send( buf.buf(), session.getRemoteAddress() );

            SelectionKey key = session.getSelectionKey();
            if( writtenBytes == 0 )
            {
                // Kernel buffer is full
                key.interestOps( key.interestOps() | SelectionKey.OP_WRITE );
            }
            else
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
                session.getFilters().dataWritten( session, marker );
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

            DatagramChannel ch = null;
            try
            {
                ch = DatagramChannel.open();
                ch.configureBlocking( false );
                ch.socket().bind( req.address );
                ch.register( selector, SelectionKey.OP_READ, req.handler );
                channels.put( req.address, ch );
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
                    req.notify();
                }

                if( ch != null && req.exception != null )
                {
                    try
                    {
                        ch.close();
                    }
                    catch( Throwable e )
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

            DatagramChannel ch = ( DatagramChannel ) channels.remove( request.address );
            // close the channel
            try
            {
                if( ch == null )
                {
                    request.exception = new IllegalArgumentException(
                            "Address not bound: " + request.address );
                }
                else
                {
                    SelectionKey key = ch.keyFor( selector );
                    key.cancel();
                    selector.wakeup(); // wake up again to trigger thread death
                    ch.close();
                }
            }
            catch( Throwable t )
            {
                exceptionMonitor.exceptionCaught( this, t );
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
    
    public IoHandlerFilterChain getFilterChain()
    {
        return filters;
    }

    private static class RegistrationRequest
    {
        private final SocketAddress address;
        
        private final IoHandler handler;
        
        private Throwable exception; 
        
        private boolean done;
        
        private RegistrationRequest( SocketAddress address, IoHandler handler )
        {
            this.address = address;
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
