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
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandlerAdapter;
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
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.Security;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test SNI matching scenarios. (tests for DIRMINA-1122)
 *
 * <pre>
 * emptykeystore.sslTest        - empty keystore
 * server-cn.keystore           - keystore with single certificate chain  (CN=mina)
 * client-cn.truststore         - keystore with trusted certificate
 * server-san-ext.keystore      - keystore with single certificate chain (CN=mina;SAN=*.bbb.ccc,xxx.yyy)
 * client-san-ext.truststore    - keystore with trusted certificate
 * </pre>
 */
public class SslIdentificationAlgorithmTest {

    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        
        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    private int port;
    private CountDownLatch handshakeDone;
    
    private class CustomSslFilter extends SslFilter {
        public CustomSslFilter(SSLContext sslContext) {
            super(sslContext);
        }
        
        protected SSLEngine createEngine(IoSession session, InetSocketAddress addr) {
            //Add your SNI host name and port in the IOSession
            String sniHostNames = (String)session.getAttribute( "SNIHostNames" );
            int portNumber = (int)session.getAttribute( "PortNumber");
            InetSocketAddress peer = new InetSocketAddress( sniHostNames, portNumber);
            
            SSLEngine sslEngine;
            
            if (addr != null) {
                sslEngine = sslContext.createSSLEngine(peer.getHostName(), peer.getPort());
            } else {
                sslEngine = sslContext.createSSLEngine();
            }

            // Always start with WANT, which will be squashed by NEED if NEED is true.
            // Actually, it makes not a lot of sense to select NEED and WANT.
            // NEED >> WANT...
           if (wantClientAuth) {
               sslEngine.setWantClientAuth(true);
           }

           if (needClientAuth) {
               sslEngine.setNeedClientAuth(true);
           }

           if (enabledCipherSuites != null) {
               sslEngine.setEnabledCipherSuites(enabledCipherSuites);
           }

           if (enabledProtocols != null) {
               sslEngine.setEnabledProtocols(enabledProtocols);
           }

           // Set the endpoint identification algorithm
           if (getEndpointIdentificationAlgorithm() != null) {
               SSLParameters sslParameters = sslEngine.getSSLParameters();
               sslParameters.setEndpointIdentificationAlgorithm(getEndpointIdentificationAlgorithm());
               sslEngine.setSSLParameters(sslParameters);
           }

           sslEngine.setUseClientMode(!session.isServer());
           
           return sslEngine;
       }
    }

    @Before
    public void setUp() {
        port = AvailablePortFinder.getNextAvailable(5555);
        handshakeDone = new CountDownLatch(2);
    }

    @Test
    public void shouldAuthenticateWhenServerCertificateCommonNameMatchesClientSNI() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-cn.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-cn.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "mina");

        assertTrue(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldFailAuthenticationWhenServerCertificateCommonNameDoesNotMatchClientSNI() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-cn.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-cn.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "example.com");

        assertFalse(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldFailAuthenticationWhenClientMissingSNIAndIdentificationAlgorithmProvided() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-cn.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-cn.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, null);

        assertFalse(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    /**
     * Subject Alternative Name (SAN) scenarios
     * 
     * @exception Exception If the test throws an exception
     */
    @Test
    public void shouldAuthenticateWhenServerCertificateAlternativeNameMatchesClientSNIExactly() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-san-ext.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-san-ext.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "xxx.yyy");

        assertTrue(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldAuthenticateWhenServerCertificateAlternativeNameMatchesClientSNIViaWildcard() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-san-ext.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-san-ext.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "aaa.bbb.ccc");

        assertTrue(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldFailAuthenticationWhenServerCommonNameMatchesSNIAndSNINotInAlternativeName() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-san-ext.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-san-ext.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "mina");

        assertFalse(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldFailAuthenticationWhenMatchingAlternativeNameWildcardExactly() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-san-ext.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-san-ext.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "*.bbb.ccc");

        assertFalse(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldFailAuthenticationWhenMatchingAlternativeNameWithTooManyLabels() throws Exception {
        SSLContext acceptorContext = createSSLContext("server-san-ext.keystore", "emptykeystore.sslTest");
        SSLContext connectorContext = createSSLContext("emptykeystore.sslTest", "client-san-ext.truststore");

        startAcceptor(acceptorContext);
        startConnector(connectorContext, "mmm.nnn.bbb.ccc");

        assertFalse(handshakeDone.await(10, TimeUnit.SECONDS));
    }

    private void startAcceptor(SSLContext sslContext) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setReuseAddress(true);

        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setEnabledProtocols(new String[] {"TLSv1.2"});

        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();
        filters.addLast("ssl", sslFilter);
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) {
                session.write("acceptor write");
            }

            @Override
            public void event(IoSession session, FilterEvent event) {
                if (event == SslEvent.SECURED) {
                    handshakeDone.countDown();
                }
            }
        });

        acceptor.bind(new InetSocketAddress(port));
    }

    private void startConnector(SSLContext sslContext, String sni) {
        NioSocketConnector connector = new NioSocketConnector();

        SslFilter sslFilter = new CustomSslFilter(sslContext) {
            @Override
            public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
                if (sni != null) {
                    IoSession session = parent.getSession();
                    
                    session.setAttribute("SNIHostNames", sni );
                    session.setAttribute("PortNumber", port);
                }
    
                super.onPreAdd(parent, name, nextFilter);
            }
        };

        sslFilter.setEndpointIdentificationAlgorithm("HTTPS");
        sslFilter.setEnabledProtocols(new String[] {"TLSv1.2"});

        DefaultIoFilterChainBuilder filters = connector.getFilterChain();
        filters.addLast("ssl", sslFilter);
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        connector.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) {
                session.write("connector write");
            }

            @Override
            public void event(IoSession session, FilterEvent event) {
                if (event == SslEvent.SECURED) {
                    handshakeDone.countDown();
                }
            }
        });

        connector.connect(new InetSocketAddress("localhost", port));
    }

    private SSLContext createSSLContext(String keyStorePath, String trustStorePath) throws Exception {
        char[] password = "password".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(SslIdentificationAlgorithmTest.class.getResourceAsStream(keyStorePath), password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        kmf.init(keyStore, password);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(SslIdentificationAlgorithmTest.class.getResourceAsStream(trustStorePath), password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        tmf.init(trustStore);

        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }
}
