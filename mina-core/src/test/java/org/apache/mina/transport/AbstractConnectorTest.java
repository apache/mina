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
package org.apache.mina.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

/**
 * Tests a generic {@link IoConnector}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractConnectorTest {

    protected abstract IoAcceptor createAcceptor();
    protected abstract IoConnector createConnector();

    @Test
    public void testConnectFutureSuccessTiming() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1025);
        IoAcceptor acceptor = createAcceptor();
        acceptor.setHandler(new IoHandlerAdapter());
        acceptor.bind(new InetSocketAddress(port));

        try {
            final StringBuffer buf = new StringBuffer();
            IoConnector connector = createConnector();
            connector.setHandler(new IoHandlerAdapter() {
                @Override
                public void sessionCreated(IoSession session) {
                    buf.append("1");
                }

                @Override
                public void sessionOpened(IoSession session) {
                    buf.append("2");
                }

                @Override
                public void exceptionCaught(IoSession session, Throwable cause) {
                    buf.append("X");
                }
            });
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port));
            future.awaitUninterruptibly();
            buf.append("3");
            future.getSession().close(true);
            // sessionCreated() will fire before the connect future completes
            // but sessionOpened() may not
            assertTrue(Pattern.matches("12?32?", buf.toString()));
        } finally {
            acceptor.dispose();
        }
    }

    @Test
    public void testConnectFutureFailureTiming() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(1025);
        final StringBuffer buf = new StringBuffer();

        IoConnector connector = createConnector();
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionCreated(IoSession session) {
                buf.append("X");
            }

            @Override
            public void sessionOpened(IoSession session) {
                buf.append("Y");
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) {
                buf.append("Z");
            }
        });
        
        try {
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port));
            future.awaitUninterruptibly();
            buf.append("1");
            try {
                future.getSession().close(true);
                fail();
            } catch (RuntimeIoException e) {
                // Signifies a successful test execution
                assertTrue(true);
            }
            assertEquals("1", buf.toString());
        } finally {
            connector.dispose();
        }
    }
    
    /**
     * Test to make sure the SessionCallback gets invoked before IoHandler.sessionCreated.
     */
    @Test
    public void testSessionCallbackInvocation() throws Exception {
        final int callbackInvoked = 0;
        final int sessionCreatedInvoked = 1;
        final int sessionCreatedInvokedBeforeCallback = 2;
        final boolean[] assertions = {false, false, false};
        final CountDownLatch latch = new CountDownLatch(2);
        final ConnectFuture[] callbackFuture = new ConnectFuture[1];
        
        int port = AvailablePortFinder.getNextAvailable(1025);

        IoAcceptor acceptor = createAcceptor();
        IoConnector connector = createConnector();

        try {
            acceptor.setHandler(new IoHandlerAdapter());
            InetSocketAddress address = new InetSocketAddress(port);
            acceptor.bind(address);
    
            connector.setHandler(new IoHandlerAdapter() {
               @Override
                public void sessionCreated(IoSession session) throws Exception {
                       assertions[sessionCreatedInvoked] = true;
                       assertions[sessionCreatedInvokedBeforeCallback] = !assertions[callbackInvoked];
                       latch.countDown();
                } 
            });
        
            ConnectFuture future = connector.connect(new InetSocketAddress("127.0.0.1", port), new IoSessionInitializer<ConnectFuture>() {
                public void initializeSession(IoSession session, ConnectFuture future) {
                    assertions[callbackInvoked] = true;
                    callbackFuture[0] = future;
                    latch.countDown();
                }
            });
            
            assertTrue("Timed out waiting for callback and IoHandler.sessionCreated to be invoked", latch.await(5, TimeUnit.SECONDS));
            assertTrue("Callback was not invoked", assertions[callbackInvoked]);
            assertTrue("IoHandler.sessionCreated was not invoked", assertions[sessionCreatedInvoked]);
            assertFalse("IoHandler.sessionCreated was invoked before session callback", assertions[sessionCreatedInvokedBeforeCallback]);
            assertSame("Callback future should have been same future as returned by connect", future, callbackFuture[0]);
        } finally {
            try {
                connector.dispose();
            } finally {
                acceptor.dispose();
            }
        }
    }
}
