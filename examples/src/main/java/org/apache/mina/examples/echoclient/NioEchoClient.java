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

package org.apache.mina.examples.echoclient;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.examples.echoserver.NioEchoServer;
import org.apache.mina.transport.nio.NioTcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple TCP client, write back to the client every received messages.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class NioEchoClient {

    static final private Logger LOG = LoggerFactory.getLogger(NioEchoServer.class);

    public static void main(String[] args) {
        LOG.info("starting echo client");

        final NioTcpClient client = new NioTcpClient();
        client.setFilters();
        client.setIoHandler(new AbstractIoHandler() {
            @Override
            public void sessionOpened(final IoSession session) {
                LOG.info("session opened {}", session);
            }

            @Override
            public void messageReceived(IoSession session, Object message) {
                LOG.info("message received {}", message);
                if (message instanceof ByteBuffer) {
                    LOG.info("echoing");
                    session.write(message);
                }
            }

            @Override
            public void messageSent(IoSession session, Object message) {
                LOG.info("message sent {}", message);
            }

            @Override
            public void sessionClosed(IoSession session) {
                LOG.info("session closed {}", session);
            }
        });

        try {
            IoFuture<IoSession> future = client.connect(new InetSocketAddress("localhost", 9999));

            try {
                IoSession session = future.get();
                LOG.info("session connected : {}", session);
            } catch (ExecutionException e) {
                LOG.error("cannot connect : ", e);
            }

            LOG.debug("Running the client for 25 sec");
            Thread.sleep(25000);
        } catch (InterruptedException e) {
        }
    }
}
