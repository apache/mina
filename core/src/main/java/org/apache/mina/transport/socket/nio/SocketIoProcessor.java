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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.WriteTimeoutException;
import org.apache.mina.util.NamePreservingRunnable;
import org.apache.mina.util.Queue;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * Performs all I/O operations for sockets which is connected or bound. This class is used by MINA internally.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$,
 */
class SocketIoProcessor
{
    private final Object lock = new Object();

    private final String threadName;
    private final Executor executor;
    /**
     * @noinspection FieldAccessedSynchronizedAndUnsynchronized
     */
    private Selector selector;

    private final Queue newSessions = new Queue();
    private final Queue removingSessions = new Queue();
    private final Queue flushingSessions = new Queue();
    private final Queue trafficControllingSessions = new Queue();

    private Worker worker;
    private long lastIdleCheckTime = System.currentTimeMillis();

    SocketIoProcessor( String threadName, Executor executor )
    {
        this.threadName = threadName;
        this.executor = executor;
    }

    void addNew( SocketSessionImpl session ) throws IOException
    {
        synchronized( newSessions )
        {
            newSessions.push( session );
        }

        startupWorker();

        selector.wakeup();
    }

    void remove( SocketSessionImpl session ) throws IOException
    {
        scheduleRemove( session );
        startupWorker();
        selector.wakeup();
    }

    private void startupWorker() throws IOException
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

    void flush( SocketSessionImpl session )
    {
        scheduleFlush( session );
        Selector selector = this.selector;
        if( selector != null )
        {
            selector.wakeup();
        }
    }

    void updateTrafficMask( SocketSessionImpl session )
    {
        scheduleTrafficControl( session );
        Selector selector = this.selector;
        if( selector != null )
        {
            selector.wakeup();
        }
    }

    private void scheduleRemove( SocketSessionImpl session )
    {
        synchronized( removingSessions )
        {
            removingSessions.push( session );
        }
    }

    private void scheduleFlush( SocketSessionImpl session )
    {
        synchronized( flushingSessions )
        {
            flushingSessions.push( session );
        }
    }

    private void scheduleTrafficControl( SocketSessionImpl session )
    {
        synchronized( trafficControllingSessions )
        {
            trafficControllingSessions.push( session );
        }
    }

    private void doAddNew()
    {
        if( newSessions.isEmpty() )
            return;

        for( ; ; )
        {
            SocketSessionImpl session;

            synchronized( newSessions )
            {
                session = ( SocketSessionImpl ) newSessions.pop();
            }

            if( session == null )
                break;

            SocketChannel ch = session.getChannel();
            try
            {
                ch.configureBlocking( false );
                session.setSelectionKey( ch.register( selector,
                                                      SelectionKey.OP_READ,
                                                      session ) );

                // AbstractIoFilterChain.CONNECT_FUTURE is cleared inside here
                // in AbstractIoFilterChain.fireSessionOpened().
                session.getServiceListeners().fireSessionCreated( session );
            }
            catch( IOException e )
            {
                // Clear the AbstractIoFilterChain.CONNECT_FUTURE attribute
                // and call ConnectFuture.setException().
                session.getFilterChain().fireExceptionCaught( session, e );
            }
        }
    }

    private void doRemove()
    {
        if( removingSessions.isEmpty() )
            return;

        for( ; ; )
        {
            SocketSessionImpl session;

            synchronized( removingSessions )
            {
                session = ( SocketSessionImpl ) removingSessions.pop();
            }

            if( session == null )
                break;

            SocketChannel ch = session.getChannel();
            SelectionKey key = session.getSelectionKey();
            // Retry later if session is not yet fully initialized.
            // (In case that Session.close() is called before addSession() is processed)
            if( key == null )
            {
                scheduleRemove( session );
                break;
            }
            // skip if channel is already closed
            if( !key.isValid() )
            {
                continue;
            }

            try
            {
                key.cancel();
                ch.close();
            }
            catch( IOException e )
            {
                session.getFilterChain().fireExceptionCaught( session, e );
            }
            finally
            {
                releaseWriteBuffers( session );
                session.getServiceListeners().fireSessionDestroyed( session );
            }
        }
    }

