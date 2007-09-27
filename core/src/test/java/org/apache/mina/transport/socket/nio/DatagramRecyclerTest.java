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

import java.net.InetSocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExpiringSessionRecycler;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests if datagram sessions are recycled properly.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DatagramRecyclerTest extends TestCase {
    private final DatagramAcceptor acceptor = new DatagramAcceptor();

    private final DatagramConnector connector = new DatagramConnector();

    public DatagramRecyclerTest() {
    }

    public void testDatagramRecycler() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1024);
        ExpiringSessionRecycler recycler = new ExpiringSessionRecycler(1, 1);

        MockHandler acceptorHandler = new MockHandler();
        MockHandler connectorHandler = new MockHandler();

        acceptor.setLocalAddress(new InetSocketAddress(port));
        acceptor.setHandler(acceptorHandler);
        acceptor.setSessionRecycler(recycler);
        acceptor.bind();

        try {
            connector.setHandler(connectorHandler);
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port));
            future.awaitUninterruptibly();

            // Write whatever to trigger the acceptor.
            future.getSession().write(ByteBuffer.allocate(1))
                    .awaitUninterruptibly();

            // Close the client-side connection.
            // This doesn't mean that the acceptor-side connection is also closed.
            // The life cycle of the acceptor-side connection is managed by the recycler.
            future.getSession().close();
            future.getSession().getCloseFuture().awaitUninterruptibly();
            Assert.assertTrue(future.getSession().getCloseFuture().isClosed());

            // Wait until the acceptor-side connection is closed.
            while (acceptorHandler.session == null) {
                Thread.yield();
            }
            acceptorHandler.session.getCloseFuture().awaitUninterruptibly(3000);

            // Is it closed?
            Assert.assertTrue(acceptorHandler.session.getCloseFuture()
                    .isClosed());

            Thread.sleep(1000);

            Assert.assertEquals("CROPSECL", connectorHandler.result);
            Assert.assertEquals("CROPRECL", acceptorHandler.result);
        } finally {
            acceptor.unbind();
        }
    }

    private class MockHandler extends IoHandlerAdapter {
        public IoSession session;

        public String result = "";

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            this.session = session;
            result += "CA";
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            this.session = session;
            result += "RE";
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            this.session = session;
            result += "SE";
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            this.session = session;
            result += "CL";
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            this.session = session;
            result += "CR";
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            this.session = session;
            result += "ID";
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            this.session = session;
            result += "OP";
        }

    }
}
