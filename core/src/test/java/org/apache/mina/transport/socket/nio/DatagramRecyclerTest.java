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
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests if datagram sessions are recycled properly.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 436993 $, $Date: 2006-08-26 07:36:56 +0900 (토, 26  8월 2006) $ 
 */
public class DatagramRecyclerTest extends TestCase {
    private final IoAcceptor acceptor = new DatagramAcceptor();

    private final IoConnector connector = new DatagramConnector();

    public DatagramRecyclerTest() {
    }

    public void testDatagramRecycler() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1024);
        DatagramAcceptorConfig config = new DatagramAcceptorConfig();
        ExpiringSessionRecycler recycler = new ExpiringSessionRecycler(1, 1);
        config.setSessionRecycler(recycler);

        MockHandler acceptorHandler = new MockHandler();
        MockHandler connectorHandler = new MockHandler();

        acceptor.bind(new InetSocketAddress(port), acceptorHandler, config);

        try {
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port), connectorHandler, config);
            future.join();

            // Write whatever to trigger the acceptor.
            future.getSession().write(ByteBuffer.allocate(1)).join();

            // Wait until the connection is closed.
            future.getSession().getCloseFuture().join(3000);
            Assert.assertTrue(future.getSession().getCloseFuture().isClosed());
            acceptorHandler.session.getCloseFuture().join(3000);
            Assert.assertTrue(acceptorHandler.session.getCloseFuture()
                    .isClosed());

            Thread.sleep(1000);

            Assert.assertEquals("CROPSECL", connectorHandler.result);
            Assert.assertEquals("CROPRECL", acceptorHandler.result);
        } finally {
            acceptor.unbind(new InetSocketAddress(port));
        }
    }

    private class MockHandler extends IoHandlerAdapter {
        public IoSession session;

        public String result = "";

        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            this.session = session;
            result += "CA";
        }

        public void messageReceived(IoSession session, Object message)
                throws Exception {
            this.session = session;
            result += "RE";
        }

        public void messageSent(IoSession session, Object message)
                throws Exception {
            this.session = session;
            result += "SE";
        }

        public void sessionClosed(IoSession session) throws Exception {
            this.session = session;
            result += "CL";
        }

        public void sessionCreated(IoSession session) throws Exception {
            this.session = session;
            result += "CR";
        }

        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            this.session = session;
            result += "ID";
        }

        public void sessionOpened(IoSession session) throws Exception {
            this.session = session;
            result += "OP";
        }

    }
}