    private void process( Set selectedKeys )
    {
        Iterator it = selectedKeys.iterator();

        while( it.hasNext() )
        {
            SelectionKey key = ( SelectionKey ) it.next();
            SocketSessionImpl session = ( SocketSessionImpl ) key.attachment();

            if( key.isReadable() && session.getTrafficMask().isReadable() )
            {
                read( session );
            }

            if( key.isWritable() && session.getTrafficMask().isWritable() )
            {
                scheduleFlush( session );
            }
        }

        selectedKeys.clear();
    }

    private void read( SocketSessionImpl session )
    {
        ByteBuffer buf = ByteBuffer.allocate( session.getReadBufferSize() );
        SocketChannel ch = session.getChannel();

        try
        {
            buf.clear();

            int readBytes = 0;
            int ret;

            try
            {
                while( ( ret = ch.read( buf.buf() ) ) > 0 )
                {
                    readBytes += ret;
                }
            }
            finally
            {
                buf.flip();
            }

            session.increaseReadBytes( readBytes );

            if( readBytes > 0 )
            {
                session.getFilterChain().fireMessageReceived( session, buf );
                buf = null;
            }
            if( ret < 0 )
            {
                scheduleRemove( session );
            }
        }
        catch( Throwable e )
        {
            if( e instanceof IOException )
                scheduleRemove( session );
            session.getFilterChain().fireExceptionCaught( session, e );
        }
        finally
        {
            if( buf != null )
                buf.release();
        }
    }

    private void notifyIdleness()
    {
        // process idle sessions
        long currentTime = System.currentTimeMillis();
        if( ( currentTime - lastIdleCheckTime ) >= 1000 )
        {
            lastIdleCheckTime = currentTime;
            Set keys = selector.keys();
            if( keys != null )
            {
                for( Iterator it = keys.iterator(); it.hasNext(); )
                {
                    SelectionKey key = ( SelectionKey ) it.next();
                    SocketSessionImpl session = ( SocketSessionImpl ) key.attachment();
                    notifyIdleness( session, currentTime );
                }
            }
        }
    }

    private void notifyIdleness( SocketSessionImpl session, long currentTime )
    {
        notifyIdleness0(
            session, currentTime,
            session.getIdleTimeInMillis( IdleStatus.BOTH_IDLE ),
            IdleStatus.BOTH_IDLE,
            Math.max( session.getLastIoTime(), session.getLastIdleTime( IdleStatus.BOTH_IDLE ) ) );
        notifyIdleness0(
            session, currentTime,
            session.getIdleTimeInMillis( IdleStatus.READER_IDLE ),
            IdleStatus.READER_IDLE,
            Math.max( session.getLastReadTime(), session.getLastIdleTime( IdleStatus.READER_IDLE ) ) );
        notifyIdleness0(
            session, currentTime,
            session.getIdleTimeInMillis( IdleStatus.WRITER_IDLE ),
            IdleStatus.WRITER_IDLE,
            Math.max( session.getLastWriteTime(), session.getLastIdleTime( IdleStatus.WRITER_IDLE ) ) );

        notifyWriteTimeout( session, currentTime, session
            .getWriteTimeoutInMillis(), session.getLastWriteTime() );
    }

    private void notifyIdleness0( SocketSessionImpl session, long currentTime,
                                  long idleTime, IdleStatus status,
                                  long lastIoTime )
    {
        if( idleTime > 0 && lastIoTime != 0
            && ( currentTime - lastIoTime ) >= idleTime )
        {
            session.increaseIdleCount( status );
            session.getFilterChain().fireSessionIdle( session, status );
        }
    }

