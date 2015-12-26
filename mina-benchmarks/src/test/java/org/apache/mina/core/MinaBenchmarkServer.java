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
package org.apache.mina.core;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MinaBenchmarkServer implements BenchmarkServer {

    private static enum State {
        WAIT_FOR_FIRST_BYTE_LENGTH, WAIT_FOR_SECOND_BYTE_LENGTH, WAIT_FOR_THIRD_BYTE_LENGTH, WAIT_FOR_FOURTH_BYTE_LENGTH, READING
    }

    private static final IoBuffer ACK = IoBuffer.allocate(1);

    static {
        ACK.put((byte) 0);
        ACK.rewind();
    }

    private static final String STATE_ATTRIBUTE = MinaBenchmarkServer.class.getName() + ".state";

    private static final String LENGTH_ATTRIBUTE = MinaBenchmarkServer.class.getName() + ".length";

    private IoAcceptor acceptor;

    /**
     * {@inheritDoc}
     */
    public void start(int port) throws IOException {
        acceptor = new NioSocketAcceptor(2 * Runtime.getRuntime().availableProcessors());
        ((NioSocketAcceptor) acceptor).getSessionConfig().setReadBufferSize(128 * 1024);
        ((NioSocketAcceptor) acceptor).getSessionConfig().setTcpNoDelay(true);
        acceptor.setHandler(new IoHandlerAdapter() {
            public void sessionOpened(IoSession session) throws Exception {
                session.setAttribute(STATE_ATTRIBUTE, State.WAIT_FOR_FIRST_BYTE_LENGTH);
            }

            public void messageReceived(IoSession session, Object message) throws Exception {
                if (message instanceof IoBuffer) {
                    IoBuffer buffer = (IoBuffer) message;

                    State state = (State) session.getAttribute(STATE_ATTRIBUTE);
                    int length = 0;
                    if (session.containsAttribute(LENGTH_ATTRIBUTE)) {
                        length = (Integer) session.getAttribute(LENGTH_ATTRIBUTE);
                    }
                    while (buffer.remaining() > 0) {
                        switch (state) {
                        case WAIT_FOR_FIRST_BYTE_LENGTH:
                            length = (buffer.get() & 0xFF) << 24;
                            state = State.WAIT_FOR_SECOND_BYTE_LENGTH;
                            break;
                        case WAIT_FOR_SECOND_BYTE_LENGTH:
                            length += (buffer.get() & 0xFF) << 16;
                            state = State.WAIT_FOR_THIRD_BYTE_LENGTH;
                            break;
                        case WAIT_FOR_THIRD_BYTE_LENGTH:
                            length += (buffer.get() & 0xFF) << 8;
                            state = State.WAIT_FOR_FOURTH_BYTE_LENGTH;
                            break;
                        case WAIT_FOR_FOURTH_BYTE_LENGTH:
                            length += (buffer.get() & 0xFF);
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
                                buffer.skip(remaining);
                            } else {
                                buffer.skip(length);
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

            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                cause.printStackTrace();
            }
        });
        acceptor.bind(new InetSocketAddress(port));
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        acceptor.dispose(true);
    }
}
