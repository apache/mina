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
 */package org.apache.mina.filter.ssl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test a SSL session where the connection is established and closed twice. It should be
 * processed correctly (Test for DIRMINA-650)
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslClientCertTest {
    /** A static port used for his test, chosen to avoid collisions */
    private static final int port = AvailablePortFinder.getNextAvailable(5555);

    private static Exception clientError = null;

    private static InetAddress address;

    private static SSLSocketFactory factory;

    private static NioSocketAcceptor acceptor;

    private static final TrustAndStoreTrustManager trustManager = new TrustAndStoreTrustManager();

    /** A JVM independant KEY_MANAGER_FACTORY algorithm */
    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    private static final Logger LOGGER = LoggerFactory.getLogger(SslClientCertTest.class);

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    private static class TestHandler extends IoHandlerAdapter {
        public void messageReceived(IoSession session, Object message) throws Exception {
            String line = (String) message;

            if (line.startsWith("hello")) {
                //System.out.println("Server got: 'hello', waiting for 'send'");
                Thread.sleep(1500);
            } else if (line.startsWith("send")) {
                //System.out.println("Server got: 'send', sending 'data'");
                StringBuilder sb = new StringBuilder();

                for ( int i = 0; i < 10000; i++) {
                    sb.append('A');
                }

                session.write(sb.toString());
                session.closeOnFlush();
            }
        }
    }

    /**
     * Starts a Server with the SSL Filter and a simple text line
     * protocol codec filter
     */
    private static void startServer() throws Exception {
        acceptor = new NioSocketAcceptor();

        acceptor.setReuseAddress(true);
        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();

        // Inject the SSL filter
        SslFilter sslFilter = new SslFilter(createSSLContext());
        filters.addLast("sslFilter", sslFilter);
        sslFilter.setNeedClientAuth(true);
        sslFilter.setWantClientAuth(true);

        // Inject the TestLine codec filter
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        acceptor.setHandler(new TestHandler());
        acceptor.bind(new InetSocketAddress(port));
    }

    private static void stopServer() {
        acceptor.dispose();
    }

    /**
     * Starts a client which will connect twice using SSL
     */
    private static void startClient() throws Exception {
        address = InetAddress.getByName("localhost");

        SSLContext context = createSSLContext();
        factory = context.getSocketFactory();

        connectAndSend();
        connectAndSend();
        connectAndSend();
    }

    private static void connectAndSend() throws Exception {
        Socket parent = new Socket(address, port);
        Socket socket = factory.createSocket(parent, address.getCanonicalHostName(), port, false);

        //System.out.println("Client sending: hello");
        socket.getOutputStream().write("hello                      \n".getBytes());
        socket.getOutputStream().flush();
        socket.setSoTimeout(1000000);

        //System.out.println("Client sending: send");
        socket.getOutputStream().write("send\n".getBytes());
        socket.getOutputStream().flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = in.readLine();
        //System.out.println("Client got: " + line);
        socket.close();

    }

    private static SSLContext createSSLContext() throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance("TLS");


        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        KeyStore ks = KeyStore.getInstance("JKS");

        ks.load(SslClientCertTest.class.getResourceAsStream("keystore.sslTest"), passphrase);

        kmf.init(ks, passphrase);
        ctx.init(kmf.getKeyManagers(), new TrustManager[] {trustManager}, new SecureRandom());

        return ctx;
    }

    @Test
    public void testClientCerts() throws Exception {
        trustManager.clear();
        Assert.assertEquals(0, trustManager.getCertCount());
        try {
            startServer();

            Thread t = new Thread() {
                public void run() {
                    try {
                        startClient();
                    } catch (Exception e) {
                        clientError = e;
                    }
                }
            };
            t.start();
            t.join();

            if (clientError != null) {
                throw clientError;
            }
        } finally {
            stopServer();
        }
        Assert.assertEquals(3, trustManager.getCertCount());
    }

    /**
     * A {@link X509TrustManager} that approves every client certificate and stores its chain into {@link #clientCertsList}
     * so that the test can querry its state via {@link #getCertCount()} and {@link #isSubjectInClientCertChain(String)}
     */
    static class TrustAndStoreTrustManager implements X509TrustManager {

        private static final X509Certificate[] EMPTY_ACCEPTED_ISSUERS = new X509Certificate[0];

        private final List<X509Certificate> clientCertsList = new ArrayList<X509Certificate>();

        public TrustAndStoreTrustManager() {
            super();
        }

        /**
         * Trust all certificates and add them to the {@link #clientCertsList}.
         *
         * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain != null && chain.length > 0) {
                synchronized (clientCertsList) {
                    Collections.addAll(clientCertsList, chain);
                    for (X509Certificate cert : chain) {
                        LOGGER.info("Adding cert " + cert.getSubjectDN());
                    }
                }
            }
        }

        /**
         * Trust all server certificates.
         *
         * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // nothing to do here
        }

        /**
         * Returns an empty array.
         *
         * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
         */
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_ACCEPTED_ISSUERS;
        }

        public boolean isSubjectInClientCertChain(String rfc2253Name) {
            if (rfc2253Name != null) {
                synchronized (clientCertsList) {
                    for (X509Certificate cert : clientCertsList) {
                        if (rfc2253Name.equals(cert.getSubjectX500Principal().getName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Remove all certificates from {@link #clientCertsList}.
         */
        public void clear() {
            synchronized (clientCertsList) {
                clientCertsList.clear();
                LOGGER.info("Clearing certs");
            }
        }

        public int getCertCount() {
            synchronized (clientCertsList) {
                return clientCertsList.size();
            }
        }
    }
}
