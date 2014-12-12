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
package org.apache.mina.core.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.core.BenchmarkServer;
import org.apache.mina.core.CounterFilter;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.transport.nio.FixedSelectorLoopPool;
import org.apache.mina.transport.nio.NioTcpServer;

/**
 * A MINA 3 TCP Server.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Mina3TcpBenchmarkServer implements BenchmarkServer {

    private static enum State {
        WAIT_FOR_FIRST_BYTE_LENGTH, WAIT_FOR_SECOND_BYTE_LENGTH, WAIT_FOR_THIRD_BYTE_LENGTH, WAIT_FOR_FOURTH_BYTE_LENGTH, READING
    }

    private static final ByteBuffer ACK = ByteBuffer.allocate(1);

    static {
        ACK.put((byte) 0);
        ACK.rewind();
    }

    private static final AttributeKey<State> STATE_ATTRIBUTE = new AttributeKey<State>(State.class,
            Mina3TcpBenchmarkServer.class.getName() + ".state");

    private static final AttributeKey<Integer> LENGTH_ATTRIBUTE = new AttributeKey<Integer>(Integer.class,
            Mina3TcpBenchmarkServer.class.getName() + ".length");

    private NioTcpServer tcpServer;

    /**
     * {@inheritDoc}
     */
    public void start(int port) throws IOException {
        tcpServer = new NioTcpServer(new FixedSelectorLoopPool("Server", 1), null);
        tcpServer.getSessionConfig().setReadBufferSize(128 * 1024);
        tcpServer.getSessionConfig().setTcpNoDelay(true);
        tcpServer.setIoHandler(new AbstractIoHandler() {
            public void sessionOpened(IoSession session) {
                session.setAttribute(STATE_ATTRIBUTE, State.WAIT_FOR_FIRST_BYTE_LENGTH);
            }

            public void messageReceived(IoSession session, Object message) {
                if (message instanceof ByteBuffer) {
                    ByteBuffer buffer = (ByteBuffer) message;

                    State state = session.getAttribute(STATE_ATTRIBUTE);
                    int length = 0;

                    if (session.getAttribute(LENGTH_ATTRIBUTE) != null) {
                        length = session.getAttribute(LENGTH_ATTRIBUTE);
                    }

                    while (buffer.remaining() > 0) {
                        switch (state) {
                        case WAIT_FOR_FIRST_BYTE_LENGTH:
                            length = (buffer.get() & 255) << 24;
                            state = State.WAIT_FOR_SECOND_BYTE_LENGTH;
                            break;
                        case WAIT_FOR_SECOND_BYTE_LENGTH:
                            length += (buffer.get() & 255) << 16;
                            state = State.WAIT_FOR_THIRD_BYTE_LENGTH;
                            break;
                        case WAIT_FOR_THIRD_BYTE_LENGTH:
                            length += (buffer.get() & 255) << 8;
                            state = State.WAIT_FOR_FOURTH_BYTE_LENGTH;
                            break;
                        case WAIT_FOR_FOURTH_BYTE_LENGTH:
                            length += (buffer.get() & 255);
                            state = State.READING;
                            if ((length == 0) && (buffer.remaining() == 0)) {
                                session.write(ACK.slice());
                                state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                            }
                            break;
                        case READING:
                            int remaining = buffer.remaining();
                            if (length > remaining) {
                                length -= remaining;
                                buffer.position(buffer.position() + remaining);
                            } else {
                                buffer.position(buffer.position() + length);
                                session.write(ACK.slice());
                                state = State.WAIT_FOR_FIRST_BYTE_LENGTH;
                                length = 0;
                            }
                        }
                    }
                    session.setAttribute(LENGTH_ATTRIBUTE, length);
                    session.setAttribute(STATE_ATTRIBUTE, state);
                }
            }

            public void exceptionCaught(IoSession session, Exception cause) {
                cause.printStackTrace();
            }

            @Override
            public void sessionClosed(IoSession session) {
            }

            @Override
            public void sessionIdle(IoSession session, IdleStatus status) {
            }

            @Override
            public void messageSent(IoSession session, Object message) {
                CounterFilter.messageSent.getAndIncrement();
            }

            @Override
            public void serviceActivated(IoService service) {
            }

            @Override
            public void serviceInactivated(IoService service) {
            }
        });

        tcpServer.bind(new InetSocketAddress(port));
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        tcpServer.unbind();
    }
}
