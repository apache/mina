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

import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class SslFilterTest {

    private static final Logger LOG = LoggerFactory.getLogger(SslFilterTest.class);

    @Test
    public void transferLargeData() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        final char[] password = "password".toCharArray();

        ks.load(this.getClass().getResourceAsStream("keystore.jks"), password);
        ts.load(this.getClass().getResourceAsStream("truststore.jks"), password);

        kmf.init(ks, password);
        tmf.init(ts);

        final SSLContext contextClient = SSLContext.getInstance("TLSv1.3");
        contextClient.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        final MySslFilter filterClient = new MySslFilter(contextClient);
        filterClient.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384" });
        filterClient.setEnabledProtocols(new String[] { "TLSv1.3" });
        filterClient.setUseNonBlockingPipeline( true );


        final IoConnector socketConnector = new NioSocketConnector();

        socketConnector.getFilterChain().addFirst("ssl", filterClient);
        socketConnector.setHandler(new DebugFilter());

        final SSLContext contextServer = SSLContext.getInstance("TLSv1.3");
        contextServer.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        final MySslFilter filterServer = new MySslFilter(contextServer);
        filterServer.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384" });
        filterServer.setEnabledProtocols(new String[] { "TLSv1.3" });
        filterServer.setUseNonBlockingPipeline( false );

        final IoAcceptor socketAcceptor = new NioSocketAcceptor();

        socketAcceptor.getFilterChain().addFirst("ssl", filterServer);
        CompareFilter filter = new CompareFilter();
        socketAcceptor.setHandler(filter);

        try {
            socketAcceptor.bind(new InetSocketAddress("localhost", 0));

            final IoFuture connectFuture = socketConnector.connect(socketAcceptor.getLocalAddress());
            connectFuture.awaitUninterruptibly();

            final IoSession clientSocket = connectFuture.getSession();
            try {
                SSLEngine sslClient = filterClient.getEngine();
                LOG.error("client packet size " + sslClient.getSession().getPacketBufferSize());
                LOG.error("client app buffer size " + sslClient.getSession().getApplicationBufferSize());
                for (int msgSize = 61000; msgSize < 80000; msgSize++) {
                    LOG.error("Trying " + msgSize);
                    CountDownLatch received = new CountDownLatch(1);
                    filter.init(received, msgSize);
                    IoBuffer data = createMosaicRequest(msgSize);
                    clientSocket.write(data).await();
                    data.free();
                    assertTrue(received.await(5, TimeUnit.SECONDS));
                    assertNoException(filter.getFailure());
                }
            } finally {
                clientSocket.closeNow().await();
            }
        } finally {
            socketAcceptor.unbind();
            socketAcceptor.dispose();
            socketConnector.dispose();
        }
    }

    private static void assertNoException(Throwable t) {
        if (t != null) {
            throw new AssertionError(t);
        }
    }

    private static class DebugFilter extends IoHandlerAdapter {

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            IoBuffer b = IoBuffer.class.cast(message);
            LOG.error("Client: received clear-text message\n" + b.getHexDump(true));
        }
    }

    private static class CompareFilter extends IoHandlerAdapter {

        private CountDownLatch done;

        private int needBytes;

        private IoBuffer acc;

        private Throwable failure;

        CompareFilter() {
            super();
        }

        void init(CountDownLatch done, int size) {
            if (this.acc != null) {
                this.acc.free();
            }
            this.done = done;
            this.needBytes = size;
            this.acc = IoBuffer.allocate(size);
        }

        Throwable getFailure() {
            return failure;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            LOG.error("Remote: got exception", cause);
            failure = cause;
            done.countDown();
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if (needBytes <= 0) {
                LOG.error("Remote: got spurious data");
                return;
            }
            IoBuffer b = IoBuffer.class.cast(message);
            int received = b.remaining();
            if (received > needBytes) {
                LOG.error("Remote: got " + received + " bytes, expected only " + needBytes);
                byte[] data = new byte[needBytes];
                b.get(data);
                acc.put(data);
            } else {
                LOG.error("Remote: got " + received + " bytes");
                acc.put(b);
            }
            needBytes -= received;
            LOG.error("Remote: still need " + needBytes + " bytes");
            if (needBytes <= 0) {
                int i = 0;
                int j = 0;
                acc.flip();
                while (acc.hasRemaining()) {
                    int val = acc.get() & 0xFF;
                    int cmp = i % 16367 == 0 ? j++ : 0x11;
                    if (val != cmp) {
                        LOG.error("Remote: data mismatch at index " + i);
                        break;
                    }
                    i++;
                    if (acc.hasRemaining()) {
                        val = acc.get() & 0xFF;
                        cmp = i % 16367 == 0 ? j++ : 0xAA;
                        if (val != cmp) {
                            LOG.error("Remote: data mismatch at index " + i);
                            break;
                        }
                        i++;
                    }
                }
                done.countDown();
            }
        }
    }

    public static IoBuffer createMosaicRequest(int size) {
        IoBuffer message = IoBuffer.allocate(size);
        int i = 0;
        int j = 0;
        while (message.hasRemaining()) {
            if (i++ % 16367 == 0) {
                message.put((byte) j);
                j++;
            } else {
                message.put((byte) 0x11);
            }
            if (message.hasRemaining()) {
                if (i++ % 16367 == 0) {
                    message.put((byte) j);
                    j++;
                } else {
                    message.put((byte) 0xAA);
                }
            }
        }
        message.flip();
        return message;
    }

    private static class MySslFilter extends SslFilter {

        MySslFilter(SSLContext sslContext) {
            super(sslContext);
        }

        private SSLEngine ssl;

        @Override
        protected SSLEngine createEngine(IoSession session, InetSocketAddress addr) {
            ssl = super.createEngine(session, addr);
            return ssl;
        }

        SSLEngine getEngine() {
            return ssl;
        }
    }
}
