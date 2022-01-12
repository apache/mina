package org.apache.mina.example.test;


import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

public class MinaServer {
    private static final int PORT = 444;

    public static void main(String[] args) throws Exception {
        System.out.println("server");
        ExecutorFilter executorFilter = new ExecutorFilter(1, 10, 1000, TimeUnit.SECONDS);
        IoAcceptor acceptor = new NioSocketAcceptor();
        DefaultIoFilterChainBuilder filterChain = acceptor.getFilterChain();
        filterChain.addFirst("sslFilter", addSSLFilter());
        ProtocolCodecFilter protocolCodecFilter = new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8")));
        filterChain.addLast("codec", protocolCodecFilter);
        filterChain.addLast("threadPool", executorFilter);
        acceptor.setHandler(new MinaServerHandler());
        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        acceptor.bind(new InetSocketAddress(PORT));
    }

    private static SslFilter addSSLFilter() throws Exception
    {
        final KeyManager[] keyManagers = getKeyManagers();
        TrustManager[] trustManagers = null;
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        final SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setNeedClientAuth(false);
        return sslFilter;
    }

    private static KeyManager[] getKeyManagers() throws Exception
    {
        final char[] keystorePassword = "123456".toCharArray();
        final KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());

        try (final FileInputStream keystoreFile = new FileInputStream("C:\\git\\commserver\\security\\.keystore")) //"q:\\TCP\\server-keystore"
        {
            store.load(keystoreFile, keystorePassword);
        }

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
        kmf.init(store, keystorePassword);
        return kmf.getKeyManagers();
    }
}