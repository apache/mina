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
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * A filter that forward events to {@link Executor}.
 * You can apply various thread model by inserting this filter to the {@link IoFilterChain}.
 * <p>
 * Please note that this filter doesn't manage the life cycle of the underlying
 * {@link Executor}.  You have to destroy or stop it by yourself.
 * <p>
 * This filter does not maintain the order of events per session and thus 
 * more than one event handler methods can be invoked at the same time with
 * mixed order.  For example, let's assume that messageReceived, messageSent,
 * and sessionClosed events are fired.
 * <ul>
 * <li>All event handler methods can be called simultaneously.
 *     (e.g. messageReceived and messageSent can be invoked at the same time.)</li>
 * <li>The event order can be mixed up.
 *     (e.g. sessionClosed or messageSent can be invoked before messageReceived
 *           is invoked.)</li>
 * </ul>
 * If you need to maintain the order of events per session, please use
 * {@link ExecutorFilter}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class UnorderedExecutorFilter extends IoFilterAdapter
{
    private final Executor executor;

    /**
     * Creates a new instance with the default thread pool implementation
     * (<tt>new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue() )</tt>).
     */
    public UnorderedExecutorFilter()
    {
        this( new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() ) );
    }
    
    /**
     * Creates a new instance with the specified <tt>executor</tt>.
     */
    public UnorderedExecutorFilter( Executor executor )
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
    public Executor getExecutor()
    {
        return executor;
    }

    private void fireEvent( NextFilter nextFilter, IoSession session,
                            EventType type, Object data )
    {
        Event event = new Event( type, nextFilter, data );
        executor.execute(new ProcessEventRunnable(session, event));
    }

    protected static class EventType
    {
        public static final EventType OPENED = new EventType( "OPENED" );

        public static final EventType CLOSED = new EventType( "CLOSED" );

        public static final EventType READ = new EventType( "READ" );

        public static final EventType WRITTEN = new EventType( "WRITTEN" );

        public static final EventType RECEIVED = new EventType( "RECEIVED" );

        public static final EventType SENT = new EventType( "SENT" );

        public static final EventType IDLE = new EventType( "IDLE" );

        public static final EventType EXCEPTION = new EventType( "EXCEPTION" );

        private final String value;

        private EventType( String value )
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value;
        }
    }

    protected static class Event
    {
        private final EventType type;
        private final NextFilter nextFilter;
        private final Object data;

        Event( EventType type, NextFilter nextFilter, Object data )
        {
            this.type = type;
            this.nextFilter = nextFilter;
            this.data = data;
        }

        public Object getData()
        {
            return data;
        }

        public NextFilter getNextFilter()
        {
            return nextFilter;
        }

        public EventType getType()
        {
            return type;
        }
    }

    @Override
    public void sessionCreated( NextFilter nextFilter, IoSession session )
    {
        nextFilter.sessionCreated( session );
    }

    @Override
    public void sessionOpened( NextFilter nextFilter,
                               IoSession session )
    {
        fireEvent( nextFilter, session, EventType.OPENED, null );
    }

    @Override
    public void sessionClosed( NextFilter nextFilter,
                               IoSession session )
    {
        fireEvent( nextFilter, session, EventType.CLOSED, null );
    }

    @Override
    public void sessionIdle( NextFilter nextFilter,
                             IoSession session, IdleStatus status )
    {
        fireEvent( nextFilter, session, EventType.IDLE, status );
    }

    @Override
    public void exceptionCaught( NextFilter nextFilter,
                                 IoSession session, Throwable cause )
    {
        fireEvent( nextFilter, session, EventType.EXCEPTION, cause );
    }

    @Override
    public void messageReceived( NextFilter nextFilter,
                                 IoSession session, Object message )
    {
        fireEvent( nextFilter, session, EventType.RECEIVED, message );
    }

    @Override
    public void messageSent( NextFilter nextFilter,
                             IoSession session, WriteRequest writeRequest )
    {
        fireEvent( nextFilter, session, EventType.SENT, writeRequest );
    }

    protected void processEvent( NextFilter nextFilter, IoSession session, EventType type, Object data )
    {
        if( type == EventType.RECEIVED )
        {
            nextFilter.messageReceived( session, data );
        }
        else if( type == EventType.SENT )
        {
            nextFilter.messageSent( session, (WriteRequest) data );
        }
        else if( type == EventType.EXCEPTION )
        {
            nextFilter.exceptionCaught( session, (Throwable) data );
        }
        else if( type == EventType.IDLE )
        {
            nextFilter.sessionIdle( session, (IdleStatus) data );
        }
        else if( type == EventType.OPENED )
        {
            nextFilter.sessionOpened( session );
        }
        else if( type == EventType.CLOSED )
        {
            nextFilter.sessionClosed( session );
        }
    }

    @Override
    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest )
    {
        nextFilter.filterWrite( session, writeRequest );
    }

    @Override
    public void filterClose( NextFilter nextFilter, IoSession session ) throws Exception
    {
        nextFilter.filterClose( session );
    }

    private class ProcessEventRunnable implements Runnable
    {
        private final IoSession session;
        private final Event event;

        ProcessEventRunnable( IoSession session, Event event )
        {
            this.session = session;
            this.event = event;
        }

        public void run()
        {
            processEvent( event.getNextFilter(), session, event.getType(), event.getData() );
        }
    }
}
