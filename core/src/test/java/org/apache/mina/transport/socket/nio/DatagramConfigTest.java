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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests if {@link NioDatagramAcceptor} session is configured properly.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DatagramConfigTest extends TestCase {
    private IoAcceptor acceptor;
    private IoConnector connector;
    String result;

    public DatagramConfigTest() {
        // Do nothing
    }

    @Override
    protected void setUp() throws Exception {
        result = "";
        acceptor = new NioDatagramAcceptor();
        connector = new NioDatagramConnector();
    }
    
    @Override
    protected void tearDown() throws Exception {
        acceptor.dispose();
        connector.dispose();
    }

    public void testAcceptorFilterChain() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1024);
        IoFilter mockFilter = new MockFilter();
        IoHandler mockHandler = new MockHandler();

        acceptor.getFilterChain().addLast("mock", mockFilter);
        acceptor.setHandler(mockHandler);
        acceptor.bind(new InetSocketAddress(port));

        try {
            connector.setHandler(new IoHandlerAdapter());
            ConnectFuture future = connector.connect(
                    new InetSocketAddress("127.0.0.1", port));
            future.awaitUninterruptibly();

            WriteFuture writeFuture = future.getSession().write(
                    IoBuffer.allocate(16).putInt(0).flip());
            writeFuture.awaitUninterruptibly();
            Assert.assertTrue(writeFuture.isWritten());

            future.getSession().close(true);

            for (int i = 0; i < 30; i++) {
                if (result.length() == 2) {
                    break;
                }
                Thread.sleep(100);
            }

            Assert.assertEquals("FH", result);
        } finally {
            acceptor.unbind();
        }
    }

    private class MockFilter extends IoFilterAdapter {
        /**
         * Default constructor
         */
        public MockFilter() {
            super();
        }
        
        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) throws Exception {
            result += "F";
            nextFilter.messageReceived(session, message);
        }

    }

    private class MockHandler extends IoHandlerAdapter {
        /**
         * Default constructor
         */
        public MockHandler() {
            super();
        }
        
        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            result += "H";
        }
    }
}
