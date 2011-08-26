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

package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.apache.mina.api.IoService;
import org.apache.mina.api.IoServiceListener;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.service.OneThreadSelectorStrategy;
import org.apache.mina.service.SelectorFactory;
import org.apache.mina.transport.tcp.NioSelectorProcessor;
import org.apache.mina.transport.tcp.nio.NioTcpServer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic Acceptor test
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class NioAcceptorTest {

    static final private Logger LOG = LoggerFactory.getLogger(NioAcceptorTest.class);

    @Test
    public void acceptorTest() {
        LOG.info("starting NioAcceptorTest");

        OneThreadSelectorStrategy strategy = new OneThreadSelectorStrategy(new SelectorFactory(
                NioSelectorProcessor.class));
        NioTcpServer acceptor = new NioTcpServer(strategy);

        // create the fitler chain for this service
        acceptor.setFilters(new LoggingFilter("LoggingFilter1"), new LoggingFilter("LoggingFilter2"));

        acceptor.addListener(new IoServiceListener() {

            @Override
            public void sessionDestroyed(IoSession session) {
                LOG.info("session destroyed {}", session);

            }

            @Override
            public void sessionCreated(IoSession session) {
                LOG.info("session created {}", session);

                ByteBuffer bf = ByteBuffer.allocate("toto".length());
                bf.put("toto".getBytes());
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
            LOG.debug("Waiting 25 sec");
            Thread.sleep(25000);
            LOG.debug("Unbinding");

            acceptor.unbind(address);

            LOG.debug("Trying to rebind the freed port");
            acceptor.bind(address);
            LOG.debug("Bound");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        Exception ex = null;
        try {
            LOG.info("Trying to bind an already bound port");
            // try to bind an already bound port
            acceptor.bind(new InetSocketAddress(9999));

            Assert.fail();

        } catch (IOException e) {
            LOG.info("catching the exception", e);
            ex = e;
        }
        Assert.assertNotNull(ex);

    }
}