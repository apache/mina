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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.example.echoserver.ssl.BogusSSLContextFactory;
import org.apache.mina.filter.ssl.SSLFilter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;

/**
 * Tests echo server example.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev:448075 $, $Date:2006-09-20 05:26:53Z $
 */
public abstract class AbstractTest extends TestCase {
    protected boolean useSSL;

    protected int port;

    protected SocketAddress boundAddress;

    protected IoAcceptor datagramAcceptor;

    protected IoAcceptor socketAcceptor;

    protected AbstractTest() {
    }

    protected static void assertEquals(byte[] expected, byte[] actual) {
        assertEquals(toString(expected), toString(actual));
    }

    protected static void assertEquals(ByteBuffer expected, ByteBuffer actual) {
        assertEquals(toString(expected), toString(actual));
    }

    protected static String toString(byte[] buf) {
        StringBuffer str = new StringBuffer(buf.length * 4);
        for (byte element : buf) {
            str.append(element);
            str.append(' ');
        }
        return str.toString();
    }

    protected static String toString(ByteBuffer buf) {
        return buf.getHexDump();
    }

    @Override
    protected void setUp() throws Exception {
        // Disable SSL by default
        useSSL = false;
        final SSLFilter sslFilter = new SSLFilter(BogusSSLContextFactory
                .getInstance(true));

        boundAddress = null;
        datagramAcceptor = new DatagramAcceptor();
        socketAcceptor = new SocketAcceptor();

        ((DatagramSessionConfig) datagramAcceptor.getSessionConfig())
                .setReuseAddress(true);
        ((SocketAcceptor) socketAcceptor).setReuseAddress(true);

        // Find an availble test port and bind to it.
        boolean socketBound = false;
        boolean datagramBound = false;

        // Let's start from port #1 to detect possible resource leak
        // because test will fail in port 1-1023 if user run this test
        // as a normal user.

        SocketAddress address = null;

        for (port = 1; port <= 65535; port++) {
            socketBound = false;
            datagramBound = false;

            address = new InetSocketAddress(port);

            try {
                socketAcceptor.setLocalAddress(address);
                socketAcceptor.setHandler(new EchoProtocolHandler() {
                    @Override
                    public void sessionCreated(IoSession session) {
                        if (useSSL) {
                            session.getFilterChain().addFirst("SSL", sslFilter);
                        }
                    }

                    // This is for TLS reentrance test
                    @Override
                    public void messageReceived(IoSession session,
                            Object message) throws Exception {
                        if (!(message instanceof ByteBuffer)) {
                            return;
                        }

                        ByteBuffer buf = (ByteBuffer) message;
                        if (session.getFilterChain().contains("SSL")
                                && buf.remaining() == 1
                                && buf.get() == (byte) '.') {
                            IoSessionLogger.info(session, "TLS Reentrance");
                            ((SSLFilter) session.getFilterChain().get("SSL"))
                                    .startSSL(session);

                            // Send a response
                            buf = ByteBuffer.allocate(1);
                            buf.put((byte) '.');
                            buf.flip();
                            session
                                    .setAttribute(SSLFilter.DISABLE_ENCRYPTION_ONCE);
                            session.write(buf);
                        } else {
                            super.messageReceived(session, message);
                        }
                    }
                });
                socketAcceptor.bind();
                socketBound = true;

                datagramAcceptor.setLocalAddress(address);
                datagramAcceptor.setHandler(new EchoProtocolHandler());
                datagramAcceptor.bind();
                datagramBound = true;

                break;
            } catch (IOException e) {
            } finally {
                if (socketBound && !datagramBound) {
                    socketAcceptor.unbind();
                }
                if (datagramBound && !socketBound) {
                    datagramAcceptor.unbind();
                }
            }
        }

        // If there is no port available, test fails.
        if (!socketBound || !datagramBound) {
            throw new IOException("Cannot bind any test port.");
        }

        boundAddress = address;
        System.out.println("Using port " + port + " for testing.");
    }

    @Override
    protected void tearDown() throws Exception {
        if (boundAddress != null) {
            socketAcceptor.unbind();
            datagramAcceptor.unbind();
        }
    }
}
