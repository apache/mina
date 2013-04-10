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
package org.apache.mina.transport.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.tcp.NioTcpServer;
import org.junit.Test;

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
        public void messageReceived(IoSession session, Object message) {
            String line = Charset.defaultCharset().decode((ByteBuffer) message).toString();

            if (line.startsWith("hello")) {
                System.out.println("Server got: 'hello', waiting for 'send'");
            } else if (line.startsWith("send")) {
                System.out.println("Server got: 'send', sending 'data'");
                session.write(Charset.defaultCharset().encode("data\n"));
            }
        }
    }

    /**
     * Starts a Server with the SSL Filter and a simple text line 
     * protocol codec filter
     */
    private static int startServer() throws Exception {
        NioTcpServer server = new NioTcpServer();

        server.setReuseAddress(true);
        server.getSessionConfig().setSslContext(createSSLContext());
        server.setIoHandler(new TestHandler());
        server.bind(new InetSocketAddress(0));
        return server.getServerSocketChannel().socket().getLocalPort();
    }

    /**
     * Starts a client which will connect twice using SSL
     */
    private static void startClient(int port) throws Exception {
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
        socket.getOutputStream().write("send\n".getBytes());
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
    public void testSSL() throws Exception {
        final int port = startServer();

        Thread t = new Thread() {
            public void run() {
                try {
                    startClient(port);
                } catch (Exception e) {
                    clientError = e;
                }
            }
        };
        t.start();
        t.join();
        if (clientError != null)
            throw clientError;
    }
}
