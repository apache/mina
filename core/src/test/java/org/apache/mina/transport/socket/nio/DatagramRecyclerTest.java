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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests if datagram sessions are recycled properly.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DatagramRecyclerTest {
    private NioDatagramAcceptor acceptor;
    private NioDatagramConnector connector;

    public DatagramRecyclerTest() {
        // Do nothing
    }

    @Before
    public void setUp() throws Exception {
        acceptor = new NioDatagramAcceptor();
        connector = new NioDatagramConnector();
    }

    @After
    public void tearDown() throws Exception {
        acceptor.dispose();
        connector.dispose();
    }

    @Test
    public void testDatagramRecycler() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1024);
        ExpiringSessionRecycler recycler = new ExpiringSessionRecycler(1, 1);

        MockHandler acceptorHandler = new MockHandler();
        MockHandler connectorHandler = new MockHandler();

        acceptor.setHandler(acceptorHandler);
        acceptor.setSessionRecycler(recycler);
        acceptor.bind(new InetSocketAddress(port));

        try {
            connector.setHandler(connectorHandler);
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port));
            future.awaitUninterruptibly();

            // Write whatever to trigger the acceptor.
            future.getSession().write(IoBuffer.allocate(1))
                    .awaitUninterruptibly();

            // Close the client-side connection.
            // This doesn't mean that the acceptor-side connection is also closed.
            // The life cycle of the acceptor-side connection is managed by the recycler.
            future.getSession().close(true);
            future.getSession().getCloseFuture().awaitUninterruptibly();
            assertTrue(future.getSession().getCloseFuture().isClosed());

            // Wait until the acceptor-side connection is closed.
            while (acceptorHandler.session == null) {
                Thread.yield();
            }
            acceptorHandler.session.getCloseFuture().awaitUninterruptibly(3000);

            // Is it closed?
            assertTrue(acceptorHandler.session.getCloseFuture()
                    .isClosed());

            Thread.sleep(1000);

            assertEquals("CROPSECL", connectorHandler.result.toString());
            assertEquals("CROPRECL", acceptorHandler.result.toString());
        } finally {
            acceptor.unbind();
        }
    }
    
    @Test
    public void testCloseRequest() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1024);
        ExpiringSessionRecycler recycler = new ExpiringSessionRecycler(10, 1);

        MockHandler acceptorHandler = new MockHandler();
        MockHandler connectorHandler = new MockHandler();

        acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 1);
        acceptor.setHandler(acceptorHandler);
        acceptor.setSessionRecycler(recycler);
        acceptor.bind(new InetSocketAddress(port));

        try {
            connector.setHandler(connectorHandler);
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port));
            future.awaitUninterruptibly();
            
            // Write whatever to trigger the acceptor.
            future.getSession().write(IoBuffer.allocate(1)).awaitUninterruptibly();

            // Make sure the connection is closed before recycler closes it.
            while (acceptorHandler.session == null) {
                Thread.yield();
            }
            acceptorHandler.session.close(true);
            assertTrue(
                    acceptorHandler.session.getCloseFuture().awaitUninterruptibly(3000));
            
            IoSession oldSession = acceptorHandler.session;

            // Wait until all events are processed and clear the state.
            long startTime = System.currentTimeMillis();
            while (acceptorHandler.result.length() < 8) {
                Thread.yield();
                if (System.currentTimeMillis() - startTime > 5000) {
                    throw new Exception();
                }
            }
            acceptorHandler.result.setLength(0);
            acceptorHandler.session = null;
            
            // Write whatever to trigger the acceptor again.
            WriteFuture wf = future.getSession().write(
                    IoBuffer.allocate(1)).awaitUninterruptibly();
            assertTrue(wf.isWritten());
            
            // Make sure the connection is closed before recycler closes it.
            while (acceptorHandler.session == null) {
                Thread.yield();
            }
            acceptorHandler.session.close(true);
            assertTrue(
                    acceptorHandler.session.getCloseFuture().awaitUninterruptibly(3000));

            future.getSession().close(true).awaitUninterruptibly();
            
            assertNotSame(oldSession, acceptorHandler.session);
        } finally {
            acceptor.unbind();
        }
    }

    private class MockHandler extends IoHandlerAdapter {
        public volatile IoSession session;
        public final StringBuffer result = new StringBuffer();

        /**
         * Default constructor
         */
        public MockHandler() {
            super();
        }
        
        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            this.session = session;
            result.append("CA");
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            this.session = session;
            result.append("RE");
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            this.session = session;
            result.append("SE");
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            this.session = session;
            result.append("CL");
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            this.session = session;
            result.append("CR");
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            this.session = session;
            result.append("ID");
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            this.session = session;
            result.append("OP");
        }
    }
}
