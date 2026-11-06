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
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.proxy.AbstractProxyIoHandler;
import org.apache.mina.proxy.ProxyConnector;
import org.apache.mina.proxy.handlers.ProxyRequest;
import org.apache.mina.proxy.handlers.socks.SocksProxyConstants;
import org.apache.mina.proxy.handlers.socks.SocksProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AcceptorBindUtil;
import org.apache.mina.util.DaemonThreadFactory;
import org.apache.mina.util.ThreadUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SocksProxyTest {

    private static final String KEY_MANAGER_FACTORY_ALGORITHM;
    private static final int MESSAGE_LENGTH = 65_536;
    private static final int MESSAGE_COUNT = 128;
    private static final int WRITER_COUNT = 2;
    private static final long WRITE_TIMEOUT_SECONDS = 16L;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    // @formatter:off
    @Parameterized.Parameters(name = "{index}: {0} / {1} / {2} / nonBlockingPipelineEnabled={3} / sslEnabled={4}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // SSL + SOCKS_4
                {"TLSv1.2", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_4, true, true},
                {"TLSv1.3", "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_4, true, true},
                {"TLSv1.2", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_4, false, true},
                {"TLSv1.3", "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_4, false, true},
                // SSL + SOCKS_5
                {"TLSv1.2", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_5, true, true},
                {"TLSv1.3", "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_5, true, true},
                {"TLSv1.2", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_5, false, true},
                {"TLSv1.3", "TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384", SocksProxyConstants.SOCKS_VERSION_5, false, true},
                // SOCKS_4
                {"", "", SocksProxyConstants.SOCKS_VERSION_4, true, false},
                {"", "", SocksProxyConstants.SOCKS_VERSION_4, false, false},
                {"", "", SocksProxyConstants.SOCKS_VERSION_4, true, false},
                {"", "", SocksProxyConstants.SOCKS_VERSION_4, false, false},
                // SOCKS_5
                {"", "", SocksProxyConstants.SOCKS_VERSION_5, true, false},
                {"", "", SocksProxyConstants.SOCKS_VERSION_5, false, false},
                {"", "", SocksProxyConstants.SOCKS_VERSION_5, true, false},
                {"", "", SocksProxyConstants.SOCKS_VERSION_5, false, false},
        });
    }
    // @formatter:on

    private final String protocol;
    private final String[] cipherSuites;
    private final byte socksVersion;
    private final boolean nonBlockingPipelineEnabled;
    private final boolean sslEnabled;

    public SocksProxyTest(String protocol, String cipherSuites, byte socksVersion,
            boolean nonBlockingPipelineEnabled, boolean sslEnabled) {
        this.protocol = protocol;
        this.cipherSuites = cipherSuites.split(",");
        this.socksVersion = socksVersion;
        this.nonBlockingPipelineEnabled = nonBlockingPipelineEnabled;
        this.sslEnabled = sslEnabled;
    }

    @Test
    public void shouldNotDeadlockUnderConcurrentReadsAndWrites() throws Exception {
        byte[] payload = createPayload(MESSAGE_LENGTH);
        long expectedCount = MESSAGE_COUNT * MESSAGE_LENGTH * WRITER_COUNT;
        long expectedSum = 0L;

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            expectedSum += sum(payload);
        }

        expectedSum *= WRITER_COUNT;

        SocksProxyServer proxyServer = new SocksProxyServer(0);

        try {
            proxyServer.start();

            InetSocketAddress proxyAddress = proxyServer.getLocalAddress();

            AcceptorHandler acceptorHandler = new AcceptorHandler();
            NioSocketAcceptor acceptor = createAcceptor(acceptorHandler);

            try {
                AcceptorBindUtil.tryBind(acceptor);
                InetSocketAddress endpoint = new InetSocketAddress("127.0.0.1", acceptor.getLocalAddress().getPort());

                ConnectorIoHandler connectorHandler = new ConnectorIoHandler();
                ProxyConnector connector = createrSocksProxyConnector(connectorHandler, proxyAddress, endpoint);

                try {
                    ConnectFuture connectFuture = connector.connect(proxyAddress);
                    assertTrue("Failed to connect", connectFuture.awaitUninterruptibly(4L, TimeUnit.SECONDS));

                    IoSession session = connectFuture.getSession();
                    ExecutorService executorService = Executors.newFixedThreadPool(WRITER_COUNT,
                            new CustomThreadFactory("writer-task"));
                    List<Future<?>> futures = new ArrayList<>();
                    CyclicBarrier start = new CyclicBarrier(WRITER_COUNT);

                    try {
                        for (int i = 0; i < WRITER_COUNT; i++) {
                            Future<?> future = executorService.submit(() -> {
                                try {
                                    send(start, session, payload.clone());
                                } catch (Exception e) {
                                    throw new RuntimeException("Send operation failed", e);
                                }

                                return null;
                            });

                            futures.add(future);
                        }

                        for (Future<?> future : futures) {
                            try {
                                future.get(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                            } catch (TimeoutException e) {
                                ThreadUtil.assertNoDeadlock();
                                fail("Write future has timed out");
                            } catch (ExecutionException e) {
                                ThreadUtil.assertNoDeadlock();
                                fail("Writer failed: " + e.getMessage());
                            }
                        }

                        // wait a bit to receive back
                        long deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(WRITE_TIMEOUT_SECONDS);

                        while ((connectorHandler.receivedCount.get() < expectedCount
                                || acceptorHandler.receivedCount.get() < expectedCount)
                                && acceptorHandler.getFailure() == null
                                && connectorHandler.getFailure() == null
                                && System.nanoTime() < deadlineNs) {
                            Thread.sleep(10L);
                        }

                        ThreadUtil.assertNoDeadlock();

                        assertNull("Acceptor failure", acceptorHandler.getFailure());
                        assertEquals("Acceptor byte count", expectedCount, acceptorHandler.receivedCount.get());
                        assertEquals("Acceptor data sum", expectedSum, acceptorHandler.receivedSum.get());

                        assertNull("Connector failure", connectorHandler.getFailure());
                        assertEquals("Connector byte count", expectedCount, connectorHandler.receivedCount.get());
                        assertEquals("Connector data sum", expectedSum, connectorHandler.receivedSum.get());
                    } finally {
                        executorService.shutdown();
                    }
                } finally {
                    connector.dispose(false);
                }
            } finally {
                acceptor.unbind();
                acceptor.dispose(false);
            }
        } finally {
            proxyServer.stop();
        }
    }

    private static void send(CyclicBarrier start, IoSession session, byte[] payload) throws Exception {
        start.await(4L, TimeUnit.SECONDS);

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            IoBuffer buffer = IoBuffer.wrap(payload);
            WriteFuture writeFuture = session.write(buffer);
            assertTrue(writeFuture.await(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    private NioSocketAcceptor createAcceptor(IoHandler handler) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);
        acceptor.setHandler(handler);

        if (sslEnabled) {
            SSLContext sslContext = createAcceptorSSLContext();

            SslFilter sslFilter = new SslFilter(sslContext);
            sslFilter.setUseNonBlockingPipeline(nonBlockingPipelineEnabled);
            sslFilter.setEnabledCipherSuites(cipherSuites);
            sslFilter.setEnabledProtocols(protocol);

            acceptor.getFilterChain().addLast("sslFilter", sslFilter);
        }

        return acceptor;
    }

    private ProxyConnector createrSocksProxyConnector(IoHandler handler, InetSocketAddress proxyAddress,
            InetSocketAddress endpointAddress) throws Exception {
        NioSocketConnector connector = new NioSocketConnector();
        connector.setHandler(handler);

        if (sslEnabled) {
            SSLContext sslContext = createConnectorSslContext();

            SslFilter sslFilter = new SslFilter(sslContext, false);
            sslFilter.setUseNonBlockingPipeline(nonBlockingPipelineEnabled);
            sslFilter.setEnabledCipherSuites(cipherSuites);
            sslFilter.setEnabledProtocols(protocol);

            connector.getFilterChain().addLast("sslFilter", sslFilter);
        }

        ProxyRequest proxyRequest = new SocksProxyRequest(socksVersion, SocksProxyConstants.ESTABLISH_TCPIP_STREAM,
                endpointAddress, "");
        ProxyConnector proxyConnector = new ProxyConnector(connector);

        ProxyIoSession proxyIoSession = new ProxyIoSession(proxyAddress, proxyRequest);
        proxyConnector.setProxyIoSession(proxyIoSession);
        proxyConnector.setHandler(handler);

        return proxyConnector;
    }

    private SSLContext createAcceptorSSLContext() throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance(protocol);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(SocksProxyTest.class.getResourceAsStream("keystore.jks"), passphrase);

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(SocksProxyTest.class.getResourceAsStream("emptykeystore.sslTest"), passphrase);

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
        ks.load(SocksProxyTest.class.getResourceAsStream("emptykeystore.sslTest"), passphrase);

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(SocksProxyTest.class.getResourceAsStream("truststore.jks"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    private static final class AcceptorHandler extends IoHandlerAdapter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AcceptorHandler.class);

        private final AtomicLong receivedCount;
        private final AtomicLong receivedSum;
        private final AtomicReference<Throwable> exception;

        private AcceptorHandler() {
            this.receivedCount = new AtomicLong();
            this.receivedSum = new AtomicLong();
            this.exception = new AtomicReference<>();
        }

        public Throwable getFailure() {
            return exception.get();
        }

        @Override
        public void sessionOpened(IoSession session) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Session opened: {}", session);
            }
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
            receivedCount.addAndGet(messageBuffer.remaining());
            receivedSum.addAndGet(sum(messageBuffer));

            IoBuffer copy = IoBuffer.allocate(messageBuffer.remaining());
            copy.put(messageBuffer);
            copy.flip();

            session.write(copy);
        }
    }

    private static final class ConnectorIoHandler extends AbstractProxyIoHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorIoHandler.class);

        private final AtomicLong receivedCount;
        private final AtomicLong receivedSum;
        private final AtomicReference<Throwable> exception;

        private ConnectorIoHandler() {
            this.receivedCount = new AtomicLong();
            this.receivedSum = new AtomicLong();
            this.exception = new AtomicReference<>();
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

            receivedCount.addAndGet(messageBuffer.remaining());
            receivedSum.addAndGet(sum(messageBuffer));
        }

        @Override
        public void proxySessionOpened(IoSession session) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Proxy session opened: {}", session);
            }
        }
    }

    private static byte[] createPayload(int length) {
        byte[] message = new byte[length];

        for (int i = 0; i < length; i++) {
            message[i] = (byte) (i * 31 + length);
        }

        return message;
    }

    private static long sum(byte[] data) {
        long sum = 0L;

        for (byte datum : data) {
            sum += datum & 0xFF;
        }

        return sum;
    }

    private static long sum(IoBuffer data) {
        long sum = 0L;

        for (int i = data.position(); i < data.limit(); i++) {
            sum += data.get(i) & 0xFF;
        }

        return sum;
    }

    private static final class CustomThreadFactory extends DaemonThreadFactory {

        private final String threadNamePrefix;
        private final AtomicLong sequence;

        public CustomThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = requireNonNull(threadNamePrefix);
            this.sequence = new AtomicLong();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = super.newThread(runnable);
            thread.setName(threadNamePrefix + "-" + sequence.getAndIncrement());
            return thread;
        }
    }
}