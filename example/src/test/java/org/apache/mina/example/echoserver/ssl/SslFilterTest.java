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
package org.apache.mina.example.echoserver.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslFilterTest {

    private int port;
    private SocketAcceptor acceptor;

    @Before
    public void setUp() throws Exception {
        acceptor = new NioSocketAcceptor();
    }

    @After
    public void tearDown() throws Exception {
        acceptor.setCloseOnDeactivation(true);
        acceptor.dispose();
    }

    @Test
    public void testMessageSentIsCalled() throws Exception {
        testMessageSentIsCalled(false);
    }

    @Test
    public void testMessageSentIsCalled_With_SSL() throws Exception {
        testMessageSentIsCalled(true);
    }

    private void testMessageSentIsCalled(boolean useSSL) throws Exception {
        // Workaround to fix TLS issue : http://java.sun.com/javase/javaseforbusiness/docs/TLSReadme.html
        java.lang.System.setProperty( "sun.security.ssl.allowUnsafeRenegotiation", "true" );

        SslFilter sslFilter = null;
        if (useSSL) {
            sslFilter = new SslFilter(BogusSslContextFactory.getInstance(true));
            acceptor.getFilterChain().addLast("sslFilter", sslFilter);
        }
        acceptor.getFilterChain().addLast(
                "codec",
                new ProtocolCodecFilter(new TextLineCodecFactory(Charset
                        .forName("UTF-8"))));

        EchoHandler handler = new EchoHandler();
        acceptor.setHandler(handler);
        acceptor.bind(new InetSocketAddress(0));
        port = acceptor.getLocalAddress().getPort();
        //System.out.println("MINA server started.");

        Socket socket = getClientSocket(useSSL);
        int bytesSent = 0;
        bytesSent += writeMessage(socket, "test-1\n");

        if (useSSL) {
            // Test renegotiation
            SSLSocket ss = (SSLSocket) socket;
            //ss.getSession().invalidate();
            ss.startHandshake();
        }

        bytesSent += writeMessage(socket, "test-2\n");

        int[] response = new int[bytesSent];
        for (int i = 0; i < response.length; i++) {
            response[i] = socket.getInputStream().read();
        }

        if (useSSL) {
            // Read SSL close notify.
            while (socket.getInputStream().read() >= 0) {
                continue;
            }
        }

        socket.close();
        while (acceptor.getManagedSessions().size() != 0) {
            Thread.sleep(100);
        }

        //System.out.println("handler: " + handler.sentMessages);
        assertEquals("handler should have sent 2 messages:", 2,
                handler.sentMessages.size());
        assertTrue(handler.sentMessages.contains("test-1"));
        assertTrue(handler.sentMessages.contains("test-2"));
    }

    private int writeMessage(Socket socket, String message) throws Exception {
        byte request[] = message.getBytes("UTF-8");
        socket.getOutputStream().write(request);
        return request.length;
    }

    private Socket getClientSocket(boolean ssl) throws Exception {
        if (ssl) {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustManagers, null);
            return ctx.getSocketFactory().createSocket("localhost", port);
        }
        return new Socket("localhost", port);
    }

    private static class EchoHandler extends IoHandlerAdapter {

        List<String> sentMessages = new ArrayList<String>();

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            //cause.printStackTrace();
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            session.write(message);
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            sentMessages.add(message.toString());

            if (sentMessages.size() >= 2) {
                session.close(true);
            }
        }
    }

    TrustManager[] trustManagers = new TrustManager[] { new TrustAnyone() };

    private static class TrustAnyone implements X509TrustManager {
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws CertificateException {
        }

        public void checkServerTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws CertificateException {
        }

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }

}
