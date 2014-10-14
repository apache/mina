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
package org.apache.mina.transport.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class test the event dispatching of {@link BioUdpServer}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BioUdpServerFilterEventTest {

    private static final Logger LOG = LoggerFactory.getLogger(BioUdpServerFilterEventTest.class);

    private static final int CLIENT_COUNT = 1;

    private static final int WAIT_TIME = 10000;

    private CountDownLatch msgSentLatch = new CountDownLatch(CLIENT_COUNT);

    private CountDownLatch msgReadLatch = new CountDownLatch(CLIENT_COUNT);

    private CountDownLatch openLatch = new CountDownLatch(CLIENT_COUNT);

    private CountDownLatch closedLatch = new CountDownLatch(CLIENT_COUNT);

    @Test
    public void generate_all_kind_of_server_event() throws IOException, InterruptedException {
        final BioUdpServer server = new BioUdpServer();
        server.getSessionConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, 2000);
        server.setFilters(new MyCodec(), new Handler());
        server.bind(0);
        // warm up
        Thread.sleep(100);

        long t0 = System.currentTimeMillis();
        final int port = server.getDatagramChannel().socket().getLocalPort();

        System.err.println("port : " + port);
        final DatagramSocket[] clients = new DatagramSocket[CLIENT_COUNT];

        InetSocketAddress serverAddy = new InetSocketAddress("127.0.0.1", port);
        // connect some clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            try {
                clients[i] = new DatagramSocket();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Creation client " + i + " failed");
            }
        }

        // write some messages
        for (int i = 0; i < CLIENT_COUNT; i++) {
            byte[] data = ("test:" + i).getBytes();
            clients[i].send(new DatagramPacket(data, data.length, serverAddy));
        }

        // does the session open message was fired ?
        assertTrue(openLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // test is message was received by the server
        assertTrue(msgReadLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // does response was wrote and sent ?
        assertTrue(msgSentLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // read the echos
        final byte[] buffer = new byte[1024];

        for (int i = 0; i < CLIENT_COUNT; i++) {
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            clients[i].receive(dp);
            final String text = new String(buffer, 0, dp.getLength());
            assertEquals("test:" + i, text);
        }

        msgReadLatch = new CountDownLatch(CLIENT_COUNT);

        // try again
        // write some messages again
        for (int i = 0; i < CLIENT_COUNT; i++) {
            byte[] data = ("test:" + i).getBytes();
            clients[i].send(new DatagramPacket(data, data.length, serverAddy));
        }

        // test is message was received by the server
        assertTrue(msgReadLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // wait echo

        // close the session
        assertEquals(CLIENT_COUNT, closedLatch.getCount());
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clients[i].close();
        }

        // does the session close event was fired ?
        assertTrue(closedLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        long t1 = System.currentTimeMillis();

        System.out.println("Delta = " + (t1 - t0));

        server.unbind();
    }

    private class MyCodec extends AbstractIoFilter {

        @Override
        public void messageReceived(final IoSession session, final Object message, final ReadFilterChainController controller) {
            if (message instanceof ByteBuffer) {
                final ByteBuffer in = (ByteBuffer) message;
                final byte[] buffer = new byte[in.remaining()];
                in.get(buffer);
                super.messageReceived(session, new String(buffer), controller);
            } else {
                fail();
            }
        }

        @Override
        public void messageWriting(IoSession session, WriteRequest writeRequest, WriteFilterChainController controller) {
            writeRequest.setMessage(ByteBuffer.wrap(writeRequest.getMessage().toString().getBytes()));
            super.messageWriting(session, writeRequest, controller);
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
        public void messageReceived(final IoSession session, final Object message, final ReadFilterChainController controller) {
            LOG.info("** message received {}", message);
            msgReadLatch.countDown();
            session.write(message.toString());
        }

        @Override
        public void messageSent(final IoSession session, final Object message) {
            LOG.info("** message sent {}", message);
            msgSentLatch.countDown();
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
            LOG.info("** sesssion idle {}", session);
            session.close(false);
        }
    }
}
