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
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.DefaultDatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramConnectorDelegate extends BaseIoConnector implements DatagramService
{
    private static volatile int nextId = 0;

    private final IoConnector wrapper;
    private final Executor executor;
    private final int id = nextId ++ ;
    private final Selector selector;
    private final Queue<RegistrationRequest> registerQueue = new ConcurrentLinkedQueue<RegistrationRequest>();
    private final Queue<DatagramSessionImpl> cancelQueue = new ConcurrentLinkedQueue<DatagramSessionImpl>();
    private final Queue<DatagramSessionImpl> flushingSessions = new ConcurrentLinkedQueue<DatagramSessionImpl>();
    private final Queue<DatagramSessionImpl> trafficControllingSessions = new ConcurrentLinkedQueue<DatagramSessionImpl>();
    private Worker worker;

    /**
     * Creates a new instance.
     */
    public DatagramConnectorDelegate( IoConnector wrapper, Executor executor )
    {
        super( new DefaultDatagramSessionConfig() );

        try
        {
            this.selector = Selector.open();
        }
        catch( IOException e )
        {
            throw new RuntimeIOException( "Failed to open a selector.", e );
        }

        this.wrapper = wrapper;
        this.executor = executor;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        try
        {
            selector.close();
        }
        catch( IOException e )
        {
            ExceptionMonitor.getInstance().exceptionCaught( e );
        }
    }

    public TransportType getTransportType() {
        return TransportType.DATAGRAM;
    }
    
    @Override
    public DatagramSessionConfig getSessionConfig() {
        return (DatagramSessionConfig) super.getSessionConfig();
    }

    @Override
    protected ConnectFuture doConnect( SocketAddress remoteAddress, SocketAddress localAddress )
    {
        DatagramChannel ch = null;
        boolean initialized = false;
        try
        {
            ch = DatagramChannel.open();
            DatagramSessionConfig cfg = getSessionConfig();
            
            ch.socket().setReuseAddress( cfg.isReuseAddress() );
            ch.socket().setBroadcast( cfg.isBroadcast() );
            ch.socket().setReceiveBufferSize( cfg.getReceiveBufferSize() );
            ch.socket().setSendBufferSize( cfg.getSendBufferSize() );

            if( ch.socket().getTrafficClass() != cfg.getTrafficClass() )
            {
                ch.socket().setTrafficClass( cfg.getTrafficClass() );
            }

            if( localAddress != null )
            {
                ch.socket().bind( localAddress );
            }
            ch.connect( remoteAddress );
            ch.configureBlocking( false );
            initialized = true;
        }
        catch( IOException e )
        {
            return DefaultConnectFuture.newFailedFuture( e );
        }
        finally
        {
            if( !initialized && ch != null )
            {
                try
                {
                    ch.disconnect();
                    ch.close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
            }
        }

        RegistrationRequest request = new RegistrationRequest( ch );

        registerQueue.offer( request );
        startupWorker();
        selector.wakeup();

        return request;
    }
    
    private synchronized void startupWorker() 
    {
        if( worker == null )
        {
            worker = new Worker();
            executor.execute( new NamePreservingRunnable( worker ) );
        }
    }

    public void closeSession( DatagramSessionImpl session )
    {
        cancelQueue.offer( session );
        startupWorker();
        selector.wakeup();
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

    private void scheduleFlush( DatagramSessionImpl session )
    {
        flushingSessions.offer( session );
    }

    public void updateTrafficMask( DatagramSessionImpl session )
    {
        scheduleTrafficControl( session );
        Selector selector = this.selector;
        if( selector != null )
        {
            selector.wakeup();
        }
        selector.wakeup();
    }
    
    private void scheduleTrafficControl( DatagramSessionImpl session )
    {
        trafficControllingSessions.offer( session );
    }
    
    private void doUpdateTrafficMask() 
    {
        for( ;; )
        {
            DatagramSessionImpl session = trafficControllingSessions.poll();
            if( session == null ) {
                break;
            }

            SelectionKey key = session.getSelectionKey();
            // Retry later if session is not yet fully initialized.
            // (In case that Session.suspend??() or session.resume??() is 
            // called before addSession() is processed)
            if( key == null )
            {
                scheduleTrafficControl( session );
                break;
            }
            // skip if channel is already closed
            if( !key.isValid() )
            {
                continue;
            }

            // The normal is OP_READ and, if there are write requests in the
            // session's write queue, set OP_WRITE to trigger flushing.
            int ops = SelectionKey.OP_READ;
            Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();
            synchronized( writeRequestQueue )
            {
                if( !writeRequestQueue.isEmpty() )
                {
                    ops |= SelectionKey.OP_WRITE;
                }
            }

            // Now mask the preferred ops with the mask of the current session
            int mask = session.getTrafficMask().getInterestOps();
            key.interestOps( ops & mask );
        }
    }
    
    private class Worker implements Runnable
    {
        public void run()
        {
            Thread.currentThread().setName( "DatagramConnector-" + id );

            for( ;; )
            {
                try
                {
                    int nKeys = selector.select();

                    registerNew();
                    doUpdateTrafficMask();

                    if( nKeys > 0 )
                    {
                        processReadySessions( selector.selectedKeys() );
                    }

                    flushSessions();
                    cancelKeys();

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( DatagramConnectorDelegate.this )
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
                    ExceptionMonitor.getInstance().exceptionCaught(  e );

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

    private void processReadySessions( Set<SelectionKey> keys )
    {
        Iterator<SelectionKey> it = keys.iterator();
        while( it.hasNext() )
        {
            SelectionKey key = it.next();
            it.remove();

            DatagramSessionImpl session = ( DatagramSessionImpl ) key.attachment();

            if( key.isReadable() && session.getTrafficMask().isReadable() )
            {
                readSession( session );
            }

            if( key.isWritable() && session.getTrafficMask().isWritable() )
            {
                scheduleFlush( session );
            }
        }
    }
    
    private void readSession( DatagramSessionImpl session )
    {

        ByteBuffer readBuf = ByteBuffer.allocate( session.getReadBufferSize() );
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
                session.getFilterChain().fireMessageReceived( session, newBuf );
            }
        }
        catch( IOException e )
        {
            session.getFilterChain().fireExceptionCaught( session, e );
        }
    }

    private void flushSessions()
    {
        for( ;; )
        {
            DatagramSessionImpl session = flushingSessions.poll();
            if( session == null ) {
                break;
            }

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

        Queue<WriteRequest> writeRequestQueue = session.getWriteRequestQueue();

        WriteRequest req;
        for( ;; )
        {
            synchronized( writeRequestQueue )
            {
                req = writeRequestQueue.peek();
            }

            if( req == null ) {
                break;
            }

            ByteBuffer buf = ( ByteBuffer ) req.getMessage();
            if( buf.remaining() == 0 )
            {
                // pop and fire event
                synchronized( writeRequestQueue )
                {
                    writeRequestQueue.poll();
                }

                session.increaseWrittenMessages();
                buf.reset();
                session.getFilterChain().fireMessageSent( session, req );
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

            int writtenBytes = ch.write( buf.buf() );

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
                    writeRequestQueue.poll();
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
        for( ;; )
        {
            RegistrationRequest req = registerQueue.poll();
            if( req == null ) {
                break;
            }

            DatagramSessionImpl session = new DatagramSessionImpl(
                    wrapper, this, req.channel, getHandler() );
            
            // AbstractIoFilterChain will notify the connect future.
            session.setAttribute( AbstractIoFilterChain.CONNECT_FUTURE, req );

            boolean success = false;
            try
            {
                SelectionKey key = req.channel.register( selector,
                        SelectionKey.OP_READ, session );

                session.setSelectionKey( key );
                buildFilterChain( session );
                // The CONNECT_FUTURE attribute is cleared and notified here.
                getListeners().fireSessionCreated( session );
                success = true;
            }
            catch( Throwable t )
            {
                // The CONNECT_FUTURE attribute is cleared and notified here.
                session.getFilterChain().fireExceptionCaught( session, t );
            }
            finally
            {
                if( !success )
                {
                    try
                    {
                        req.channel.disconnect();
                        req.channel.close();
                    }
                    catch (IOException e)
                    {
                        ExceptionMonitor.getInstance().exceptionCaught( e );
                    }
                }
            }
        }
    }

    private void buildFilterChain( IoSession session ) throws Exception
    {
        getFilterChainBuilder().buildFilterChain( session.getFilterChain() );
    }

    private void cancelKeys()
    {
        for( ;; )
        {
            DatagramSessionImpl session = cancelQueue.poll();
            if( session == null ) {
                break;
            } else
            {
                SelectionKey key = session.getSelectionKey();
                DatagramChannel ch = ( DatagramChannel ) key.channel();
                try
                {
                    ch.disconnect();
                    ch.close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
                
                getListeners().fireSessionDestroyed( session );
                session.getCloseFuture().setClosed();
                key.cancel();
                selector.wakeup(); // wake up again to trigger thread death
            }
        }
    }

    private static class RegistrationRequest extends DefaultConnectFuture
    {
        private final DatagramChannel channel;

        private RegistrationRequest( DatagramChannel channel )
        {
            this.channel = channel;
        }
    }
}
