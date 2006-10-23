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
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.ExpiringSessionRecycler;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.IoSessionRecycler;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoConnector;
import org.apache.mina.common.support.DefaultConnectFuture;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.Queue;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramConnectorDelegate extends BaseIoConnector implements DatagramService
{
    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();

    private static volatile int nextId = 0;

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;
    private final IoSessionConfig sessionConfig = new DatagramSessionConfigImpl();

    private final IoConnector wrapper;
    private final Executor executor;
    private final int id = nextId ++ ;
    private Selector selector;
    private final Queue registerQueue = new Queue();
    private final Queue cancelQueue = new Queue();
    private final Queue flushingSessions = new Queue();
    private final Queue trafficControllingSessions = new Queue();
    private Worker worker;

    /**
     * Creates a new instance.
     */
    public DatagramConnectorDelegate( IoConnector wrapper, Executor executor )
    {
        this.wrapper = wrapper;
        this.executor = executor;
    }

    protected Class getAddressType()
    {
        return InetSocketAddress.class;
    }

    protected ConnectFuture doConnect( SocketAddress remoteAddress, SocketAddress localAddress )
    {
        DatagramChannel ch = null;
        boolean initialized = false;
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
        synchronized( this )
        {
            try
            {
                startupWorker();
            }
            catch( IOException e )
            {
                try
                {
                    ch.disconnect();
                    ch.close();
                }
                catch( IOException e2 )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e2 );
                }

                return DefaultConnectFuture.newFailedFuture( e );
            }
            
            synchronized( registerQueue )
            {
                registerQueue.push( request );
            }
        }

        selector.wakeup();
        return request;
    }
    
    public IoSessionRecycler getSessionRecycler()
    {
        return sessionRecycler;
    }

    // FIXME There can be a problem if a user changes the recycler after the service is activated.
    public void setSessionRecycler( IoSessionRecycler sessionRecycler )
    {
        if( sessionRecycler == null )
        {
            sessionRecycler = DEFAULT_RECYCLER;
        }
        this.sessionRecycler = sessionRecycler;
    }

    public IoSessionConfig getSessionConfig()
    {
        return sessionConfig;
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

    public void closeSession( DatagramSessionImpl session )
    {
        synchronized( this )
        {
            try
            {
                startupWorker();
            }
            catch( IOException e )
            {
                // IOException is thrown only when Worker thread is not
                // running and failed to open a selector.  We simply return
                // silently here because it we can simply conclude that
                // this session is not managed by this connector or
                // already closed.
                return;
            }

            synchronized( cancelQueue )
            {
                cancelQueue.push( session );
            }
        }

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
        synchronized( flushingSessions )
        {
            flushingSessions.push( session );
        }
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
        synchronized( trafficControllingSessions )
        {
            trafficControllingSessions.push( session );
        }
    }
    
    private void doUpdateTrafficMask() 
    {
        if( trafficControllingSessions.isEmpty() )
            return;

        for( ;; )
        {
            DatagramSessionImpl session;

            synchronized( trafficControllingSessions )
            {
                session = ( DatagramSessionImpl ) trafficControllingSessions.pop();
            }

            if( session == null )
                break;

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
            Queue writeRequestQueue = session.getWriteRequestQueue();
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

    private void processReadySessions( Set keys )
    {
        Iterator it = keys.iterator();
        while( it.hasNext() )
        {
            SelectionKey key = ( SelectionKey ) it.next();
            it.remove();

            DatagramSessionImpl session = ( DatagramSessionImpl ) key.attachment();

            DatagramSessionImpl replaceSession = getRecycledSession(session);

            if(replaceSession != null)
            {
                session = replaceSession;
            }

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
    
    private DatagramSessionImpl getRecycledSession( IoSession session )
    {
        IoSessionRecycler sessionRecycler = getSessionRecycler();
        DatagramSessionImpl replaceSession = null;

        if ( sessionRecycler != null )
        {
            synchronized ( sessionRecycler )
            {
                replaceSession = ( DatagramSessionImpl ) sessionRecycler.recycle( session.getLocalAddress(), session
                        .getRemoteAddress() );

                if ( replaceSession != null )
                {
                    return replaceSession;
                }

                sessionRecycler.put( session );
            }
        }

        return null;
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

            DatagramSessionImpl session = new DatagramSessionImpl(
                    wrapper, this, req.channel, getHandler() );
            
            // AbstractIoFilterChain will notify the connect future.
            session.setAttribute( AbstractIoFilterChain.CONNECT_FUTURE, req );

            boolean success = false;
            try
            {
                DatagramSessionImpl replaceSession = getRecycledSession( session );

                if ( replaceSession != null )
                {
                    session = replaceSession;
                }
                else
                {
                    SelectionKey key = req.channel.register( selector,
                            SelectionKey.OP_READ, session );

                    session.setSelectionKey( key );
                    buildFilterChain( session );
                    // The CONNECT_FUTURE attribute is cleared and notified here.
                    getListeners().fireSessionCreated( session );
                }
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
        getThreadModel().buildFilterChain( session.getFilterChain() );
    }

    private void cancelKeys()
    {
        if( cancelQueue.isEmpty() )
            return;

        for( ;; )
        {
            DatagramSessionImpl session;
            synchronized( cancelQueue )
            {
                session = ( DatagramSessionImpl ) cancelQueue.pop();
            }

            if( session == null )
                break;
            else
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
