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
import org.apache.mina.service.OneThreadSelectorStrategy;
import org.apache.mina.transport.nio.NioSelectorProcessor;
import org.apache.mina.transport.nio.NioTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic Acceptor test
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class NioEchoServer {

    static final private Logger LOG = LoggerFactory.getLogger(NioEchoServer.class);

    public static void main(String[] args) {
        LOG.info("starting echo server");

        OneThreadSelectorStrategy<NioSelectorProcessor> strategy = new OneThreadSelectorStrategy<NioSelectorProcessor>(new NioSelectorProcessor());
        
        NioTcpServer acceptor = new NioTcpServer(strategy);

        // create the fitler chain for this service
        acceptor.setFilters(new LoggingFilter("LoggingFilter1"), new IoFilter() {

            @Override
            public void sessionOpened(IoSession session) {
                LOG.info("session {} open", session);
            }

            @Override
            public void sessionIdle(IoSession session, IdleStatus status) {
                LOG.info("session {} idle", session);
            }

            @Override
            public void sessionCreated(IoSession session) {
                LOG.info("session {} created", session);
            }

            @Override
            public void sessionClosed(IoSession session) {
                LOG.info("session {} open", session);
            }

            @Override
            public void messageWriting(IoSession session, Object message, WriteFilterChainController controller) {
                // we just push the message in the chain
                controller.callWriteNextFilter(message);
            }

            @Override
            public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {

                if (message instanceof ByteBuffer) {
                    LOG.info("echoing");
                    session.write(message);
                }
            }
        });

        acceptor.addListeners(new IoServiceListener() {

            @Override
            public void sessionDestroyed(IoSession session) {
                LOG.info("session destroyed {}", session);

            }

            @Override
            public void sessionCreated(IoSession session) {
                LOG.info("session created {}", session);

                String welcomeStr = "welcome\n";
                ByteBuffer bf = ByteBuffer.allocate(welcomeStr.length());
                bf.put(welcomeStr.getBytes());
                bf.flip();
                session.write(bf);
            }

            @Override
            public void serviceInactivated(IoService service) {
                LOG.info("service deactivated {}", service);
            }

            @Override
            public void serviceActivated(IoService service) {
                LOG.info("service activated {}", service);
            }
        });

        try {
            SocketAddress address = new InetSocketAddress(9999);
            acceptor.bind(address);
            LOG.debug("Running the server for 25 sec");
            Thread.sleep(25000);
            LOG.debug("Unbinding the TCP port");
            acceptor.unbind();
        } catch (IOException e) {
            LOG.error("I/O exception", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted exception", e);
        }
    }
}