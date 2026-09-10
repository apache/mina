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
package org.apache.mina.filter.ssl;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.Security;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SslFilterScheduledWriteMessagesTest {

    private static final String KEY_STORE_PATH = "keystore.jks";
    private static final String TRUST_STORE_PATH = "truststore.jks";
    private static final String[] ENABLED_PROTOCOLS = new String[] { "TLSv1.2" };
    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");

        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    private CountDownLatch handshakeDone;
    private CountDownLatch sessionsOpened;
    private int port;

    @Before
    public void setUp() {
        handshakeDone = new CountDownLatch(2);
        sessionsOpened = new CountDownLatch(2);
        port = AvailablePortFinder.getNextAvailable(5555);
    }

    @Test
    public void shouldDecrementScheduledWriteMessages() throws Exception {
        CountDownLatch handshakeDone = new CountDownLatch(0);
        AcceptorIoHandler acceptorIoHandler = new AcceptorIoHandler(handshakeDone, sessionsOpened);
        ConnectionIoHandler connectionIoHandler = new ConnectionIoHandler(handshakeDone, sessionsOpened);

        IoService acceptorService = startAcceptor(acceptorIoHandler);

        try {
            IoService connectorService = startConnector(connectionIoHandler);

            try {
                assertTrue(sessionsOpened.await(10L, TimeUnit.SECONDS));

                IoSession acceptorSession = acceptorIoHandler.session;
                IoSession connectorSession = connectionIoHandler.session;

                assertEquals(0, acceptorSession.getWrittenMessages());
                assertEquals(0, connectorSession.getWrittenMessages());
                assertEquals(0, acceptorSession.getScheduledWriteMessages());
                assertEquals(0, connectorSession.getScheduledWriteMessages());
                assertEquals(0, acceptorIoHandler.sentMessageCount);
                assertEquals(0, connectionIoHandler.sentMessageCount);

                WriteFuture connectorWriteFuture = connectorSession.write("connector message");
                assertTrue(connectorWriteFuture.await(2L, TimeUnit.SECONDS));

                Thread.sleep(1000L);

                assertEquals(0, acceptorSession.getWrittenMessages());
                assertEquals(1, connectorSession.getWrittenMessages());
                assertEquals(0, acceptorSession.getScheduledWriteMessages());
                assertEquals(0, connectorSession.getScheduledWriteMessages());
                assertEquals(0, acceptorIoHandler.sentMessageCount);
                assertEquals(1, connectionIoHandler.sentMessageCount);

                WriteFuture acceptorWriteFuture = acceptorSession.write("acceptor message");
                assertTrue(acceptorWriteFuture.await(2L, TimeUnit.SECONDS));

                Thread.sleep(1000L);

                assertEquals(1, acceptorSession.getWrittenMessages());
                assertEquals(1, connectorSession.getWrittenMessages());
                assertEquals(0, acceptorSession.getScheduledWriteMessages());
                assertEquals(0, connectorSession.getScheduledWriteMessages());
                assertEquals(1, acceptorIoHandler.sentMessageCount);
                assertEquals(1, connectionIoHandler.sentMessageCount);
            } finally {
                connectorService.dispose();
            }
        } finally {
            acceptorService.dispose();
        }
    }

    @Test
    public void shouldDecrementScheduledWriteMessagesWithSsl() throws Exception {
        SSLContext sslContext = createSSLContext();

        AcceptorIoHandler acceptorIoHandler = new AcceptorIoHandler(handshakeDone, sessionsOpened);
        ConnectionIoHandler connectionIoHandler = new ConnectionIoHandler(handshakeDone, sessionsOpened);

        IoService acceptorService = startSslAcceptor(sslContext, acceptorIoHandler);

        try {
            IoService connectorService = startSslConnector(sslContext, connectionIoHandler);

            try {
                assertTrue(handshakeDone.await(10L, TimeUnit.SECONDS));
                assertTrue(sessionsOpened.await(10L, TimeUnit.SECONDS));

                IoSession acceptorSession = acceptorIoHandler.session;
                IoSession connectorSession = connectionIoHandler.session;

                assertEquals(0, acceptorSession.getWrittenMessages());
                assertEquals(0, connectorSession.getWrittenMessages());
                assertEquals(0, acceptorSession.getScheduledWriteMessages());
                assertEquals(0, connectorSession.getScheduledWriteMessages());
                assertEquals(0, acceptorIoHandler.sentMessageCount);
                assertEquals(0, connectionIoHandler.sentMessageCount);

                WriteFuture connectorWriteFuture = connectorSession.write("connector message");
                assertTrue(connectorWriteFuture.await(2L, TimeUnit.SECONDS));

                Thread.sleep(1000L);

                assertEquals(0, acceptorSession.getWrittenMessages());
                assertEquals(1, connectorSession.getWrittenMessages());
                assertEquals(0, acceptorSession.getScheduledWriteMessages());
                assertEquals(0, connectorSession.getScheduledWriteMessages());
                assertEquals(0, acceptorIoHandler.sentMessageCount);
                assertEquals(1, connectionIoHandler.sentMessageCount);

                WriteFuture acceptorWriteFuture = acceptorSession.write("acceptor message");
                assertTrue(acceptorWriteFuture.await(2L, TimeUnit.SECONDS));

                Thread.sleep(1000L);

                assertEquals(1, acceptorSession.getWrittenMessages());
                assertEquals(1, connectorSession.getWrittenMessages());
                assertEquals(0, acceptorSession.getScheduledWriteMessages());
                assertEquals(0, connectorSession.getScheduledWriteMessages());
                assertEquals(1, acceptorIoHandler.sentMessageCount);
                assertEquals(1, connectionIoHandler.sentMessageCount);
            } finally {
                connectorService.dispose();
            }
        } finally {
            acceptorService.dispose();
        }
    }

    private IoService startAcceptor(IoHandler handler) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);

        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        acceptor.setHandler(handler);
        acceptor.bind(new InetSocketAddress(port));

        return acceptor;
    }

    private IoService startSslAcceptor(SSLContext sslContext, IoHandler handler) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);

        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setEnabledProtocols(ENABLED_PROTOCOLS);

        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();
        filters.addLast("ssl", sslFilter);
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        acceptor.setHandler(handler);
        acceptor.bind(new InetSocketAddress(port));

        return acceptor;
    }

    private IoService startSslConnector(SSLContext sslContext, IoHandler handler) {
        NioSocketConnector connector = new NioSocketConnector();

        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setEnabledProtocols(ENABLED_PROTOCOLS);

        DefaultIoFilterChainBuilder filters = connector.getFilterChain();
        filters.addLast("ssl", sslFilter);
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        connector.setHandler(handler);
        connector.connect(new InetSocketAddress("localhost", port));

        return connector;
    }

    private IoService startConnector(IoHandler handler) {
        NioSocketConnector connector = new NioSocketConnector();

        DefaultIoFilterChainBuilder filters = connector.getFilterChain();
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        connector.setHandler(handler);
        connector.connect(new InetSocketAddress("localhost", port));

        return connector;
    }


    private static SSLContext createSSLContext() throws Exception {
        char[] password = "password".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(SslIdentificationAlgorithmTest.class.getResourceAsStream(KEY_STORE_PATH), password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        kmf.init(keyStore, password);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(SslIdentificationAlgorithmTest.class.getResourceAsStream(TRUST_STORE_PATH), password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }

    private static final class AcceptorIoHandler extends IoHandlerAdapter {

        private final CountDownLatch handshakeDone;
        private final CountDownLatch sessionsOpened;
        private IoSession session;
        private int sentMessageCount;

        public AcceptorIoHandler(CountDownLatch handshakeDone, CountDownLatch sessionsOpened) {
            this.handshakeDone = handshakeDone;
            this.sessionsOpened = sessionsOpened;
        }

        @Override
        public void sessionOpened(IoSession session) {
            this.session = session;
            sessionsOpened.countDown();
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            sentMessageCount++;
        }

        @Override
        public void event(IoSession session, FilterEvent event) {
            if (event == SslEvent.SECURED) {
                handshakeDone.countDown();
            }
        }
    }

    private static final class ConnectionIoHandler extends IoHandlerAdapter {

        private final CountDownLatch handshakeDone;
        private final CountDownLatch sessionsOpened;
        private IoSession session;
        private int sentMessageCount;

        public ConnectionIoHandler(CountDownLatch handshakeDone, CountDownLatch sessionsOpened) {
            this.handshakeDone = handshakeDone;
            this.sessionsOpened = sessionsOpened;
        }

        @Override
        public void sessionOpened(IoSession session) {
            this.session = session;
            sessionsOpened.countDown();
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            sentMessageCount++;
        }

        @Override
        public void event(IoSession session, FilterEvent event) {
            if (event == SslEvent.SECURED) {
                handshakeDone.countDown();
            }
        }
    }
}
