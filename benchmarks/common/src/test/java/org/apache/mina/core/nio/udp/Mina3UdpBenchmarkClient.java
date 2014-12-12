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
package org.apache.mina.core.nio.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.core.BenchmarkClient;
import org.apache.mina.transport.nio.NioUdpClient;

/**
 * A MINA 3 based UDP client
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Mina3UdpBenchmarkClient implements BenchmarkClient {
    // The UDP client
    private NioUdpClient udpClient;

    /**
     * {@inheritDoc}
     */
    public void start(int port, final CountDownLatch counter, final byte[] data) throws IOException {
        udpClient = new NioUdpClient();
        udpClient.setIoHandler(new AbstractIoHandler() {
            private void sendMessage(IoSession session, byte[] data) {
                ByteBuffer iobuf = ByteBuffer.wrap(data);
                session.write(iobuf);
            }

            public void sessionOpened(IoSession session) {
                sendMessage(session, data);
            }

            public void messageReceived(IoSession session, Object message) {
                if (message instanceof ByteBuffer) {
                    ByteBuffer buffer = (ByteBuffer) message;
                    for (int i = 0; i < buffer.remaining(); ++i) {
                        counter.countDown();
                        long count = counter.getCount();
                        if (count > 0) {
                            sendMessage(session, data);
                        }
                    }
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
            }

            @Override
            public void serviceActivated(IoService service) {
            }

            @Override
            public void serviceInactivated(IoService service) {
            }
        });

        IoFuture<IoSession> future = udpClient.connect(new InetSocketAddress(port));

        try {
            IoSession session = future.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * {@inheritedDoc}
     */
    public void stop() throws IOException {
    }
}
