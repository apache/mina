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
package org.apache.mina.transport.socket.nio.support;

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
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.ExpiringSessionRecycler;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.IoServiceListenerSupport;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.Queue;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramAcceptorDelegate extends BaseIoAcceptor implements IoAcceptor, DatagramService
{
    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();
    
    private static volatile int nextId = 0;

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;
    private final IoSessionConfig sessionConfig = new DatagramSessionConfigImpl();
        
    private final IoAcceptor wrapper;
    private final Executor executor;
    private final int id = nextId ++ ;
    private Selector selector;
    private final Map channels = new HashMap();
    private final Queue registerQueue = new Queue();
    private final Queue cancelQueue = new Queue();
    private final Queue flushingSessions = new Queue();
    private Worker worker;
    
    /**
     * Creates a new instance.
     */
    public DatagramAcceptorDelegate( IoAcceptor wrapper, Executor executor )
    {
        this.wrapper = wrapper;
        this.executor = executor;
    }

    protected Class getAddressType()
    {
        return InetSocketAddress.class;
    }

    protected void doBind() throws IOException
    {
        if( ( ( InetSocketAddress ) getLocalAddress() ).getPort() == 0 )
            throw new IllegalArgumentException( "Unsupported port number: 0" );
        
        RegistrationRequest request = new RegistrationRequest();
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

    protected void doUnbind()
    {
        CancellationRequest request = new CancellationRequest();
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
                throw new IllegalArgumentException( "Address not bound: " + getLocalAddress() );
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
    
    public IoSession newSession( SocketAddress remoteAddress )
    {
        if( remoteAddress == null )
        {
            throw new NullPointerException( "remoteAddress" );
        }
        if( getLocalAddress() == null )
        {
            throw new NullPointerException( "localAddress" );
        }
        
        Selector selector = this.selector;
        DatagramChannel ch = ( DatagramChannel ) channels.get( getLocalAddress() );
        if( selector == null || ch == null )
        {
            throw new IllegalArgumentException( "Unknown localAddress: " + getLocalAddress() );
        }
            
        SelectionKey key = ch.keyFor( selector );
        if( key == null )
        {
            throw new IllegalArgumentException( "Unknown localAddress: " + getLocalAddress() );
        }

        IoSession session;
        IoSessionRecycler sessionRecycler = getSessionRecycler();
        synchronized ( sessionRecycler )
        {
            session = sessionRecycler.recycle( getLocalAddress(), remoteAddress );
            if( session != null )
            {
                return session;
            }

            // If a new session needs to be created.
            DatagramSessionImpl datagramSession = new DatagramSessionImpl(
                    wrapper, this, ch, getHandler(), getLocalAddress() );
            datagramSession.setRemoteAddress( remoteAddress );
            datagramSession.setSelectionKey( key );
            
            getSessionRecycler().put( datagramSession );
            session = datagramSession;
        }
        
        try
        {
            buildFilterChain( session );
            getListeners().fireSessionCreated( session );
        }
        catch( Throwable t )
        {
            ExceptionMonitor.getInstance().exceptionCaught( t );
        }
        
        return session;
    }

    public IoSessionRecycler getSessionRecycler()
    {
        return sessionRecycler;
    }

    public void setSessionRecycler( IoSessionRecycler sessionRecycler )
    {
        synchronized( bindLock )
        {
            if( isBound() )
            {
                throw new IllegalStateException(
                        "sessionRecycler can't be set while the acceptor is bound." );
            }

            if( sessionRecycler == null )
            {
                sessionRecycler = DEFAULT_RECYCLER;
            }
            this.sessionRecycler = sessionRecycler;
        }
    }


    public IoSessionConfig getSessionConfig()
    {
        return sessionConfig;
    }

    public IoServiceListenerSupport getListeners()
    {
        return super.getListeners();
    }

    private void buildFilterChain( IoSession session ) throws Exception
    {
        this.getFilterChainBuilder().buildFilterChain( session.getFilterChain() );
        this.getThreadModel().buildFilterChain( session.getFilterChain() );
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

    private class Worker implements Runnable
    {
        public void run()
        {
            Thread.currentThread().setName( "DatagramAcceptor-" + id );

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

            try
            {
                if( key.isReadable() )
                {
                    readSession( ch );
                }

                if( key.isWritable() )
                {
                    for( Iterator i = getManagedSessions().iterator();
                         i.hasNext(); )
                    {
                        scheduleFlush( ( DatagramSessionImpl ) i.next() );
                    }
                }
            }
            catch( Throwable t )
            {
                ExceptionMonitor.getInstance().exceptionCaught( t );
            }
        }
    }

    private void readSession( DatagramChannel channel ) throws Exception
    {
        ByteBuffer readBuf = ByteBuffer.allocate(
                ( ( DatagramSessionConfig ) getSessionConfig() ).getReceiveBufferSize() );
        try
        {
            SocketAddress remoteAddress = channel.receive(
                readBuf.buf() );
            if( remoteAddress != null )
            {
                DatagramSessionImpl session =
                    ( DatagramSessionImpl ) newSession( remoteAddress );

                readBuf.flip();

                ByteBuffer newBuf = ByteBuffer.allocate( readBuf.limit() );
                newBuf.put( readBuf );
                newBuf.flip();

                session.increaseReadBytes( newBuf.remaining() );
                session.getFilterChain().fireMessageReceived( session, newBuf );
            }
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
                session.getFilterChain().fireExceptionCaught( session, e );
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

                session.increaseWrittenMessages();
                buf.reset();
                ( ( DatagramFilterChain ) session.getFilterChain() ).fireMessageSent( session, req );
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

            SocketAddress destination = req.getDestination();
            if( destination == null )
            {
                destination = session.getRemoteAddress();
            }
            
            int writtenBytes = ch.send( buf.buf(), destination );

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
                session.increaseWrittenMessages();
                buf.reset();
                session.getFilterChain().fireMessageSent( session, req );
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
                DatagramSessionConfig cfg = ( DatagramSessionConfig ) getSessionConfig();
                ch.socket().setReuseAddress( cfg.isReuseAddress() );
                ch.socket().setBroadcast( cfg.isBroadcast() );
                ch.socket().setReceiveBufferSize( cfg.getReceiveBufferSize() );
                ch.socket().setSendBufferSize( cfg.getSendBufferSize() );

                if( ch.socket().getTrafficClass() != cfg.getTrafficClass() )
                {
                    ch.socket().setTrafficClass( cfg.getTrafficClass() );
                }

                ch.configureBlocking( false );
                ch.socket().bind( getLocalAddress() );
                ch.register( selector, SelectionKey.OP_READ, req );
                synchronized( channels )
                {
                    channels.put( getLocalAddress(), ch );
                }
                
                getListeners().fireServiceActivated();
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
                ch = ( DatagramChannel ) channels.remove( getLocalAddress() );
            }

            // close the channel
            try
            {
                if( ch == null )
                {
                    request.exception = new IllegalArgumentException(
                            "Address not bound: " + getLocalAddress() );
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

                if( request.exception == null )
                {
                    getListeners().fireServiceDeactivated();
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
        private Throwable exception; 
        private boolean done;
    }

    private static class CancellationRequest
    {
        private boolean done;
        private RuntimeException exception;
    }
}
