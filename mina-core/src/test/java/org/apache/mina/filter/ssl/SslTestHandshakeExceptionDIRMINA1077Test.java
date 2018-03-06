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

import static org.junit.Assert.fail;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.concurrent.CountDownLatch;

/**
 * Test a SSL session and provoke HandshakeException.
 * This test should not hang or timeout when DIRMINA-1076/1077 is fixed.
 * 
 * @author chrjohn
 */
public class SslTestHandshakeExceptionDIRMINA1077Test {

    private static InetAddress address;
    private static NioSocketAcceptor acceptor;
    private volatile int port = 0;

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
        public void messageReceived(IoSession session, Object message) throws Exception {}

        @Override
        public void exceptionCaught( IoSession session, Throwable cause )
            throws Exception {}
    }

    /**
     * Starts a Server with the SSL Filter and a simple text line 
     * protocol codec filter
     */
    private void startServer( int port ) throws Exception {
        acceptor = new NioSocketAcceptor();

        acceptor.setReuseAddress(true);
        DefaultIoFilterChainBuilder filters = acceptor.getFilterChain();

        // Inject the SSL filter
        SslFilter sslFilter = new SslFilter(createSSLContext(true));
        filters.addLast("sslFilter", sslFilter);
        sslFilter.setNeedClientAuth(true);

        // Inject the TestLine codec filter
        filters.addLast("text", new ProtocolCodecFilter(new TextLineCodecFactory()));

        acceptor.setHandler(new TestHandler());
        acceptor.bind(new InetSocketAddress(port));
    }
    
    private static void stopServer() {
        acceptor.unbind();
        acceptor.dispose(true);
    }

    private void startAndStopClient( int port, CountDownLatch disposalLatch ) throws Exception {
        NioSocketConnector nioSocketConnector = new NioSocketConnector();
        nioSocketConnector.setHandler(new TestHandler());
        DefaultIoFilterChainBuilder filters = nioSocketConnector.getFilterChain();

        // Inject the SSL filter
        SslFilter sslFilter = new SslFilter(createSSLContext(false));
        sslFilter.setUseClientMode( true );
        filters.addLast("sslFilter", sslFilter);

        address = InetAddress.getByName("localhost");
        SocketAddress remoteAddress = new InetSocketAddress( address, port );
        ConnectFuture connect = nioSocketConnector.connect( remoteAddress );
        connect.awaitUninterruptibly();
//        System.out.println( "Closing connection..." );
        nioSocketConnector.dispose( true );
        disposalLatch.countDown();
//        System.out.println( "Connection closed!" );
    }

    private static SSLContext createSSLContext(boolean emptyKeystore) throws IOException, GeneralSecurityException {
        char[] passphrase = "password".toCharArray();

        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        // use empty keystore to provoke handshake exception
        if (emptyKeystore) {
            ks.load(SslTestHandshakeExceptionDIRMINA1077Test.class.getResourceAsStream("emptykeystore.sslTest"), passphrase);
        } else {
            ks.load(SslTestHandshakeExceptionDIRMINA1077Test.class.getResourceAsStream("keystore.sslTest"), passphrase);
        }
        ts.load(SslTestHandshakeExceptionDIRMINA1077Test.class.getResourceAsStream("truststore.sslTest"), passphrase);

        kmf.init(ks, passphrase);
        tmf.init(ts);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    @Test(timeout=15000)
    public void testSSL() throws Exception {
        long startTime = System.currentTimeMillis();
        // without DIRMINA-1076/1077 fixed, the test will hang after short time
        while (System.currentTimeMillis() < startTime + 10000) {
            try {
                final CountDownLatch disposalLatch = new CountDownLatch( 1 );
                boolean successfulBind = false;
                while ( !successfulBind ) {
                    port = AvailablePortFinder.getNextAvailable();
                    try {
                        startServer( port );
                        successfulBind = true;
                    } catch ( Exception e ) {
                        System.err.println( "Could not bind to address, retrying..." );
                        stopServer();
                    }
                }
                
                Thread t = new Thread() {
                    public void run() {
                        try {
                            startAndStopClient( port, disposalLatch );
                        } catch ( Exception e ) {}
                    }
                };
                t.setDaemon( true );
                t.start();
                disposalLatch.await();
                t.join( 1000 );

                if ( t.isAlive() ) {
                    for ( StackTraceElement stackTraceElement : t.getStackTrace() ) {
                        if ( "dispose".equals( stackTraceElement.getMethodName() )
                             && AbstractIoService.class.getCanonicalName()
                                                       .equals( stackTraceElement.getClassName() ) ) {
                            System.err.println( "Detected hang in AbstractIoService.dispose()!" );
                        }
                    }
                    fail( "Thread should have died by now, supposed hang in AbstractIoService.dispose()" );
                }
            } finally {
                stopServer();
            }
        }
    }
}