    private void notifyWriteTimeout( SocketSessionImpl session,
                                     long currentTime,
                                     long writeTimeout, long lastIoTime )
    {
        SelectionKey key = session.getSelectionKey();
        if( writeTimeout > 0
            && ( currentTime - lastIoTime ) >= writeTimeout
            && key != null && key.isValid()
            && ( key.interestOps() & SelectionKey.OP_WRITE ) != 0 )
        {
            session.getFilterChain().fireExceptionCaught( session, new WriteTimeoutException() );
        }
    }

    private void doFlush()
    {
        if( flushingSessions.size() == 0 )
            return;

        for( ; ; )
        {
            SocketSessionImpl session;

            synchronized( flushingSessions )
            {
                session = ( SocketSessionImpl ) flushingSessions.pop();
            }

            if( session == null )
                break;

            if( !session.isConnected() )
            {
                releaseWriteBuffers( session );
                continue;
            }

            SelectionKey key = session.getSelectionKey();
            // Retry later if session is not yet fully initialized.
            // (In case that Session.write() is called before addSession() is processed)
            if( key == null )
            {
                scheduleFlush( session );
                break;
            }
            // skip if channel is already closed
            if( !key.isValid() )
            {
                continue;
            }

            try
            {
                doFlush( session );
            }
            catch( IOException e )
            {
                scheduleRemove( session );
                session.getFilterChain().fireExceptionCaught( session, e );
            }
        }
    }

    private void releaseWriteBuffers( SocketSessionImpl session )
    {
        Queue writeRequestQueue = session.getWriteRequestQueue();
        WriteRequest req;

        while( ( req = ( WriteRequest ) writeRequestQueue.pop() ) != null )
        {
            try
            {
                ( ( ByteBuffer ) req.getMessage() ).release();
            }
            catch( IllegalStateException e )
            {
                session.getFilterChain().fireExceptionCaught( session, e );
            }
            finally
            {
                req.getFuture().setWritten( false );
            }
        }
    }

    private void doFlush( SocketSessionImpl session ) throws IOException
    {
        // Clear OP_WRITE
        SelectionKey key = session.getSelectionKey();
        key.interestOps( key.interestOps() & ( ~SelectionKey.OP_WRITE ) );

        SocketChannel ch = session.getChannel();
        Queue writeRequestQueue = session.getWriteRequestQueue();

        for( ; ; )
        {
            WriteRequest req;

            synchronized( writeRequestQueue )
            {
                req = ( WriteRequest ) writeRequestQueue.first();
            }

            if( req == null )
                break;

            ByteBuffer buf = ( ByteBuffer ) req.getMessage();
            if( buf.remaining() == 0 )
            {
                synchronized( writeRequestQueue )
                {
                    writeRequestQueue.pop();
                }

                session.increaseWrittenMessages();

                buf.reset();
                session.getFilterChain().fireMessageSent( session, req );
                continue;
            }

            int writtenBytes = ch.write( buf.buf() );
            if( writtenBytes > 0 )
            {
                session.increaseWrittenBytes( writtenBytes );
            }

            if( buf.hasRemaining() )
            {
                // Kernel buffer is full
                key.interestOps( key.interestOps() | SelectionKey.OP_WRITE );
                break;
            }
        }
    }

    private void doUpdateTrafficMask()
    {
        if( trafficControllingSessions.isEmpty() )
            return;

        for( ; ; )
        {
            SocketSessionImpl session;

            synchronized( trafficControllingSessions )
            {
                session = ( SocketSessionImpl ) trafficControllingSessions.pop();
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
            Thread.currentThread().setName( SocketIoProcessor.this.threadName );

            for( ; ; )
            {
                try
                {
                    int nKeys = selector.select( 1000 );
                    doAddNew();
                    doUpdateTrafficMask();

                    if( nKeys > 0 )
                    {
                        process( selector.selectedKeys() );
                    }

                    doFlush();
                    doRemove();
                    notifyIdleness();

                    if( selector.keys().isEmpty() )
                    {
                        synchronized( lock )
                        {
                            if( selector.keys().isEmpty() && newSessions.isEmpty() )
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
                catch( Throwable t )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( t );

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

}