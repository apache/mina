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
package org.apache.mina.common.support;

import java.io.IOException;
import java.net.SocketAddress;

import junit.framework.TestCase;

import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;

/**
 * Tests {@link IoFuture} implementations.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$ 
 */
public class FutureTest extends TestCase
{
    
    public void testCloseFuture() throws Exception
    {
        DefaultCloseFuture future = new DefaultCloseFuture( null );
        assertFalse( future.isReady() );
        assertFalse( future.isClosed() );
        
        TestThread thread = new TestThread( future );
        thread.start();
        
        future.setClosed();
        thread.join();
        
        assertTrue( thread.success );
        assertTrue( future.isReady() );
        assertTrue( future.isClosed() );
    }
    
    public void testConnectFuture() throws Exception
    {
        DefaultConnectFuture future = new DefaultConnectFuture();
        assertFalse( future.isReady() );
        assertFalse( future.isConnected() );
        assertNull( future.getSession() );
        assertNull( future.getException() );

        TestThread thread = new TestThread( future );
        thread.start();
        
        IoSession session = new BaseIoSession()
        {
            public IoHandler getHandler()
            {
                return null;
            }

            public IoFilterChain getFilterChain()
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

            public int getScheduledWriteMessages()
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
        };
        
        future.setSession( session );
        thread.join();
        
        assertTrue( thread.success );
        assertTrue( future.isReady() );
        assertTrue( future.isConnected() );
        assertEquals( session, future.getSession() );
        assertNull( future.getException() );
        
        future = new DefaultConnectFuture();
        thread = new TestThread( future );
        thread.start();
        future.setException( new IOException() );
        thread.join();
        
        assertTrue( thread.success );
        assertTrue( future.isReady() );
        assertFalse( future.isConnected() );
        assertTrue( future.getException() instanceof IOException );

        try
        {
            future.getSession();
            fail( "IOException should be thrown." );
        }
        catch( Exception e )
        {
        }
    }
    
    public void testWriteFuture() throws Exception
    {
        DefaultWriteFuture future = new DefaultWriteFuture( null );
        assertFalse( future.isReady() );
        assertFalse( future.isWritten() );
        
        TestThread thread = new TestThread( future );
        thread.start();
        
        future.setWritten( true );
        thread.join();
        
        assertTrue( thread.success );
        assertTrue( future.isReady() );
        assertTrue( future.isWritten() );

        future = new DefaultWriteFuture( null );
        thread = new TestThread( future );
        thread.start();
        
        future.setWritten( false );
        thread.join();
        
        assertTrue( thread.success );
        assertTrue( future.isReady() );
        assertFalse( future.isWritten() );
    }
    
    private static class TestThread extends Thread
    {
        private final IoFuture future;
        private boolean success;
        
        public TestThread( IoFuture future )
        {
            this.future = future;
        }
        
        public void run()
        {
            success = future.join( 10000 );
        }
    }
}
