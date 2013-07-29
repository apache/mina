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

package org.apache.mina.monitoring;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.nio.NioTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 *
 */
public class NioTcpEchoServerWithMonitoring {

    static final Logger LOG = LoggerFactory.getLogger(NioTcpEchoServerWithMonitoring.class);

    public static void main(final String[] args) {
        LOG.info("starting echo server");

        final NioTcpServer server = new NioTcpServer();
        final MetricRegistry metrics = new MetricRegistry();
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();
        server.getSessionConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, 60*600*1000);
        server.getSessionConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE, 60*600*1000);

        // create the filter chain for this service
        server.setFilters(new MonitoringFilter(metrics), new LoggingFilter("LoggingFilter1"), new IoFilter() {

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
            public void messageWriting(final IoSession session, WriteRequest message,
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

            @Override
            public void messageSent(final IoSession session, final Object message) {
                LOG.info("message {} sent", message);
            }
        });
        server.setIoHandler(new AbstractIoHandler() {
            @Override
            public void sessionOpened(final IoSession session) {
                LOG.info("session opened {}", session);

                final String welcomeStr = "welcome\n";
                final ByteBuffer bf = ByteBuffer.allocate(welcomeStr.length());
                bf.put(welcomeStr.getBytes());
                bf.flip();
                session.write(bf);

            }

            @Override
            public void exceptionCaught(IoSession session, Exception cause) {
                cause.printStackTrace();
            }
        });

        try {
            final SocketAddress address = new InetSocketAddress(51000);
            server.bind(address);
            LOG.debug("Running the server for 25 sec");
            Thread.sleep(25000 * 1000);
            LOG.debug("Unbinding the UDP port");
            server.unbind();
        } catch (final InterruptedException e) {
            LOG.error("Interrupted exception", e);
        } finally {
            reporter.stop();
        }
    }

}
