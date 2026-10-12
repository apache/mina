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
package org.apache.mina.transport.socket.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.AcceptorBindUtil;
import org.junit.Test;

public class UseReadOperationTest {

    @Test
    public void shouldDeliverMessageViaReadFutureWhenUseReadOperationIsTrueBeforeConnect() {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) {
                IoBuffer buffer = IoBuffer.allocate(1);
                buffer.put((byte) 125);
                buffer.rewind();
                session.write(buffer);
            }

        });

        int port = AcceptorBindUtil.tryBind(acceptor);

        try {
            IoConnector connector = new NioSocketConnector();
            connector.getSessionConfig().setUseReadOperation(true);
            connector.setHandler(new IoHandlerAdapter());

            try {
                ConnectFuture connectFuture = connector.connect(new InetSocketAddress("localhost", port));
                assertTrue("Connect failed", connectFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                IoSession session = connectFuture.getSession();

                ReadFuture readFuture = session.read();
                assertTrue("Read failed", readFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                IoBuffer message = (IoBuffer) readFuture.getMessage();
                assertEquals(1, message.remaining());
                assertEquals(125, message.get());
            } finally {
                connector.dispose();
            }
        } finally {
            acceptor.dispose();
        }
    }

    @Test
    public void shouldDeliverMessageViaReadFutureOrHandler() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) {
                IoBuffer buffer = IoBuffer.allocate(1);
                buffer.put((byte) 125);
                buffer.rewind();
                session.write(buffer);
            }
        });

        int port = AcceptorBindUtil.tryBind(acceptor);

        AtomicReference<IoBuffer> receivedMessage = new AtomicReference<>();
        CountDownLatch receiveViaHandler = new CountDownLatch(1);

        try {
            IoConnector connector = new NioSocketConnector();
            connector.setHandler(new IoHandlerAdapter() {
                @Override
                public void messageReceived(IoSession session, Object message) {
                    receivedMessage.setOpaque((IoBuffer) message);
                    receiveViaHandler.countDown();
                }
            });

            try {
                ConnectFuture connectFuture = connector.connect(new InetSocketAddress("localhost", port));
                assertTrue("Connect failed", connectFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                connectFuture.getSession().getConfig().setUseReadOperation(true);

                ReadFuture readFuture = connectFuture.getSession().read();
                boolean readReceived = readFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS);
                IoBuffer message;

                if (readReceived) {
                    // message received via read future (useReadOperation flag was picked up before message delivery)
                    message = (IoBuffer) readFuture.getMessage();
                } else {
                    // message received via IO handler (useReadOperation flag was not picked up before message delivery)
                    assertTrue("Message handler receive failed", receiveViaHandler.await(4L, TimeUnit.SECONDS));
                    message = receivedMessage.getOpaque();
                }

                assertEquals(1, message.remaining());
                assertEquals(125, message.get());
            } finally {
                connector.dispose();
            }
        } finally {
            acceptor.dispose();
        }
    }

    @Test
    public void shouldDeliverMessageViaReadFutureWhenUseReadOperationIsTrue() {
        AtomicBoolean readOperationSet = new AtomicBoolean();

        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) {
                long deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(4L);

                while (true) {

                    if (readOperationSet.getOpaque()) {
                        break;
                    }

                    if (System.nanoTime() >= deadlineNs) {
                        return;
                    }

                    Thread.yield();
                }

                IoBuffer buffer = IoBuffer.allocate(1);
                buffer.put((byte) 125);
                buffer.rewind();
                session.write(buffer);
            }
        });

        int port = AcceptorBindUtil.tryBind(acceptor);

        try {
            IoConnector connector = new NioSocketConnector();
            connector.setHandler(new IoHandlerAdapter());

            try {
                ConnectFuture connectFuture = connector.connect(new InetSocketAddress("localhost", port));
                assertTrue("Connect failed", connectFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                connectFuture.getSession().getConfig().setUseReadOperation(true);
                readOperationSet.setOpaque(true);

                ReadFuture readFuture = connectFuture.getSession().read();
                assertTrue("Message receive failed", readFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                IoBuffer message = (IoBuffer) readFuture.getMessage();
                assertEquals(1, message.remaining());
                assertEquals(125, message.get());
            } finally {
                connector.dispose();
            }
        } finally {
            acceptor.dispose();
        }
    }
}
