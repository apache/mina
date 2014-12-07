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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpClient;
import org.apache.mina.transport.nio.NioTcpServer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class test the resource management of {@link NioTcpClient}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpClientReleaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(NioTcpClientReleaseTest.class);

    private static final int CLIENT_COUNT = 10;

    private static final int WAIT_TIME = 30000;

    private final CountDownLatch closedLatch = new CountDownLatch(CLIENT_COUNT);

    /**
     * Create an old IO server and use a bunch of MINA client on it. Test if the events occurs correctly in the
     * different IoFilters.
     */
    @Test
    public void checkSessionsAreClosedWhenClientIsDisconnected() throws IOException, InterruptedException,
            ExecutionException {

        NioTcpServer server = new NioTcpServer();
        server.setIoHandler(new Handler());
        server.bind(0);

        NioTcpClient client = new NioTcpClient();
        client.setIoHandler(new AbstractIoHandler() {
        });
        for (int i = 0; i < CLIENT_COUNT; ++i) {
            client.connect(new InetSocketAddress(server.getServerSocketChannel().socket().getLocalPort())).get();
        }
        client.disconnect();
        assertTrue(closedLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));
    }

    private class Handler extends AbstractIoHandler {

        @Override
        public void sessionClosed(final IoSession session) {
            LOG.info("** session closed");
            closedLatch.countDown();
        }
    }
    
    /**
     * Test added for DIRMINA-999
     */
    @Test
    public void checkSessionCloseEventIsSentClientSideWhenImmediateIsFalse() throws IOException, InterruptedException,
            ExecutionException {

        NioTcpServer server = new NioTcpServer();
        server.bind(0);

        NioTcpClient client = new NioTcpClient();
        final CountDownLatch closeCounter = new CountDownLatch(1);
        client.setIoHandler(new AbstractIoHandler() {

            @Override
            public void sessionOpened(IoSession session) {
                session.close(false);
            }

            @Override
            public void sessionClosed(IoSession session) {
                closeCounter.countDown();
            }
            
        });
        client.connect(new InetSocketAddress(server.getServerSocketChannel().socket().getLocalPort()));
        assertTrue(closeCounter.await(WAIT_TIME, TimeUnit.MILLISECONDS));
    }
    
}
