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
package org.apache.mina.example.proxy.telnet;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.proxy.ProxyConnector;
import org.apache.mina.proxy.handlers.http.HttpProxyConstants;
import org.apache.mina.proxy.handlers.http.HttpProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * ProxyTelnetTestClient.java - Tests a classical text communication through a proxy.
 * Changing the params and request type will allow to test the multiple options
 * (http or socks proxying, various authentications methods, ...).
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class ProxyTelnetTestClient {
    
    /**
     * The user login used to authenticate with the proxy.
     */
    public final static String USER = "TED_KODS";

    /**
     * The password used to authenticate with the proxy.
     */
    public final static String PWD = "EDOUARD";

    /**
     * The address we really want to connect to.
     */
    public final static InetSocketAddress serverAddress = new InetSocketAddress(
            "localhost", 25);

    /**
     * The address of the proxy server.
     */
    public final static InetSocketAddress proxyAddress = new InetSocketAddress(
            "localhost", 8080);
    
    /**
     * Connects to the endpoint running a text based protocol server through the
     * proxy and allows user to type commands in the console to dialog with the
     * server.
     * 
     * @throws Exception
     */
    public ProxyTelnetTestClient() throws Exception {
        // Create proxy connector.
        NioSocketConnector targetConnector = new NioSocketConnector(Runtime
                .getRuntime().availableProcessors() + 1);
        ProxyConnector connector = new ProxyConnector(targetConnector);

        /*
        // Example of socks v5 proxy use
        SocksProxyRequest req = new SocksProxyRequest(
                SocksProxyConstants.SOCKS_VERSION_5,
                SocksProxyConstants.ESTABLISH_TCPIP_STREAM, serverAddress, USER);
        req.setPassword(PWD);
        */

        HttpProxyRequest req = new HttpProxyRequest(serverAddress);
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(HttpProxyConstants.USER_PROPERTY, USER);
        props.put(HttpProxyConstants.PWD_PROPERTY, PWD);
        req.setProperties(props);        

        ProxyIoSession proxyIoSession = new ProxyIoSession(proxyAddress, req);
        connector.setProxyIoSession(proxyIoSession);

        LineDelimiter delim = new LineDelimiter("\r\n");
        targetConnector.getFilterChain().addLast(
                "codec",
                new ProtocolCodecFilter(new TextLineCodecFactory(Charset
                        .forName("UTF-8"), delim, delim)));

        connector.setHandler(new TelnetSessionHandler());

        IoSession session;
        for (;;) {
            try {
                ConnectFuture future = connector.connect();
                future.awaitUninterruptibly();
                session = future.getSession();
                break;
            } catch (RuntimeIoException e) {
                System.err.println("Failed to connect. Retrying in 5 secs ...");
                Thread.sleep(5000);
            }
        }

        // Wait until done
        if (session != null) {
            session.getCloseFuture().awaitUninterruptibly();
        }
        connector.dispose();
        System.exit(0);
    }

    /**
     * {@inheritDoc}
     */
    public static void main(String[] args) throws Exception {
        new ProxyTelnetTestClient();
    }
}