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

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test an SSL session where the connection cannot be established with the server due to 
 * incompatible protocols (Test for DIRMINA-937)
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslDIRMINA937Test {
    /** A static port used for his test, chosen to avoid collisions */
    private static final int port = AvailablePortFinder.getNextAvailable(5555);

    private static Exception clientError = null;

    /** A JVM independant KEY_MANAGER_FACTORY algorithm */
    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

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
                session.write("data");
            }
        }
    }

    /**
     * Starts a Server with the SSL Filter and a simple text line 
     * protocol codec filter
     */
    private static void startServer() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        acceptor.setReuseAddress(true);
        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();

        // Inject the SSL filter
        SSLContext context = createSSLContext("TLSv1");
        SslFilter sslFilter = new SslFilter(context);
        sslFilter.setEnabledProtocols(new String[] { "TLSv1" });
        //sslFilter.setEnabledCipherSuites(getServerCipherSuites(context.getDefaultSSLParameters().getCipherSuites()));
        filters.addLast("sslFilter", sslFilter);

        // Inject the TestLine codec filter
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        acceptor.setHandler(new TestHandler());
        acceptor.bind(new InetSocketAddress(port));
    }

    /**
     * Starts a client which will connect twice using SSL
     */
    private static void startClient(final CountDownLatch counter) throws Exception {
        NioSocketConnector connector = new NioSocketConnector();
        
        DefaultIoFilterChainBuilder filters = connector.getFilterChain();
        SslFilter sslFilter = new SslFilter(createSSLContext("TLSv1.1"));
        sslFilter.setEnabledProtocols(new String[] { "TLSv1.1" });
        sslFilter.setUseClientMode(true);
        //sslFilter.setEnabledCipherSuites(getClientCipherSuites());
        filters.addLast("sslFilter", sslFilter);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                session.setAttribute(SslFilter.USE_NOTIFICATION, Boolean.TRUE);
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                if (message == SslFilter.SESSION_SECURED) {
                    counter.countDown();
                }
            }


        });
        connector.connect(new InetSocketAddress("localhost", port));
    }

    private static SSLContext createSSLContext(String protocol) throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance(protocol);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        ks.load(SslDIRMINA937Test.class.getResourceAsStream("keystore.sslTest"), passphrase);
        ts.load(SslDIRMINA937Test.class.getResourceAsStream("truststore.sslTest"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    /**
     * Test is ignore as it will cause the build to fail
     */
    @Test
    @Ignore("This test is not yet fully functionnal, it servers as the basis for validating DIRMINA-937")
    public void testDIRMINA937() throws Exception {
        startServer();

        final CountDownLatch counter = new CountDownLatch(1);
        startClient(counter);
        assertTrue(counter.await(10, TimeUnit.SECONDS));
    }
}
