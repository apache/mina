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

import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import jdk.jfr.Recording;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AcceptorBindUtil;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Opt-in TLS workload for comparing contention on the same JVM and host.
 * Run with {@code -Dtest=SslContentionBenchmarkTest -Dmina.ssl.profile=true} and select
 * {@code -Dmina.ssl.profile.mode=traffic} or {@code connections}. Traffic can
 * be sized with {@code mina.ssl.profile.writers}, {@code .writes}, and
 * {@code .payload}; connection bursts use {@code .connections}. Set
 * {@code mina.ssl.profile.recording} to keep a separate JFR file for each run.
 * Connection mode warms up 64 handshakes before timing; override with
 * {@code mina.ssl.profile.warmup}.
 * The test uses loopback TLSv1.2 with one client and one server context. It
 * records monitor and park events with JFR, which adds overhead. Compare
 * multiple runs under the same JDK and host conditions; the printed rates are
 * observations, not performance assertions or production throughput estimates.
 */
public class SslContentionBenchmarkTest {

    @Test
    public void profileSslLocks() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("mina.ssl.profile"));

        String mode = System.getProperty("mina.ssl.profile.mode", "traffic");
        int connections = Integer.getInteger("mina.ssl.profile.connections", 384);
        int writers = Integer.getInteger("mina.ssl.profile.writers", 4);
        int writesPerWriter = Integer.getInteger("mina.ssl.profile.writes", 30000);
        int payloadSize = Integer.getInteger("mina.ssl.profile.payload", 1024);
        int warmupConnections = Integer.getInteger("mina.ssl.profile.warmup", 64);
        assertTrue("mode must be traffic or connections", "traffic".equals(mode) || "connections".equals(mode));
        assertTrue("connections must be positive", connections > 0);
        assertTrue("writers must be positive", writers > 0);
        assertTrue("writes must be positive", writesPerWriter > 0);
        assertTrue("payload must be positive", payloadSize > 0);
        assertTrue("warmup count cannot be negative", warmupConnections >= 0);
        Path recordingPath = Path.of(System.getProperty("mina.ssl.profile.recording",
                "target/ssl-lock-" + mode + ".jfr"));

        SSLContext serverContext = sslContext();
        SSLContext clientContext = sslContext();
        CountDownLatch secured = new CountDownLatch("connections".equals(mode)
                ? 2 * (connections + warmupConnections) : 2);
        CountDownLatch warmupSecured = new CountDownLatch(2 * warmupConnections);
        Set<String> negotiatedSessions = ConcurrentHashMap.newKeySet();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicLong echoedBytes = new AtomicLong();
        long expectedBytes = (long) writers * writesPerWriter * payloadSize;
        CountDownLatch echoed = new CountDownLatch(1);

        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        NioSocketConnector connector = new NioSocketConnector();
        int connectorThreads = Math.min(connections, 32);
        ExecutorService pool = Executors.newFixedThreadPool("traffic".equals(mode) ? writers : connectorThreads);
        try {
            acceptor.setHandler(new ProfileHandler(secured, warmupSecured, failure, null, null, null, 0));
            connector.setHandler(new ProfileHandler(secured, warmupSecured, failure, negotiatedSessions,
                    echoedBytes, echoed, expectedBytes));
            addSsl(acceptor.getFilterChain(), serverContext);
            addSsl(connector.getFilterChain(), clientContext);
            AcceptorBindUtil.tryBind(acceptor);

            if ("connections".equals(mode)) {
                ConnectFuture[] warmup = new ConnectFuture[warmupConnections];
                for (int i = 0; i < warmup.length; i++) {
                    warmup[i] = connector.connect(acceptor.getLocalAddress());
                }
                for (ConnectFuture connection : warmup) {
                    assertTrue("warmup connect timeout", connection.awaitUninterruptibly(30, TimeUnit.SECONDS));
                    connection.getSession();
                }
                assertTrue("warmup handshake timeout", warmupSecured.await(120, TimeUnit.SECONDS));
                assertNull(failure.get());
                negotiatedSessions.clear();
            }

            if ("traffic".equals(mode)) {
                ConnectFuture connected = connector.connect(acceptor.getLocalAddress());
                assertTrue("connect timeout", connected.awaitUninterruptibly(30, TimeUnit.SECONDS));
                assertTrue("handshake timeout", secured.await(30, TimeUnit.SECONDS));
                assertNull(failure.get());
                IoSession session = connected.getSession();
                try (Recording recording = recording(recordingPath)) {
                    long start = System.nanoTime();
                    CountDownLatch ready = new CountDownLatch(writers);
                    CountDownLatch begin = new CountDownLatch(1);
                    for (int i = 0; i < writers; i++) {
                        pool.execute(() -> {
                            ready.countDown();
                            try {
                                begin.await();
                                Deque<WriteFuture> pending = new ArrayDeque<>();
                                for (int n = 0; n < writesPerWriter; n++) {
                                    pending.addLast(session.write(IoBuffer.wrap(new byte[payloadSize])));
                                    if (pending.size() == 32) {
                                        awaitWrite(pending.removeFirst());
                                    }
                                }
                                while (!pending.isEmpty()) {
                                    awaitWrite(pending.removeFirst());
                                }
                            } catch (Throwable t) {
                                failure.compareAndSet(null, t);
                                echoed.countDown();
                            }
                        });
                    }
                    assertTrue("writers did not start", ready.await(30, TimeUnit.SECONDS));
                    begin.countDown();
                    assertTrue("echo timeout", echoed.await(120, TimeUnit.SECONDS));
                    long elapsed = System.nanoTime() - start;
                    assertNull(failure.get());
                    assertTrue("wrong echoed byte count: " + echoedBytes.get(), echoedBytes.get() == expectedBytes);
                    System.out.printf("SSL_PROFILE mode=traffic bytes=%d seconds=%.3f MiB_per_second=%.2f%n",
                            expectedBytes, elapsed / 1e9, expectedBytes / (elapsed / 1e9) / 1048576.0);
                }
            } else if ("connections".equals(mode)) {
                try (Recording recording = recording(recordingPath)) {
                    long start = System.nanoTime();
                    CountDownLatch ready = new CountDownLatch(connectorThreads);
                    CountDownLatch begin = new CountDownLatch(1);
                    for (int i = 0; i < connections; i++) {
                        pool.execute(() -> {
                            ready.countDown();
                            try {
                                begin.await();
                                ConnectFuture connected = connector.connect(acceptor.getLocalAddress());
                                if (!connected.awaitUninterruptibly(30, TimeUnit.SECONDS)) {
                                    throw new AssertionError("connect timeout");
                                }
                                connected.getSession();
                            } catch (Throwable t) {
                                failure.compareAndSet(null, t);
                                secured.countDown();
                            }
                        });
                    }
                    assertTrue("connectors did not start", ready.await(30, TimeUnit.SECONDS));
                    begin.countDown();
                    assertTrue("handshake timeout", secured.await(120, TimeUnit.SECONDS));
                    long elapsed = System.nanoTime() - start;
                    assertNull(failure.get());
                    System.out.printf("SSL_PROFILE mode=connections warmup=%d count=%d unique_sessions=%d seconds=%.3f connections_per_second=%.2f%n",
                            warmupConnections, connections, negotiatedSessions.size(), elapsed / 1e9,
                            connections / (elapsed / 1e9));
                }
            }
        } finally {
            pool.shutdownNow();
            connector.dispose();
            acceptor.unbind();
            acceptor.dispose();
        }
    }

    private static Recording recording(Path path) throws Exception {
        Recording recording = new Recording();
        recording.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ZERO).withStackTrace();
        recording.enable("jdk.ThreadPark").withThreshold(Duration.ZERO).withStackTrace();
        recording.setDestination(path);
        recording.start();
        return recording;
    }

    private static void awaitWrite(WriteFuture future) throws Exception {
        if (!future.awaitUninterruptibly(30, TimeUnit.SECONDS) || !future.isWritten()) {
            throw new AssertionError("write did not complete", future.getException());
        }
    }

    private static void addSsl(DefaultIoFilterChainBuilder chain, SSLContext context) {
        SslFilter filter = new SslFilter(context);
        filter.setUseNonBlockingPipeline(true);
        filter.setEnabledProtocols("TLSv1.2");
        chain.addLast("ssl", filter);
    }

    private static SSLContext sslContext() throws Exception {
        char[] password = "password".toCharArray();
        KeyStore keys = KeyStore.getInstance("JKS");
        KeyStore trust = KeyStore.getInstance("JKS");
        try (InputStream in = SslContentionBenchmarkTest.class.getResourceAsStream("keystore.jks")) {
            keys.load(in, password);
        }
        try (InputStream in = SslContentionBenchmarkTest.class.getResourceAsStream("truststore.jks")) {
            trust.load(in, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        kmf.init(keys, password);
        tmf.init(trust);
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return context;
    }

    private static final class ProfileHandler extends IoHandlerAdapter {
        private final CountDownLatch secured;
        private final CountDownLatch warmupSecured;
        private final AtomicReference<Throwable> failure;
        private final Set<String> negotiatedSessions;
        private final AtomicLong echoedBytes;
        private final CountDownLatch echoed;
        private final long expectedBytes;

        private ProfileHandler(CountDownLatch secured, CountDownLatch warmupSecured, AtomicReference<Throwable> failure,
                Set<String> negotiatedSessions, AtomicLong echoedBytes, CountDownLatch echoed, long expectedBytes) {
            this.secured = secured;
            this.warmupSecured = warmupSecured;
            this.failure = failure;
            this.negotiatedSessions = negotiatedSessions;
            this.echoedBytes = echoedBytes;
            this.echoed = echoed;
            this.expectedBytes = expectedBytes;
        }

        @Override
        public void event(IoSession session, FilterEvent event) {
            if (event == SslEvent.SECURED) {
                if (negotiatedSessions != null) {
                    SSLSession sslSession = (SSLSession) session.getAttribute(SslFilter.SSL_SECURED);
                    negotiatedSessions.add(Base64.getEncoder().encodeToString(sslSession.getId()));
                }
                secured.countDown();
                warmupSecured.countDown();
            }
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            IoBuffer incoming = (IoBuffer) message;
            if (echoedBytes == null) {
                IoBuffer copy = IoBuffer.allocate(incoming.remaining());
                copy.put(incoming);
                copy.flip();
                session.write(copy);
            } else if (echoedBytes.addAndGet(incoming.remaining()) >= expectedBytes) {
                echoed.countDown();
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            failure.compareAndSet(null, cause);
            if (echoed != null) {
                echoed.countDown();
            }
            secured.countDown();
            warmupSecured.countDown();
        }
    }
}
