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
package org.apache.mina.transport.nio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.WriteAbortedException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpServer;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test a SSL session where the connection is established and closed twice. It should be
 * processed correctly (Test for DIRMINA-650)
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslTest {
    private static Exception clientError = null;

    private static InetAddress address;

    private static SSLSocketFactory factory;

    /** A JVM independant KEY_MANAGER_FACTORY algorithm */
    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    private static class TestHandler extends AbstractIoHandler {
        private String data = "";
        private boolean second = false;
        public void messageReceived(IoSession session, Object message) {
            String line = Charset.defaultCharset().decode((ByteBuffer) message).toString();
            data += line;
            if (!second && data.startsWith("hello")) {
                second = true;
            } else if (second && data.contains("send")) {
                session.write(Charset.defaultCharset().encode("data\n"));
                data = "";
                second = false;
            }
        }
    }
    
    private static enum Client {
        JDK,
        MINA_BEFORE_HANDSHAKE,
        MINA_AFTER_HANDSHAKE;
    }

    /**
     * Starts a Server with the SSL Filter and a simple text line 
     * protocol codec filter
     */
    private static NioTcpServer startServer(AbstractIoHandler handler) throws Exception {
        NioTcpServer server = new NioTcpServer();

        server.setReuseAddress(true);
        server.getSessionConfig().setSslContext(createSSLContext());
        server.setIoHandler(handler);
        server.bind(new InetSocketAddress(0));
        return server;
    }
    
    private static NioTcpClient startClient(AbstractIoHandler handler, int port) throws Exception {
        NioTcpClient client = new NioTcpClient();
        
        client.getSessionConfig().setSslContext(createSSLContext());
        client.setIoHandler(handler);
        client.connect(new InetSocketAddress("localhost", port));
        return client;
    }

    /**
     * Starts a client which will connect twice using SSL
     */
    private static void startJDKClient(int port) throws Exception {
        address = InetAddress.getByName("localhost");

        SSLContext context = createSSLContext();
        factory = context.getSocketFactory();

        connectAndSend(port);

        // This one will throw a SocketTimeoutException if DIRMINA-650 is not fixed
        connectAndSend(port);
    }

    private static void connectAndSend(int port) throws Exception {
        Socket parent = new Socket(address, port);
        Socket socket = factory.createSocket(parent, address.getCanonicalHostName(), port, false);

        System.out.println("Client sending: hello");
        socket.getOutputStream().write("hello                      \n".getBytes());
        socket.getOutputStream().flush();
        socket.setSoTimeout(10000);

        System.out.println("Client sending: send");
        socket.getOutputStream().write("send                       \n".getBytes());
        socket.getOutputStream().flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = in.readLine();
        System.out.println("Client got: " + line);
        socket.close();

    }

    private static SSLContext createSSLContext() throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        ks.load(SslTest.class.getResourceAsStream("keystore.sslTest"), passphrase);
        ts.load(SslTest.class.getResourceAsStream("truststore.sslTest"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    @Test
    //@Ignore("check for fragmentation")
    public void testSSL() throws Exception {
        final NioTcpServer server  = startServer(new TestHandler());

        Thread t = new Thread() {
            public void run() {
                try {
                    startJDKClient(server.getServerSocketChannel().socket().getLocalPort());
                } catch (Exception e) {
                    clientError = e;
                }
            }
        };
        t.start();
        t.join();
        server.unbind();
        if (clientError != null)
            throw clientError;
    }

    @Test
    public void checkThatSecureEventsArePropagatedServerSide() throws Exception {
        final AtomicInteger startHandshakeCount = new AtomicInteger();
        final AtomicInteger completedHandshakeCount = new AtomicInteger();
        final AtomicInteger secureClosedCount = new AtomicInteger();
        final CountDownLatch closedCount = new CountDownLatch(1);
        final NioTcpServer server = startServer(new AbstractIoHandler() {

            @Override
            public void handshakeStarted(IoSession abstractIoSession) {
                startHandshakeCount.incrementAndGet();
            }

            @Override
            public void handshakeCompleted(IoSession session) {
                completedHandshakeCount.incrementAndGet();
            }

            @Override
            public void secureClosed(IoSession session) {
                secureClosedCount.incrementAndGet();
            }

            @Override
            public void sessionClosed(IoSession session) {
                closedCount.countDown();
            }
        });
        SSLSocketFactory factory = createSSLContext().getSocketFactory();
        SSLSocket s = (SSLSocket) factory.createSocket("localhost", server.getServerSocketChannel().socket().getLocalPort());
        s.startHandshake();
        s.close();
        assertTrue(closedCount.await(10, TimeUnit.SECONDS));
        assertEquals(1, startHandshakeCount.get());
        assertEquals(1, completedHandshakeCount.get());
        assertEquals(1, secureClosedCount.get());
    }
    
    @Test
    public void checkThatSecureEventsArePropagatedServerSideWithSecondHandshake() throws Exception {
        final CountDownLatch closeCount = new CountDownLatch(1);
        final AtomicInteger startHandshakeCount = new AtomicInteger();
        final AtomicInteger completedHandshakeCount = new AtomicInteger();
        final AtomicInteger secureClosedCount = new AtomicInteger();
        final NioTcpServer server = startServer(new AbstractIoHandler() {

            @Override
            public void handshakeStarted(IoSession abstractIoSession) {
                startHandshakeCount.incrementAndGet();
            }

            @Override
            public void handshakeCompleted(IoSession session) {
                completedHandshakeCount.incrementAndGet();
            }

            @Override
            public void secureClosed(IoSession session) {
                secureClosedCount.incrementAndGet();
            }

            @Override
            public void sessionClosed(IoSession session) {
                closeCount.countDown();
            }
        });
        SSLSocketFactory factory = createSSLContext().getSocketFactory();
        SSLSocket s = (SSLSocket) factory.createSocket("localhost", server.getServerSocketChannel().socket().getLocalPort());
        final AtomicInteger handskaheCounter = new AtomicInteger();
        s.addHandshakeCompletedListener(new HandshakeCompletedListener() {
            @Override
            public void handshakeCompleted(HandshakeCompletedEvent event) {
                final int count = handskaheCounter.getAndIncrement();
                if (count == 0) {
                    try {
                        event.getSocket().startHandshake();
                        event.getSocket().setSoTimeout(5000);
                        event.getSocket().getInputStream().read();
                    }
                    catch (IOException e) {}
                } else {
                    try {
                        event.getSocket().close();
                    }
                    catch (IOException e) {}
                }
            }
        });
        s.startHandshake();
        assertTrue(closeCount.await(10, TimeUnit.SECONDS));
        assertEquals(2, startHandshakeCount.get());
        assertEquals(2, completedHandshakeCount.get());
        assertEquals(1, secureClosedCount.get());
        server.unbind();
    }

    @Test
    public void checkThatSecureEventsArePropagatedClientSide() throws Exception {
        final AtomicInteger startHandshakeCount = new AtomicInteger();
        final AtomicInteger completedHandshakeCount = new AtomicInteger();
        final AtomicInteger secureClosedCount = new AtomicInteger();
        final CountDownLatch closeCount = new CountDownLatch(1);
        final NioTcpServer server = startServer(new AbstractIoHandler() {});
        final NioTcpClient client = startClient(new AbstractIoHandler() {
            @Override
            public void handshakeStarted(IoSession abstractIoSession) {
                startHandshakeCount.incrementAndGet();
            }

            @Override
            public void handshakeCompleted(IoSession session) {
                completedHandshakeCount.incrementAndGet();
                session.close(false);
            }

            @Override
            public void secureClosed(IoSession session) {
                secureClosedCount.incrementAndGet();
            }

            @Override
            public void sessionClosed(IoSession session) {
                closeCount.countDown();
            }
        }, server.getServerSocketChannel().socket().getLocalPort());
        assertTrue(closeCount.await(10, TimeUnit.SECONDS));
        assertEquals(1, startHandshakeCount.get());
        assertEquals(1, completedHandshakeCount.get());
        assertEquals(1, secureClosedCount.get());
    }

    private static NioTcpServer createReceivingServer(final int size, final CountDownLatch counter, final OutputStream stream) throws IOException, GeneralSecurityException {
        NioTcpServer server = new NioTcpServer();
        server.setReuseAddress(true);
        server.getSessionConfig().setSslContext(createSSLContext());
        final WritableByteChannel channel = (stream!=null)?Channels.newChannel(stream):null;
        server.setIoHandler(new AbstractIoHandler() {
            private int receivedSize = 0;

            /**
             * {@inheritedDoc}
             */
            @Override
            public void messageReceived(IoSession session, Object message) {
                receivedSize += ((ByteBuffer) message).remaining();
                if (channel != null) {
                    try {
                        channel.write((ByteBuffer) message);
                    } catch (IOException e) {
                        exceptionCaught(session, e);
                    }
                }
                if (receivedSize == size) {
                    counter.countDown();
                    if (channel != null) {
                        try {
                            channel.close();
                        } catch (IOException e) {
                            exceptionCaught(session, e);
                        }
                    }
                }
            }
        });
        server.bind(new InetSocketAddress(0));
        return server;
    }
    
    protected void testMessage(final int size, final Client clientType) throws IOException, GeneralSecurityException, InterruptedException {
        final CountDownLatch counter = new CountDownLatch(1);
        final byte[] message = new byte[size];
        new Random().nextBytes(message);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            /*
                 * Server
                 */
            NioTcpServer server = createReceivingServer(size, counter, bos);
            try {
                int port = server.getServerSocketChannel().socket().getLocalPort();
                /*
                 * Client
                 */
                if (clientType == Client.JDK) {
                    Socket socket = server.getSessionConfig().getSslContext().getSocketFactory()
                            .createSocket("localhost", port);
                    socket.getOutputStream().write(message);
                    socket.getOutputStream().flush();
                    socket.close();
                } else {
                    NioTcpClient client = new NioTcpClient();
                    client.setIoHandler(new AbstractIoHandler() {
                        @Override
                        public void sessionOpened(IoSession session) {
                            if (clientType == Client.MINA_BEFORE_HANDSHAKE) {
                                session.write(ByteBuffer.wrap(message));
                            }
                        }

                        @Override
                        public void handshakeCompleted(IoSession session) {
                            if (clientType == Client.MINA_AFTER_HANDSHAKE) {
                                session.write(ByteBuffer.wrap(message));
                            }
                        }
                    });
                    client.getSessionConfig().setSslContext(createSSLContext());
                    client.connect(new InetSocketAddress(port));
                    
                }
                assertTrue(counter.await(10, TimeUnit.MINUTES));
                assertArrayEquals(message, bos.toByteArray());
            } finally {
                server.unbind();
            }
        } finally {
            bos.close();
        }
    }
    
    @Test
    public void testSingleByteMessageWithJDKClient() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1, Client.JDK);
    }
    
    @Test
    public void testSingleByteMessageWithMINAClientAfterHandkhake() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1, Client.MINA_AFTER_HANDSHAKE);
    }
    
    @Test
    public void testSingleByteMessageWithMINAClientBeforeHandkhake() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1, Client.MINA_BEFORE_HANDSHAKE);
    }

    @Test
    public void test1KMessageWithJDKClient() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1024, Client.JDK);
    }
    
    @Test
    public void test1KMessageWithMINAClientAfterHandskahe() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1024, Client.MINA_AFTER_HANDSHAKE);
    }
    
    @Test
    public void test1KMessageWithMINAClientBeforeHandskahe() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1024, Client.MINA_BEFORE_HANDSHAKE);
    }
    
    @Test
    public void test1MMessageWithJDKClient() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1024 * 1024, Client.JDK);
    }
    
    @Test
    public void test1MMessageWithMINAClientAfterHandshake() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1024 * 1024, Client.MINA_AFTER_HANDSHAKE);
    }
    
    @Test
    public void test1MMessageWithMINAClientBeforeHandshake() throws IOException, GeneralSecurityException, InterruptedException {
        testMessage(1024 * 1024, Client.MINA_BEFORE_HANDSHAKE);
    }
    
    @Test
    public void checkThatASingleMessageSentEventIsSent() throws IOException, GeneralSecurityException, InterruptedException {
        final CountDownLatch counter = new CountDownLatch(1);
        final byte[] message = new byte[1024 * 1024];
        new Random().nextBytes(message);
        final AtomicInteger sentCounter = new AtomicInteger();

        NioTcpServer server = createReceivingServer(1024 * 1024, counter, null);
        NioTcpClient client = new NioTcpClient();
        client.getSessionConfig().setSslContext(createSSLContext());
        client.setIoHandler(new AbstractIoHandler() {

            @Override
            public void handshakeCompleted(IoSession session) {
                session.write(ByteBuffer.wrap(message));
            }

            @Override
            public void messageSent(IoSession session, Object message) {
                sentCounter.incrementAndGet();
            }
        });
        client.connect(new InetSocketAddress(server.getServerSocketChannel().socket().getLocalPort()));
        assertTrue(counter.await(10, TimeUnit.SECONDS));
        assertEquals(5, sentCounter.get());
    }
}
