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
package org.apache.mina.example.reverser;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * (<b>Entry point</b>) Reverser server which reverses all text lines from
 * clients.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        // Prepare the configuration
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast(
                "codec",
                new ProtocolCodecFilter(new TextLineCodecFactory(StandardCharsets.UTF_8)));

        // Bind
        acceptor.setHandler(new ReverseProtocolHandler());
        acceptor.bind(new InetSocketAddress(PORT));

        System.out.println("Listening on port " + PORT);
    }
}
