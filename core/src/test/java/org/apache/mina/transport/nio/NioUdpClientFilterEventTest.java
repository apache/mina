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
package org.apache.mina.transport.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class test the event dispatching of {@link NioUdpClient}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpClientFilterEventTest {

    private static final Logger LOG = LoggerFactory.getLogger(NioUdpClientFilterEventTest.class);

    private static final int CLIENT_COUNT = 10;

    private static final int WAIT_TIME = 30000;

    private final CountDownLatch msgSentLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch msgReadLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch openLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch idleLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch closedLatch = new CountDownLatch(CLIENT_COUNT);

    /**
     * Create an old IO server and use a bunch of MINA client on it. Test if the
     * events occurs correctly in the different IoFilters.
     */
    @Test
    public void generate_all_kind_of_client_event() throws IOException, InterruptedException, ExecutionException {
        NioUdpClient client = new NioUdpClient();
        client.getSessionConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, 2000);

        client.setFilters(new MyCodec(), new Handler());

        DatagramSocket serverSocket = new DatagramSocket();
        int port = serverSocket.getLocalPort();

        // warm up
        Thread.sleep(100);
        final long t0 = System.currentTimeMillis();

        // now connect the clients

        List<IoFuture<IoSession>> cf = new ArrayList<IoFuture<IoSession>>();
        for (int i = 0; i < CLIENT_COUNT; i++) {
            cf.add(client.connect(new InetSocketAddress("localhost", port)));
        }

        // does the session open message was fired ?
        assertTrue(openLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // gather sessions from futures
        IoSession[] sessions = new IoSession[CLIENT_COUNT];
        for (int i = 0; i < CLIENT_COUNT; i++) {
            sessions[i] = cf.get(i).get();
            assertNotNull(sessions[i]);
        }

        // receive and send back some message
        for (int i = 0; i < CLIENT_COUNT; i++) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String sentence = new String(receivePacket.getData());
            LOG.info("RECEIVED  :" + sentence);

            InetAddress IPAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            DatagramPacket sendPacket = new DatagramPacket("tata".getBytes(), "tata".getBytes().length, IPAddress, clientPort);
            serverSocket.send(sendPacket);
        }

        // does response was wrote and sent ?
        assertTrue(msgSentLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // test is message was received by the client
        assertTrue(msgReadLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // the session idled
        assertEquals(CLIENT_COUNT, idleLatch.getCount());

        // close the session
        assertEquals(CLIENT_COUNT, closedLatch.getCount());
        serverSocket.close();

        // does the session close event was fired ?
        assertTrue(closedLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        long t1 = System.currentTimeMillis();

        System.out.println("Delta = " + (t1 - t0));

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
            session.write("toto");
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
        }

        @Override
        public void messageSent(final IoSession session, final Object message) {
            LOG.info("** message sent {}", message);
            msgSentLatch.countDown();
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
            LOG.info("** session idle {}", session);
            idleLatch.countDown();
            session.close(true);
        }
    }
}
