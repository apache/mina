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
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.util.Queue;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptorDelegate extends BaseIoAcceptor implements IoAcceptor, DatagramService
{
    private static volatile int nextId = 0;

    private final IoAcceptor wrapper;
    private final int id = nextId ++ ;
    private Selector selector;
    private final DatagramAcceptorConfig defaultConfig = new DatagramAcceptorConfig();
    private final Map channels = new HashMap();
    private final Queue registerQueue = new Queue();
    private final Queue cancelQueue = new Queue();
    private final Queue flushingSessions = new Queue();
    private Worker worker;

    /**
     * Creates a new instance.
     */
    public DatagramAcceptorDelegate( IoAcceptor wrapper )
    {
        this.wrapper = wrapper;
    }

    public void bind( SocketAddress address, IoHandler handler, IoServiceConfig config )
            throws IOException
    {
        if( address == null )
            throw new NullPointerException( "address" );
        if( handler == null )
            throw new NullPointerException( "handler" );
        if( config == null )
        {
            config = getDefaultConfig();
        }

        if( !( address instanceof InetSocketAddress ) )
            throw new IllegalArgumentException( "Unexpected address type: "
                                                + address.getClass() );
        if( ( ( InetSocketAddress ) address ).getPort() == 0 )
            throw new IllegalArgumentException( "Unsupported port number: 0" );
        
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
            throw ( IOException ) new IOException( "Failed to bind" ).initCause( request.exception );
        }
    }

    public void unbind( SocketAddress address )
    {
        // TODO: DIRMINA-93
        if( address == null )
            throw new NullPointerException( "address" );

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
            throw new RuntimeException( "Failed to unbind" , request.exception );
        }
    }
    
    public void unbindAll()
    {
        List addresses;
        synchronized( channels )
        {
            addresses = new ArrayList( channels.keySet() );
        }
        
        for( Iterator i = addresses.iterator(); i.hasNext(); )
        {
            unbind( ( SocketAddress ) i.next() );
        }
    }
    
    public boolean isBound( SocketAddress address )
    {
        synchronized( channels )
        {
            return channels.containsKey( address );
        }
    }
    
    public IoSession newSession( SocketAddress remoteAddress, SocketAddress localAddress )
    {
        if( remoteAddress == null )
        {
            throw new NullPointerException( "remoteAddress" );
        }
        if( localAddress == null )
        {
            throw new NullPointerException( "localAddress" );
        }
        
        Selector selector = this.selector;
        DatagramChannel ch = ( DatagramChannel ) channels.get( localAddress );
        if( selector == null || ch == null )
        {
            throw new IllegalArgumentException( "Unknown localAddress: " + localAddress );
        }
            
        SelectionKey key = ch.keyFor( selector );
        if( key == null )
        {
            throw new IllegalArgumentException( "Unknown localAddress: " + localAddress );
        }

        RegistrationRequest req = ( RegistrationRequest ) key.attachment();
        DatagramSessionImpl s = new DatagramSessionImpl(
                wrapper, this,
                req.config.getSessionConfig(), ch, req.handler,
                req.address );
        s.setRemoteAddress( remoteAddress );
        s.setSelectionKey( key );
        
        try
        {
            this.getFilterChainBuilder().buildFilterChain( s.getFilterChain() );
            req.config.getFilterChainBuilder().buildFilterChain( s.getFilterChain() );
            ( ( DatagramFilterChain ) s.getFilterChain() ).sessionCreated( s );
        }
        catch( Throwable t )
        {
            ExceptionMonitor.getInstance().exceptionCaught( t );
        }
        
        return s;
    }
    
    public IoServiceConfig getDefaultConfig()
    {
        return defaultConfig;
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

    public void flushSession( DatagramSessionImpl session )
    {
        scheduleFlush( session );
        Selector selector = this.selector;
        if( selector != null )
        {
            selector.wakeup();
        }
    }

    public void closeSession( DatagramSessionImpl session )
    {
    }

    private void scheduleFlush( DatagramSessionImpl session )
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
                        synchronized( DatagramAcceptorDelegate.this )
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
    }

    private void processReadySessions( Set keys )
    {
        Iterator it = keys.iterator();
        while( it.hasNext() )
        {
            SelectionKey key = ( SelectionKey ) it.next();
            it.remove();

            DatagramChannel ch = ( DatagramChannel ) key.channel();

            RegistrationRequest req = ( RegistrationRequest ) key.attachment();
            DatagramSessionImpl session = new DatagramSessionImpl(
                    wrapper, this,
                    req.config.getSessionConfig(),
                    ch, req.handler,
                    req.address );
            session.setSelectionKey( key );
            
            try
            {
                ( ( DatagramFilterChain ) session.getFilterChain() ).sessionCreated( session );

                if( key.isReadable() )
                {
                    readSession( session );
                }

                if( key.isWritable() )
                {
                    scheduleFlush( session );
                }
            }
            catch( Throwable t )
            {
                ExceptionMonitor.getInstance().exceptionCaught( t );
            }
        }
    }

    private void readSession( DatagramSessionImpl session )
    {

        ByteBuffer readBuf = ByteBuffer.allocate( session.getReadBufferSize() );
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
                ( ( DatagramFilterChain ) session.getFilterChain() ).messageReceived( session, newBuf );
            }
        }
        catch( IOException e )
        {
            ( ( DatagramFilterChain ) session.getFilterChain() ).exceptionCaught( session, e );
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
            DatagramSessionImpl session;

            synchronized( flushingSessions )
            {
                session = ( DatagramSessionImpl ) flushingSessions.pop();
            }

            if( session == null )
                break;

            try
            {
                flush( session );
            }
            catch( IOException e )
            {
                ( ( DatagramFilterChain ) session.getFilterChain() ).exceptionCaught( session, e );
            }
        }
    }

    private void flush( DatagramSessionImpl session ) throws IOException
    {
        DatagramChannel ch = session.getChannel();

        Queue writeRequestQueue = session.getWriteRequestQueue();

        WriteRequest req;
        for( ;; )
        {
            synchronized( writeRequestQueue )
            {
                req = ( WriteRequest ) writeRequestQueue.first();
            }

            if( req == null )
                break;

            ByteBuffer buf = ( ByteBuffer ) req.getMessage();
            if( buf.remaining() == 0 )
            {
                // pop and fire event
                synchronized( writeRequestQueue )
                {
                    writeRequestQueue.pop();
                }

                req.getFuture().setWritten( true );
                session.increaseWrittenWriteRequests();
                ( ( DatagramFilterChain ) session.getFilterChain() ).messageSent( session, buf );
                continue;
            }

            SelectionKey key = session.getSelectionKey();
            if( key == null )
            {
                scheduleFlush( session );
                break;
            }
            if( !key.isValid() )
            {
                continue;
            }

            int pos = buf.position();
            int writtenBytes = ch
                    .send( buf.buf(), session.getRemoteAddress() );

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
                synchronized( writeRequestQueue )
                {
                    writeRequestQueue.pop();
                }

                session.increaseWrittenBytes( writtenBytes );
                req.getFuture().setWritten( true );
                session.increaseWrittenWriteRequests();
                ( ( DatagramFilterChain ) session.getFilterChain() ).messageSent( session, buf.position( pos ) );
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
                DatagramSessionConfig cfg;
                if( req.config.getSessionConfig() instanceof DatagramSessionConfig )
                {
                    cfg = ( DatagramSessionConfig ) req.config.getSessionConfig();
                }
                else
                {
                    cfg = ( DatagramSessionConfig ) getDefaultConfig().getSessionConfig();
                }

                ch.socket().setReuseAddress( cfg.isReuseAddress() );
                ch.socket().setBroadcast( cfg.isBroadcast() );
                ch.socket().setReceiveBufferSize( cfg.getReceiveBufferSize() );
                ch.socket().setSendBufferSize( cfg.getSendBufferSize() );
                ch.socket().setTrafficClass( cfg.getTrafficClass() );
                
                ch.configureBlocking( false );
                ch.socket().bind( req.address );
                ch.register( selector, SelectionKey.OP_READ, req );
                synchronized( channels )
                {
                    channels.put( req.address, ch );
                }
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
                        ch.disconnect();
                        ch.close();
                    }
                    catch( Throwable e )
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

            DatagramChannel ch;
            synchronized( channels )
            {
                ch = ( DatagramChannel ) channels.remove( request.address );
            }

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
                    ch.disconnect();
                    ch.close();
                }
            }
            catch( Throwable t )
            {
                ExceptionMonitor.getInstance().exceptionCaught( t );
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
    
    public void updateTrafficMask( DatagramSessionImpl session )
    {
        // There's no point in changing the traffic mask for sessions originating
        // from this acceptor since new sessions are created every time data is
        // received.
    }

    private static class RegistrationRequest
    {
        private final SocketAddress address;
        private final IoHandler handler;
        private final IoServiceConfig config;

        private Throwable exception; 
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
        private RuntimeException exception;
        
        private CancellationRequest( SocketAddress address )
        {
            this.address = address;
        }
    }
}
