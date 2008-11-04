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
package org.apache.mina.example.chat.client;

import java.net.SocketAddress;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.example.echoserver.ssl.BogusSslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filter.logging.MdcInjectionFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * A simple chat client for a given user.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ChatClientSupport {
    private final IoHandler handler;

    private final String name;

    private IoSession session;

    public ChatClientSupport(String name, IoHandler handler) {
        if (name == null) {
            throw new IllegalArgumentException("Name can not be null");
        }
        this.name = name;
        this.handler = handler;
    }

    public boolean connect(NioSocketConnector connector, SocketAddress address,
            boolean useSsl) {
        if (session != null && session.isConnected()) {
            throw new IllegalStateException(
                    "Already connected. Disconnect first.");
        }

        try {
            IoFilter LOGGING_FILTER = new LoggingFilter("logger", (String)null);

            IoFilter CODEC_FILTER = new ProtocolCodecFilter("codec",
                    new TextLineCodecFactory());
            
            List<IoFilter> chainIn = connector.getFilterChainIn();
            List<IoFilter> chainOut = connector.getFilterChainOut();
            
            chainIn.add(new MdcInjectionFilter("mdc"));
            chainIn.add(CODEC_FILTER);
            chainIn.add(LOGGING_FILTER);

            chainOut.add(new MdcInjectionFilter("mdc"));
            chainOut.add(CODEC_FILTER);
            chainOut.add(LOGGING_FILTER);

            if (useSsl) {
                SSLContext sslContext = BogusSslContextFactory
                        .getInstance(false);
                SslFilter sslFilter = new SslFilter("sslFilter", sslContext);
                sslFilter.setUseClientMode(true);
                connector.getFilterChainIn().add(sslFilter);
                connector.getFilterChainOut().add(sslFilter);
            }

            // Add the handler to the incoming chain
            connector.setHandler(handler);
            chainIn.add(handler);
            
            ConnectFuture future1 = connector.connect(address);
            future1.awaitUninterruptibly();
            if (!future1.isConnected()) {
                return false;
            }
            session = future1.getSession();
            login();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void login() {
        session.write("LOGIN " + name);
    }

    public void broadcast(String message) {
        session.write("BROADCAST " + message);
    }

    public void quit() {
        if (session != null) {
            if (session.isConnected()) {
                session.write("QUIT");
                // Wait until the chat ends.
                session.getCloseFuture().awaitUninterruptibly();
            }
            session.close();
        }
    }

}
