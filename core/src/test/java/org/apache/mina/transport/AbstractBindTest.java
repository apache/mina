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
import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
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
                acceptor.setLocalAddress(createSocketAddress(port));
                acceptor.bind();
                socketBound = true;
                break;
            } catch (IOException e) {
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
            acceptor.unbind();
        } catch (Exception e) {
            // ignore
        }

        acceptor.setLocalAddress(null);
    }

    public void testAnonymousBind() throws Exception {
        acceptor.setHandler(new IoHandlerAdapter());
        acceptor.setLocalAddress(null);
        acceptor.bind();
        Assert.assertNotNull(acceptor.getLocalAddress());
        acceptor.unbind();
        acceptor.setLocalAddress(createSocketAddress(0));
        acceptor.bind();
        Assert.assertNotNull(acceptor.getLocalAddress());
        Assert.assertTrue(getPort(acceptor.getLocalAddress()) != 0);
        acceptor.unbind();
    }

    public void testDuplicateBind() throws IOException {
        bind(false);

        try {
            acceptor.bind();
            Assert.fail("IllegalStateException is not thrown");
        } catch (IllegalStateException e) {
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

    public void _testRegressively() throws IOException {
        setReuseAddress(true);

        SocketAddress addr = createSocketAddress(port);
        EchoProtocolHandler handler = new EchoProtocolHandler();
        acceptor.setLocalAddress(addr);
        acceptor.setHandler(handler);
        for (int i = 0; i < 1048576; i++) {
            acceptor.bind();
            testDuplicateBind();
            testDuplicateUnbind();
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
            if (!(message instanceof ByteBuffer)) {
                return;
            }

            ByteBuffer rb = (ByteBuffer) message;
            // Write the received data back to remote peer
            ByteBuffer wb = ByteBuffer.allocate(rb.remaining());
            wb.put(rb);
            wb.flip();
            session.write(wb);
        }
    }
}
