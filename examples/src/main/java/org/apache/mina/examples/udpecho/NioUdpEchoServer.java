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
package org.apache.mina.examples.udpecho;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoServiceListener;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.transport.nio.NioSelectorLoop;
import org.apache.mina.transport.nio.NioUdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UDP base echo server sending back every datagram received
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioUdpEchoServer {
    static final Logger LOG = LoggerFactory.getLogger(NioUdpEchoServer.class);

    public static void main(final String[] args) {
        LOG.info("starting echo server");

        final NioUdpServer server = new NioUdpServer(new NioSelectorLoop());

        // create the fitler chain for this service
        server.setFilters(new LoggingFilter("LoggingFilter1"), new IoFilter() {

            @Override
            public void sessionOpened(final IoSession session) {
                LOG.info("session {} open", session);
            }

            @Override
            public void sessionIdle(final IoSession session, final IdleStatus status) {
                LOG.info("session {} idle", session);
            }

            @Override
            public void sessionClosed(final IoSession session) {
                LOG.info("session {} open", session);
            }

            @Override
            public void messageWriting(final IoSession session, final Object message,
                    final WriteFilterChainController controller) {
                // we just push the message in the chain
                controller.callWriteNextFilter(message);
            }

            @Override
            public void messageReceived(final IoSession session, final Object message,
                    final ReadFilterChainController controller) {

                if (message instanceof ByteBuffer) {
                    LOG.info("echoing");
                    session.write(message);
                }
            }
        });

        server.addListeners(new IoServiceListener() {

            @Override
            public void sessionDestroyed(final IoSession session) {
                LOG.info("session destroyed {}", session);

            }

            @Override
            public void sessionCreated(final IoSession session) {
                LOG.info("session created {}", session);

                final String welcomeStr = "welcome\n";
                final ByteBuffer bf = ByteBuffer.allocate(welcomeStr.length());
                bf.put(welcomeStr.getBytes());
                bf.flip();
                session.write(bf);
            }

            @Override
            public void serviceInactivated(final IoService service) {
                LOG.info("service deactivated {}", service);
            }

            @Override
            public void serviceActivated(final IoService service) {
                LOG.info("service activated {}", service);
            }
        });

        try {
            final SocketAddress address = new InetSocketAddress(9999);
            server.bind(address);
            LOG.debug("Running the server for 25 sec");
            Thread.sleep(25000);
            LOG.debug("Unbinding the UDP port");
            server.unbind();
        } catch (final IOException e) {
            LOG.error("I/O exception", e);
        } catch (final InterruptedException e) {
            LOG.error("Interrupted exception", e);
        }
    }
}
