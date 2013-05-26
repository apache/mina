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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpServer;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Run a TCP server and wait for idle events to be generated.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IdleTcpServerTest {

    private static final int CLIENT_COUNT = 3;

    @BeforeClass
    public static void setup() {
        // BasicConfigurator.configure();
    }

    @Test
    public void readIdleTest() throws IOException {
        final NioTcpServer server = new NioTcpServer();

        final CountDownLatch idleLatch = new CountDownLatch(CLIENT_COUNT);

        // 3 seconds idle time
        server.getSessionConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, 2000);

        // start the server
        server.bind(new InetSocketAddress(0));

        final int boundPort = server.getServerSocketChannel().socket().getLocalPort();
        server.setFilters(new IdleHandler(idleLatch));

        // fire the clients and let them idle
        final Socket[] clients = new Socket[CLIENT_COUNT];

        for (int i = 0; i < CLIENT_COUNT; i++) {
            clients[i] = new Socket("127.0.0.1", boundPort);
        }

        long start = System.currentTimeMillis();
        try {
            assertTrue("idle event missing ! ", idleLatch.await(4, TimeUnit.SECONDS));
            System.err.println((System.currentTimeMillis() - start));
            assertTrue(2000 <= (System.currentTimeMillis() - start));
        } catch (final InterruptedException e) {
            fail(e.getMessage());
        }
    }

    private class IdleHandler extends AbstractIoFilter {

        private final CountDownLatch latch;

        public IdleHandler(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void sessionIdle(final IoSession session, final IdleStatus status) {
            if (status == IdleStatus.READ_IDLE) {
                // happy
                latch.countDown();
                session.close(true);
            }
        }
    }
}
