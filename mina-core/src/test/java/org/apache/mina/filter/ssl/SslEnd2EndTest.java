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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AcceptorBindUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SslEnd2EndTest {

    private static final String KEY_MANAGER_FACTORY_ALGORITHM;
    private static final int MAX_LENGTH = 1024 * 1024 * 8;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    @Parameterized.Parameters(name = "{index}: {0} / {1} / {2} / {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"TLSv1.2", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", 2048, 0},
                {"TLSv1.2", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", 2048, 4},
                {"TLSv1.3", "TLS_AES_256_GCM_SHA384", 2048, 0},
                {"TLSv1.3", "TLS_AES_256_GCM_SHA384", 2048, 4},
        });
    }

    private final String protocol;
    private final String[] cipherSuites;
    private final int messageCount;
    private final int messageLengthDelta;

    public SslEnd2EndTest(String protocol, String cipherSuites, int messageCount, int messageLengthDelta) {
        this.protocol = protocol;
        this.cipherSuites = cipherSuites.split(",");
        this.messageCount = messageCount;
        this.messageLengthDelta = messageLengthDelta;
    }

    @Test
    public void shouldSendLargeMessages() throws Exception {
        ByteBuffer acceptorReceiveBuffer = ByteBuffer.allocate(MAX_LENGTH);
        AcceptorHandler acceptorHandler = new AcceptorHandler(acceptorReceiveBuffer);
        IoAcceptor acceptor = createAcceptor(acceptorHandler);

        try {
            AcceptorBindUtil.tryBind(acceptor);

            ByteBuffer connectorReceiveBuffer = ByteBuffer.allocate(MAX_LENGTH);
            ConnectorIoHandler connectorHandler = new ConnectorIoHandler(connectorReceiveBuffer);
            IoConnector connector = createConnector(connectorHandler);

            try {
                ConnectFuture connectFuture = connector.connect(acceptor.getLocalAddress());
                assertTrue("Failed to connect", connectFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                IoBuffer connectorSendBuffer = IoBuffer.wrap(ByteBuffer.allocate(MAX_LENGTH));

                for (int i = 0; i < connectorSendBuffer.limit(); i += 4) {
                    connectorSendBuffer.putInt(i);
                }

                connectorSendBuffer.flip();

                IoSession session = connectFuture.getSession();
                int messageLength = 65_536;

                for (int i = 0; i < messageCount; i++) {
                    if (messageLength > MAX_LENGTH) {
                        throw new RuntimeException("Message length exceeds send buffer capacity. Increase send buffer capacity");
                    }

                    connectorSendBuffer.limit(messageLength);
                    connectorSendBuffer.rewind();

                    CountDownLatch acceptorMessageReceivedLatch = new CountDownLatch(1);
                    acceptorHandler.reset(acceptorMessageReceivedLatch, messageLength);

                    CountDownLatch connectorMessageReceivedLatch = new CountDownLatch(1);
                    connectorHandler.reset(connectorMessageReceivedLatch, messageLength);

                    WriteFuture writeFuture = session.write(connectorSendBuffer);
                    assertTrue("Connector write failed", writeFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                    boolean messageReceived = acceptorMessageReceivedLatch.await(4L, TimeUnit.SECONDS);

                    if (!messageReceived) {
                        assertNoException(acceptorHandler.getFailure());
                        fail("Failed to receive from connector");
                    }

                    acceptorReceiveBuffer.flip();

                    for (int j = 0; j < acceptorReceiveBuffer.limit(); j += 4) {
                        assertEquals(j, acceptorReceiveBuffer.getInt());
                    }

                    boolean connectorMessageReceived = connectorMessageReceivedLatch.await(4L, TimeUnit.SECONDS);

                    if (!connectorMessageReceived) {
                        assertNoException(connectorHandler.getFailure());
                        fail("Failed to receive from acceptor");
                    }

                    connectorReceiveBuffer.flip();

                    for (int j = 0; j < connectorReceiveBuffer.limit(); j += 4) {
                        assertEquals(j, connectorReceiveBuffer.getInt());
                    }

                    messageLength += messageLengthDelta;
                }
            } finally {
                connector.dispose();
            }
        } finally {
            acceptor.unbind();
            acceptor.dispose();
        }
    }

    private void assertNoException(Throwable exception) {
        if (exception != null) {
            throw new AssertionError(exception);
        }
    }

    private IoAcceptor createAcceptor(IoHandler handler) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        acceptor.setHandler(handler);

        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();

        SSLContext sslContext = createAcceptorSSLContext();

        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setEnabledCipherSuites(cipherSuites);
        sslFilter.setEnabledProtocols(protocol);

        filters.addLast("sslFilter", sslFilter);

        return acceptor;
    }

    private IoConnector createConnector(IoHandler ioHandler) throws GeneralSecurityException, IOException {
        NioSocketConnector connector = new NioSocketConnector();
        connector.setHandler(ioHandler);

        DefaultIoFilterChainBuilder filters = connector.getFilterChain();

        SSLContext sslContext = createConnectorSslContext();

        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setEnabledCipherSuites(cipherSuites);
        sslFilter.setEnabledProtocols(protocol);

        filters.addLast("sslFilter", sslFilter);

        return connector;
    }

    private SSLContext createAcceptorSSLContext() throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance(protocol);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(SslEnd2EndTest.class.getResourceAsStream("keystore.jks"), passphrase);

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(SslEnd2EndTest.class.getResourceAsStream("emptykeystore.sslTest"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    private SSLContext createConnectorSslContext() throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance(protocol);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(SslEnd2EndTest.class.getResourceAsStream("emptykeystore.sslTest"), passphrase);

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(SslEnd2EndTest.class.getResourceAsStream("truststore.jks"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    private static final class AcceptorHandler extends IoHandlerAdapter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AcceptorHandler.class);
        private final ByteBuffer receiveBuffer;
        private final AtomicReference<Throwable> exception;
        private CountDownLatch messageReceivedLatch;
        private int expectedLength;

        private AcceptorHandler(ByteBuffer receiveBuffer) {
            this.receiveBuffer = receiveBuffer;
            this.exception = new AtomicReference<>();
        }

        public void reset(CountDownLatch messageReceivedLatch, int expectedLength) {
            this.messageReceivedLatch = messageReceivedLatch;
            this.receiveBuffer.clear();

            // zero data
            for (int i = 0; i < receiveBuffer.capacity(); i++) {
                receiveBuffer.put(i, (byte) 0);
            }

            this.expectedLength = expectedLength;
        }

        public Throwable getFailure() {
            return exception.get();
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception caught: {}", session, cause);
            }

            exception.compareAndSet(null, cause);
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            IoBuffer messageBuffer = (IoBuffer) message;
            receiveBuffer.put(messageBuffer.buf());

            if (receiveBuffer.position() == expectedLength) {
                receiveBuffer.flip();

                // respond with the same message
                IoBuffer buffer = IoBuffer.wrap(receiveBuffer);
                session.write(buffer);

                messageReceivedLatch.countDown();
            }
        }
    }

    private static final class ConnectorIoHandler extends IoHandlerAdapter {

        private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorIoHandler.class);

        private final ByteBuffer receiveBuffer;
        private final AtomicReference<Throwable> exception;
        private CountDownLatch messageReceivedLatch;
        private int expectedLength;

        private ConnectorIoHandler(ByteBuffer receiveBuffer) {
            this.receiveBuffer = receiveBuffer;
            this.exception = new AtomicReference<>();
        }

        public Throwable getFailure() {
            return exception.get();
        }

        public void reset(CountDownLatch messageReceivedLatch, int expectedLength) {
            this.messageReceivedLatch = messageReceivedLatch;
            this.receiveBuffer.clear();

            // zero data
            for (int i = 0; i < receiveBuffer.capacity(); i++) {
                receiveBuffer.put(i, (byte) 0);
            }

            this.expectedLength = expectedLength;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception caught: {}", session, cause);
            }

            exception.compareAndSet(null, cause);
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            IoBuffer messageBuffer = (IoBuffer) message;
            receiveBuffer.put(messageBuffer.buf());

            if (receiveBuffer.position() == expectedLength) {
                messageReceivedLatch.countDown();
            }
        }
    }
}
