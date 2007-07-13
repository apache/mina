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
package org.apache.mina.filter.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterEvent;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * A base abstract class for generic filters that forward I/O events to
 * {@link Executor} to enforce a certain thread model.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractExecutorFilter extends IoFilterAdapter
{
    private final Executor executor;

    /**
     * Creates a new instance with the default thread pool implementation
     * (<tt>new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue() )</tt>).
     */
    protected AbstractExecutorFilter()
    {
        this( new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() ) );
    }
    
    /**
     * Creates a new instance with the specified <tt>executor</tt>.
     */
    protected AbstractExecutorFilter( Executor executor )
    {
        if( executor == null )
        {
            throw new NullPointerException( "executor" );
        }

        this.executor = executor;
    }

    /**
     * Returns the underlying {@link Executor} instance this filter uses.
     */
    public final Executor getExecutor()
    {
        return executor;
    }

    protected abstract void fireEvent(IoFilterEvent event);

    public final void sessionCreated( NextFilter nextFilter, IoSession session )
    {
        nextFilter.sessionCreated( session );
    }

    public final void sessionOpened( NextFilter nextFilter,
                               IoSession session )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.SESSION_OPENED, session, null) );
    }

    public final void sessionClosed( NextFilter nextFilter,
                               IoSession session )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.SESSION_CLOSED, session, null) );
    }

    public final void sessionIdle( NextFilter nextFilter,
                             IoSession session, IdleStatus status )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.SESSION_IDLE, session, status) );
    }

    public final void exceptionCaught( NextFilter nextFilter,
                                 IoSession session, Throwable cause )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.EXCEPTION_CAUGHT, session, cause) );
    }

    public final void messageReceived( NextFilter nextFilter,
                                 IoSession session, Object message )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.MESSAGE_RECEIVED, session, message) );
    }

    public final void messageSent( NextFilter nextFilter,
                             IoSession session, WriteRequest writeRequest )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.MESSAGE_SENT, session, writeRequest) );
    }

    public final void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest )
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.WRITE, session, writeRequest) );
    }

    public final void filterClose( NextFilter nextFilter, IoSession session ) throws Exception
    {
        fireEvent( new IoFilterEvent(nextFilter, IoEventType.CLOSE, session, null) );
    }

    protected final void processEvent( IoFilterEvent event )
    {
        NextFilter nextFilter = event.getNextFilter();
        IoSession session = event.getSession();
        Object data = event.getParameter();
        
        switch (event.getType()) {
        case MESSAGE_RECEIVED:
            nextFilter.messageReceived(session, data);
            break;
        case MESSAGE_SENT:
            nextFilter.messageSent( session, (WriteRequest) data );
            break;
        case WRITE:
            nextFilter.filterWrite( session, (WriteRequest) data );
            break;
        case CLOSE:
            nextFilter.filterClose( session );
            break;
        case EXCEPTION_CAUGHT:
            nextFilter.exceptionCaught( session, (Throwable) data );
            break;
        case SESSION_IDLE:
            nextFilter.sessionIdle( session, (IdleStatus) data );
            break;
        case SESSION_OPENED:
            nextFilter.sessionOpened( session );
            break;
        case SESSION_CLOSED:
            nextFilter.sessionClosed( session );
            break;
        default:
            throw new InternalError("Unknown event type: " + event.getType());
        }
    }
}
