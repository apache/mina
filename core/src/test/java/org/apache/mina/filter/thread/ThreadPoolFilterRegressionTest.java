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
package org.apache.mina.filter.thread;

import java.net.SocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.DefaultWriteFuture;

public class ThreadPoolFilterRegressionTest extends TestCase
{
    private static final IoFilterChain FILTER_PARENT = new AbstractIoFilterChain( new DummySession() )
    {
        protected void doWrite( IoSession session, WriteRequest writeRequest )
        {
        }
        protected void doClose( IoSession session )
        {
        }
    };

    private ThreadPoolFilter filter;
    
    public ThreadPoolFilterRegressionTest()
    {
    }
    
    public void setUp() throws Exception
    {
        filter = new ThreadPoolFilter();
        filter.init();
    }
    
    public void tearDown() throws Exception
    {
        filter.destroy();
        Assert.assertEquals( 0, filter.getThreadPool().getPoolSize() );
        filter = null;
    }
    
    public void testEventOrder() throws Throwable
    {
        final EventOrderChecker nextFilter = new EventOrderChecker();
        final EventOrderCounter[] sessions = new EventOrderCounter[]
        {
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
            new EventOrderCounter(),
        };
        final int loop = 1000000;
        final int end = sessions.length - 1;
        final ThreadPoolFilter filter = this.filter;
        filter.getThreadPool().setKeepAliveTime( 3000 );
        
        for( int i = 0; i < loop ; i++ )
        {
            Integer objI = new Integer( i );

            for( int j = end; j >= 0; j-- )
            {
                filter.messageReceived( nextFilter, sessions[ j ], objI );
            }

            if( nextFilter.throwable != null )
            {
                throw nextFilter.throwable;
            }
        }
        
        Thread.sleep( 3500 );
        
        Assert.assertEquals( 1, filter.getThreadPool().getPoolSize() );
        for( int i = end; i >= 0; i-- )
        {
            Assert.assertEquals( loop - 1, sessions[ i ].lastCount.intValue() );
        }
    }
    
    public void testShutdown() throws Exception
    {
        final IoSession[] sessions = new IoSession[]
        {
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
            new DummySession(),
        };
        final int end = sessions.length - 1;
        final NextFilter nextFilter = new DummyNextFilter();

        for( int i = 0; i < 100000; i ++ )
        {
            if( i % 1000 == 0 )
            {
                System.out.println( "Shutdown: " + i );
            }
            
            WriteFuture future = null;
            for( int j = end; j >= 0; j-- )
            {
                future = new DefaultWriteFuture( sessions[ j ] );
                filter.messageReceived( nextFilter, sessions[ j ], future );
            }
            
            future.join();
            
            filter.onPostRemove( FILTER_PARENT, "", null );
            filter.onPostAdd( FILTER_PARENT, "", null );
        }
    }
    
    private static class EventOrderCounter extends BaseIoSession
    {
        private Integer lastCount = null;

        public synchronized void setLastCount( Integer newCount )
        {
            if( lastCount != null )
            {
                Assert.assertEquals( lastCount.intValue() + 1, newCount.intValue() );
            }
            
            lastCount = newCount;
        }

        public IoHandler getHandler()
        {
            return null;
        }

        public IoFilterChain getFilterChain()
        {
            return null;
        }

        public CloseFuture close()
        {
            return null;
        }

        public TransportType getTransportType()
        {
            return null;
        }

        public SocketAddress getRemoteAddress()
        {
            return null;
        }

        public SocketAddress getLocalAddress()
        {
            return null;
        }

        public int getScheduledWriteRequests()
        {
            return 0;
        }

        protected void updateTrafficMask()
        {
        }

        public boolean isClosing()
        {
            return false;
        }

        public IoService getService()
        {
            return null;
        }

        public IoServiceConfig getServiceConfig()
        {
            return null;
        }

        public IoSessionConfig getConfig()
        {
            return null;
        }

        public SocketAddress getServiceAddress()
        {
            return null;
        }

        public int getScheduledWriteBytes()
        {
            return 0;
        }
    }
    
    private static class EventOrderChecker implements NextFilter
    {
        private Throwable throwable;

        public void sessionOpened( IoSession session )
        {
        }

        public void sessionClosed( IoSession session )
        {
        }

        public void sessionIdle( IoSession session, IdleStatus status )
        {
        }

        public void exceptionCaught( IoSession session, Throwable cause )
        {
        }

        public void messageReceived( IoSession session, Object message )
        {
            try
            {
                ( ( EventOrderCounter ) session ).setLastCount( ( Integer ) message );
            }
            catch( Throwable t )
            {
                if( this.throwable == null )
                {
                    this.throwable = t;
                }
            }
        }

        public void messageSent( IoSession session, Object message )
        {
        }

        public void filterWrite( IoSession session, WriteRequest writeRequest )
        {
        }

        public void filterClose( IoSession session )
        {
        }

        public void sessionCreated( IoSession session )
        {
        }
    }
    
    private static class DummySession extends BaseIoSession
    {
        protected void updateTrafficMask()
        {
        }

        public IoHandler getHandler()
        {
            return null;
        }

        public IoFilterChain getFilterChain()
        {
            return null;
        }

        public CloseFuture close()
        {
            return null;
        }

        public TransportType getTransportType()
        {
            return null;
        }

        public SocketAddress getRemoteAddress()
        {
            return null;
        }

        public SocketAddress getLocalAddress()
        {
            return null;
        }

        public int getScheduledWriteRequests()
        {
            return 0;
        }

        public boolean isClosing()
        {
            return false;
        }

        public IoService getService()
        {
            return null;
        }

        public IoServiceConfig getServiceConfig()
        {
            return null;
        }

        public IoSessionConfig getConfig()
        {
            return null;
        }

        public SocketAddress getServiceAddress()
        {
            return null;
        }

        public int getScheduledWriteBytes()
        {
            return 0;
        }
    }
    
    private static class DummyNextFilter implements NextFilter
    {
        public void sessionCreated( IoSession session )
        {
        }

        public void sessionOpened( IoSession session )
        {
        }

        public void sessionClosed( IoSession session )
        {
        }

        public void sessionIdle( IoSession session, IdleStatus status )
        {
        }

        public void exceptionCaught( IoSession session, Throwable cause )
        {
        }

        public void messageReceived( IoSession session, Object message )
        {
            ( ( DefaultWriteFuture ) message ).setWritten( true );
        }

        public void messageSent( IoSession session, Object message )
        {
        }

        public void filterWrite( IoSession session, WriteRequest writeRequest )
        {
        }

        public void filterClose( IoSession session )
        {
        }
    }
    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( ThreadPoolFilterRegressionTest.class );
    }
}
