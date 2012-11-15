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
package org.apache.mina.transport.tcp;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.transport.nio.NioTcpServer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class test the event dispatching of {@link NioTcpServer}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpServerFilterEventTest {

    private static final Logger LOG = LoggerFactory.getLogger(NioTcpServerFilterEventTest.class);

    private static final int CLIENT_COUNT = 50;

    private final CountDownLatch msgSentLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch msgReadLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch openLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch closedLatch = new CountDownLatch(CLIENT_COUNT);

    @Test
    public void generate_all_kind_of_server_event() throws IOException, InterruptedException {
        final NioTcpServer server = new NioTcpServer();
        server.setFilters(new MyCodec(), new Handler());
        server.bind(0);
        // warm up
        Thread.sleep(100);

        final int port = server.getServerSocketChannel().socket().getLocalPort();

        final Socket[] clients = new Socket[CLIENT_COUNT];

        // connect some clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clients[i] = new Socket("127.0.0.1", port);
        }

        // does the session open message was fired ?
        assertTrue(openLatch.await(200, TimeUnit.MILLISECONDS));

        // write some messages
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clients[i].getOutputStream().write(("test:" + i).getBytes());
            clients[i].getOutputStream().flush();
        }

        // test is message was received by the server
        assertTrue(msgReadLatch.await(200, TimeUnit.MILLISECONDS));

        // does response was wrote and sent ?
        assertTrue(msgSentLatch.await(200, TimeUnit.MILLISECONDS));

        // read the echos
        final byte[] buffer = new byte[1024];

        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int bytes = clients[i].getInputStream().read(buffer);
            final String text = new String(buffer, 0, bytes);
            assertEquals("test:" + i, text);
        }

        // close the session
        assertEquals(CLIENT_COUNT, closedLatch.getCount());
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clients[i].close();
        }

        // does the session close event was fired ?
        assertTrue(closedLatch.await(200, TimeUnit.MILLISECONDS));

        server.unbind();
    }

    private class MyCodec extends AbstractIoFilter {

        @Override
        public void messageReceived(final IoSession session, final Object message,
                final ReadFilterChainController controller) {
            if (message instanceof ByteBuffer) {
                final ByteBuffer in = (ByteBuffer) message;
                final byte[] buffer = new byte[in.remaining()];
                in.get(buffer);
                controller.callReadNextFilter(new String(buffer));
            } else {
                fail();
            }
        }

        @Override
        public void messageWriting(final IoSession session, final Object message,
                final WriteFilterChainController controller) {
            controller.callWriteNextFilter(ByteBuffer.wrap(message.toString().getBytes()));
        }
    }

    private class Handler extends AbstractIoFilter {

        @Override
        public void sessionOpened(final IoSession session) {
            LOG.info("** session open");
            openLatch.countDown();
        }

        @Override
        public void sessionClosed(final IoSession session) {
            LOG.info("** session closed");
            closedLatch.countDown();
        }

        @Override
        public void messageReceived(final IoSession session, final Object message,
                final ReadFilterChainController controller) {
            LOG.info("** message received {}", message);
            msgReadLatch.countDown();
            session.write(message.toString());
        }

        @Override
        public void messageSent(final IoSession session, final Object message) {
            LOG.info("** message sent {}", message);
            msgSentLatch.countDown();
        }
    }
}
