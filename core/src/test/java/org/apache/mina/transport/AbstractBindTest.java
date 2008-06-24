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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.buffer.IoBuffer;
import org.apache.mina.common.future.ConnectFuture;
import org.apache.mina.common.service.IoAcceptor;
import org.apache.mina.common.service.IoConnector;
import org.apache.mina.common.service.IoHandlerAdapter;
import org.apache.mina.common.session.IdleStatus;
import org.apache.mina.common.session.IoSession;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests {@link IoAcceptor} resource leakage by repeating bind and unbind.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractBindTest extends TestCase {
    protected final IoAcceptor acceptor;

    protected int port;

    public AbstractBindTest(IoAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    protected abstract SocketAddress createSocketAddress(int port);

    protected abstract int getPort(SocketAddress address);

    protected abstract IoConnector newConnector();

    protected void bind(boolean reuseAddress) throws IOException {
        acceptor.setHandler(new EchoProtocolHandler());

        setReuseAddress(reuseAddress);

        // Find an available test port and bind to it.
        boolean socketBound = false;

        // Let's start from port #1 to detect possible resource leak
        // because test will fail in port 1-1023 if user run this test
        // as a normal user.
        for (port = 1; port <= 65535; port++) {
            socketBound = false;
            try {
                acceptor.setDefaultLocalAddress(createSocketAddress(port));
                acceptor.bind();
                socketBound = true;
                break;
            } catch (IOException e) {
                //System.out.println(e.getMessage());
            }
        }

        // If there is no port available, test fails.
        if (!socketBound) {
            throw new IOException("Cannot bind any test port.");
        }

        //System.out.println( "Using port " + port + " for testing." );
    }

    private void setReuseAddress(boolean reuseAddress) {
        if (acceptor instanceof DatagramAcceptor) {
            ((DatagramSessionConfig) acceptor.getSessionConfig())
                    .setReuseAddress(reuseAddress);
        } else if (acceptor instanceof SocketAcceptor) {
            ((SocketAcceptor) acceptor).setReuseAddress(reuseAddress);
        }
    }

    @Override
    public void tearDown() {
        try {
            acceptor.dispose();
        } catch (Exception e) {
            // ignore
        }

        acceptor.setDefaultLocalAddress(null);
    }

    public void testAnonymousBind() throws Exception {
        acceptor.setHandler(new IoHandlerAdapter());
        acceptor.setDefaultLocalAddress(null);
        acceptor.bind();
        Assert.assertNotNull(acceptor.getLocalAddress());
        acceptor.unbind(acceptor.getLocalAddress());
        Assert.assertNull(acceptor.getLocalAddress());
        acceptor.setDefaultLocalAddress(createSocketAddress(0));
        acceptor.bind();
        Assert.assertNotNull(acceptor.getLocalAddress());
        Assert.assertTrue(getPort(acceptor.getLocalAddress()) != 0);
        acceptor.unbind(acceptor.getLocalAddress());
    }

    public void testDuplicateBind() throws IOException {
        bind(false);

        try {
            acceptor.bind();
            Assert.fail("Exception is not thrown");
        } catch (Exception e) {
        }
    }

    public void testDuplicateUnbind() throws IOException {
        bind(false);

        // this should succeed
        acceptor.unbind();

        // this shouldn't fail
        acceptor.unbind();
    }

    public void testManyTimes() throws IOException {
        bind(true);

        for (int i = 0; i < 1024; i++) {
            acceptor.unbind();
            acceptor.bind();
        }
    }

    public void testUnbindDisconnectsClients() throws Exception {
        bind(true);
        IoConnector connector = newConnector();
        IoSession[] sessions = new IoSession[5];
        connector.setHandler(new IoHandlerAdapter());
        for (int i = 0; i < sessions.length; i++) {
            ConnectFuture future = connector.connect(createSocketAddress(port));
            future.awaitUninterruptibly();
            sessions[i] = future.getSession();
            Assert.assertTrue(sessions[i].isConnected());
            Assert.assertTrue(sessions[i].write(IoBuffer.allocate(1)).awaitUninterruptibly().isWritten());
        }

        // Wait for the server side sessions to be created.
        Thread.sleep(500);

        Collection<IoSession> managedSessions = acceptor.getManagedSessions().values();
        Assert.assertEquals(5, managedSessions.size());

        acceptor.unbind();

        // Wait for the client side sessions to close.
        Thread.sleep(500);

        Assert.assertEquals(0, managedSessions.size());
        for (IoSession element : managedSessions) {
            Assert.assertFalse(element.isConnected());
        }
    }

    public void _testRegressively() throws IOException {
        setReuseAddress(true);

        SocketAddress addr = createSocketAddress(port);
        EchoProtocolHandler handler = new EchoProtocolHandler();
        acceptor.setDefaultLocalAddress(addr);
        acceptor.setHandler(handler);
        for (int i = 0; i < 1048576; i++) {
            acceptor.bind();
            acceptor.unbind();
            if (i % 100 == 0) {
                System.out.println(i + " (" + new Date() + ")");
            }
        }
        bind(false);
    }

    private static class EchoProtocolHandler extends IoHandlerAdapter {
        private static final Logger log = LoggerFactory
                .getLogger(EchoProtocolHandler.class);

        @Override
        public void sessionCreated(IoSession session) {
            if (session.getConfig() instanceof SocketSessionConfig) {
                ((SocketSessionConfig) session.getConfig())
                        .setReceiveBufferSize(2048);
            }

            session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
            log.info("*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE)
                    + " ***");
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            cause.printStackTrace();
            session.close();
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            if (!(message instanceof IoBuffer)) {
                return;
            }

            IoBuffer rb = (IoBuffer) message;
            // Write the received data back to remote peer
            IoBuffer wb = IoBuffer.allocate(rb.remaining());
            wb.put(rb);
            wb.flip();
            session.write(wb);
        }
    }
}
