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
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MinaBenchmarkClient implements BenchmarkClient {

    private static final Random random = new Random();

    private IoConnector connector;

    /**
     * {@inheritDoc}
     */
    public void start(int port, final CountDownLatch counter, final byte[] data) throws IOException {
        connector = new NioSocketConnector(2 * Runtime.getRuntime().availableProcessors());
        ((SocketConnector) connector).getSessionConfig().setSendBufferSize(64 * 1024);
        ((SocketConnector) connector).getSessionConfig().setTcpNoDelay(true);
        connector.setHandler(new IoHandlerAdapter() {
            private void sendMessage(IoSession session, byte[] data) throws IOException {
                IoBuffer iobuf = IoBuffer.wrap(data);
                session.write(iobuf);
            }

            public void sessionOpened(IoSession session) throws Exception {
                sendMessage(session, data);
            }

            public void messageReceived(IoSession session, Object message) throws Exception {
                if (message instanceof IoBuffer) {
                    IoBuffer buffer = (IoBuffer) message;
                    //System.out.println("length="+buffer.remaining());
                    for (int i = 0; i < buffer.remaining(); ++i) {
                        counter.countDown();
                        if (counter.getCount() > 0) {
                            sendMessage(session, data);
                        }
                    }
                }
            }

            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                cause.printStackTrace();
            }
        });
        connector.connect(new InetSocketAddress(port));
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
        connector.dispose(true);
    }
}
