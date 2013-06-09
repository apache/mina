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

package org.apache.mina.examples.echoserver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.nio.NioTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple TCP server, write back to the client every received messages.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class NioEchoServer {

    static final private Logger LOG = LoggerFactory.getLogger(NioEchoServer.class);

    public static void main(final String[] args) {
        LOG.info("starting echo server");

        final NioTcpServer acceptor = new NioTcpServer();

        // create the filter chain for this service
        acceptor.setFilters(new LoggingFilter("LoggingFilter1"));

        acceptor.setIoHandler(new AbstractIoHandler() {
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
            public void messageReceived(IoSession session, Object message) {
                if (message instanceof ByteBuffer) {
                    LOG.info("echoing");
                    session.write(message);
                }
            }
        });
        try {
            final SocketAddress address = new InetSocketAddress(9999);
            acceptor.bind(address);
            LOG.debug("Running the server for 25 sec");
            Thread.sleep(25000);
            LOG.debug("Unbinding the TCP port");
            acceptor.unbind();
        } catch (final InterruptedException e) {
            LOG.error("Interrupted exception", e);
        }
    }
}