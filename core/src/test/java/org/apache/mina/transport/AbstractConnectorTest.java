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

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionInitializer;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests a generic {@link IoConnector}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractConnectorTest extends TestCase {

    protected abstract IoAcceptor createAcceptor();
    protected abstract IoConnector createConnector();

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
            future.getSession().close();
            Assert.assertEquals("123", buf.toString());
        } finally {
            acceptor.dispose();
        }
    }

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
                future.getSession().close();
                fail();
            } catch (RuntimeIoException e) {
                // OK.
            }
            Assert.assertEquals("1", buf.toString());
        } finally {
            connector.dispose();
        }
    }
    
    /**
     * Test to make sure the SessionCallback gets invoked before IoHandler.sessionCreated.
     */
    public void testSessionCallbackInvocation() throws Exception {
        final int callbackInvoked = 0;
        final int sessionCreatedInvoked = 1;
        final int sessionCreatedInvokedBeforeCallback = 2;
        final boolean[] assertions = {false, false, false};
        final CountDownLatch latch = new CountDownLatch(2);
        final ConnectFuture[] callbackFuture = new ConnectFuture[1];
        
        int port = AvailablePortFinder.getNextAvailable(1025);
        IoAcceptor acceptor = createAcceptor();
        acceptor.setHandler(new IoHandlerAdapter());
        InetSocketAddress address = new InetSocketAddress(port);
        acceptor.bind(address);

        IoConnector connector = createConnector();
        connector.setHandler(new IoHandlerAdapter() {
           @Override
            public void sessionCreated(IoSession session) throws Exception {
                   assertions[sessionCreatedInvoked] = true;
                   assertions[sessionCreatedInvokedBeforeCallback] = !assertions[callbackInvoked];
                   latch.countDown();
            } 
        });
        
        ConnectFuture future = connector.connect(address, new IoSessionInitializer<ConnectFuture>() {
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
    }
}
