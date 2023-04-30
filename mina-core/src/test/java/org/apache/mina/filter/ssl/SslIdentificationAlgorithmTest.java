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
import javax.net.ssl.SSLException;
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
        sslFilter.setEnabledProtocols(new String[] {"TLSv1"});

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

    private void startConnector(SSLContext sslContext, final String sni) {
        NioSocketConnector connector = new NioSocketConnector();

        SslFilter sslFilter = new SslFilter(sslContext) {

            @Override
            public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws SSLException {
                if (sni != null) {
                    IoSession session = parent.getSession();
                    session.setAttribute(SslFilter.PEER_ADDRESS, new InetSocketAddress(sni, port));
                }

                super.onPreAdd(parent, name, nextFilter);
            }
        };

        sslFilter.setUseClientMode(true);
        sslFilter.setEndpointIdentificationAlgorithm("HTTPS");
        sslFilter.setEnabledProtocols(new String[] {"TLSv1"});

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
        keyStore.load(SslTest.class.getResourceAsStream(keyStorePath), password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        kmf.init(keyStore, password);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(SslTest.class.getResourceAsStream(trustStorePath), password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        tmf.init(trustStore);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }
}
