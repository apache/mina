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
package org.apache.mina.example.echoserver;

import java.net.InetSocketAddress;

import junit.framework.Assert;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.example.echoserver.ssl.BogusSslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Tests echo server example.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev:448075 $, $Date:2006-09-20 05:26:53Z $
 */
public class ConnectorTest extends AbstractTest {
    private static final int TIMEOUT = 10000; // 10 seconds

    private final int COUNT = 10;

    private final int DATA_SIZE = 16;

    private SslFilter connectorSSLFilter;

    public ConnectorTest() {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        connectorSSLFilter = new SslFilter(BogusSslContextFactory
                .getInstance(false));
        connectorSSLFilter.setUseClientMode(true); // set client mode
    }

    public void testTCP() throws Exception {
        IoConnector connector = new NioSocketConnector();
        testConnector(connector);
    }

    public void testTCPWithSSL() throws Exception {
        useSSL = true;
        // Create a connector
        IoConnector connector = new NioSocketConnector();

        // Add an SSL filter to connector
        connector.getFilterChain().addLast("SSL", connectorSSLFilter);
        testConnector(connector);
    }

    public void testUDP() throws Exception {
        IoConnector connector = new NioDatagramConnector();
        testConnector(connector);
    }

    private void testConnector(IoConnector connector) throws Exception {
        System.out.println("* Without localAddress");
        testConnector(connector, false);

        System.out.println("* With localAddress");
        testConnector(connector, true);
    }

    private void testConnector(IoConnector connector, boolean useLocalAddress)
            throws Exception {
        EchoConnectorHandler handler = new EchoConnectorHandler();

        IoSession session = null;
        if (!useLocalAddress) {
            connector.setHandler(handler);
            ConnectFuture future = connector.connect(new InetSocketAddress(
                    "localhost", port));
            future.awaitUninterruptibly();
            session = future.getSession();
        } else {
            int clientPort = port;
            for (int i = 0; i < 65536; i++) {
                clientPort = AvailablePortFinder
                        .getNextAvailable(clientPort + 1);
                try {
                    connector.setHandler(handler);
                    ConnectFuture future = connector.connect(
                            new InetSocketAddress("localhost", port),
                            new InetSocketAddress(clientPort));
                    future.awaitUninterruptibly();
                    session = future.getSession();
                    break;
                } catch (RuntimeIoException e) {
                    // Try again until we succeed to bind.
                }
            }

            if (session == null) {
                Assert.fail("Failed to find out an appropriate local address.");
            }
        }

        // Run a basic connector test.
        testConnector0(session);

        // Send closeNotify to test TLS closure if it is TLS connection.
        if (useSSL) {
            connectorSSLFilter.stopSsl(session).awaitUninterruptibly();

            System.out
                    .println("-------------------------------------------------------------------------------");
            // Test again after we finished TLS session.
            testConnector0(session);

            System.out
                    .println("-------------------------------------------------------------------------------");

            // Test if we can enter TLS mode again.
            //// Send StartTLS request.
            handler.readBuf.clear();
            IoBuffer buf = IoBuffer.allocate(1);
            buf.put((byte) '.');
            buf.flip();
            session.write(buf).awaitUninterruptibly();

            //// Wait for StartTLS response.
            waitForResponse(handler, 1);

            handler.readBuf.flip();
            Assert.assertEquals(1, handler.readBuf.remaining());
            Assert.assertEquals((byte) '.', handler.readBuf.get());

            // Now start TLS connection
            Assert.assertTrue(connectorSSLFilter.startSsl(session));
            testConnector0(session);
        }

        session.close().awaitUninterruptibly();
    }

    private void testConnector0(IoSession session) throws InterruptedException {
        EchoConnectorHandler handler = (EchoConnectorHandler) session
                .getHandler();
        IoBuffer readBuf = handler.readBuf;
        readBuf.clear();
        WriteFuture writeFuture = null;
        for (int i = 0; i < COUNT; i++) {
            IoBuffer buf = IoBuffer.allocate(DATA_SIZE);
            buf.limit(DATA_SIZE);
            fillWriteBuffer(buf, i);
            buf.flip();

            writeFuture = session.write(buf);

            if (session.getService().getTransportMetadata().isConnectionless()) {
                // This will align message arrival order in connectionless transport types
                waitForResponse(handler, (i + 1) * DATA_SIZE);
            }
        }

        writeFuture.awaitUninterruptibly();

        waitForResponse(handler, DATA_SIZE * COUNT);

        // Assert data
        //// Please note that BufferOverflowException can be thrown
        //// in SocketIoProcessor if there was a read timeout because
        //// we share readBuf.
        readBuf.flip();
        IoSessionLogger.getLogger(session).info("readBuf: " + readBuf);
        Assert.assertEquals(DATA_SIZE * COUNT, readBuf.remaining());
        IoBuffer expectedBuf = IoBuffer.allocate(DATA_SIZE * COUNT);
        for (int i = 0; i < COUNT; i++) {
            expectedBuf.limit((i + 1) * DATA_SIZE);
            fillWriteBuffer(expectedBuf, i);
        }
        expectedBuf.position(0);

        assertEquals(expectedBuf, readBuf);
    }

    private void waitForResponse(EchoConnectorHandler handler, int bytes)
            throws InterruptedException {
        for (int j = 0; j < TIMEOUT / 10; j++) {
            if (handler.readBuf.position() >= bytes) {
                break;
            }
            Thread.sleep(10);
        }

        Assert.assertEquals(bytes, handler.readBuf.position());
    }

    private void fillWriteBuffer(IoBuffer writeBuf, int i) {
        while (writeBuf.remaining() > 0) {
            writeBuf.put((byte) i++);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ConnectorTest.class);
    }

    private static class EchoConnectorHandler extends IoHandlerAdapter {
        private final IoBuffer readBuf = IoBuffer.allocate(1024);

        private EchoConnectorHandler() {
            readBuf.setAutoExpand(true);
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            readBuf.put((IoBuffer) message);
        }

        @Override
        public void messageSent(IoSession session, Object message) {
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            cause.printStackTrace();
        }
    }
}
