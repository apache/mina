package org.apache.mina.example.test;


import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class MinaClient {
    private static final int PORT = 444;

    public static void main(String[] args) throws Exception{
        ExecutorFilter executorFilter = new ExecutorFilter(1, 10, 1000, TimeUnit.SECONDS);
        IoConnector connector = new NioSocketConnector();
        connector.getSessionConfig().setReadBufferSize(2048);
        DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
        filterChain.addFirst("sslFilter", addSSLFilter(false, null, "/tmp/truststore", null, null));
        ProtocolCodecFilter protocolCodecFilter = new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8")));
        filterChain.addLast("codec", protocolCodecFilter);
        filterChain.addLast("threadPool", executorFilter);
        connector.setHandler((IoHandler) new MinaClientHandler("Hello Server.."));
        ConnectFuture future = connector.connect(new InetSocketAddress(
                "127.0.0.1", PORT));
        future.awaitUninterruptibly();

        if (!future.isConnected()) {
            return;
        }

        IoSession session = future.getSession();
        session.getConfig().setUseReadOperation(true);
        session.getCloseFuture().awaitUninterruptibly();
        System.out.println("After Writing");
        connector.dispose();
    }

    public static SslFilter addSSLFilter(boolean clientAuthentication, String keystore, String truststore, String[] protocols, String[] cipherSuites)
            throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            NoSuchProviderException, IOException
    {
        KeyManager[] keyManagers = null;
        if (clientAuthentication)
        {
            char[] keystorePassword = "123456".toCharArray();
            FileInputStream keystorefile = new FileInputStream(keystore);
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(keystorefile, keystorePassword);
            keystorefile.close();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
            kmf.init(store, keystorePassword);
            keyManagers = kmf.getKeyManagers();
        }

        TrustManager[] trustManagers;

        char[] truststorePassword = "123456".toCharArray();
        FileInputStream truststorefile = new FileInputStream(truststore);
        KeyStore                anotherStore   = KeyStore.getInstance(KeyStore.getDefaultType());
        anotherStore.load(truststorefile, truststorePassword);
        truststorefile.close();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
        tmf.init(anotherStore);

        trustManagers = tmf.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        SslFilter sslFilter = new SslFilter(sslContext);

        if (protocols != null)
        {
            sslFilter.setEnabledProtocols(protocols);
        }

        if (cipherSuites != null)
        {
            sslFilter.setEnabledCipherSuites(cipherSuites);
        }
        sslFilter.setUseClientMode(true);
        return sslFilter;
    }
}