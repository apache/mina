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
package org.apache.mina.example.chat;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.example.echoserver.ssl.BogusSslContextFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filter.logging.MdcInjectionFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * (<b>Entry point</b>) Chat server
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class Main {
    /** Choose your favorite port number. */
    private static final int PORT = 1234;

    /** Set this to true if you want to make the server SSL */
    private static final boolean USE_SSL = false;

    public static void main(String[] args) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        List<IoFilter> chainIn = acceptor.getFilterChainIn();
        List<IoFilter> chainOut = acceptor.getFilterChainOut();

        MdcInjectionFilter mdcInjectionFilter = new MdcInjectionFilter("mdc");
        chainIn.add(mdcInjectionFilter);

        // Add SSL filter if SSL is enabled.
        if (USE_SSL) {
            addSSLSupport(chainIn, chainOut);
        }

        chainIn.add(new ProtocolCodecFilter("codec",
                new TextLineCodecFactory()));

        addLogger(chainIn, chainOut);

        // Bind
        acceptor.setHandler(new ChatProtocolHandler());
        acceptor.bind(new InetSocketAddress(PORT));

        System.out.println("Listening on port " + PORT);
    }

    private static void addSSLSupport(List<IoFilter> chainIn, List<IoFilter> chainOut)
            throws Exception {
        SslFilter sslFilter = new SslFilter("sslFilter", BogusSslContextFactory
                .getInstance(true));
        chainIn.add(sslFilter);
        chainOut.add(sslFilter);
        System.out.println("SSL ON");
    }

    private static void addLogger(List<IoFilter> chainIn, List<IoFilter> chainOut)
            throws Exception {
    	IoFilter loggerFilter = new LoggingFilter("logger", (String)null);
        chainIn.add(loggerFilter);
        chainOut.add(loggerFilter);
        System.out.println("Logging ON");
    }
}
